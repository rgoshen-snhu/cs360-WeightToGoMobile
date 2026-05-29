"""FastAPI router for /api/v1/achievements endpoints (SRS §9.7)."""

from __future__ import annotations

import structlog
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from weighttogo.achievements.infrastructure.repositories import SqlAlchemyAchievementRepository
from weighttogo.achievements.interface.schemas import AchievementListResponse, AchievementResponse
from weighttogo.auth.interface.router import get_current_user_id
from weighttogo.shared.db import get_db_session

logger = structlog.stdlib.get_logger(__name__)

router = APIRouter(prefix="/achievements", tags=["achievements"])

_MAX_LIST = 50


def _ach_repo(session: Session) -> SqlAlchemyAchievementRepository:
    """Construct the repository for the current request."""
    return SqlAlchemyAchievementRepository(session)


@router.get(
    "",
    status_code=status.HTTP_200_OK,
    response_model=AchievementListResponse,
    summary="List achievements for the current user (FR-Ach-4)",
)
def list_achievements(
    session: Session = Depends(get_db_session),
    current_user_id: int = Depends(get_current_user_id),
) -> AchievementListResponse:
    """Return the most recent achievements for the authenticated user.

    Args:
        session: The active database session.
        current_user_id: The authenticated user's ID.

    Returns:
        List of up to 50 achievements, newest first.
    """
    achievements = _ach_repo(session).list_for_user(current_user_id, limit=_MAX_LIST)
    return AchievementListResponse(
        items=[AchievementResponse.model_validate(a) for a in achievements]
    )


@router.get(
    "/{achievement_id}",
    status_code=status.HTTP_200_OK,
    response_model=AchievementResponse,
    summary="Get a single achievement (IDOR-safe)",
)
def get_achievement(
    achievement_id: int,
    session: Session = Depends(get_db_session),
    current_user_id: int = Depends(get_current_user_id),
) -> AchievementResponse:
    """Retrieve a single achievement scoped to the authenticated user.

    Returns 404 for achievements owned by other users (IDOR guard per SRS §7).

    Args:
        achievement_id: The achievement primary key.
        session: The active database session.
        current_user_id: The authenticated user's ID.

    Returns:
        The matching achievement.

    Raises:
        HTTPException: 404 when not found or owned by another user.
    """
    ach = _ach_repo(session).get_by_id(achievement_id, current_user_id)
    if ach is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found.",
        )
    logger.info("achievement_fetched", achievement_id=achievement_id, user_id=current_user_id)
    return AchievementResponse.model_validate(ach)
