"""Unit tests for GetActiveGoalWithProgress use case (FR-G-2, Option B)."""

from __future__ import annotations

from datetime import UTC, datetime
from decimal import Decimal
from unittest.mock import MagicMock

import pytest

from weighttogo.goals.application.get_active_goal_with_progress import (
    GetActiveGoalWithProgress,
    GetActiveGoalWithProgressCommand,
    GoalWithProgress,
)
from weighttogo.goals.domain.entities import Goal, GoalType


def _make_goal(
    goal_id: int = 1,
    user_id: int = 1,
    start_value: str = "200",
    target_value: str = "150",
    target_unit: str = "lbs",
    goal_type: GoalType = GoalType.LOSE,
) -> Goal:
    now = datetime.now(UTC)
    return Goal(
        goal_id=goal_id,
        user_id=user_id,
        target_value=Decimal(target_value),
        target_unit=target_unit,
        start_value=Decimal(start_value),
        goal_type=goal_type,
        target_date=None,
        is_active=True,
        is_achieved=False,
        achieved_at=None,
        created_at=now,
        updated_at=now,
    )


def _make_repo(active_goal: Goal | None = None) -> MagicMock:
    repo = MagicMock()
    repo.get_active_for_user.return_value = active_goal
    return repo


def _run(
    repo: MagicMock,
    user_id: int = 1,
    latest_weight_value: Decimal | None = None,
    latest_weight_unit: str | None = None,
    readonly: bool = False,
) -> GoalWithProgress:
    uc = GetActiveGoalWithProgress(goal_repo=repo)
    return uc.execute(
        GetActiveGoalWithProgressCommand(
            user_id=user_id,
            latest_weight_value=latest_weight_value,
            latest_weight_unit=latest_weight_unit,
            readonly=readonly,
        )
    )


# ── no active goal ────────────────────────────────────────────────────────────


def test_no_active_goal_returns_none_goal() -> None:
    repo = _make_repo(active_goal=None)
    result = _run(repo)
    assert result.goal is None
    assert result.progress is None
    assert result.current_value is None


# ── active goal, no weight entries ────────────────────────────────────────────


def test_active_goal_with_no_entries_returns_none_progress() -> None:
    repo = _make_repo(active_goal=_make_goal())
    result = _run(repo, latest_weight_value=None, latest_weight_unit=None)
    assert result.goal is not None
    assert result.progress is None
    assert result.current_value is None


# ── active goal, matching units ───────────────────────────────────────────────


def test_active_goal_halfway_progress_matching_unit() -> None:
    # start=200 lbs, target=150 lbs, current=175 lbs → 50%
    repo = _make_repo(active_goal=_make_goal(start_value="200", target_value="150"))
    result = _run(repo, latest_weight_value=Decimal("175"), latest_weight_unit="lbs")
    assert result.progress is not None
    assert result.progress.percent == pytest.approx(50.0)
    assert result.current_value == Decimal("175")


def test_active_goal_at_target_is_100_percent() -> None:
    repo = _make_repo(active_goal=_make_goal(start_value="200", target_value="150"))
    result = _run(repo, latest_weight_value=Decimal("150"), latest_weight_unit="lbs")
    assert result.progress is not None
    assert result.progress.percent == pytest.approx(100.0)


def test_reaching_target_calls_mark_achieved_and_saves() -> None:
    goal = _make_goal(start_value="200", target_value="150")
    repo = _make_repo(active_goal=goal)
    repo.save.return_value = goal

    _run(repo, latest_weight_value=Decimal("150"), latest_weight_unit="lbs")

    assert goal.is_achieved is True
    repo.save.assert_called_once_with(goal)


def test_already_achieved_goal_does_not_call_save_again() -> None:
    goal = _make_goal(start_value="200", target_value="150")
    goal.mark_achieved()  # pre-mark so is_achieved=True
    repo = _make_repo(active_goal=goal)

    _run(repo, latest_weight_value=Decimal("150"), latest_weight_unit="lbs")

    repo.save.assert_not_called()


# ── active goal, MISMATCHED units (Option B — must convert) ───────────────────


def test_active_goal_progress_with_kg_entry_and_lbs_goal() -> None:
    # Goal: lose from 200 lbs to 150 lbs.
    # Latest entry: 79.37866 kg ≈ 175 lbs → should compute ~50%.
    # current_value must be returned in lbs (goal unit), not kg.
    repo = _make_repo(
        active_goal=_make_goal(start_value="200", target_value="150", target_unit="lbs")
    )
    result = _run(
        repo,
        latest_weight_value=Decimal("79.37866"),  # ≈ 175 lbs
        latest_weight_unit="kg",
    )
    assert result.progress is not None
    assert result.progress.percent == pytest.approx(50.0, abs=0.5)
    # current_value must be in the goal's unit (lbs), not the entry unit (kg)
    assert result.current_value is not None
    assert float(result.current_value) == pytest.approx(175.0, abs=0.5)


# ── gain goal ─────────────────────────────────────────────────────────────────


def test_readonly_skips_mark_achieved() -> None:
    """readonly=True must not trigger the mark_achieved write."""
    goal = _make_goal(start_value="200", target_value="150")
    repo = _make_repo(active_goal=goal)

    _run(repo, latest_weight_value=Decimal("150"), latest_weight_unit="lbs", readonly=True)

    assert goal.is_achieved is False
    repo.save.assert_not_called()


def test_gain_goal_progress_halfway() -> None:
    # start=150 lbs, target=200 lbs, current=175 lbs → 50%
    repo = _make_repo(
        active_goal=_make_goal(start_value="150", target_value="200", goal_type=GoalType.GAIN)
    )
    result = _run(repo, latest_weight_value=Decimal("175"), latest_weight_unit="lbs")
    assert result.progress is not None
    assert result.progress.percent == pytest.approx(50.0)
    assert result.current_value == Decimal("175")
