import { useQuery } from '@tanstack/react-query';
import { type GoalListResponse, goalClient } from '../api/goal-client';

export const GOALS_LIST_KEY = ['goals', 'list'] as const;

/**
 * Return goals for the current user. Pass `{ history: true }` for past
 * (achieved or abandoned) goals only — the FR-G-5 history view.
 *
 * The query key includes the `history` flag so the full list and the history
 * list cache independently. Mutation hooks invalidate the `GOALS_LIST_KEY`
 * prefix, which matches both variants.
 */
export function useGoals(options?: { history?: boolean }) {
  const history = options?.history ?? false;
  return useQuery<GoalListResponse>({
    queryKey: [...GOALS_LIST_KEY, { history }],
    queryFn: () => goalClient.list({ history }),
  });
}
