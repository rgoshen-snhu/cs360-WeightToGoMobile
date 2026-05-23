"""SQLAlchemy ORM models for the auth bounded context.

These models map Python objects to the PostgreSQL tables defined in SRS §8.2.
They exist only in the infrastructure layer and are never imported by the
domain or application layers (enforced by import-linter contracts).

Relationships:
    ``UserModel`` ← ``RefreshTokenModel`` via ``user_id`` foreign key.
"""

from __future__ import annotations

import uuid
from datetime import UTC, datetime

from sqlalchemy import BigInteger, Boolean, DateTime, ForeignKey, Integer, String, Text, Uuid
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship

# SQLite auto-increments only INTEGER PKs (not BIGINT).  Use BigInteger on
# PostgreSQL and fall back to Integer on SQLite so tests remain functional.
_BigInt = BigInteger().with_variant(Integer(), "sqlite")


class Base(DeclarativeBase):
    """Shared declarative base for all ORM models in this application."""


class UserModel(Base):
    """ORM model for the ``users`` table (SRS §8.2.1).

    Column notes:
        email is stored case-insensitively in PostgreSQL via CITEXT, which
        is represented here as ``Text`` — SQLAlchemy does not need to know
        the CITEXT type because the column carries a DB-level constraint.
        The Alembic migration uses ``citext.CIText`` for the exact DDL.
    """

    __tablename__ = "users"

    user_id: Mapped[int] = mapped_column(_BigInt, primary_key=True, autoincrement=True)
    email: Mapped[str] = mapped_column(Text, nullable=False, unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(Text, nullable=False)
    display_name: Mapped[str] = mapped_column(String(50), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    failed_login_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    locked_until: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=lambda: datetime.now(UTC)
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=lambda: datetime.now(UTC)
    )
    last_login_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    # One user → many refresh tokens
    refresh_tokens: Mapped[list[RefreshTokenModel]] = relationship(
        "RefreshTokenModel", back_populates="user", cascade="all, delete-orphan"
    )


class RefreshTokenModel(Base):
    """ORM model for the ``refresh_tokens`` table (SRS §8.2.2).

    ``token_hash`` stores the SHA-256 hex digest of the raw token.  The raw
    value is never persisted.  ``family_id`` is used for family-level revocation
    when token replay is detected (ADR-0013).
    """

    __tablename__ = "refresh_tokens"

    token_id: Mapped[int] = mapped_column(_BigInt, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(
        _BigInt, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False
    )
    token_hash: Mapped[str] = mapped_column(Text, nullable=False, unique=True)
    family_id: Mapped[uuid.UUID] = mapped_column(
        Uuid(as_uuid=True, native_uuid=True), nullable=False
    )
    issued_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=lambda: datetime.now(UTC)
    )
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    replaced_by: Mapped[int | None] = mapped_column(
        _BigInt, ForeignKey("refresh_tokens.token_id"), nullable=True
    )

    user: Mapped[UserModel] = relationship("UserModel", back_populates="refresh_tokens")
