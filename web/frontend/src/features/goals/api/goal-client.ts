/**
 * Typed wrappers around the /api/v1/goals endpoints.
 */

import { fetchJson } from '../../../lib/api-client';

const BASE = '/api/v1/goals';

/** A goal record returned by the goals API. */
export interface GoalRecord {
  goal_id: number;
  user_id: number;
  target_value: number;
  target_unit: string;
  start_value: number;
  goal_type: string;
  target_date: string | null;
  is_active: boolean;
  is_achieved: boolean;
  achieved_at: string | null;
  created_at: string;
  updated_at: string;
}

/** Response from GET /goals/active — goal is null when none set. */
export interface ActiveGoalResponse {
  goal: GoalRecord | null;
  progress_percent: number | null;
  current_value: number | null;
}

/** Response from GET /goals. */
export interface GoalListResponse {
  goals: GoalRecord[];
}

/** Request body for POST /goals. */
export interface GoalPayload {
  goal_type: string;
  target_value: number;
  target_unit: string;
  start_value: number;
  target_date: string | null;
}

/** Request body for PUT /goals/{goal_id}. */
export interface GoalUpdatePayload {
  target_value: number;
  target_date: string | null;
}

/** Typed API wrappers for all goals endpoints. */
export const goalClient = {
  /** Create a new active goal. */
  create: (body: GoalPayload) => fetchJson<GoalRecord>(BASE, { method: 'POST', body }),

  /** Return the active goal with progress, or null-goal envelope. */
  getActive: () => fetchJson<ActiveGoalResponse>(`${BASE}/active`, { method: 'GET' }),

  /** Return goals (active and historical). Pass `{ history: true }` for past goals only. */
  list: (options?: { history?: boolean }) =>
    fetchJson<GoalListResponse>(options?.history ? `${BASE}?history=true` : BASE, {
      method: 'GET',
    }),

  /** Update target_value / target_date on an existing goal. */
  update: (goalId: number, body: GoalUpdatePayload) =>
    fetchJson<GoalRecord>(`${BASE}/${goalId}`, { method: 'PUT', body }),

  /** Abandon an active goal. Returns void (204 No Content). */
  abandon: (goalId: number) => fetchJson<void>(`${BASE}/${goalId}`, { method: 'DELETE' }),
};
