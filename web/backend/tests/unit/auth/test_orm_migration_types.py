"""C15 regression: ORM column types must match the migration (BigInteger + UUID)."""

from sqlalchemy import BigInteger

from weighttogo.auth.infrastructure.models import RefreshTokenModel, UserModel


def test_user_id_is_biginteger() -> None:
    assert isinstance(UserModel.__table__.c.user_id.type, BigInteger)


def test_token_id_is_biginteger() -> None:
    assert isinstance(RefreshTokenModel.__table__.c.token_id.type, BigInteger)


def test_token_user_id_fk_is_biginteger() -> None:
    assert isinstance(RefreshTokenModel.__table__.c.user_id.type, BigInteger)


def test_token_replaced_by_fk_is_biginteger() -> None:
    assert isinstance(RefreshTokenModel.__table__.c.replaced_by.type, BigInteger)


def test_family_id_column_uses_uuid_type() -> None:
    """family_id column must use SQLAlchemy's Uuid type (not Text or String)."""
    from sqlalchemy import Uuid

    col_type = RefreshTokenModel.__table__.c.family_id.type
    assert isinstance(col_type, Uuid), f"expected Uuid type, got {type(col_type)}"
