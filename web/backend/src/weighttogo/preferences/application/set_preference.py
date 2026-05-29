"""SetPreference use case — validate, upsert, and return the full resolved set."""

from __future__ import annotations

from dataclasses import dataclass

from weighttogo.preferences.domain.entities import DEFAULT_PREFERENCES, PreferenceKey
from weighttogo.preferences.domain.ports import IPreferenceRepository
from weighttogo.preferences.domain.validation import validate_preference_value


@dataclass(frozen=True)
class SetPreferenceCommand:
    """Input for SetPreference.execute."""

    user_id: int
    key: PreferenceKey
    value: bool | str


class SetPreference:
    """Validate, upsert one preference, then return the fully-resolved set."""

    def __init__(self, repository: IPreferenceRepository) -> None:
        """Initialise with an IPreferenceRepository adapter."""
        self._repository = repository

    def execute(self, command: SetPreferenceCommand) -> dict[PreferenceKey, str]:
        """Validate the value, upsert it, and return all four resolved preferences.

        Steps run in a single caller-managed session (wired by the router's
        get_db_session dependency) so the upsert and the read-back share one
        transaction [G11].

        Args:
            command: user_id, key, and raw value from the API request.

        Returns:
            Fully-resolved dict of all four preferences after the write.

        Raises:
            InvalidPreferenceValueError: When value is not valid for key.
        """
        canonical = validate_preference_value(command.key, command.value)
        self._repository.upsert(command.user_id, command.key, canonical)
        stored = self._repository.get_all_for_user(command.user_id)
        return {**DEFAULT_PREFERENCES, **stored}
