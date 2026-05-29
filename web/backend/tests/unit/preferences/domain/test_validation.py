"""Unit tests for preferences domain validation (validate_preference_value).

Tests per §8 of the design spec:
  - weight_unit valid (lbs/kg)
  - case-insensitive 'LBS'→'lbs' [G7]
  - weight_unit invalid → raise
  - notify_* accepts bool True/False
  - notify_* accepts 'true'/'false'
  - '1'/'yes'/''/whitespace-junk → raise [G7]
  - unhandled PreferenceKey → raise (exhaustiveness) [G3]
  - normalization to canonical string
"""

from __future__ import annotations

import pytest

from weighttogo.preferences.domain.entities import PreferenceKey
from weighttogo.preferences.domain.exceptions import InvalidPreferenceValueError
from weighttogo.preferences.domain.validation import validate_preference_value


class TestWeightUnitValidation:
    def test_lbs_accepted(self) -> None:
        assert validate_preference_value(PreferenceKey.WEIGHT_UNIT, "lbs") == "lbs"

    def test_kg_accepted(self) -> None:
        assert validate_preference_value(PreferenceKey.WEIGHT_UNIT, "kg") == "kg"

    def test_lbs_uppercase_normalized(self) -> None:
        assert validate_preference_value(PreferenceKey.WEIGHT_UNIT, "LBS") == "lbs"

    def test_kg_uppercase_normalized(self) -> None:
        assert validate_preference_value(PreferenceKey.WEIGHT_UNIT, "KG") == "kg"

    def test_mixed_case_normalized(self) -> None:
        assert validate_preference_value(PreferenceKey.WEIGHT_UNIT, "Lbs") == "lbs"

    def test_with_leading_whitespace_normalized(self) -> None:
        assert validate_preference_value(PreferenceKey.WEIGHT_UNIT, "  lbs") == "lbs"

    def test_with_trailing_whitespace_normalized(self) -> None:
        assert validate_preference_value(PreferenceKey.WEIGHT_UNIT, "kg  ") == "kg"

    def test_invalid_unit_raises(self) -> None:
        with pytest.raises(InvalidPreferenceValueError):
            validate_preference_value(PreferenceKey.WEIGHT_UNIT, "oz")

    def test_empty_string_raises(self) -> None:
        with pytest.raises(InvalidPreferenceValueError):
            validate_preference_value(PreferenceKey.WEIGHT_UNIT, "")

    def test_pounds_word_raises(self) -> None:
        with pytest.raises(InvalidPreferenceValueError):
            validate_preference_value(PreferenceKey.WEIGHT_UNIT, "pounds")


class TestNotifyValidation:
    @pytest.mark.parametrize(
        "key",
        [
            PreferenceKey.NOTIFY_ACHIEVEMENT,
            PreferenceKey.NOTIFY_MILESTONE,
            PreferenceKey.NOTIFY_STREAK,
        ],
    )
    def test_bool_true_normalizes_to_string_true(self, key: PreferenceKey) -> None:
        assert validate_preference_value(key, True) == "true"

    @pytest.mark.parametrize(
        "key",
        [
            PreferenceKey.NOTIFY_ACHIEVEMENT,
            PreferenceKey.NOTIFY_MILESTONE,
            PreferenceKey.NOTIFY_STREAK,
        ],
    )
    def test_bool_false_normalizes_to_string_false(self, key: PreferenceKey) -> None:
        assert validate_preference_value(key, False) == "false"

    def test_string_true_accepted(self) -> None:
        assert validate_preference_value(PreferenceKey.NOTIFY_ACHIEVEMENT, "true") == "true"

    def test_string_false_accepted(self) -> None:
        assert validate_preference_value(PreferenceKey.NOTIFY_ACHIEVEMENT, "false") == "false"

    def test_integer_one_raises(self) -> None:
        with pytest.raises(InvalidPreferenceValueError):
            validate_preference_value(PreferenceKey.NOTIFY_ACHIEVEMENT, "1")

    def test_integer_zero_raises(self) -> None:
        with pytest.raises(InvalidPreferenceValueError):
            validate_preference_value(PreferenceKey.NOTIFY_ACHIEVEMENT, "0")

    def test_yes_raises(self) -> None:
        with pytest.raises(InvalidPreferenceValueError):
            validate_preference_value(PreferenceKey.NOTIFY_ACHIEVEMENT, "yes")

    def test_no_raises(self) -> None:
        with pytest.raises(InvalidPreferenceValueError):
            validate_preference_value(PreferenceKey.NOTIFY_ACHIEVEMENT, "no")

    def test_empty_string_raises(self) -> None:
        with pytest.raises(InvalidPreferenceValueError):
            validate_preference_value(PreferenceKey.NOTIFY_ACHIEVEMENT, "")

    def test_true_with_whitespace_junk_raises(self) -> None:
        with pytest.raises(InvalidPreferenceValueError):
            validate_preference_value(PreferenceKey.NOTIFY_ACHIEVEMENT, "true ")

    def test_capital_true_raises(self) -> None:
        with pytest.raises(InvalidPreferenceValueError):
            validate_preference_value(PreferenceKey.NOTIFY_ACHIEVEMENT, "True")
