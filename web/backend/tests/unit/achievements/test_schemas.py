"""Unit tests for achievements interface schemas."""

from __future__ import annotations

from datetime import UTC, datetime
from decimal import Decimal


def test_achievement_response_serialises_milestone() -> None:
    from weighttogo.achievements.interface.schemas import AchievementResponse

    # ARRANGE / ACT
    resp = AchievementResponse(
        achievement_id=1,
        goal_id=7,
        achievement_type="milestone",
        threshold=Decimal("5"),
        earned_at=datetime(2026, 1, 1, tzinfo=UTC),
    )
    data = resp.model_dump()
    # ASSERT
    assert data["achievement_type"] == "milestone"
    assert data["threshold"] == Decimal("5")


def test_achievement_response_serialises_goal_reached_with_null_threshold() -> None:
    from weighttogo.achievements.interface.schemas import AchievementResponse

    resp = AchievementResponse(
        achievement_id=2,
        goal_id=7,
        achievement_type="goal_reached",
        threshold=None,
        earned_at=datetime(2026, 1, 1, tzinfo=UTC),
    )
    data = resp.model_dump()
    assert data["threshold"] is None
