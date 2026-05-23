"""C3 regression: RefreshSession must populate replaced_by on the old token."""

import uuid
from datetime import UTC, datetime, timedelta
from unittest.mock import MagicMock

from weighttogo.auth.application.refresh_session import RefreshSession, RefreshSessionCommand
from weighttogo.auth.domain.entities import RefreshToken


def _make_token(token_id: int = 1, family_id: uuid.UUID | None = None) -> RefreshToken:
    return RefreshToken(
        token_id=token_id,
        user_id=42,
        token_hash="oldhash",
        family_id=family_id or uuid.uuid4(),
        expires_at=datetime.now(UTC) + timedelta(days=7),
    )


def _make_jwt_adapter(new_token_id: int = 2) -> MagicMock:
    adapter = MagicMock()
    adapter.hash_token.return_value = "oldhash"
    adapter.issue_access_token.return_value = "access_jwt"
    adapter.issue_refresh_token.return_value = ("raw_new", "newhash")
    return adapter


def _make_token_repo(old_token: RefreshToken, new_token_id: int = 2) -> MagicMock:
    repo = MagicMock()
    repo.get_by_hash_for_update.return_value = old_token

    saved_tokens: list[RefreshToken] = []

    def save_side_effect(tok: RefreshToken) -> RefreshToken:
        if tok.token_id is None:
            # new token — assign a token_id
            object.__setattr__(tok, "token_id", new_token_id)
        saved_tokens.append(tok)
        return tok

    repo.save.side_effect = save_side_effect
    repo._saved = saved_tokens
    return repo


def test_refresh_session_sets_replaced_by_on_old_token() -> None:
    """After rotation the old token's replaced_by must equal the new token's token_id."""
    old_token = _make_token(token_id=1)
    token_repo = _make_token_repo(old_token, new_token_id=99)
    jwt_adapter = _make_jwt_adapter()

    use_case = RefreshSession(
        jwt_adapter=jwt_adapter,
        token_repo=token_repo,
        refresh_ttl_days=7,
        access_ttl_minutes=15,
    )
    use_case.execute(RefreshSessionCommand(raw_refresh_token="rawold"))

    # The old token must have replaced_by set to the new token's id
    assert old_token.replaced_by == 99, f"expected replaced_by=99, got {old_token.replaced_by!r}"
