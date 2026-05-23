import { useQuery } from '@tanstack/react-query';
import { weightClient } from '../api/weight-client';

export const WEIGHT_ENTRIES_KEY = ['weight-entries'] as const;

/** Return a paginated list of weight entries for the current user. */
export function useWeightEntries(params?: { limit?: number; cursor?: number }) {
  return useQuery({
    queryKey: [...WEIGHT_ENTRIES_KEY, params],
    queryFn: () => weightClient.list(params),
  });
}
