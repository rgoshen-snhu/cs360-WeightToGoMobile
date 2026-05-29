"""Pure domain validation for preference values.

No framework imports — O(1) per call.
"""

from __future__ import annotations

from weighttogo.preferences.domain.entities import PreferenceKey
from weighttogo.preferences.domain.exceptions import InvalidPreferenceValueError

_VALID_UNITS: frozenset[str] = frozenset({"lbs", "kg"})
_VALID_BOOL_STRINGS: frozenset[str] = frozenset({"true", "false"})


def validate_preference_value(key: PreferenceKey, value: bool | str) -> str:
    """Normalise and validate a preference value to its canonical stored string.

    weight_unit  -> 'lbs' | 'kg'     (strip + lowercase; case-insensitive match)
    notify_*     -> 'true' | 'false'  (Python bool accepted; strings must be
                                       exactly 'true' or 'false' after strip)

    Coercion policy [G7]:
      * weight_unit: strip whitespace, lowercase, then check against valid set.
        'LBS' -> 'lbs', 'kg  ' -> 'kg', 'oz' -> raise.
      * notify_*: Python bool True -> 'true', False -> 'false'. String must
        equal 'true' or 'false' EXACTLY after strip+lower — but internal
        whitespace or non-canonical casing ('True', '1', 'yes') raises.
        Exception: strip() removes only leading/trailing spaces; if the
        stripped value is in {'true','false'} it is accepted.
        Note: 'true ' after strip is 'true' which IS valid; but 'True' (capital
        T) lowercased is 'true' which would accept — by design, notify strings
        are NOT case-insensitive. Only exact 'true'/'false' is accepted.

    Args:
        key: The preference key determining validation rules.
        value: The raw value from the API request.

    Returns:
        The canonical stored string.

    Raises:
        InvalidPreferenceValueError: When value is not valid for key, or key
            has no defined validation rule (exhaustiveness guard [G3]).
    """
    match key:
        case PreferenceKey.WEIGHT_UNIT:
            normalised = str(value).strip().lower()
            if normalised not in _VALID_UNITS:
                raise InvalidPreferenceValueError(
                    f"Invalid weight_unit value {value!r}. Must be one of: lbs, kg."
                )
            return normalised

        case (
            PreferenceKey.NOTIFY_ACHIEVEMENT
            | PreferenceKey.NOTIFY_MILESTONE
            | PreferenceKey.NOTIFY_STREAK
        ):
            if isinstance(value, bool):
                return "true" if value else "false"
            # Exact match only — no strip, no case-fold. 'True', 'true ', etc. raise.
            if str(value) not in _VALID_BOOL_STRINGS:
                raise InvalidPreferenceValueError(
                    f"Invalid notify value {value!r}. Must be exactly 'true' or 'false', or a bool."
                )
            return str(value)

        case _:
            raise InvalidPreferenceValueError(
                f"No validation rule defined for preference key {key!r}. [G3 exhaustiveness guard]"
            )
