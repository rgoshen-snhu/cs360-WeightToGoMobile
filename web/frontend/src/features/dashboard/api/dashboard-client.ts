/**
 * Typed wrapper around the GET /api/v1/dashboard/summary endpoint.
 */

import { fetchJson } from '../../../lib/api-client';
import type { WeightEntryRecord } from '../../weight/api/weight-client';

const BASE = '/api/v1/dashboard';

/** Dashboard summary response from the API. */
export interface DashboardSummaryResponse {
  latest_entry: WeightEntryRecord | null;
  total_entries: number;
  active_goal: null;
}

/** Typed API wrapper for the dashboard summary endpoint. */
export const dashboardClient = {
  /** Return the dashboard summary for the current user. */
  summary: () => fetchJson<DashboardSummaryResponse>(`${BASE}/summary`, { method: 'GET' }),
};
