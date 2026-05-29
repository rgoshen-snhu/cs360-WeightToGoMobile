"""Verify migration 0006_user_preferences applies cleanly and has correct identifiers.

EAV table: (user_id, pref_key, pref_value) with UNIQUE(user_id, pref_key),
a pref_key domain CHECK, and a conditional pref_value CHECK.
"""

from __future__ import annotations

import importlib.util
import pathlib
import types


def _load_migration() -> types.ModuleType:
    spec = importlib.util.spec_from_file_location(
        "migration_0006",
        pathlib.Path("alembic/versions/0006_user_preferences.py"),
    )
    assert spec is not None and spec.loader is not None
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def _create_users_table(conn) -> None:  # type: ignore[no-untyped-def]
    from sqlalchemy import text

    conn.execute(
        text(
            "CREATE TABLE users ("
            "  user_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "  email TEXT NOT NULL UNIQUE,"
            "  password_hash TEXT NOT NULL,"
            "  display_name TEXT NOT NULL,"
            "  is_active INTEGER NOT NULL DEFAULT 1,"
            "  failed_login_count INTEGER NOT NULL DEFAULT 0,"
            "  locked_until TEXT,"
            "  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            "  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            "  last_login_at TEXT"
            ")"
        )
    )


def test_migration_0006_file_exists() -> None:
    path = pathlib.Path("alembic/versions/0006_user_preferences.py")
    assert path.is_file(), f"Migration not found at {path}"


def test_migration_0006_revision_identifiers() -> None:
    mod = _load_migration()
    assert mod.revision == "0006"
    assert mod.down_revision == "0005"


def test_migration_0006_has_upgrade_and_downgrade() -> None:
    mod = _load_migration()
    assert callable(mod.upgrade)
    assert callable(mod.downgrade)


def test_migration_0006_upgrade_creates_user_preferences_table() -> None:
    """upgrade() must create user_preferences with all expected columns."""
    from alembic.operations import Operations
    from alembic.runtime.migration import MigrationContext
    from sqlalchemy import create_engine, inspect
    from sqlalchemy.pool import StaticPool

    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )

    with engine.begin() as conn:
        _create_users_table(conn)

    mod = _load_migration()

    with engine.begin() as conn:
        ctx = MigrationContext.configure(conn)
        with Operations.context(ctx):
            mod.upgrade()

    insp = inspect(engine)
    assert "user_preferences" in insp.get_table_names()

    col_names = {col["name"] for col in insp.get_columns("user_preferences")}
    expected = {"id", "user_id", "pref_key", "pref_value", "updated_at"}
    assert expected <= col_names


def test_migration_0006_downgrade_drops_user_preferences_table() -> None:
    """downgrade() must drop user_preferences cleanly."""
    from alembic.operations import Operations
    from alembic.runtime.migration import MigrationContext
    from sqlalchemy import create_engine, inspect
    from sqlalchemy.pool import StaticPool

    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )

    with engine.begin() as conn:
        _create_users_table(conn)

    mod = _load_migration()

    with engine.begin() as conn:
        ctx = MigrationContext.configure(conn)
        with Operations.context(ctx):
            mod.upgrade()

    with engine.begin() as conn:
        ctx = MigrationContext.configure(conn)
        with Operations.context(ctx):
            mod.downgrade()

    assert "user_preferences" not in inspect(engine).get_table_names()
