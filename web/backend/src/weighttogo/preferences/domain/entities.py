"""Domain entities for the preferences bounded context.

No framework or persistence dependencies — pure Python.
"""

from __future__ import annotations

from dataclasses import dataclass
from enum import StrEnum


class PreferenceKey(StrEnum):
    """Known preference keys stored in the user_preferences EAV table."""

    WEIGHT_UNIT = "weight_unit"
    NOTIFY_ACHIEVEMENT = "notify_achievement"
    NOTIFY_MILESTONE = "notify_milestone"
    NOTIFY_STREAK = "notify_streak"


@dataclass
class Preference:
    """A single preference row for a user.

    Attributes:
        user_id: FK to the owning user.
        key: The preference key (one of PreferenceKey).
        value: Canonical stored string ('lbs', 'kg', 'true', 'false').
    """

    user_id: int
    key: PreferenceKey
    value: str


DEFAULT_PREFERENCES: dict[PreferenceKey, str] = {
    PreferenceKey.WEIGHT_UNIT: "lbs",
    PreferenceKey.NOTIFY_ACHIEVEMENT: "true",
    PreferenceKey.NOTIFY_MILESTONE: "true",
    PreferenceKey.NOTIFY_STREAK: "true",
}
