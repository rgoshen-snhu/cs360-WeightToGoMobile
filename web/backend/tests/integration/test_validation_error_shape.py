"""Integration test for RFC 7807 validation error shape (SRS §9.2).

Verifies that a 422 response from the register endpoint conforms to the
RFC 7807 Problem Details shape that the frontend api-client expects, rather
than FastAPI's default ``{"detail": [...]}`` format.
"""

from fastapi.testclient import TestClient


def test_register_with_invalid_payload_returns_rfc_7807_shape(client: TestClient) -> None:
    response = client.post(
        "/api/v1/auth/register",
        json={"email": "not-an-email", "password": "short", "display_name": "x"},
    )
    assert response.status_code == 422
    body = response.json()
    assert body["status"] == 422
    assert body["title"] == "Validation failed"
    assert body["instance"] == "/api/v1/auth/register"
    assert isinstance(body["errors"], list)
    fields = {e["field"] for e in body["errors"]}
    assert {"email", "password", "display_name"}.issubset(fields)
    for e in body["errors"]:
        assert "code" in e and "message" in e
