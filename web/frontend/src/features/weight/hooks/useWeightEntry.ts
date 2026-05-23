import { useQuery } from '@tanstack/react-query';
import { weightClient } from '../api/weight-client';

export const weightEntryKey = (id: number) => ['weight-entries', id] as const;

/** Return a single weight entry by ID. Pass null to disable the query. */
export function useWeightEntry(entryId: number | null) {
  return useQuery({
    queryKey: entryId !== null ? weightEntryKey(entryId) : ['weight-entries', null],
    queryFn: () => weightClient.get(entryId!),
    enabled: entryId !== null,
  });
}
