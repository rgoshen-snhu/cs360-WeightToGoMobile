"""BuildDashboardSummary use case (FR-D-1) with active-goal aggregation (FR-D-4)."""

from __future__ import annotations

from dataclasses import dataclass

from weighttogo.goals.application.get_active_goal_with_progress import (
    GetActiveGoalWithProgress,
    GetActiveGoalWithProgressCommand,
    GoalWithProgress,
)
from weighttogo.weight_tracking.domain.entities import WeightEntry
from weighttogo.weight_tracking.domain.ports import IWeightEntryRepository


@dataclass(frozen=True)
class DashboardSummary:
    """Read-model returned by BuildDashboardSummary.

    Attributes:
        latest_entry: The most recent active weight entry, or ``None``.
        total_entries: Count of non-deleted weight entries.
        active_goal: The active goal with progress, or ``None`` when the user
            has no active goal.
    """

    latest_entry: WeightEntry | None
    total_entries: int
    active_goal: GoalWithProgress | None = None


class BuildDashboardSummary:
    """Assemble the dashboard read model for the requesting user.

    Composes the ``weight_tracking`` and ``goals`` bounded contexts. Goal
    progress is delegated to ``GetActiveGoalWithProgress`` (reused, not
    reimplemented), fed the latest entry this use case already fetches.

    Args:
        weight_repo: The weight entry repository port.
        get_active_goal_with_progress: The goals use case that returns the
            active goal and its progress.
    """

    def __init__(
        self,
        weight_repo: IWeightEntryRepository,
        get_active_goal_with_progress: GetActiveGoalWithProgress,
    ) -> None:
        """Initialise with the weight repository and the goal-progress use case."""
        self._repo = weight_repo
        self._get_active_goal = get_active_goal_with_progress

    def execute(self, user_id: int) -> DashboardSummary:
        """Build the dashboard summary for *user_id*.

        Args:
            user_id: The requesting user's surrogate ID.

        Returns:
            A ``DashboardSummary``; ``active_goal`` is ``None`` when the user
            has no active goal.
        """
        latest_entry = self._repo.get_latest_for_user(user_id)
        total_entries = self._repo.count_for_user(user_id)
        goal_with_progress = self._get_active_goal.execute(
            GetActiveGoalWithProgressCommand(
                user_id=user_id,
                latest_weight_value=latest_entry.weight_value if latest_entry else None,
                latest_weight_unit=latest_entry.weight_unit if latest_entry else None,
                readonly=True,
            )
        )
        active_goal = goal_with_progress if goal_with_progress.goal is not None else None
        return DashboardSummary(
            latest_entry=latest_entry,
            total_entries=total_entries,
            active_goal=active_goal,
        )
