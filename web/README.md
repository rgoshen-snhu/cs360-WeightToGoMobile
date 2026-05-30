# Weigh to Go! — Web

The full-stack web rebuild of *Weigh to Go!* (CS 499 capstone): a React +
TypeScript frontend and a FastAPI + Python backend over PostgreSQL. The
authoritative specification — architecture, requirements, API, and quality
gates — is the
[Software Requirements Specification](../docs/specs/WeighToGo_Web_SRS_v2.md).

## Layout

```
web/
├── frontend/   # React + TypeScript (Vite)  — see frontend/README.md
└── backend/    # FastAPI + Python (PostgreSQL) — see backend/README.md
```

## Milestone status

- **Milestone 2** — authentication + weight-entry vertical slice + dashboard (`v0.1.0`)
- **Milestone 3** — goals, achievements (milestone & streak detection),
  preferences, dashboard aggregation, trend analytics, composite indexes, and
  TTL caching (`v0.2.0`)
- **Milestone 4** — database enhancements (upcoming)

## Running locally

Both servers must run. See [`backend/README.md`](backend/README.md) and
[`frontend/README.md`](frontend/README.md) for prerequisites and details.

```bash
# Terminal 1 — backend (from web/backend)
docker compose up -d                          # PostgreSQL
uv sync
uv run alembic upgrade head
uv run uvicorn weighttogo.main:app --reload   # http://localhost:8000

# Terminal 2 — frontend (from web/frontend)
npm install
npm run dev                                   # http://localhost:5173
```

## Documentation

- [Software Requirements Specification](../docs/specs/WeighToGo_Web_SRS_v2.md)
- [ADR index](../docs/adr/README.md) · [DDR index](../docs/ddr/README.md)
- [OpenAPI snapshot](../docs/api/openapi.json)
