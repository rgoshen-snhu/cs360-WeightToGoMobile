"""SetActiveGoal use case (FR-G-1)."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import UTC, date, datetime
from decimal import Decimal

from weighttogo.goals.domain.entities import Goal, GoalType
from weighttogo.goals.domain.exceptions import ActiveGoalAlreadyExistsError
from weighttogo.goals.domain.ports import IGoalRepository
from weighttogo.goals.domain.validation import validate_target_direction


@dataclass(frozen=True)
class SetActiveGoalCommand:
    """Input for the SetActiveGoal use case."""

    user_id: int
    target_value: Decimal
    target_unit: str
    start_value: Decimal
    goal_type: str
    target_date: date | None


class SetActiveGoal:
    """Set a new active goal for a user (FR-G-1).

    Args:
        goal_repo: The goal repository port.
    """

    def __init__(self, goal_repo: IGoalRepository) -> None:
        """Initialise with the goal repository port."""
        self._repo = goal_repo

    def execute(self, command: SetActiveGoalCommand) -> Goal:
        """Execute the use case.

        Args:
            command: The command carrying the new goal parameters.

        Returns:
            The persisted ``Goal`` with a database-assigned ``goal_id``.

        Raises:
            ActiveGoalAlreadyExistsError: When the user already has an active goal.
        """
        if self._repo.get_active_for_user(command.user_id) is not None:
            raise ActiveGoalAlreadyExistsError()

        goal_type = GoalType(command.goal_type)
        validate_target_direction(goal_type, command.start_value, command.target_value)

        now = datetime.now(UTC)
        goal = Goal(
            goal_id=None,
            user_id=command.user_id,
            target_value=command.target_value,
            target_unit=command.target_unit,
            start_value=command.start_value,
            goal_type=goal_type,
            target_date=command.target_date,
            is_active=True,
            is_achieved=False,
            achieved_at=None,
            created_at=now,
            updated_at=now,
        )
        return self._repo.save(goal)
