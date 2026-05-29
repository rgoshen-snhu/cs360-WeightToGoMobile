"""Unit tests for DetectAchievements use case (FR-Ach-1, FR-Ach-2, FR-G-4)."""

from __future__ import annotations

from decimal import Decimal
from unittest.mock import MagicMock

from weighttogo.achievements.domain.entities import Achievement, AchievementType


def _make_repo(recorded: frozenset[Decimal] | None = None) -> MagicMock:
    repo = MagicMock()
    repo.get_recorded_thresholds.return_value = recorded or frozenset()
    repo.save.side_effect = lambda a: a
    return repo


def _run(
    repo: MagicMock,
    goal_type: str = "lose",
    start_value: str = "200",
    target_value: str = "150",
    current_weight: str = "195",
) -> list[Achievement]:
    from weighttogo.achievements.application.detect_achievements import (
        DetectAchievements,
        DetectAchievementsCommand,
    )

    return DetectAchievements(achievement_repo=repo).execute(
        DetectAchievementsCommand(
            user_id=1,
            goal_id=7,
            goal_type=goal_type,
            start_value=Decimal(start_value),
            target_value=Decimal(target_value),
            current_weight=Decimal(current_weight),
        )
    )


def test_detect_achievements_emits_milestone_when_threshold_crossed() -> None:
    # ARRANGE: delta=5, crosses 5 lb threshold
    # ACT
    result = _run(_make_repo(), current_weight="195")
    # ASSERT
    assert len(result) == 1
    assert result[0].achievement_type == AchievementType.MILESTONE
    assert result[0].threshold == Decimal("5")


def test_detect_achievements_returns_empty_when_no_threshold_crossed() -> None:
    # ARRANGE: delta=2, below 5 lb threshold
    result = _run(_make_repo(), current_weight="198")
    assert result == []


def test_detect_achievements_emits_goal_reached_when_target_met() -> None:
    repo = _make_repo()
    repo.has_goal_reached_been_recorded.return_value = False
    # ARRANGE: current=150, target=150 → exactly at goal
    result = _run(repo, current_weight="150")
    types = {a.achievement_type for a in result}
    assert AchievementType.GOAL_REACHED in types


def test_detect_achievements_emits_both_milestone_and_goal_reached() -> None:
    repo = _make_repo()
    repo.has_goal_reached_been_recorded.return_value = False
    # ARRANGE: start=200, target=150, current=150 → delta=50 (all milestones) + goal_reached
    result = _run(repo, current_weight="150")
    types = [a.achievement_type for a in result]
    assert AchievementType.MILESTONE in types
    assert AchievementType.GOAL_REACHED in types


def test_detect_achievements_skips_already_recorded_milestones() -> None:
    repo = _make_repo(recorded=frozenset({Decimal("5"), Decimal("10")}))
    # ARRANGE: delta=12, but 5 and 10 already recorded → nothing new
    result = _run(repo, current_weight="188")
    assert result == []


def test_detect_achievements_does_not_re_emit_goal_reached() -> None:
    repo = _make_repo()
    repo.has_goal_reached_been_recorded.return_value = True
    # ARRANGE: goal already reached — no new goal_reached achievement
    result = _run(repo, current_weight="150")
    types = [a.achievement_type for a in result]
    assert AchievementType.GOAL_REACHED not in types


def test_detect_achievements_gain_direction_detects_milestone() -> None:
    repo = _make_repo()
    # ARRANGE: gain goal, start=150, current=156 → delta=6, crosses 5 lb
    result = _run(
        repo,
        goal_type="gain",
        start_value="150",
        target_value="200",
        current_weight="156",
    )
    assert len(result) == 1
    assert result[0].threshold == Decimal("5")
