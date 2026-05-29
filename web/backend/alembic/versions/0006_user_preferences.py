"""Create user_preferences table (EAV key-value, Issue #55 FR-P-1 + FR-P-3).

Revision ID: 0006
Revises: 0005
Create Date: 2026-05-29

EAV key-value design selected in ADR-0020. Four fixed preference keys:

  weight_unit          — 'lbs' | 'kg'
  notify_achievement   — 'true' | 'false'
  notify_milestone     — 'true' | 'false'
  notify_streak        — 'true' | 'false'

Two CHECK constraints enforce value-domain rules at the database level
as defense-in-depth (matching the goals/achievements precedent):

  user_preferences_key_valid:
      pref_key must be one of the four known keys.

  user_preferences_value_valid:
      For weight_unit, pref_value must be 'lbs' or 'kg'.
      For notify_* keys, pref_value must be 'true' or 'false'.

A UNIQUE(user_id, pref_key) constraint enforces one row per preference per
user and serves as the conflict target for the atomic upsert in the
SqlAlchemyPreferenceRepository (ON CONFLICT DO UPDATE, ADR-0020 §Upsert).

updated_at is re-stamped on every upsert so staleness can be observed.
"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision: str = "0006"
down_revision: str = "0005"
branch_labels: str | None = None
depends_on: str | None = None


def upgrade() -> None:
    """Create user_preferences EAV table with constraints and index."""
    op.create_table(
        "user_preferences",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column(
            "user_id",
            sa.BigInteger(),
            sa.ForeignKey("users.user_id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("pref_key", sa.String(40), nullable=False),
        sa.Column("pref_value", sa.String(40), nullable=False),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
        sa.UniqueConstraint("user_id", "pref_key", name="user_preferences_unique_key"),
        sa.CheckConstraint(
            "pref_key IN ('weight_unit','notify_achievement','notify_milestone','notify_streak')",
            name="user_preferences_key_valid",
        ),
        sa.CheckConstraint(
            "(pref_key = 'weight_unit' AND pref_value IN ('lbs','kg'))"
            " OR (pref_key LIKE 'notify_%' AND pref_value IN ('true','false'))",
            name="user_preferences_value_valid",
        ),
    )

    op.create_index(
        "idx_user_preferences_user",
        "user_preferences",
        ["user_id"],
    )


def downgrade() -> None:
    """Drop user_preferences table and its index."""
    op.drop_index("idx_user_preferences_user", table_name="user_preferences")
    op.drop_table("user_preferences")
