"""SQLAlchemy ORM model for the achievements bounded context.

This model maps Python objects to the ``achievements`` table defined in the
design spec for Issue #54.  It exists only in the infrastructure layer and is
never imported by the domain or application layers (import-linter enforces this).

The shared ``Base`` from auth infrastructure is reused so that a single
``create_all`` call in tests covers all tables (ADR-0012).
"""

from __future__ import annotations

from datetime import UTC, datetime
from decimal import Decimal

from sqlalchemy import BigInteger, DateTime, ForeignKey, Integer, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from weighttogo.auth.infrastructure.models import Base

# SQLite auto-increments only INTEGER PKs (not BIGINT). Use BigInteger on
# PostgreSQL and fall back to Integer on SQLite so in-memory tests work.
_BigInt = BigInteger().with_variant(Integer(), "sqlite")


class AchievementModel(Base):
    """ORM model for the ``achievements`` table (Issue #54 design spec)."""

    __tablename__ = "achievements"

    id: Mapped[int] = mapped_column(_BigInt, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(
        _BigInt, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False
    )
    goal_id: Mapped[int] = mapped_column(
        _BigInt, ForeignKey("goals.goal_id", ondelete="CASCADE"), nullable=False
    )
    achievement_type: Mapped[str] = mapped_column(String(20), nullable=False)
    threshold: Mapped[Decimal | None] = mapped_column(Numeric(precision=6, scale=2), nullable=True)
    earned_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=lambda: datetime.now(UTC)
    )
