"""GetPreferences use case — returns the fully-resolved preference set for a user."""

from __future__ import annotations

from dataclasses import dataclass

from weighttogo.preferences.domain.entities import DEFAULT_PREFERENCES, PreferenceKey
from weighttogo.preferences.domain.ports import IPreferenceRepository


@dataclass(frozen=True)
class GetPreferencesCommand:
    """Input for GetPreferences.execute."""

    user_id: int


class GetPreferences:
    """Return all four preferences for a user, merging lazy defaults for missing rows."""

    def __init__(self, repository: IPreferenceRepository) -> None:
        """Initialise with an IPreferenceRepository adapter."""
        self._repository = repository

    def execute(self, command: GetPreferencesCommand) -> dict[PreferenceKey, str]:
        """Fetch stored rows and merge over built-in defaults.

        Args:
            command: Contains the requesting user's ID.

        Returns:
            A fully-resolved dict with all four PreferenceKey entries.
        """
        stored = self._repository.get_all_for_user(command.user_id)
        return {**DEFAULT_PREFERENCES, **stored}
