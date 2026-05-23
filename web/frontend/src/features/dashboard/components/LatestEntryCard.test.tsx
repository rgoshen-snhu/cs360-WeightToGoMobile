import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import type { WeightEntryRecord } from '../../weight/api/weight-client';
import { LatestEntryCard } from './LatestEntryCard';

const entry: WeightEntryRecord = {
  entry_id: 1,
  weight_value: 175.5,
  weight_unit: 'lbs',
  observation_date: '2026-05-20',
  notes: null,
  created_at: '2026-05-20T12:00:00Z',
  updated_at: '2026-05-20T12:00:00Z',
};

describe('LatestEntryCard', () => {
  it('renders loading skeleton when isLoading=true', () => {
    render(<LatestEntryCard entry={undefined} isLoading={true} />);
    expect(document.querySelector('.MuiSkeleton-root')).toBeInTheDocument();
  });

  it('renders weight value when entry is provided', () => {
    render(<LatestEntryCard entry={entry} isLoading={false} />);
    expect(screen.getByText(/175/)).toBeInTheDocument();
  });

  it('renders empty state when entry is null', () => {
    render(<LatestEntryCard entry={null} isLoading={false} />);
    expect(screen.getByText(/no entries/i)).toBeInTheDocument();
  });
});
