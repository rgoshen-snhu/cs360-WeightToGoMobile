"""Unit tests for detect_milestones pure function (FR-Ach-2).

Algorithm: O(k) scan over k=4 fixed thresholds.  O(1) in practice.
"""

from __future__ import annotations

from decimal import Decimal

from weighttogo.achievements.domain.milestone_detector import GoalSnapshot


def _snap(
    goal_id: int = 1,
    goal_type: str = "lose",
    start_value: str = "200",
    target_value: str = "150",
) -> GoalSnapshot:
    return GoalSnapshot(
        goal_id=goal_id,
        goal_type=goal_type,
        start_value=Decimal(start_value),
        target_value=Decimal(target_value),
    )


def test_detect_milestones_returns_empty_when_no_threshold_crossed() -> None:
    from weighttogo.achievements.domain.milestone_detector import detect_milestones

    # ARRANGE: start=200, current=198 → delta=2, below 5 lb threshold
    # ACT
    result = detect_milestones(_snap(), Decimal("198"), frozenset())
    # ASSERT
    assert result == []


def test_detect_milestones_returns_five_when_delta_equals_five() -> None:
    from weighttogo.achievements.domain.milestone_detector import detect_milestones

    # ARRANGE: start=200, current=195 → delta=5, exactly at 5 lb threshold
    result = detect_milestones(_snap(), Decimal("195"), frozenset())
    assert result == [Decimal("5")]


def test_detect_milestones_returns_multiple_thresholds_on_big_jump() -> None:
    from weighttogo.achievements.domain.milestone_detector import detect_milestones

    # ARRANGE: start=200, current=188 → delta=12, crosses 5 and 10
    result = detect_milestones(_snap(), Decimal("188"), frozenset())
    assert result == [Decimal("5"), Decimal("10")]


def test_detect_milestones_skips_already_recorded_thresholds() -> None:
    from weighttogo.achievements.domain.milestone_detector import detect_milestones

    # ARRANGE: delta=12, but 5 is already recorded — only 10 should be new
    result = detect_milestones(_snap(), Decimal("188"), frozenset({Decimal("5")}))
    assert result == [Decimal("10")]


def test_detect_milestones_gain_direction_uses_positive_delta() -> None:
    from weighttogo.achievements.domain.milestone_detector import detect_milestones

    # ARRANGE: gain goal, start=150, current=156 → delta=6, crosses 5 lb
    snap = _snap(goal_type="gain", start_value="150", target_value="200")
    result = detect_milestones(snap, Decimal("156"), frozenset())
    assert result == [Decimal("5")]


def test_detect_milestones_returns_all_four_thresholds_on_huge_jump() -> None:
    from weighttogo.achievements.domain.milestone_detector import detect_milestones

    # ARRANGE: start=200, current=140 → delta=60, crosses all four thresholds
    result = detect_milestones(_snap(), Decimal("140"), frozenset())
    assert result == [Decimal("5"), Decimal("10"), Decimal("25"), Decimal("50")]


def test_detect_milestones_returns_empty_when_no_progress() -> None:
    from weighttogo.achievements.domain.milestone_detector import detect_milestones

    # ARRANGE: start=200, current=200 → delta=0, no progress
    result = detect_milestones(_snap(), Decimal("200"), frozenset())
    assert result == []


def test_detect_milestones_returns_empty_when_all_recorded() -> None:
    from weighttogo.achievements.domain.milestone_detector import detect_milestones

    # ARRANGE: all four thresholds already recorded
    all_recorded: frozenset[Decimal] = frozenset(
        {Decimal("5"), Decimal("10"), Decimal("25"), Decimal("50")}
    )
    result = detect_milestones(_snap(), Decimal("140"), all_recorded)
    assert result == []
