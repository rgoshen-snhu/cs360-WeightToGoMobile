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
