"""Domain validation rules for the goals bounded context.

Pure functions that enforce invariants shared across use cases.
No framework dependencies.
"""

from __future__ import annotations

from decimal import Decimal

from weighttogo.goals.domain.entities import GoalType
from weighttogo.goals.domain.exceptions import InvalidGoalTargetError


def validate_target_direction(
    goal_type: GoalType, start_value: Decimal, target_value: Decimal
) -> None:
    """Raise InvalidGoalTargetError when target contradicts the goal direction.

    LOSE goals require target < start; GAIN goals require target > start.
    Equality is rejected because it would make progress permanently stuck at 0%.

    Args:
        goal_type: The direction of the goal (lose or gain).
        start_value: The starting weight reference.
        target_value: The proposed target weight.

    Raises:
        InvalidGoalTargetError: When target_value violates the direction invariant.
    """
    if goal_type == GoalType.LOSE and target_value >= start_value:
        raise InvalidGoalTargetError("Target must be below start value for a lose goal.")
    if goal_type == GoalType.GAIN and target_value <= start_value:
        raise InvalidGoalTargetError("Target must be above start value for a gain goal.")
