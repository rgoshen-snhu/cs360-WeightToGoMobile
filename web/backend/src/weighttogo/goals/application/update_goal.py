"""UpdateGoal use case (FR-G-3)."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import UTC, date, datetime
from decimal import Decimal

from weighttogo.goals.domain.entities import Goal
from weighttogo.goals.domain.exceptions import (
    GoalNotActiveError,
    GoalNotFoundError,
)
from weighttogo.goals.domain.ports import IGoalRepository
from weighttogo.goals.domain.validation import validate_target_direction


@dataclass(frozen=True)
class UpdateGoalCommand:
    """Input for the UpdateGoal use case.

    Only ``target_value`` and ``target_date`` are mutable per FR-G-3.
    """

    user_id: int
    goal_id: int
    target_value: Decimal
    target_date: date | None


class UpdateGoal:
    """Update an active goal's target value or target date (FR-G-3).

    Args:
        goal_repo: The goal repository port.
    """

    def __init__(self, goal_repo: IGoalRepository) -> None:
        """Initialise with the goal repository port."""
        self._repo = goal_repo

    def execute(self, command: UpdateGoalCommand) -> Goal:
        """Execute the use case.

        Args:
            command: The update command with the new field values.

        Returns:
            The updated and persisted ``Goal``.

        Raises:
            GoalNotFoundError: When the goal does not exist or belongs to
                a different user.
        """
        goal = self._repo.get_by_id(command.goal_id, command.user_id)
        if goal is None:
            raise GoalNotFoundError()

        if not goal.is_active:
            raise GoalNotActiveError()

        validate_target_direction(goal.goal_type, goal.start_value, command.target_value)

        goal.target_value = command.target_value
        goal.target_date = command.target_date
        goal.updated_at = datetime.now(UTC)

        return self._repo.save(goal)
