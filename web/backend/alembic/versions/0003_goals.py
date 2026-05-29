"""Create goals table with constraints and indexes.

Revision ID: 0003
Revises: 0002
Create Date: 2026-05-28

Implements SRS §8.2.4 and §8.3 migration 0003_goals.

Five CHECK constraints enforce value-domain rules at the database level:
  - goal_type limited to ('lose', 'gain')
  - target_value and start_value positive and within the 1–1500 range
  - target_unit limited to ('lbs', 'kg')
  - achieved-consistency: achieved_at is NULL iff is_achieved is FALSE

One partial unique index enforces the one-active-goal-per-user business rule
at the DB level as a race-condition backstop:

    CREATE UNIQUE INDEX idx_goals_one_active_per_user
        ON goals(user_id) WHERE is_active = TRUE;

IMPORTANT: Both ``postgresql_where`` AND ``sqlite_where`` are supplied so that
the WHERE clause is applied on SQLite (the integration-test harness) as well as
PostgreSQL.  Omitting ``sqlite_where`` would degrade the index to a full
UNIQUE ON goals(user_id), permanently capping each user to one goal row ever
and breaking abandon-then-recreate.  SQLite uses integer 1 for TRUE.
"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision: str = "0003"
down_revision: str = "0002"
branch_labels: str | None = None
depends_on: str | None = None


def upgrade() -> None:
    """Create goals table with all SRS-specified constraints and indexes."""
    op.create_table(
        "goals",
        sa.Column("goal_id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column(
            "user_id",
            sa.BigInteger(),
            sa.ForeignKey("users.user_id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("target_value", sa.Numeric(precision=6, scale=2), nullable=False),
        sa.Column("target_unit", sa.String(3), nullable=False),
        sa.Column("start_value", sa.Numeric(precision=6, scale=2), nullable=False),
        sa.Column("goal_type", sa.String(10), nullable=False),
        sa.Column("target_date", sa.Date(), nullable=True),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default="true"),
        sa.Column("is_achieved", sa.Boolean(), nullable=False, server_default="false"),
        sa.Column("achieved_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
        # CHECK constraints (milestone-three-plan.md Step 1 + SRS §8.2.4)
        sa.CheckConstraint("goal_type IN ('lose', 'gain')", name="goals_goal_type_valid"),
        sa.CheckConstraint("target_value > 0", name="goals_target_value_positive"),
        sa.CheckConstraint("target_value <= 1500", name="goals_target_value_max"),
        sa.CheckConstraint("start_value > 0", name="goals_start_value_positive"),
        sa.CheckConstraint("start_value <= 1500", name="goals_start_value_max"),
        sa.CheckConstraint("target_unit IN ('lbs', 'kg')", name="goals_target_unit_valid"),
        sa.CheckConstraint(
            "(is_achieved = FALSE AND achieved_at IS NULL)"
            " OR (is_achieved = TRUE AND achieved_at IS NOT NULL)",
            name="goals_achieved_consistency",
        ),
    )

    # Partial unique index: at most one active goal per user.
    # Both postgresql_where and sqlite_where must be supplied so the WHERE
    # clause is enforced under the SQLite integration-test harness.
    op.create_index(
        "idx_goals_one_active_per_user",
        "goals",
        ["user_id"],
        unique=True,
        postgresql_where=sa.text("is_active = TRUE"),
        sqlite_where=sa.text("is_active = 1"),
    )


def downgrade() -> None:
    """Drop goals table and its indexes."""
    op.drop_index("idx_goals_one_active_per_user", table_name="goals")
    op.drop_table("goals")
