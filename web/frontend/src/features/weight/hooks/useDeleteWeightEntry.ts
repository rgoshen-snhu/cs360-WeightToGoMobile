import { useMutation, useQueryClient } from '@tanstack/react-query';
import { weightClient } from '../api/weight-client';
import { WEIGHT_ENTRIES_KEY } from './useWeightEntries';
import { DASHBOARD_SUMMARY_KEY } from '../../dashboard/hooks/useDashboardSummary';

/** Soft-delete a weight entry and invalidate related queries on success. */
export function useDeleteWeightEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (entryId: number) => weightClient.remove(entryId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: WEIGHT_ENTRIES_KEY });
      void queryClient.invalidateQueries({ queryKey: DASHBOARD_SUMMARY_KEY });
    },
  });
}
