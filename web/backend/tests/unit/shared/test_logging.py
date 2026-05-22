"""Unit tests for the shared structured logging module."""

import structlog
from weighttogo.shared.logging import get_logger, mask_pii


def test_get_logger_returns_bound_logger() -> None:
    """get_logger() must return a structlog BoundLogger instance."""
    logger = get_logger("test.module")
    assert isinstance(logger, structlog.stdlib.BoundLogger)


def test_get_logger_accepts_bound_context() -> None:
    """get_logger() result must accept bind() with arbitrary context keys."""
    logger = get_logger("test.module")
    bound = logger.bind(user_id="u-123", action="test")
    # bind() returns a new BoundLogger; verifying it does not raise is sufficient.
    assert isinstance(bound, structlog.stdlib.BoundLogger)


def test_mask_pii_replaces_email_with_placeholder() -> None:
    """mask_pii() must replace a well-formed email address with [email]."""
    result = mask_pii("contact user@example.com for details")
    assert "user@example.com" not in result
    assert "[email]" in result


def test_mask_pii_replaces_multiple_emails() -> None:
    """mask_pii() must replace every email address in the input string."""
    result = mask_pii("from a@b.com to c@d.org")
    assert "a@b.com" not in result
    assert "c@d.org" not in result
    assert result.count("[email]") == 2


def test_mask_pii_leaves_non_email_strings_unchanged() -> None:
    """mask_pii() must not alter strings that contain no email addresses."""
    plain = "weight entry 75.5 kg on 2026-05-22"
    assert mask_pii(plain) == plain
