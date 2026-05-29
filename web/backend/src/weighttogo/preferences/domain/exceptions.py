"""Domain exceptions for the preferences bounded context."""

from __future__ import annotations


class InvalidPreferenceValueError(Exception):
    """Raised when a preference value is not valid for its key."""
