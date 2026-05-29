import { useQuery } from '@tanstack/react-query';
import { achievementClient } from '../api/achievement-client';
import type { AchievementListResponse } from '../schemas/achievement';

export const ACHIEVEMENTS_KEY = ['achievements'] as const;

/** Return all achievements for the authenticated user. */
export function useAchievements() {
  return useQuery<AchievementListResponse>({
    queryKey: ACHIEVEMENTS_KEY,
    queryFn: () => achievementClient.list(),
  });
}
