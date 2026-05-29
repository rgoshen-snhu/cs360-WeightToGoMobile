import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import * as achClientModule from '../api/achievement-client';
import { AchievementsPage } from './AchievementsPage';

function wrapper({ children }: { children: ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('AchievementsPage', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the page heading', async () => {
    vi.spyOn(achClientModule.achievementClient, 'list').mockResolvedValue({ items: [] });
    render(<AchievementsPage />, { wrapper });
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: /achievements/i })).toBeInTheDocument(),
    );
  });

  it('shows empty-state message when no achievements', async () => {
    vi.spyOn(achClientModule.achievementClient, 'list').mockResolvedValue({ items: [] });
    render(<AchievementsPage />, { wrapper });
    await waitFor(() => expect(screen.getByText(/no achievements yet/i)).toBeInTheDocument());
  });

  it('renders an earned milestone achievement', async () => {
    vi.spyOn(achClientModule.achievementClient, 'list').mockResolvedValue({
      items: [
        {
          achievement_id: 1,
          goal_id: 7,
          achievement_type: 'milestone',
          threshold: 5,
          earned_at: '2026-01-01T00:00:00Z',
        },
      ],
    });
    render(<AchievementsPage />, { wrapper });
    await waitFor(() => expect(screen.getByText(/5 lb milestone/i)).toBeInTheDocument());
  });

  it('renders a goal_reached achievement with correct label', async () => {
    vi.spyOn(achClientModule.achievementClient, 'list').mockResolvedValue({
      items: [
        {
          achievement_id: 2,
          goal_id: 7,
          achievement_type: 'goal_reached',
          threshold: null,
          earned_at: '2026-01-01T00:00:00Z',
        },
      ],
    });
    render(<AchievementsPage />, { wrapper });
    await waitFor(() => expect(screen.getByText(/goal reached/i)).toBeInTheDocument());
  });
});
