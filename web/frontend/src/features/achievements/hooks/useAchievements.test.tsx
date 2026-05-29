import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import * as achClientModule from '../api/achievement-client';
import { useAchievements } from './useAchievements';

const mockResponse = { items: [] };

function wrapper({ children }: { children: ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('useAchievements', () => {
  beforeEach(() => {
    vi.spyOn(achClientModule.achievementClient, 'list').mockResolvedValue(mockResponse);
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('calls achievementClient.list', async () => {
    const { result } = renderHook(() => useAchievements(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(achClientModule.achievementClient.list).toHaveBeenCalled();
  });

  it('returns empty items when no achievements', async () => {
    const { result } = renderHook(() => useAchievements(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.items).toEqual([]);
  });
});
