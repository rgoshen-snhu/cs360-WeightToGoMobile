/**
 * Zod schemas and camelCase↔snake_case mappers for the preferences API [G12].
 *
 * The API speaks snake_case; the context and components speak camelCase.
 * All translation happens here — no implicit field-name coincidences elsewhere.
 */

import { z } from 'zod';

/** Zod schema for the snake_case API response shape (GET + PUT /api/v1/preferences). */
export const preferencesApiSchema = z.object({
  weight_unit: z.enum(['lbs', 'kg']),
  notify_achievement: z.boolean(),
  notify_milestone: z.boolean(),
  notify_streak: z.boolean(),
});

/** Typed API response (snake_case). */
export type PreferencesApiResponse = z.infer<typeof preferencesApiSchema>;

/** Application-level camelCase preferences shape. */
export interface Preferences {
  weightUnit: 'lbs' | 'kg';
  notifyAchievement: boolean;
  notifyMilestone: boolean;
  notifyStreak: boolean;
}

/** Map the API snake_case response to the camelCase Preferences shape. */
export function apiToPreferences(api: PreferencesApiResponse): Preferences {
  return {
    weightUnit: api.weight_unit,
    notifyAchievement: api.notify_achievement,
    notifyMilestone: api.notify_milestone,
    notifyStreak: api.notify_streak,
  };
}

/** Map the camelCase Preferences shape back to snake_case for the PUT body. */
export function preferencesToApi(prefs: Preferences): PreferencesApiResponse {
  return {
    weight_unit: prefs.weightUnit,
    notify_achievement: prefs.notifyAchievement,
    notify_milestone: prefs.notifyMilestone,
    notify_streak: prefs.notifyStreak,
  };
}

/** Default preferences used while the query is loading. */
export const DEFAULT_PREFERENCES: Preferences = {
  weightUnit: 'lbs',
  notifyAchievement: true,
  notifyMilestone: true,
  notifyStreak: true,
};
