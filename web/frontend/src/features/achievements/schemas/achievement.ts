import { z } from 'zod';

export const achievementSchema = z.object({
  achievement_id: z.number(),
  goal_id: z.number(),
  achievement_type: z.enum(['goal_reached', 'milestone']),
  threshold: z.union([z.string(), z.number()]).nullable(),
  earned_at: z.string(),
});

export const achievementListSchema = z.object({
  items: z.array(achievementSchema),
});

export type AchievementRecord = z.infer<typeof achievementSchema>;
export type AchievementListResponse = z.infer<typeof achievementListSchema>;
