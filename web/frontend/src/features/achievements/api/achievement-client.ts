/**
 * Typed wrappers around the /api/v1/achievements endpoints (SRS §9.7).
 */

import { fetchJson } from '../../../lib/api-client';
import type { AchievementListResponse, AchievementRecord } from '../schemas/achievement';

const BASE = '/api/v1/achievements';

export const achievementClient = {
  /** Return all achievements for the authenticated user, newest first. */
  list: () => fetchJson<AchievementListResponse>(BASE, { method: 'GET' }),

  /** Retrieve a single achievement by ID. Returns 404 if not owned by caller. */
  getById: (id: number) => fetchJson<AchievementRecord>(`${BASE}/${id}`, { method: 'GET' }),
};
