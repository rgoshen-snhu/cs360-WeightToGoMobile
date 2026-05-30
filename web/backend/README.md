# Weigh to Go! — Backend

FastAPI + Python service over PostgreSQL, organized in a three-pattern
architecture (Screaming + Clean + Hexagonal; see
[ADR-0012](../../docs/adr/0012-three-pattern-backend-architecture.md)). The
authoritative specification is the
[SRS](../../docs/specs/WeighToGo_Web_SRS_v2.md) — architecture (§4), API (§9),
database schema (§8), and quality engineering (§11).

## Stack

- Python 3.12+ · FastAPI · Pydantic 2 · SQLAlchemy 2.0 · Alembic
- PostgreSQL 16 (via Docker) · [uv](https://docs.astral.sh/uv/) for dependencies
- pytest (coverage) · ruff (lint/format) · mypy (types)

## Prerequisites

Python 3.12+, uv, and Docker. PostgreSQL must be running before applying
migrations.

## Commands

```bash
cp .env.example .env                           # adjust values as needed
docker compose up -d                           # local PostgreSQL
uv sync                                         # install dependencies
uv run alembic upgrade head                     # apply migrations
uv run uvicorn weighttogo.main:app --reload     # http://localhost:8000
```

`GET /health` reports service status.

Quality gates (also run by pre-commit and CI):

```bash
uv run ruff check .
uv run ruff format --check .
uv run mypy
uv run pytest --cov=weighttogo
```

## Tests

592 pytest tests at 97% coverage. Coverage thresholds are enforced in CI.

## Documentation

- API contract: [OpenAPI snapshot](../../docs/api/openapi.json)
- Engineering decisions: [ADR index](../../docs/adr/README.md)
