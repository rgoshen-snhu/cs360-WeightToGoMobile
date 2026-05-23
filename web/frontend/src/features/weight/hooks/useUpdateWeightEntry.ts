import { useMutation, useQueryClient } from '@tanstack/react-query';
import { type WeightEntryPayload, weightClient } from '../api/weight-client';
import { WEIGHT_ENTRIES_KEY } from './useWeightEntries';
import { DASHBOARD_SUMMARY_KEY } from '../../dashboard/hooks/useDashboardSummary';

interface UpdateArgs {
  entryId: number;
  payload: WeightEntryPayload;
}

/** Update an existing weight entry and invalidate related queries on success. */
export function useUpdateWeightEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ entryId, payload }: UpdateArgs) => weightClient.update(entryId, payload),
    onSuccess: (_data, { entryId }) => {
      void queryClient.invalidateQueries({ queryKey: WEIGHT_ENTRIES_KEY });
      void queryClient.invalidateQueries({ queryKey: ['weight-entries', entryId] });
      void queryClient.invalidateQueries({ queryKey: DASHBOARD_SUMMARY_KEY });
    },
  });
}
