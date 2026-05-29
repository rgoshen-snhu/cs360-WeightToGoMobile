"""NFR-P-3: weight-history reads use composite indexes on PostgreSQL.

Runs only when ``WEIGHTTOGO_TEST_POSTGRES_DSN`` is set. CI provides a Postgres
service; locally run ``docker compose up -d postgres`` and export the DSN. The
partial ``WHERE is_deleted = FALSE`` indexes only materialize on PostgreSQL, so
this is the one test that validates NFR-P-3 against the production engine.

This module tests BOTH sets of indexes:
- Migration 0002: (user_id, observation_date DESC, entry_id DESC) — validated
  by ``test_list_query_uses_index_not_seqscan``
- Migration 0007: (user_id, created_at DESC) — validated by
  ``test_created_at_index_used_for_created_at_ordered_query``
"""

from __future__ import annotations

import contextlib
import os
from collections.abc import Iterator
from datetime import date, timedelta

import pytest
from sqlalchemy import Engine, create_engine, text

pytestmark = pytest.mark.postgres

_DSN = os.environ.get("WEIGHTTOGO_TEST_POSTGRES_DSN")
_NFR_P3_THRESHOLD_ROWS = 150  # must exceed the 100-row SRS §7.2 threshold


@pytest.fixture(scope="module")
def pg_engine() -> Iterator[Engine]:
    # [G6] Must run with CWD = web/backend so Config("alembic.ini") and its
    # relative script_location resolve correctly (CI sets working-directory).
    if not _DSN:
        pytest.skip("WEIGHTTOGO_TEST_POSTGRES_DSN not set; skipping Postgres index test")

    from alembic import command
    from alembic.config import Config

    from weighttogo.config import get_settings

    # [G2] Save/restore DATABASE_URL so running the full suite with a DSN set
    # cannot leak the Postgres URL into subsequent SQLite tests.
    prior_db_url = os.environ.get("DATABASE_URL")
    os.environ["DATABASE_URL"] = _DSN
    get_settings.cache_clear()

    try:
        cfg = Config("alembic.ini")
        command.upgrade(cfg, "head")
        engine = create_engine(_DSN)
        try:
            yield engine
        finally:
            engine.dispose()
            with contextlib.suppress(Exception):
                command.downgrade(cfg, "base")  # [G1] DSN MUST be a throwaway test DB; best-effort
    finally:
        if prior_db_url is None:
            os.environ.pop("DATABASE_URL", None)
        else:
            os.environ["DATABASE_URL"] = prior_db_url
        get_settings.cache_clear()


def _seed(engine: Engine, n: int) -> int:
    """Insert two users and n active entries each; return the target user_id."""
    with engine.begin() as conn:
        target = conn.execute(
            text(
                "INSERT INTO users (email, password_hash, display_name) "
                "VALUES ('perf@example.com', 'x', 'Perf') RETURNING user_id"
            )
        ).scalar_one()
        other = conn.execute(
            text(
                "INSERT INTO users (email, password_hash, display_name) "
                "VALUES ('other@example.com', 'x', 'Other') RETURNING user_id"
            )
        ).scalar_one()
        base = date(2020, 1, 1)
        for uid in (target, other):
            rows = [{"u": uid, "d": base + timedelta(days=i)} for i in range(n)]
            conn.execute(
                text(
                    "INSERT INTO weight_entries "
                    "(user_id, weight_value, weight_unit, observation_date, is_deleted) "
                    "VALUES (:u, 180.0, 'lbs', :d, FALSE)"
                ),
                rows,
            )
        conn.execute(text("ANALYZE weight_entries"))
    return int(target)


def test_list_query_uses_index_not_seqscan(pg_engine: Engine) -> None:
    """The (user_id, observation_date DESC, entry_id DESC) index from migration
    0002 is used for observation_date-ordered list queries.

    This validates the pre-existing 0002 partial index; see
    ``test_created_at_index_used_for_created_at_ordered_query`` for the 0007
    index added by this PR.
    """
    uid = _seed(pg_engine, n=_NFR_P3_THRESHOLD_ROWS)
    sql = (
        "EXPLAIN SELECT * FROM weight_entries "
        "WHERE user_id = :u AND is_deleted = FALSE "
        "ORDER BY observation_date DESC, entry_id DESC LIMIT 20"
    )
    with pg_engine.connect() as conn:
        conn.execute(text("SET enable_seqscan = off"))
        plan = "\n".join(row[0] for row in conn.execute(text(sql), {"u": uid}))
    assert "Index Scan using" in plan, plan
    assert "Seq Scan on weight_entries" not in plan, plan


def test_created_at_index_used_for_created_at_ordered_query(pg_engine: Engine) -> None:
    """The (user_id, created_at) index from migration 0007 is used for created_at reads.

    This directly tests the index added by this PR.  The observation_date EXPLAIN
    test above validates the pre-existing 0002 indexes; this test validates 0007.
    """
    uid = _seed(pg_engine, n=_NFR_P3_THRESHOLD_ROWS)
    sql = (
        "EXPLAIN SELECT * FROM weight_entries "
        "WHERE user_id = :u AND is_deleted = FALSE "
        "ORDER BY created_at DESC LIMIT 20"
    )
    with pg_engine.connect() as conn:
        conn.execute(text("SET enable_seqscan = off"))
        plan = "\n".join(row[0] for row in conn.execute(text(sql), {"u": uid}))
    assert "Index Scan using idx_weight_entries_user_created_at" in plan, plan


def test_created_at_composite_index_present(pg_engine: Engine) -> None:
    with pg_engine.connect() as conn:
        names = {
            r[0]
            for r in conn.execute(
                text("SELECT indexname FROM pg_indexes WHERE tablename = 'weight_entries'")
            )
        }
    assert "idx_weight_entries_user_created_at" in names, names
