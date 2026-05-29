"""DetectAchievements use case (FR-Ach-1, FR-Ach-2, FR-G-4)."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import UTC, datetime
from decimal import Decimal

from weighttogo.achievements.domain.entities import Achievement, AchievementType
from weighttogo.achievements.domain.milestone_detector import GoalSnapshot, detect_milestones
from weighttogo.achievements.domain.ports import IAchievementRepository


@dataclass(frozen=True)
class DetectAchievementsCommand:
    """Input for the DetectAchievements use case.

    Attributes:
        user_id: The authenticated user's ID.
        goal_id: The active goal's primary key.
        goal_type: ``'lose'`` or ``'gain'``.
        start_value: The goal's starting weight.
        target_value: The goal's target weight.
        current_weight: The weight value from the new entry (same unit as
            start/target — the router normalises before calling this use case).
    """

    user_id: int
    goal_id: int
    goal_type: str
    start_value: Decimal
    target_value: Decimal
    current_weight: Decimal


class DetectAchievements:
    """Detect and persist newly earned achievements for a weight entry.

    Orchestrates:

    1. Load already-recorded milestone thresholds into a ``frozenset``
       (single DB read — O(k) space).
    2. Run ``detect_milestones()`` — O(k) pure function.
    3. Check whether the goal's target weight is now reached (FR-G-4).
    4. Persist each new ``Achievement`` via the repository.
    5. Return the list of newly persisted achievements (empty if none).

    No cross-domain imports: ``GoalSnapshot`` is a ``NamedTuple`` defined in
    the achievements domain so this use case never imports
    ``weighttogo.goals`` — preserving the Clean Architecture isolation
    contract (ADR-0019).

    Args:
        achievement_repo: The achievement repository port.
    """

    def __init__(self, achievement_repo: IAchievementRepository) -> None:
        """Initialise with the achievement repository port."""
        self._repo = achievement_repo

    def execute(self, cmd: DetectAchievementsCommand) -> list[Achievement]:
        """Execute milestone and goal-reached detection.

        Args:
            cmd: Command carrying goal and weight entry fields.

        Returns:
            List of newly earned ``Achievement`` entities (may be empty).
        """
        now = datetime.now(UTC)
        newly_earned: list[Achievement] = []

        # ── 1. Milestone detection (FR-Ach-2) ────────────────────────────────
        recorded = self._repo.get_recorded_thresholds(cmd.goal_id)
        snap = GoalSnapshot(
            goal_id=cmd.goal_id,
            goal_type=cmd.goal_type,
            start_value=cmd.start_value,
            target_value=cmd.target_value,
        )
        for threshold in detect_milestones(snap, cmd.current_weight, recorded):
            ach = self._repo.save(
                Achievement(
                    achievement_id=None,
                    user_id=cmd.user_id,
                    goal_id=cmd.goal_id,
                    achievement_type=AchievementType.MILESTONE,
                    threshold=threshold,
                    earned_at=now,
                )
            )
            newly_earned.append(ach)

        # ── 2. Goal-reached detection (FR-G-4, FR-Ach-1) ─────────────────────
        if self._goal_is_reached(cmd) and not self._repo.has_goal_reached_been_recorded(
            cmd.goal_id
        ):
            ach = self._repo.save(
                Achievement(
                    achievement_id=None,
                    user_id=cmd.user_id,
                    goal_id=cmd.goal_id,
                    achievement_type=AchievementType.GOAL_REACHED,
                    threshold=None,
                    earned_at=now,
                )
            )
            newly_earned.append(ach)

        return newly_earned

    def _goal_is_reached(self, cmd: DetectAchievementsCommand) -> bool:
        """Return ``True`` when *current_weight* has met or passed the target."""
        if cmd.goal_type == "lose":
            return cmd.current_weight <= cmd.target_value
        return cmd.current_weight >= cmd.target_value
