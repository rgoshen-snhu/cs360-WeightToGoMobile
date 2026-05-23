import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import type { WeightEntryRecord } from '../api/weight-client';
import { WeightEntryTable } from './WeightEntryTable';

const entries: WeightEntryRecord[] = [
  {
    entry_id: 1,
    weight_value: 175.5,
    weight_unit: 'lbs',
    observation_date: '2026-05-20',
    notes: null,
    created_at: '2026-05-20T12:00:00Z',
    updated_at: '2026-05-20T12:00:00Z',
  },
  {
    entry_id: 2,
    weight_value: 174.0,
    weight_unit: 'lbs',
    observation_date: '2026-05-19',
    notes: 'Morning',
    created_at: '2026-05-19T08:00:00Z',
    updated_at: '2026-05-19T08:00:00Z',
  },
];

function renderTable(props: Partial<Parameters<typeof WeightEntryTable>[0]> = {}) {
  const defaults = {
    entries,
    onDelete: vi.fn(),
  };
  return render(
    <MemoryRouter>
      <WeightEntryTable {...defaults} {...props} />
    </MemoryRouter>,
  );
}

describe('WeightEntryTable', () => {
  it('renders the correct number of rows', () => {
    renderTable();
    // Each entry has an edit and a delete button; look for delete buttons
    expect(screen.getAllByRole('button', { name: /delete/i })).toHaveLength(2);
  });

  it('displays weight values', () => {
    renderTable();
    expect(screen.getByText(/175/)).toBeInTheDocument();
    expect(screen.getByText(/174/)).toBeInTheDocument();
  });

  it('calls onDelete when Delete button is clicked', async () => {
    const onDelete = vi.fn();
    renderTable({ onDelete });
    const [firstDelete] = screen.getAllByRole('button', { name: /delete/i });
    await userEvent.click(firstDelete!);
    expect(onDelete).toHaveBeenCalledWith(1);
  });

  it('renders Edit links', () => {
    renderTable();
    const editLinks = screen.getAllByRole('link', { name: /edit/i });
    expect(editLinks.length).toBeGreaterThan(0);
  });

  it('renders empty message when entries is empty', () => {
    renderTable({ entries: [] });
    expect(screen.getByText(/no entries/i)).toBeInTheDocument();
  });
});
