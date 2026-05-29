"""Unit tests for SetActiveGoal use case (FR-G-1)."""

from __future__ import annotations

from datetime import UTC, date, datetime
from decimal import Decimal
from unittest.mock import MagicMock

import pytest

from weighttogo.goals.application.set_active_goal import SetActiveGoal, SetActiveGoalCommand
from weighttogo.goals.domain.entities import Goal, GoalType
from weighttogo.goals.domain.exceptions import ActiveGoalAlreadyExistsError, InvalidGoalTargetError


def _make_repo(active_goal: Goal | None = None) -> MagicMock:
    repo = MagicMock()
    repo.get_active_for_user.return_value = active_goal
    repo.save.side_effect = lambda g: g
    return repo


def _make_existing_goal() -> Goal:
    now = datetime.now(UTC)
    return Goal(
        goal_id=1,
        user_id=1,
        target_value=Decimal("150"),
        target_unit="lbs",
        start_value=Decimal("200"),
        goal_type=GoalType.LOSE,
        target_date=None,
        is_active=True,
        is_achieved=False,
        achieved_at=None,
        created_at=now,
        updated_at=now,
    )


def _run(repo: MagicMock, **overrides: object) -> Goal:
    defaults: dict[str, object] = {
        "user_id": 1,
        "target_value": Decimal("150"),
        "target_unit": "lbs",
        "start_value": Decimal("200"),
        "goal_type": "lose",
        "target_date": None,
    }
    defaults.update(overrides)
    uc = SetActiveGoal(goal_repo=repo)
    return uc.execute(SetActiveGoalCommand(**defaults))  # type: ignore[arg-type]


# ── happy path ────────────────────────────────────────────────────────────────


def test_set_active_goal_calls_save() -> None:
    repo = _make_repo()
    _run(repo)
    repo.save.assert_called_once()


def test_set_active_goal_returns_goal_with_is_active_true() -> None:
    repo = _make_repo()
    result = _run(repo)
    assert result.is_active is True


def test_set_active_goal_sets_correct_fields() -> None:
    repo = _make_repo()
    result = _run(repo, target_value=Decimal("155"), target_date=date(2026, 12, 31))
    assert result.target_value == Decimal("155")
    assert result.target_date == date(2026, 12, 31)
    assert result.is_achieved is False
    assert result.achieved_at is None


# ── conflict ──────────────────────────────────────────────────────────────────


def test_set_active_goal_raises_when_active_goal_exists() -> None:
    repo = _make_repo(active_goal=_make_existing_goal())
    with pytest.raises(ActiveGoalAlreadyExistsError):
        _run(repo)


def test_set_active_goal_does_not_call_save_on_conflict() -> None:
    repo = _make_repo(active_goal=_make_existing_goal())
    with pytest.raises(ActiveGoalAlreadyExistsError):
        _run(repo)
    repo.save.assert_not_called()


# ── direction invariant (enforced at use-case layer) ─────────────────────────


def test_set_active_goal_rejects_lose_goal_with_target_above_start() -> None:
    repo = _make_repo()
    with pytest.raises(InvalidGoalTargetError):
        _run(repo, goal_type="lose", start_value=Decimal("150"), target_value=Decimal("200"))


def test_set_active_goal_rejects_lose_goal_with_equal_target_and_start() -> None:
    repo = _make_repo()
    with pytest.raises(InvalidGoalTargetError):
        _run(repo, goal_type="lose", start_value=Decimal("200"), target_value=Decimal("200"))


def test_set_active_goal_rejects_gain_goal_with_target_below_start() -> None:
    repo = _make_repo()
    with pytest.raises(InvalidGoalTargetError):
        _run(repo, goal_type="gain", start_value=Decimal("200"), target_value=Decimal("150"))
