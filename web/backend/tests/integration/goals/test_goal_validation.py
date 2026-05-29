"""Integration tests for goals endpoint input validation (422 cases)."""

from __future__ import annotations

from typing import Any

from fastapi.testclient import TestClient


def _register_and_login(client: TestClient) -> None:
    client.post(
        "/api/v1/auth/register",
        json={
            "email": "validation@example.com",
            "password": "ValidPass123!",
            "display_name": "Validation Tester",
        },
    )


def _post_goal(client: TestClient, payload: dict[str, Any]) -> int:
    return client.post("/api/v1/goals", json=payload).status_code


def _base(**overrides: Any) -> dict[str, Any]:
    base: dict[str, Any] = {
        "target_value": 150.0,
        "target_unit": "lbs",
        "start_value": 200.0,
        "goal_type": "lose",
        "target_date": None,
    }
    base.update(overrides)
    return base


# ── target_value validation ───────────────────────────────────────────────────


def test_create_goal_negative_target_value_returns_422(client: TestClient) -> None:
    _register_and_login(client)
    assert _post_goal(client, _base(target_value=-1.0)) == 422


def test_create_goal_zero_target_value_returns_422(client: TestClient) -> None:
    _register_and_login(client)
    assert _post_goal(client, _base(target_value=0.0)) == 422


def test_create_goal_over_max_target_value_returns_422(client: TestClient) -> None:
    _register_and_login(client)
    assert _post_goal(client, _base(target_value=1501.0)) == 422


# ── start_value validation ────────────────────────────────────────────────────


def test_create_goal_negative_start_value_returns_422(client: TestClient) -> None:
    _register_and_login(client)
    assert _post_goal(client, _base(start_value=-1.0)) == 422


def test_create_goal_over_max_start_value_returns_422(client: TestClient) -> None:
    _register_and_login(client)
    assert _post_goal(client, _base(start_value=1501.0)) == 422


# ── unit validation ───────────────────────────────────────────────────────────


def test_create_goal_bad_target_unit_returns_422(client: TestClient) -> None:
    _register_and_login(client)
    assert _post_goal(client, _base(target_unit="stones")) == 422


# ── goal_type validation ──────────────────────────────────────────────────────


def test_create_goal_bad_goal_type_returns_422(client: TestClient) -> None:
    _register_and_login(client)
    assert _post_goal(client, _base(goal_type="maintain")) == 422


# ── direction validation (model_validator) ────────────────────────────────────


def test_create_goal_target_equals_start_returns_422(client: TestClient) -> None:
    _register_and_login(client)
    assert _post_goal(client, _base(target_value=200.0, start_value=200.0)) == 422


def test_create_goal_lose_with_target_above_start_returns_422(client: TestClient) -> None:
    # Lose goal: target must be LESS than start
    _register_and_login(client)
    assert _post_goal(client, _base(goal_type="lose", target_value=210.0, start_value=200.0)) == 422


def test_create_goal_gain_with_target_below_start_returns_422(client: TestClient) -> None:
    # Gain goal: target must be GREATER than start
    _register_and_login(client)
    assert _post_goal(client, _base(goal_type="gain", target_value=150.0, start_value=200.0)) == 422


# ── PUT validation ────────────────────────────────────────────────────────────


def test_update_goal_negative_target_value_returns_422(client: TestClient) -> None:
    _register_and_login(client)
    resp = client.post("/api/v1/goals", json=_base())
    assert resp.status_code == 201
    goal_id = resp.json()["goal_id"]
    resp2 = client.put(f"/api/v1/goals/{goal_id}", json={"target_value": -5.0})
    assert resp2.status_code == 422
