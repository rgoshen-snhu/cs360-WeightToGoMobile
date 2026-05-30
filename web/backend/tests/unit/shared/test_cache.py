"""Unit tests for the shared TTL cache (NFR-P-5, ADR-0023)."""

from __future__ import annotations

from weighttogo.shared.cache import TTLCache


def test_get_returns_none_on_miss() -> None:
    # ARRANGE
    cache: TTLCache[int, str] = TTLCache()

    # ACT / ASSERT
    assert cache.get(1) is None


def test_get_returns_value_after_set_hit() -> None:
    # ARRANGE
    cache: TTLCache[int, str] = TTLCache()

    # ACT
    cache.set(1, "summary")

    # ASSERT
    assert cache.get(1) == "summary"


def test_get_returns_none_after_ttl_expires() -> None:
    # ARRANGE — controllable monotonic clock
    clock = {"t": 1000.0}
    cache: TTLCache[int, str] = TTLCache(ttl_seconds=30.0, now=lambda: clock["t"])
    cache.set(1, "summary")

    # ACT — advance past the TTL
    clock["t"] = 1031.0

    # ASSERT
    assert cache.get(1) is None


def test_get_returns_value_just_before_ttl_expires() -> None:
    # ARRANGE
    clock = {"t": 1000.0}
    cache: TTLCache[int, str] = TTLCache(ttl_seconds=30.0, now=lambda: clock["t"])
    cache.set(1, "summary")

    # ACT — still inside the window
    clock["t"] = 1029.0

    # ASSERT
    assert cache.get(1) == "summary"


def test_expired_entry_is_evicted_not_just_hidden() -> None:
    # ARRANGE — prove lazy eviction frees storage, not only hides the value
    clock = {"t": 1000.0}
    cache: TTLCache[int, str] = TTLCache(ttl_seconds=30.0, now=lambda: clock["t"])
    cache.set(1, "summary")

    # ACT — read after expiry triggers eviction
    clock["t"] = 1031.0
    assert cache.get(1) is None

    # ASSERT — the underlying store no longer holds the key
    assert 1 not in cache._store  # noqa: SLF001 — white-box storage check
