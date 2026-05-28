# Design: Consolidate ADR-0010 Auth-Safe Message Strings

- **Date**: 2026-05-28
- **Status**: Approved (design); pending implementation
- **Issue**: GH-42 — `refactor(auth): consolidate ADR-0010 auth-safe message strings into a shared module`
- **Parent**: GH-34 (M2 web-quality remediation)
- **Stack**: Web (frontend)
- **Type**: Refactor (no behavioral change)

## Problem

The user-visible, auth-safe error strings governed by **ADR-0010 (Generic
Authentication Error Policy)** are duplicated as inline string literals across
two hooks, with no shared constant to grep on or import from:

- `web/frontend/src/features/auth/hooks/useLogin.ts` — 4 strings
- `web/frontend/src/features/auth/hooks/useRegister.ts` — 2 strings (one shared
  with login)

Five distinct strings total. The M3 roadmap adds three more auth flows
(FR-A-6 password change, FR-A-7 password reset, FR-A-8 account deactivation)
that would each invent their own wording if the pattern is not fixed first.

Surfaced as Finding 2 in the PR #39 review; deferred from PR #39 to keep that
PR surgical.

## Goals

1. All five ADR-0010 strings live in a single module with named exports.
2. `useLogin` and `useRegister` import from that module — no inline message
   literals remain in either hook's `onError` branches.
3. Tests assert via imported constants, not re-typed inline strings, so wording
   changes propagate automatically.
4. `grep -rn "Invalid credentials\." web/frontend/src` returns one definition
   site plus test imports — no scattered duplicates.

## Non-Goals (YAGNI)

- **Do not** modify `web/frontend/src/lib/error-mapping.ts`. It is a *general*
  HTTP-status→message mapper whose 401/429 wording deliberately differs from the
  auth-safe family (`mapApiError(401)` = "Your session has expired…", vs. auth's
  "Invalid credentials."). The two concerns — general UX messaging vs. ADR-0010
  non-disclosure — must evolve independently. Merging them would couple
  unrelated policies and create two competing 401 messages in one file.
- **Do not** pre-build constants for the unbuilt M3 flows. They import the
  module when they are written.
- **Do not** add i18n/translation scaffolding. Out of scope.

## Key Findings From Code Review

- The auth logic is **not** a pure status→message lookup. `useLogin` branches on
  401/423/429 with distinct messages; `useRegister` collapses *every* ApiError
  status into one message (proven by its `it.each([409,401,423,429,500,503])`
  test). A status-keyed `Record` would misrepresent this — flat named constants
  are the correct shape.
- Existing tests assert inconsistently: exact `.toBe('Invalid credentials.')`
  for some cases, loose `.toMatch(/locked/i)`, `/too many/i`,
  `/something went wrong/i)` for others. Migrating to imported constants with
  exact `.toBe(CONST)` tightens the loose matchers into exact equality — a
  quality win at no extra cost.

## Design

### Module: `web/frontend/src/features/auth/messages.ts`

Flat named `export const`s (greppable, tree-shakeable, single source of truth),
with a module-level docstring citing ADR-0010 explicitly. Values are
byte-identical to the current inline literals — no behavior change.

| Constant | Value | Consumer |
|---|---|---|
| `AUTH_INVALID_CREDENTIALS` | `Invalid credentials.` | login 401 |
| `AUTH_ACCOUNT_LOCKED` | `Account is temporarily locked. Please try again later.` | login 423 |
| `AUTH_RATE_LIMITED` | `Too many attempts. Please wait a moment and try again.` | login 429 |
| `AUTH_REGISTER_FAILED` | `The account could not be created. Please try again.` | register (any ApiError) |
| `AUTH_GENERIC_FAILURE` | `Something went wrong. Please try again.` | login + register fallback |

### Consumers

- **`useLogin.ts`** — replace the four inline literals in the `onError`
  ApiError branch (401, 423, 429, `else`) and the non-ApiError fallback with the
  constants. The `else` branch and the non-ApiError branch both reference
  `AUTH_GENERIC_FAILURE`. The existing comment explaining why the password field
  is reset stays — it documents intent, not a message.
- **`useRegister.ts`** — replace `AUTH_REGISTER_FAILED` (the any-ApiError case)
  and `AUTH_GENERIC_FAILURE` (the non-ApiError fallback).

After migration, zero inline message literals remain in either hook's `onError`.

### Tests

Both test files import the same constants and assert with exact `.toBe(CONST)`:

- `useLogin.test.tsx` — 401 → `AUTH_INVALID_CREDENTIALS`; 423 →
  `AUTH_ACCOUNT_LOCKED`; 429 → `AUTH_RATE_LIMITED`; 500 and non-ApiError →
  `AUTH_GENERIC_FAILURE`. The `/locked/i`, `/too many/i`, `/something went
  wrong/i` regex matchers become exact equality against the constants.
- `useRegister.test.tsx` — `it.each([...])` asserts `.toBe(AUTH_REGISTER_FAILED)`;
  the network-error case asserts `.toBe(AUTH_GENERIC_FAILURE)`.

## TDD Cycle

Pure string-extraction has no new behavior, so the discipline is test-first
against the new module's existence; the existing suite is the safety net:

1. **RED** — point one test file's import at `../messages` and assert against a
   constant. Fails to resolve — module does not exist yet.
2. **GREEN** — create `messages.ts` with the five constants. Passes (the hook
   still emits the literal, which equals the constant value).
3. **REFACTOR** — migrate the hooks to import the constants; migrate the second
   test file. Suite stays green throughout.

Assertions check each hook's runtime output against the canonical constant, so
the tests are not tautological.

## Verification (Definition of Done)

- `grep -rn "Invalid credentials\." web/frontend/src` → one definition site
  (`messages.ts`) plus test imports; no scattered duplicates (AC #4).
- `pnpm lint`, formatter, `tsc` typecheck, and the full `vitest` suite green
  (Task 7).
- Commits follow the red → green → refactor split per project commit discipline.

## Risks & Dependencies

- Low risk: no behavioral change; values are byte-identical to current literals.
- No dependency on other in-flight work.
- Should land before the first M3 auth flow (password change / reset) to set the
  reuse pattern for new code.

## Affected Files

- **New**: `web/frontend/src/features/auth/messages.ts`
- **Modified**: `useLogin.ts`, `useRegister.ts`, `useLogin.test.tsx`,
  `useRegister.test.tsx` (all under `web/frontend/src/features/auth/hooks/`)
- **Untouched (by design)**: `web/frontend/src/lib/error-mapping.ts`

## Related

- ADR-0010 — Generic Authentication Error Policy (`docs/adr/0010-*.md`)
- SRS §6 FR-A-1, FR-A-2, FR-A-9 (current); FR-A-6, FR-A-7, FR-A-8 (M3 future)
- PR #39 review, Finding 2 (origin)
