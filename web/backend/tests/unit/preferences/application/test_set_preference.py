"""Unit tests for SetPreference use case."""

from __future__ import annotations

import contextlib

from weighttogo.preferences.application.set_preference import SetPreference, SetPreferenceCommand
from weighttogo.preferences.domain.entities import DEFAULT_PREFERENCES, Preference, PreferenceKey
from weighttogo.preferences.domain.exceptions import InvalidPreferenceValueError


class FakePreferenceRepository:
    """Test double that tracks upsert calls."""

    def __init__(self, rows: dict[PreferenceKey, str] | None = None) -> None:
        self._rows: dict[PreferenceKey, str] = dict(rows or {})
        self.upsert_called_with: list[tuple[int, PreferenceKey, str]] = []

    def get_all_for_user(self, user_id: int) -> dict[PreferenceKey, str]:
        return dict(self._rows)

    def upsert(self, user_id: int, key: PreferenceKey, value: str) -> Preference:
        self.upsert_called_with.append((user_id, key, value))
        self._rows[key] = value
        return Preference(user_id=user_id, key=key, value=value)


class TestSetPreferenceValidValue:
    def test_valid_weight_unit_upserts(self) -> None:
        repo = FakePreferenceRepository()
        SetPreference(repo).execute(
            SetPreferenceCommand(user_id=1, key=PreferenceKey.WEIGHT_UNIT, value="kg")
        )
        assert repo.upsert_called_with == [(1, PreferenceKey.WEIGHT_UNIT, "kg")]

    def test_valid_notify_toggle_upserts(self) -> None:
        repo = FakePreferenceRepository()
        SetPreference(repo).execute(
            SetPreferenceCommand(user_id=1, key=PreferenceKey.NOTIFY_MILESTONE, value=False)
        )
        assert repo.upsert_called_with == [(1, PreferenceKey.NOTIFY_MILESTONE, "false")]

    def test_returns_full_resolved_set_after_upsert(self) -> None:
        repo = FakePreferenceRepository()
        result = SetPreference(repo).execute(
            SetPreferenceCommand(user_id=1, key=PreferenceKey.WEIGHT_UNIT, value="kg")
        )
        assert set(result.keys()) == set(PreferenceKey)
        assert result[PreferenceKey.WEIGHT_UNIT] == "kg"

    def test_returns_merged_defaults_for_unset_keys(self) -> None:
        repo = FakePreferenceRepository()
        result = SetPreference(repo).execute(
            SetPreferenceCommand(user_id=1, key=PreferenceKey.WEIGHT_UNIT, value="kg")
        )
        expected = DEFAULT_PREFERENCES[PreferenceKey.NOTIFY_ACHIEVEMENT]
        assert result[PreferenceKey.NOTIFY_ACHIEVEMENT] == expected


class TestSetPreferenceInvalidValue:
    def test_invalid_weight_unit_raises_before_repo_call(self) -> None:
        repo = FakePreferenceRepository()
        with contextlib.suppress(InvalidPreferenceValueError):
            SetPreference(repo).execute(
                SetPreferenceCommand(user_id=1, key=PreferenceKey.WEIGHT_UNIT, value="oz")
            )
        assert repo.upsert_called_with == []

    def test_invalid_notify_value_raises_before_repo_call(self) -> None:
        repo = FakePreferenceRepository()
        with contextlib.suppress(InvalidPreferenceValueError):
            SetPreference(repo).execute(
                SetPreferenceCommand(user_id=1, key=PreferenceKey.NOTIFY_ACHIEVEMENT, value="yes")
            )
        assert repo.upsert_called_with == []
