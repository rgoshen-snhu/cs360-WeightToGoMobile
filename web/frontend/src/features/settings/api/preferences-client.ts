/**
 * Typed wrappers around the /api/v1/preferences endpoints (SRS §9.x).
 */

import { fetchJson } from '../../../lib/api-client';
import type { PreferencesApiResponse } from '../schemas/preferences-schemas';

const BASE = '/api/v1/preferences';

export const preferencesClient = {
  /** Fetch all preferences for the authenticated user (lazy-defaulted on the server). */
  fetch: () => fetchJson<PreferencesApiResponse>(BASE, { method: 'GET' }),

  /** Update one preference by key and return the full resolved preference set. */
  update: (key: string, value: boolean | string) =>
    fetchJson<PreferencesApiResponse>(`${BASE}/${key}`, {
      method: 'PUT',
      body: { value },
    }),
};
