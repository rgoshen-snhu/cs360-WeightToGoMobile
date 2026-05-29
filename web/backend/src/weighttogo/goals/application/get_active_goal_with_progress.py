"""GetActiveGoalWithProgress use case (FR-G-2)."""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal

from weighttogo.goals.domain.entities import Goal
from weighttogo.goals.domain.ports import IGoalRepository
from weighttogo.goals.domain.progress import GoalProgress, calculate_progress
from weighttogo.shared.units import convert_weight


@dataclass(frozen=True)
class GetActiveGoalWithProgressCommand:
    """Input for the GetActiveGoalWithProgress use case.

    Attributes:
        user_id: The requesting user's ID.
        latest_weight_value: The user's most recent weight entry value, or
            ``None`` when no entries exist.
        latest_weight_unit: The unit of the most recent weight entry, or
            ``None`` when no entries exist.  Must accompany
            ``latest_weight_value``.
        readonly: When ``True``, skip the mark-achieved write so callers on
            safe HTTP methods (GET) do not mutate goal state.
    """

    user_id: int
    latest_weight_value: Decimal | None
    latest_weight_unit: str | None
    readonly: bool = False


@dataclass(frozen=True)
class GoalWithProgress:
    """Result of GetActiveGoalWithProgress.

    Attributes:
        goal: The user's active goal, or ``None`` when no active goal exists.
        progress: The progress toward the goal, or ``None`` when the goal is
            absent or the user has no weight entries yet.
        current_value: The latest weight entry converted to the goal's unit,
            or ``None`` when no entries exist or no goal is active.
    """

    goal: Goal | None
    progress: GoalProgress | None
    current_value: Decimal | None


class GetActiveGoalWithProgress:
    """Return the active goal and its progress percentage (FR-G-2).

    The caller (router) is responsible for fetching the latest weight entry
    and passing its value and unit to this use case.  Converting the entry
    unit into the goal's unit happens here (Option B — progress is never
    ``None`` solely because of a unit mismatch).

    Args:
        goal_repo: The goal repository port.
    """

    def __init__(self, goal_repo: IGoalRepository) -> None:
        """Initialise with the goal repository port."""
        self._repo = goal_repo

    def execute(self, command: GetActiveGoalWithProgressCommand) -> GoalWithProgress:
        """Execute the use case.

        Args:
            command: The command containing the user ID and the latest weight.

        Returns:
            A :class:`GoalWithProgress` with ``goal=None`` when no active goal
            exists, or ``progress=None`` when the user has no weight entries.
        """
        goal = self._repo.get_active_for_user(command.user_id)
        if goal is None:
            return GoalWithProgress(goal=None, progress=None, current_value=None)

        if command.latest_weight_value is None or command.latest_weight_unit is None:
            return GoalWithProgress(goal=goal, progress=None, current_value=None)

        current_in_goal_unit = convert_weight(
            command.latest_weight_value,
            command.latest_weight_unit,
            goal.target_unit,
        )

        progress = calculate_progress(
            start=goal.start_value,
            current=current_in_goal_unit,
            target=goal.target_value,
        )

        if progress.percent >= 100 and not goal.is_achieved and not command.readonly:
            goal.mark_achieved()
            self._repo.save(goal)

        return GoalWithProgress(goal=goal, progress=progress, current_value=current_in_goal_unit)
