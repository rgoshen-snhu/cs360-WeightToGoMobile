"""FastAPI router for /api/v1/preferences endpoints (FR-P-1, FR-P-3).

GET  /api/v1/preferences          — return the fully-resolved preference set.
PUT  /api/v1/preferences/{key}    — update one preference; return full resolved set.

Authentication: every endpoint requires a valid access-token cookie.
IDOR: user_id derives from the token — no user-id in any path parameter.
Rate-limit: 30 requests/minute per user (matching the goals router).
"""

from __future__ import annotations

import structlog
from fastapi import APIRouter, Depends, Request, status
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session

from weighttogo.auth.interface.router import get_current_user_id, limiter
from weighttogo.preferences.application.get_preferences import GetPreferences, GetPreferencesCommand
from weighttogo.preferences.application.set_preference import SetPreference, SetPreferenceCommand
from weighttogo.preferences.domain.entities import PreferenceKey
from weighttogo.preferences.domain.exceptions import InvalidPreferenceValueError
from weighttogo.preferences.infrastructure.repositories import SqlAlchemyPreferenceRepository
from weighttogo.preferences.interface.schemas import (
    PreferencesResponse,
    UpdatePreferenceRequest,
    preferences_response_from_dict,
)
from weighttogo.shared.db import get_db_session
from weighttogo.shared.problem_detail import build_problem_detail

logger = structlog.stdlib.get_logger(__name__)

router = APIRouter(prefix="/preferences", tags=["preferences"])


def _pref_repo(session: Session) -> SqlAlchemyPreferenceRepository:
    return SqlAlchemyPreferenceRepository(session)


@router.get(
    "",
    status_code=status.HTTP_200_OK,
    response_model=PreferencesResponse,
    summary="Get all preferences for the current user (FR-P-1, FR-P-3)",
)
@limiter.limit("30/minute")
def get_preferences(
    request: Request,
    session: Session = Depends(get_db_session),
    current_user_id: int = Depends(get_current_user_id),
) -> PreferencesResponse:
    """Return the fully-resolved preference set, merging lazy defaults.

    Args:
        request: The incoming HTTP request (required by slowapi limiter).
        session: The active database session.
        current_user_id: The authenticated user's ID.

    Returns:
        All four preferences with defaults applied for any missing rows.
    """
    resolved = GetPreferences(_pref_repo(session)).execute(
        GetPreferencesCommand(user_id=current_user_id)
    )
    logger.info("preferences_fetched", user_id=current_user_id)
    return preferences_response_from_dict(resolved)


@router.put(
    "/{key}",
    status_code=status.HTTP_200_OK,
    response_model=PreferencesResponse,
    summary="Update one preference; return the full resolved set (FR-P-1, FR-P-3)",
)
@limiter.limit("30/minute")
def update_preference(
    key: PreferenceKey,
    body: UpdatePreferenceRequest,
    request: Request,
    session: Session = Depends(get_db_session),
    current_user_id: int = Depends(get_current_user_id),
) -> PreferencesResponse | JSONResponse:
    """Update a single preference and return all four resolved values.

    Unknown keys yield 422 automatically (PreferenceKey path-param type).
    Invalid values yield 422 via InvalidPreferenceValueError.

    Args:
        key: The preference key to update (path parameter).
        body: Request body containing the new value.
        request: The incoming HTTP request (required by slowapi limiter).
        session: The active database session.
        current_user_id: The authenticated user's ID.

    Returns:
        The fully-resolved preference set after the write.
    """
    try:
        resolved = SetPreference(_pref_repo(session)).execute(
            SetPreferenceCommand(user_id=current_user_id, key=key, value=body.value)
        )
    except InvalidPreferenceValueError as exc:
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content=build_problem_detail(
                status=422,
                title="Validation failed",
                detail=str(exc),
                instance=request.url.path,
                errors=[
                    {"field": "value", "code": "invalid_preference_value", "message": str(exc)}
                ],
                request_id=request.headers.get("x-request-id"),
            ),
        )

    logger.info("preference_updated", key=key, user_id=current_user_id)
    return preferences_response_from_dict(resolved)
