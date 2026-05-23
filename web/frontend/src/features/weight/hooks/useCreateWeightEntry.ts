import { useMutation, useQueryClient } from '@tanstack/react-query';
import { type WeightEntryPayload, weightClient } from '../api/weight-client';
import { WEIGHT_ENTRIES_KEY } from './useWeightEntries';
import { DASHBOARD_SUMMARY_KEY } from '../../dashboard/hooks/useDashboardSummary';

/** Create a new weight entry and invalidate related queries on success. */
export function useCreateWeightEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: WeightEntryPayload) => weightClient.create(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: WEIGHT_ENTRIES_KEY });
      void queryClient.invalidateQueries({ queryKey: DASHBOARD_SUMMARY_KEY });
    },
  });
}
