"""Unit tests for GoalModel ORM class."""

from __future__ import annotations

from weighttogo.goals.infrastructure.models import GoalModel


def test_goal_model_tablename() -> None:
    assert GoalModel.__tablename__ == "goals"


def test_goal_model_has_required_columns() -> None:
    mapper = GoalModel.__mapper__
    col_names = {c.key for c in mapper.columns}
    expected = {
        "goal_id",
        "user_id",
        "target_value",
        "target_unit",
        "start_value",
        "goal_type",
        "target_date",
        "is_active",
        "is_achieved",
        "achieved_at",
        "created_at",
        "updated_at",
    }
    assert expected <= col_names
