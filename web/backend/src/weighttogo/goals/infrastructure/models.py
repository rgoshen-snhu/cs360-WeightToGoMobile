"""SQLAlchemy ORM models for the goals bounded context.

These models map Python objects to the ``goals`` table defined in SRS §8.2.4.
They exist only in the infrastructure layer and are never imported by the domain
or application layers (import-linter enforces this).

The shared ``Base`` from auth infrastructure is reused so that a single
``create_all`` call in tests covers all tables (ADR-0012).
"""

from __future__ import annotations

from datetime import UTC, datetime
from datetime import date as date_type
from decimal import Decimal

from sqlalchemy import (
    BigInteger,
    Boolean,
    Date,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    Numeric,
    String,
    text,
)
from sqlalchemy.orm import Mapped, mapped_column

from weighttogo.auth.infrastructure.models import Base

# SQLite auto-increments only INTEGER PKs (not BIGINT). Use BigInteger on
# PostgreSQL and fall back to Integer on SQLite so in-memory tests work.
_BigInt = BigInteger().with_variant(Integer(), "sqlite")


class GoalModel(Base):
    """ORM model for the ``goals`` table (SRS §8.2.4)."""

    __tablename__ = "goals"
    __table_args__ = (
        # Partial unique index: at most one active goal per user.
        # Both postgresql_where and sqlite_where ensure the WHERE clause is
        # enforced by both dialects (see migration 0003 for the rationale).
        Index(
            "idx_goals_one_active_per_user",
            "user_id",
            unique=True,
            postgresql_where=text("is_active = TRUE"),
            sqlite_where=text("is_active = 1"),
        ),
    )

    goal_id: Mapped[int] = mapped_column(_BigInt, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(
        _BigInt, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False
    )
    target_value: Mapped[Decimal] = mapped_column(Numeric(precision=6, scale=2), nullable=False)
    target_unit: Mapped[str] = mapped_column(String(3), nullable=False)
    start_value: Mapped[Decimal] = mapped_column(Numeric(precision=6, scale=2), nullable=False)
    goal_type: Mapped[str] = mapped_column(String(10), nullable=False)
    target_date: Mapped[date_type | None] = mapped_column(Date(), nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    is_achieved: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    achieved_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=lambda: datetime.now(UTC)
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=lambda: datetime.now(UTC)
    )
