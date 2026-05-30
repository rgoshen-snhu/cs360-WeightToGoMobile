# Weigh to Go! — Frontend

React + TypeScript single-page application (Vite), with Material UI components,
TanStack Query for server state, and Playwright for end-to-end tests. The
component specification, architecture, and quality gates are defined in
[SRS §10](../../docs/specs/WeighToGo_Web_SRS_v2.md).

## Stack

- React 19 · TypeScript 6 (strict mode) · Vite 8
- Material UI 9 · TanStack Query 5 · Recharts 3
- Vitest (unit/component) · Playwright (E2E)

## Prerequisites

Node.js 20.19+ or 22+. The backend must be running for full end-to-end behavior
(see [`../backend/README.md`](../backend/README.md)).

## Commands

```bash
npm install
cp .env.example .env       # adjust values as needed
npm run dev                # Vite dev server (http://localhost:5173)
npm run build              # production build
```

Quality gates (also run by pre-commit and CI):

```bash
npm run lint               # eslint
npm run format:check       # prettier (write with `npm run format`)
npm run typecheck          # tsc
npm run test:ci            # vitest with coverage
npm run test:e2e           # playwright (requires backend running)
```

## Tests

377 Vitest unit/component tests and 19 Playwright E2E specs. Coverage thresholds
are enforced in CI.
