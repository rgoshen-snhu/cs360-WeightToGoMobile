"""Unit tests for GetPreferences use case."""

from __future__ import annotations

from weighttogo.preferences.application.get_preferences import GetPreferences, GetPreferencesCommand
from weighttogo.preferences.domain.entities import DEFAULT_PREFERENCES, PreferenceKey


class FakePreferenceRepository:
    """Test double — returns only the rows it was seeded with."""

    def __init__(self, rows: dict[PreferenceKey, str] | None = None) -> None:
        self._rows: dict[PreferenceKey, str] = rows or {}

    def get_all_for_user(self, user_id: int) -> dict[PreferenceKey, str]:
        return dict(self._rows)

    def upsert(self, user_id: int, key: PreferenceKey, value: str):  # type: ignore[no-untyped-def]
        raise NotImplementedError


class TestGetPreferencesReturnsDefaults:
    def test_empty_repo_returns_all_defaults(self) -> None:
        repo = FakePreferenceRepository()
        result = GetPreferences(repo).execute(GetPreferencesCommand(user_id=1))
        assert result == DEFAULT_PREFERENCES

    def test_empty_repo_weight_unit_is_lbs(self) -> None:
        repo = FakePreferenceRepository()
        result = GetPreferences(repo).execute(GetPreferencesCommand(user_id=1))
        assert result[PreferenceKey.WEIGHT_UNIT] == "lbs"

    def test_empty_repo_all_notify_defaults_true(self) -> None:
        repo = FakePreferenceRepository()
        result = GetPreferences(repo).execute(GetPreferencesCommand(user_id=1))
        assert result[PreferenceKey.NOTIFY_ACHIEVEMENT] == "true"
        assert result[PreferenceKey.NOTIFY_MILESTONE] == "true"
        assert result[PreferenceKey.NOTIFY_STREAK] == "true"

    def test_all_four_keys_always_present(self) -> None:
        repo = FakePreferenceRepository()
        result = GetPreferences(repo).execute(GetPreferencesCommand(user_id=1))
        assert set(result.keys()) == set(PreferenceKey)


class TestGetPreferencesMergesDefaults:
    def test_partial_rows_use_set_values(self) -> None:
        repo = FakePreferenceRepository({PreferenceKey.WEIGHT_UNIT: "kg"})
        result = GetPreferences(repo).execute(GetPreferencesCommand(user_id=1))
        assert result[PreferenceKey.WEIGHT_UNIT] == "kg"

    def test_partial_rows_fill_missing_with_defaults(self) -> None:
        repo = FakePreferenceRepository({PreferenceKey.WEIGHT_UNIT: "kg"})
        result = GetPreferences(repo).execute(GetPreferencesCommand(user_id=1))
        assert result[PreferenceKey.NOTIFY_ACHIEVEMENT] == "true"
        assert result[PreferenceKey.NOTIFY_MILESTONE] == "true"
        assert result[PreferenceKey.NOTIFY_STREAK] == "true"

    def test_full_rows_override_all_defaults(self) -> None:
        rows = {
            PreferenceKey.WEIGHT_UNIT: "kg",
            PreferenceKey.NOTIFY_ACHIEVEMENT: "false",
            PreferenceKey.NOTIFY_MILESTONE: "false",
            PreferenceKey.NOTIFY_STREAK: "false",
        }
        repo = FakePreferenceRepository(rows)
        result = GetPreferences(repo).execute(GetPreferencesCommand(user_id=1))
        assert result[PreferenceKey.WEIGHT_UNIT] == "kg"
        assert result[PreferenceKey.NOTIFY_ACHIEVEMENT] == "false"
        assert result[PreferenceKey.NOTIFY_MILESTONE] == "false"
        assert result[PreferenceKey.NOTIFY_STREAK] == "false"
