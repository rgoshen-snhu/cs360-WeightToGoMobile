/**
 * Typed wrappers around the /api/v1/auth/* endpoints.
 *
 * Each method delegates to fetchJson so that auth requests benefit from the
 * same error handling and credential inclusion as every other API call.
 *
 * SRS §5 (FR-A-1 … FR-A-4) defines the endpoints this module wraps.
 */

import { fetchJson } from '../../../lib/api-client';

/** User record returned by the auth endpoints. */
export interface AuthUser {
  user_id: number;
  email: string;
  display_name: string;
  created_at: string;
}

/** Payload sent to POST /api/v1/auth/register. */
export interface RegisterPayload {
  email: string;
  password: string;
  displayName: string;
}

/** Payload sent to POST /api/v1/auth/login. */
export interface LoginPayload {
  email: string;
  password: string;
}

const BASE = '/api/v1/auth';

/** Typed API wrappers for all auth endpoints. */
export const authClient = {
  /**
   * Register a new user account.
   *
   * Maps camelCase `displayName` to the snake_case `display_name` field the
   * API expects.
   */
  register: (p: RegisterPayload) =>
    fetchJson<AuthUser>(`${BASE}/register`, {
      method: 'POST',
      body: { email: p.email, password: p.password, display_name: p.displayName },
    }),

  /** Authenticate with email + password and set the session cookie. */
  login: (p: LoginPayload) => fetchJson<AuthUser>(`${BASE}/login`, { method: 'POST', body: p }),

  /** Invalidate the current session and clear the cookie. */
  logout: () => fetchJson<void>(`${BASE}/logout`, { method: 'POST' }),

  /** Exchange the refresh cookie for a new access token. */
  refresh: () => fetchJson<AuthUser>(`${BASE}/refresh`, { method: 'POST' }),

  /** Return the currently authenticated user. */
  me: () => fetchJson<AuthUser>(`${BASE}/me`),
};
