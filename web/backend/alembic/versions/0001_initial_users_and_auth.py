"""Create users and refresh_tokens tables with CITEXT extension.

Revision ID: 0001
Revises:
Create Date: 2026-05-22

This migration creates the initial authentication schema per SRS §8.2.1 and
§8.2.2. The ``citext`` PostgreSQL extension enables case-insensitive email
comparison without application-layer normalisation.

Both upgrade and downgrade paths are defined so the migration can be
rolled back cleanly during development.
"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

# Revision identifiers used by Alembic.
revision: str = "0001"
down_revision: str | None = None
branch_labels: str | None = None
depends_on: str | None = None


def upgrade() -> None:
    """Create the CITEXT extension, users table, and refresh_tokens table."""
    # Install CITEXT for case-insensitive email comparison (SRS §8.2.1)
    op.execute("CREATE EXTENSION IF NOT EXISTS citext")

    # ── users ─────────────────────────────────────────────────────────────────
    op.create_table(
        "users",
        sa.Column("user_id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column(
            "email",
            sa.Text(),
            nullable=False,
        ),
        sa.Column("password_hash", sa.Text(), nullable=False),
        sa.Column("display_name", sa.String(50), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default="true"),
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
        sa.Column("last_login_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("failed_login_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("locked_until", sa.DateTime(timezone=True), nullable=True),
        # CHECK constraints (SRS §8.2.1)
        sa.CheckConstraint(
            "email ~* '^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$'",
            name="users_email_format",
        ),
        sa.CheckConstraint(
            "length(trim(display_name)) BETWEEN 2 AND 50",
            name="users_display_name_length",
        ),
        sa.CheckConstraint(
            "failed_login_count >= 0",
            name="users_failed_login_nonneg",
        ),
    )
    # Use ALTER TABLE to set CITEXT type (requires the extension to be installed first)
    op.execute("ALTER TABLE users ALTER COLUMN email TYPE citext")
    op.create_unique_constraint("uq_users_email", "users", ["email"])
    op.create_index(
        "idx_users_email_active",
        "users",
        ["email"],
        postgresql_where=sa.text("is_active = TRUE"),
    )

    # ── refresh_tokens ────────────────────────────────────────────────────────
    op.create_table(
        "refresh_tokens",
        sa.Column("token_id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column(
            "user_id",
            sa.BigInteger(),
            sa.ForeignKey("users.user_id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("token_hash", sa.Text(), nullable=False),
        sa.Column("family_id", sa.UUID(as_uuid=True), nullable=False),
        sa.Column(
            "issued_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column(
            "replaced_by",
            sa.BigInteger(),
            sa.ForeignKey("refresh_tokens.token_id"),
            nullable=True,
        ),
        # CHECK constraint
        sa.CheckConstraint(
            "expires_at > issued_at",
            name="refresh_tokens_expiry_after_issuance",
        ),
    )
    op.create_unique_constraint("uq_refresh_tokens_hash", "refresh_tokens", ["token_hash"])
    op.create_index(
        "idx_refresh_tokens_user_active",
        "refresh_tokens",
        ["user_id"],
        postgresql_where=sa.text("revoked_at IS NULL"),
    )
    op.create_index(
        "idx_refresh_tokens_family",
        "refresh_tokens",
        ["family_id"],
    )


def downgrade() -> None:
    """Drop refresh_tokens, users tables, and the CITEXT extension."""
    op.drop_table("refresh_tokens")
    op.drop_table("users")
    op.execute("DROP EXTENSION IF EXISTS citext")
