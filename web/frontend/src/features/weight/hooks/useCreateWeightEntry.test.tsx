import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import * as weightClientModule from '../api/weight-client';
import { useCreateWeightEntry } from './useCreateWeightEntry';

const mockEntry: weightClientModule.WeightEntryRecord = {
  entry_id: 2,
  weight_value: 180,
  weight_unit: 'lbs',
  observation_date: '2026-05-21',
  notes: null,
  created_at: '2026-05-21T12:00:00Z',
  updated_at: '2026-05-21T12:00:00Z',
};

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('useCreateWeightEntry', () => {
  beforeEach(() => {
    vi.spyOn(weightClientModule.weightClient, 'create').mockResolvedValue(mockEntry);
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('calls weightClient.create on mutate', async () => {
    const { result } = renderHook(() => useCreateWeightEntry(), { wrapper });
    const payload = { weight_value: 180, weight_unit: 'lbs', observation_date: '2026-05-21' };
    result.current.mutate(payload);
    await waitFor(() =>
      expect(weightClientModule.weightClient.create).toHaveBeenCalledWith(payload),
    );
  });
});
