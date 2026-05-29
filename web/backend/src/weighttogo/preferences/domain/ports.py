"""Repository port interfaces for the preferences bounded context.

Ports are defined in the domain layer. Infrastructure adapters implement them.
Use cases depend only on these abstractions.
"""

from __future__ import annotations

from typing import Protocol

from weighttogo.preferences.domain.entities import Preference, PreferenceKey


class IPreferenceRepository(Protocol):
    """Read/write port for the user_preferences EAV table."""

    def get_all_for_user(self, user_id: int) -> dict[PreferenceKey, str]:
        """Return all persisted preference rows for a user as a key→value dict.

        Only rows that exist in the DB are returned; defaults are NOT merged here.
        Missing keys are handled by the application layer (lazy defaults).

        Args:
            user_id: The owning user's ID.

        Returns:
            A dict mapping PreferenceKey to its stored canonical string value.
            Empty dict when the user has no stored preferences.
        """
        ...

    def upsert(self, user_id: int, key: PreferenceKey, value: str) -> Preference:
        """Atomically insert or update a single preference row.

        Uses ON CONFLICT DO UPDATE so the call is idempotent and race-free
        (ADR-0020). updated_at is re-stamped on every call.

        Args:
            user_id: The owning user's ID.
            key: The preference key to set.
            value: The canonical stored string value.

        Returns:
            A Preference entity reflecting the written state.
        """
        ...
