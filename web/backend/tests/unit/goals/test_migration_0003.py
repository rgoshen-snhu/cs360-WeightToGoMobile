"""Verify migration 0003_goals applies cleanly and has correct revision identifiers."""

from __future__ import annotations

import importlib.util
import pathlib
import types


def _load_migration() -> types.ModuleType:
    spec = importlib.util.spec_from_file_location(
        "migration_0003",
        pathlib.Path("alembic/versions/0003_goals.py"),
    )
    assert spec is not None and spec.loader is not None
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def test_migration_0003_file_exists() -> None:
    path = pathlib.Path("alembic/versions/0003_goals.py")
    assert path.is_file(), f"Migration not found at {path}"


def test_migration_0003_revision_identifiers() -> None:
    mod = _load_migration()
    assert mod.revision == "0003"
    assert mod.down_revision == "0002"


def test_migration_0003_has_upgrade_and_downgrade() -> None:
    mod = _load_migration()
    assert callable(mod.upgrade)
    assert callable(mod.downgrade)


def test_migration_0003_creates_goals_table() -> None:
    """upgrade() must produce a goals table with all 12 columns."""
    from alembic.operations import Operations
    from alembic.runtime.migration import MigrationContext
    from sqlalchemy import create_engine, inspect, text
    from sqlalchemy.pool import StaticPool

    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )

    # Prerequisite: create the users table (migration 0001 output)
    with engine.begin() as conn:
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

    mod = _load_migration()

    with engine.begin() as conn:
        migration_ctx = MigrationContext.configure(conn)
        with Operations.context(migration_ctx):
            mod.upgrade()

    insp = inspect(engine)
    assert "goals" in insp.get_table_names()

    col_names = {col["name"] for col in insp.get_columns("goals")}
    expected = {
        "goal_id",
        "user_id",
        "target_value",
        "target_unit",
        "start_value",
        "goal_type",
        "target_date",
        "is_active",
        "is_achieved",
        "achieved_at",
        "created_at",
        "updated_at",
    }
    assert expected <= col_names


def test_migration_0003_downgrade_removes_goals_table() -> None:
    """downgrade() must drop goals cleanly."""
    from alembic.operations import Operations
    from alembic.runtime.migration import MigrationContext
    from sqlalchemy import create_engine, inspect, text
    from sqlalchemy.pool import StaticPool

    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )

    with engine.begin() as conn:
        conn.execute(
            text(
                "CREATE TABLE users ("
                "  user_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                "  email TEXT NOT NULL,"
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

    mod = _load_migration()

    with engine.begin() as conn:
        ctx = MigrationContext.configure(conn)
        with Operations.context(ctx):
            mod.upgrade()

    with engine.begin() as conn:
        ctx = MigrationContext.configure(conn)
        with Operations.context(ctx):
            mod.downgrade()

    assert "goals" not in inspect(engine).get_table_names()
