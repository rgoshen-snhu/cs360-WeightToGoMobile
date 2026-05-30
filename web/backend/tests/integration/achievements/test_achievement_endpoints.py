"""Integration tests for /api/v1/achievements endpoints (SRS §9.7)."""

from __future__ import annotations

from fastapi.testclient import TestClient


def _register_and_login(client: TestClient, email: str = "ach@example.com") -> None:
    client.post(
        "/api/v1/auth/register",
        json={"email": email, "password": "ValidPass123!", "display_name": "Ach User"},
    )


# ── Auth guard ────────────────────────────────────────────────────────────────


def test_list_achievements_requires_auth(client: TestClient) -> None:
    resp = client.get("/api/v1/achievements")
    assert resp.status_code == 401


def test_get_achievement_requires_auth(client: TestClient) -> None:
    resp = client.get("/api/v1/achievements/1")
    assert resp.status_code == 401


# ── Happy path ────────────────────────────────────────────────────────────────


def test_list_achievements_returns_empty_when_none_earned(client: TestClient) -> None:
    _register_and_login(client)
    resp = client.get("/api/v1/achievements")
    assert resp.status_code == 200
    assert resp.json() == {"items": []}


# ── IDOR guard ────────────────────────────────────────────────────────────────


def test_get_achievement_returns_404_for_nonexistent(client: TestClient) -> None:
    _register_and_login(client, "ach2@example.com")
    resp = client.get("/api/v1/achievements/99999")
    assert resp.status_code == 404


# ── Ordering (FR-Ach-4: sorted by date descending) ────────────────────────────


def test_list_achievements_returns_newest_first(client: TestClient) -> None:
    from datetime import date, timedelta

    _register_and_login(client, "ach-order@example.com")
    # Active lose goal from 200 lbs; milestones fire at 5 and 10 lb lost.
    client.post(
        "/api/v1/goals",
        json={
            "target_value": 150.0,
            "target_unit": "lbs",
            "start_value": 200.0,
            "goal_type": "lose",
            "target_date": None,
        },
    )
    earlier = (date.today() - timedelta(days=1)).isoformat()
    # First entry crosses the 5 lb milestone (195 lbs), earned first.
    client.post(
        "/api/v1/weight-entries",
        json={
            "weight_value": 195.0,
            "weight_unit": "lbs",
            "observation_date": earlier,
            "notes": None,
        },
    )
    # Second entry crosses the 10 lb milestone (190 lbs), earned later.
    client.post(
        "/api/v1/weight-entries",
        json={
            "weight_value": 190.0,
            "weight_unit": "lbs",
            "observation_date": date.today().isoformat(),
            "notes": None,
        },
    )

    resp = client.get("/api/v1/achievements")
    assert resp.status_code == 200
    items = resp.json()["items"]
    assert len(items) >= 2
    earned = [i["earned_at"] for i in items]
    assert earned == sorted(earned, reverse=True)
