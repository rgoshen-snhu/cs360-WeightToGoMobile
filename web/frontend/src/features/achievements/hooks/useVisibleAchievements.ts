/**
 * Single toggle-enforcement home for achievement notifications [G10].
 *
 * Filters the achievements queue against the user's notify preferences.
 * One home → SRP; independently unit-testable without rendering a toast.
 *
 * notify_streak is stored-but-inert (no streak producer in Phase 3).
 */

import { useMemo } from 'react';
import { usePreferences } from '../../../contexts/PreferencesContext';
import type { AchievementRecord } from '../schemas/achievement';

/**
 * Return the subset of achievements whose notification toggle is enabled.
 *
 * @param achievements - The raw queue from the weight-entry response.
 * @returns Filtered queue; empty array when all relevant toggles are off.
 */
export function useVisibleAchievements(achievements: AchievementRecord[]): AchievementRecord[] {
  const { preferences } = usePreferences();

  return useMemo(
    () =>
      achievements.filter((a) => {
        if (a.achievement_type === 'goal_reached') return preferences.notifyAchievement;
        if (a.achievement_type === 'milestone') return preferences.notifyMilestone;
        return true;
      }),
    [achievements, preferences.notifyAchievement, preferences.notifyMilestone],
  );
}
