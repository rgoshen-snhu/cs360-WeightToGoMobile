/**
 * Thin fetch wrapper for the Weigh to Go! API.
 *
 * Enforces JSON Content-Type on outgoing requests, parses response bodies as
 * JSON, and throws typed errors for any non-2xx response so callers can rely
 * on a single error-handling path.
 *
 * Supports:
 * - credentials: include on every request (SRS §10.3 – cookie-based auth)
 * - RFC 7807 Problem Details parsing for 422 responses
 * - Automatic token-refresh on 401 via installAuthRefreshInterceptor
 * - Request cancellation via AbortController signal
 *
 * SRS §10.3 governs the API client design.
 */

import { mapApiError, mapValidationErrors } from './error-mapping';

/**
 * Error thrown when the API returns a non-2xx response.
 */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/**
 * Error thrown when the API returns a 422 Unprocessable Entity response.
 * Carries a field-keyed map of validation messages.
 */
export class ValidationError extends ApiError {
  constructor(
    public readonly fieldErrors: Record<string, string>,
    message: string,
  ) {
    super(422, message);
    this.name = 'ValidationError';
  }
}

/** Subset of RFC 7807 Problem Details relevant to this client. */
export interface RfcProblem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: Array<{ field: string; code: string; message: string }>;
}

interface FetchJsonOptions {
  method?: string;
  body?: unknown;
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

interface AuthInterceptor {
  /** Called when a 401 is received on a non-refresh endpoint.  Should call
   *  the refresh endpoint and update any auth state.  Throw to signal that
   *  refresh itself failed. */
  refresh: () => Promise<void>;
  /** Called when the session cannot be recovered (refresh failed, or the
   *  failing request was itself the refresh endpoint). */
  onLogout: () => void;
}

let interceptor: AuthInterceptor | null = null;

/**
 * Install an interceptor that will attempt a token refresh when any request
 * returns 401.  Only one interceptor is active at a time.
 */
export function installAuthRefreshInterceptor(i: AuthInterceptor): void {
  interceptor = i;
}

/**
 * Remove the active interceptor.  Useful in tests.
 */
export function resetAuthRefreshInterceptor(): void {
  interceptor = null;
}

async function handle401AndRetry<T>(url: string, init: RequestInit): Promise<T> {
  const isRefreshEndpoint = url.includes('/api/v1/auth/refresh');
  if (!interceptor || isRefreshEndpoint) {
    interceptor?.onLogout();
    throw new ApiError(401, mapApiError(401));
  }
  try {
    await interceptor.refresh();
  } catch {
    interceptor.onLogout();
    throw new ApiError(401, mapApiError(401));
  }
  const retry = await fetch(url, init);
  if (retry.ok) {
    if (retry.status === 204) return undefined as T;
    return (await retry.json()) as T;
  }
  if (retry.status === 422) {
    const problem = (await retry.json().catch(() => ({}))) as RfcProblem;
    throw new ValidationError(
      mapValidationErrors(problem.errors ?? []),
      problem.detail ?? mapApiError(422),
    );
  }
  throw new ApiError(retry.status, mapApiError(retry.status));
}

/**
 * Fetch a JSON resource from the given URL.
 *
 * Automatically sets `Content-Type: application/json`, includes credentials,
 * and serialises the `body` option to JSON.
 *
 * @param url     - Absolute or relative URL to fetch.
 * @param options - Optional fetch options (method, body, additional headers, signal).
 *
 * @throws {ValidationError} When the response status is 422 with field errors.
 * @throws {ApiError}        When the response status is any other non-2xx code.
 */
export async function fetchJson<T = unknown>(
  url: string,
  options: FetchJsonOptions = {},
): Promise<T> {
  const { method = 'GET', body, headers = {}, signal } = options;
  const init: RequestInit = {
    method,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...headers },
    signal,
  };
  if (body !== undefined) init.body = JSON.stringify(body);

  const res = await fetch(url, init);

  if (res.ok) {
    if (res.status === 204) return undefined as T;
    return (await res.json()) as T;
  }

  if (res.status === 401) return handle401AndRetry<T>(url, init);

  if (res.status === 422) {
    const problem = (await res.json().catch(() => ({}))) as RfcProblem;
    throw new ValidationError(
      mapValidationErrors(problem.errors ?? []),
      problem.detail ?? mapApiError(422),
    );
  }

  throw new ApiError(res.status, mapApiError(res.status));
}
