"""Create achievements table with idempotency constraints and indexes.

Revision ID: 0005
Revises: 0004
Create Date: 2026-05-29

Implements the achievements table for Issue #54 (FR-Ach-1, FR-Ach-2, FR-G-4).

Idempotency is enforced at the DB level by two partial unique indexes:

  idx_achievements_unique_milestone:
      UNIQUE (goal_id, achievement_type, threshold) WHERE threshold IS NOT NULL
      — one row per (goal, type, threshold) tuple for milestones

  idx_achievements_unique_goal_reached:
      UNIQUE (goal_id, achievement_type) WHERE threshold IS NULL
      — one goal_reached row per goal

Two indexes are required because PostgreSQL and SQLite both treat NULL != NULL
in unique indexes.  A single UNIQUE (goal_id, achievement_type, threshold) would
allow duplicate goal_reached rows (both have threshold=NULL, so the constraint
never fires).  The partial index WHERE threshold IS NULL closes that gap.

Both postgresql_where AND sqlite_where are supplied on each partial index so
the WHERE clause is enforced under the SQLite integration-test harness as well
as PostgreSQL (same pattern as idx_goals_one_active_per_user in migration 0003).
"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision: str = "0005"
down_revision: str = "0004"
branch_labels: str | None = None
depends_on: str | None = None


def upgrade() -> None:
    """Create achievements table with all constraints and indexes."""
    op.create_table(
        "achievements",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column(
            "user_id",
            sa.BigInteger(),
            sa.ForeignKey("users.user_id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column(
            "goal_id",
            sa.BigInteger(),
            sa.ForeignKey("goals.goal_id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("achievement_type", sa.String(20), nullable=False),
        sa.Column("threshold", sa.Numeric(precision=6, scale=2), nullable=True),
        sa.Column(
            "earned_at",
            sa.DateTime(timezone=True),
            nullable=False,
        ),
        sa.CheckConstraint(
            "achievement_type IN ('goal_reached', 'milestone')",
            name="achievements_type_valid",
        ),
    )

    # One milestone row per (goal, type, threshold) — threshold is non-null
    op.create_index(
        "idx_achievements_unique_milestone",
        "achievements",
        ["goal_id", "achievement_type", "threshold"],
        unique=True,
        postgresql_where=sa.text("threshold IS NOT NULL"),
        sqlite_where=sa.text("threshold IS NOT NULL"),
    )

    # One goal_reached row per goal — threshold is null
    op.create_index(
        "idx_achievements_unique_goal_reached",
        "achievements",
        ["goal_id", "achievement_type"],
        unique=True,
        postgresql_where=sa.text("threshold IS NULL"),
        sqlite_where=sa.text("threshold IS NULL"),
    )

    # Fast user-scoped listing ordered newest-first
    op.create_index(
        "idx_achievements_user_earned",
        "achievements",
        ["user_id", "earned_at"],
    )


def downgrade() -> None:
    """Drop achievements table and its indexes."""
    op.drop_index("idx_achievements_user_earned", table_name="achievements")
    op.drop_index("idx_achievements_unique_goal_reached", table_name="achievements")
    op.drop_index("idx_achievements_unique_milestone", table_name="achievements")
    op.drop_table("achievements")
