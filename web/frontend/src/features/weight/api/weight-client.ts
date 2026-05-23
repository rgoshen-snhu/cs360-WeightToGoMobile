/**
 * Typed wrappers around the /api/v1/weight-entries endpoints.
 *
 * Each method delegates to fetchJson so that requests benefit from the
 * same credential inclusion and error handling as all other API calls.
 */

import { fetchJson } from '../../../lib/api-client';

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
}

/** Paginated list response envelope for weight entries. */
export interface WeightEntryListResponse {
  items: WeightEntryRecord[];
  next_cursor: number | null;
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
  /** Return a paginated list of weight entries for the current user. */
  list: (params?: { limit?: number; cursor?: number }) => {
    const search = new URLSearchParams();
    if (params?.limit !== undefined) search.set('limit', String(params.limit));
    if (params?.cursor !== undefined) search.set('cursor', String(params.cursor));
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
