/**
 * Typed wrappers around the /api/v1/weight-entries endpoints.
 *
 * Each method delegates to fetchJson so that requests benefit from the
 * same credential inclusion and error handling as all other API calls.
 */

import { fetchJson } from '../../../lib/api-client';
import type { AchievementRecord } from '../../achievements/schemas/achievement';

const BASE = '/api/v1/weight-entries';

/** A weight entry record returned by the weight-entries API. */
export interface WeightEntryRecord {
  entry_id: number;
  weight_value: number;
  weight_unit: string;
  observation_date: string;
  notes: string | null;
  created_at: string;
  updated_at: string;
  /** Achievements earned by this entry (empty array when none or no active goal). */
  newly_earned_achievements?: AchievementRecord[];
}

/** Paginated list response envelope for weight entries.
 *
 * `next_cursor` is an opaque base64 token returned by the backend (ADR-0015).
 * Clients must round-trip the value unchanged to fetch the next page; the
 * format is intentionally not part of the API contract.
 */
export interface WeightEntryListResponse {
  items: WeightEntryRecord[];
  next_cursor: string | null;
}

/** Request body for POST and PUT weight-entry endpoints. */
export interface WeightEntryPayload {
  weight_value: number;
  weight_unit: string;
  observation_date: string;
  notes?: string;
}

/** Typed API wrappers for all weight-entries endpoints. */
export const weightClient = {
  /** Return a paginated list of weight entries for the current user.
   *
   * `cursor` is the opaque token returned as `next_cursor` on a previous
   * page; pass it back unchanged to fetch the next page.
   */
  list: (params?: { limit?: number; cursor?: string }) => {
    const search = new URLSearchParams();
    if (params?.limit !== undefined) search.set('limit', String(params.limit));
    if (params?.cursor !== undefined) search.set('cursor', params.cursor);
    const qs = search.toString();
    return fetchJson<WeightEntryListResponse>(qs ? `${BASE}?${qs}` : BASE, { method: 'GET' });
  },

  /** Return a single weight entry by ID. */
  get: (entryId: number) => fetchJson<WeightEntryRecord>(`${BASE}/${entryId}`, { method: 'GET' }),

  /** Create a new weight entry. */
  create: (body: WeightEntryPayload) =>
    fetchJson<WeightEntryRecord>(BASE, { method: 'POST', body }),

  /** Update an existing weight entry. */
  update: (entryId: number, body: WeightEntryPayload) =>
    fetchJson<WeightEntryRecord>(`${BASE}/${entryId}`, { method: 'PUT', body }),

  /** Soft-delete a weight entry. Returns void (204 No Content). */
  remove: (entryId: number) => fetchJson<void>(`${BASE}/${entryId}`, { method: 'DELETE' }),
};
