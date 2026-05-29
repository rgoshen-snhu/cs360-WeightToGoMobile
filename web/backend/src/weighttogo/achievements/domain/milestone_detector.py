"""Milestone detection algorithm for the achievements bounded context.

Pure function — zero framework imports.  Primary algorithmic showcase for the
CS 499 Milestone Three rubric.

Time complexity:  O(k) where k = 4 (fixed threshold count).  O(1) in practice.
Space complexity: O(k) for the output list.  frozenset membership is O(1).
"""

from __future__ import annotations

from decimal import Decimal
from typing import NamedTuple

THRESHOLDS: tuple[Decimal, ...] = (
    Decimal("5"),
    Decimal("10"),
    Decimal("25"),
    Decimal("50"),
)


class GoalSnapshot(NamedTuple):
    """Lightweight projection of goal fields required by the detector.

    Defined here (not in the goals domain) so the achievements domain
    carries no import from ``weighttogo.goals``, preserving the Clean
    Architecture cross-domain isolation contract (ADR-0012, ADR-0019).

    Attributes:
        goal_id: The goal's surrogate primary key.
        goal_type: ``'lose'`` or ``'gain'``.
        start_value: The weight at goal creation time.
        target_value: The weight the user wants to reach.
    """

    goal_id: int
    goal_type: str
    start_value: Decimal
    target_value: Decimal


def detect_milestones(
    goal: GoalSnapshot,
    current_weight: Decimal,
    already_recorded: frozenset[Decimal],
) -> list[Decimal]:
    """Return thresholds newly crossed by *current_weight*.

    For a ``'lose'`` goal: ``delta = start_value - current_weight``.
    For a ``'gain'`` goal: ``delta = current_weight - start_value``.
    A threshold T is newly crossed when ``delta >= T`` and
    ``T not in already_recorded``.

    The *already_recorded* frozenset is the primary idempotency guard.
    The database partial unique indexes are the race-condition backstop
    (ADR-0019).

    Args:
        goal: Snapshot of goal fields needed for direction and start value.
        current_weight: The weight value from the new entry (in the same
            unit as ``goal.start_value``).
        already_recorded: Thresholds already persisted for this goal.

    Returns:
        List of thresholds (Decimal) newly crossed, in ascending order.
        Empty list when nothing new is earned.
    """
    if goal.goal_type == "lose":
        delta = goal.start_value - current_weight
    else:
        delta = current_weight - goal.start_value

    return [t for t in THRESHOLDS if delta >= t and t not in already_recorded]
