"""SQLAlchemy repository adapter for the preferences bounded context.

Implements IPreferenceRepository using an SQLAlchemy ORM session.
Only this component in the preferences slice may import SQLAlchemy
(enforced by the import-linter contract).

ADR-0020 upsert: atomic INSERT … ON CONFLICT DO UPDATE (PostgreSQL dialect)
so the "set this preference" operation is race-free and idempotent.
"""

from __future__ import annotations

from datetime import UTC, datetime

from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from weighttogo.preferences.domain.entities import Preference, PreferenceKey
from weighttogo.preferences.infrastructure.models import UserPreferenceModel


class SqlAlchemyPreferenceRepository:
    """SQLAlchemy implementation of IPreferenceRepository.

    Args:
        session: An active SQLAlchemy Session (scoped to the request).
    """

    def __init__(self, session: Session) -> None:
        """Initialise with an active SQLAlchemy session."""
        self._session = session

    def get_all_for_user(self, user_id: int) -> dict[PreferenceKey, str]:
        """Return stored preference rows for *user_id* as a key→value dict.

        Args:
            user_id: The owning user's ID.

        Returns:
            A dict of stored PreferenceKey→canonical-string entries.
            Empty when the user has no stored preferences.
        """
        rows = self._session.query(UserPreferenceModel).filter_by(user_id=user_id).all()
        return {PreferenceKey(r.pref_key): r.pref_value for r in rows}

    def upsert(self, user_id: int, key: PreferenceKey, value: str) -> Preference:
        """Atomically insert or update one preference row (ON CONFLICT DO UPDATE).

        The (user_id, pref_key) UNIQUE constraint is the conflict target.
        updated_at is re-stamped on every call [G8].

        Args:
            user_id: The owning user's ID.
            key: The preference key to set.
            value: The canonical stored string value.

        Returns:
            A Preference entity reflecting the written state.
        """
        now = datetime.now(UTC)
        stmt = (
            pg_insert(UserPreferenceModel)
            .values(user_id=user_id, pref_key=key.value, pref_value=value, updated_at=now)
            .on_conflict_do_update(
                index_elements=["user_id", "pref_key"],
                set_={"pref_value": value, "updated_at": now},
            )
        )
        self._session.execute(stmt)
        return Preference(user_id=user_id, key=key, value=value)
