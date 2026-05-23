import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import * as weightClientModule from '../api/weight-client';
import { useUpdateWeightEntry } from './useUpdateWeightEntry';

const mockEntry: weightClientModule.WeightEntryRecord = {
  entry_id: 1,
  weight_value: 185,
  weight_unit: 'lbs',
  observation_date: '2026-05-20',
  notes: null,
  created_at: '2026-05-20T12:00:00Z',
  updated_at: '2026-05-21T12:00:00Z',
};

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('useUpdateWeightEntry', () => {
  beforeEach(() => {
    vi.spyOn(weightClientModule.weightClient, 'update').mockResolvedValue(mockEntry);
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('calls weightClient.update with entryId and payload', async () => {
    const { result } = renderHook(() => useUpdateWeightEntry(), { wrapper });
    const payload = { weight_value: 185, weight_unit: 'lbs', observation_date: '2026-05-20' };
    result.current.mutate({ entryId: 1, payload });
    await waitFor(() =>
      expect(weightClientModule.weightClient.update).toHaveBeenCalledWith(1, payload),
    );
  });
});
