"""Unit tests for AchievementModel ORM definition."""

from __future__ import annotations


def test_achievement_model_tablename() -> None:
    from weighttogo.achievements.infrastructure.models import AchievementModel

    assert AchievementModel.__tablename__ == "achievements"


def test_achievement_model_has_required_columns() -> None:
    from weighttogo.achievements.infrastructure.models import AchievementModel

    cols = {c.key for c in AchievementModel.__table__.columns}
    assert {"id", "user_id", "goal_id", "achievement_type", "threshold", "earned_at"} <= cols
