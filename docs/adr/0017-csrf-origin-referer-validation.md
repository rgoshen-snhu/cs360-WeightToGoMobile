# ADR-0017: CSRF Origin/Referer Validation for State-Changing Requests

- **Date**: 2026-05-27
- **Status**: Accepted

## Context

SRS §NFR-S-9 requires server-side CSRF protection as a defense-in-depth layer beyond the `SameSite=Strict` cookie attribute. The M2 Web App Quality Review (2026-05-23) identified this as a blocking gap: `main.py` configures CORS and `SameSite=Strict` cookies but performs no server-side `Origin`/`Referer` validation on state-changing requests.

`SameSite=Strict` alone is strong but not universally sufficient:
- Some older browser versions do not enforce it.
- Cross-site form POST attacks from non-browser clients (e.g., curl, native apps) bypass cookie attribute enforcement.
- Defense-in-depth requires a server-side check independent of the cookie mechanism.

The existing codebase already has a `cors_allowed_origins` setting in `Settings` that defines which origins the API accepts requests from.

## Decision

Create a new `CsrfOriginRefererMiddleware` at `web/backend/src/weighttogo/interface/middleware/csrf.py` (Hexagonal architecture outer-layer adapter placement, matching `interface/` convention).

**Middleware behaviour:**

| Request method | Action |
|---|---|
| `GET`, `HEAD`, `OPTIONS` (safe) | Pass through unconditionally — no CSRF risk |
| `POST`, `PUT`, `DELETE`, `PATCH` (unsafe) | Validate `Origin` header; if absent, fall back to extracting the origin from `Referer`; if neither is present or the value does not match an allowed origin, return `403` |

**Allowed-origin list:** The union of two sets:
1. Origins parsed from `Settings.cors_allowed_origins` (the same comma-separated string consumed by `CORSMiddleware`) — single source of truth for configured allowed frontends.
2. The API's own origin, derived from the request's scheme and netloc (`request.url.scheme + "://" + request.url.netloc`) — permits same-origin flows such as Swagger UI at `/api/docs` posting back to the same host.

**Error response format:** RFC 7807 `application/problem+json`:
```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Origin or Referer required and must match an allowed origin."
}
```

**Middleware registration order:** Registered in `main.py` *after* `CORSMiddleware` in source order. Starlette applies `add_middleware` calls in reverse registration order (last call = outermost layer), so registering CSRF after CORS means CORS runs outermost and handles `OPTIONS` preflight before CSRF evaluates the request. CSRF independently exempts safe methods as a defensive redundancy.

## Rationale

**Why check Origin before Referer?**
`Origin` is the canonical header for cross-origin checks — it is always present on cross-origin requests and contains only the scheme + host + port without path. `Referer` is a fallback for environments where `Origin` may be stripped (some proxies, certain browser privacy settings), but it requires extracting the origin portion from a full URL. Checking `Origin` first and falling back to `Referer` maximises coverage.

**Why include the API's own origin in the allowed set?**
Same-origin requests are never CSRF attacks by definition — CSRF exploits cross-site trust, not same-site requests. When Swagger UI (served at `/api/docs` on the same host as the API) submits a form or AJAX call, the browser sends `Origin` or `Referer` pointing to the API host itself. Blocking those requests would break the interactive API documentation flow without providing any security benefit. The API's own origin is derived from each `Request` object (`scheme://netloc`) rather than from a static config so that the middleware is host-agnostic across development, staging, and production without additional configuration.

**Why reuse `cors_allowed_origins` instead of a separate CSRF allowlist?**
A separate allowlist would create a second place to configure allowed origins, which would inevitably drift out of sync with the CORS list. Reusing the same setting guarantees that any origin permitted by CORS is also permitted by CSRF validation, and vice versa.

**Why safe-method exemption?**
`GET`, `HEAD`, and `OPTIONS` do not change server state; CSRF attacks target state-changing operations. Checking `Origin` on read requests would break browser prefetch, health probes, and monitoring systems that do not send an `Origin` header.

**Why not double-submit cookie?**
The double-submit cookie pattern requires coordination between the frontend and backend to generate and validate a token. The `SameSite=Strict` + Origin/Referer approach achieves the same goal without additional token management complexity.

**Playwright E2E compatibility:** Playwright spawns Chromium, which sends `Origin: http://localhost:5173` on cross-origin requests. That origin is already in `cors_allowed_origins`, so the middleware is transparent to the E2E suite.

## Consequences

- **Positive**: Server-side CSRF protection independent of browser cookie enforcement. Single source of truth for allowed origins. No frontend changes required.
- **Negative**: Any POST/PUT/DELETE/PATCH request without a valid `Origin` or `Referer` header will receive a 403. This affects non-browser clients (e.g., API testing tools) that do not send an `Origin` header unless configured to do so.
- **Follow-ups**: ADR-0018 (concurrent refresh token coalescing) addresses the remaining ADR-0013 compliance gap identified in the same quality review.

## Alternatives Considered

- **Double-submit cookie** — rejected because it requires additional token generation and validation machinery coordinated across frontend and backend. `SameSite=Strict` + server-side Origin check achieves equivalent protection with less complexity.
- **Separate CSRF allowlist in `Settings`** — rejected because it creates a second place to configure allowed origins, which will drift out of sync with the CORS list.
- **Check only `Origin`, no `Referer` fallback** — rejected because some browser privacy configurations and corporate proxies strip the `Origin` header on same-origin navigation. The `Referer` fallback increases coverage with minimal added complexity.
- **Production-only enforcement** — rejected because enforcing CSRF only in production would allow integration tests to skip validation, reducing confidence that the protection works as intended.
