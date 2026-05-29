"""Unit tests for IAchievementRepository port (Protocol duck-typing checks)."""

from __future__ import annotations


def test_iachievement_repository_has_required_methods() -> None:
    from weighttogo.achievements.domain.ports import IAchievementRepository

    for method in (
        "save",
        "get_recorded_thresholds",
        "has_goal_reached_been_recorded",
        "get_by_id",
        "list_for_user",
    ):
        assert hasattr(IAchievementRepository, method), f"Missing method: {method}"
