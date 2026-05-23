import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { TotalEntriesCard } from './TotalEntriesCard';

describe('TotalEntriesCard', () => {
  it('renders loading skeleton when isLoading=true', () => {
    render(<TotalEntriesCard total={0} isLoading={true} />);
    expect(document.querySelector('.MuiSkeleton-root')).toBeInTheDocument();
  });

  it('renders the total count', () => {
    render(<TotalEntriesCard total={12} isLoading={false} />);
    expect(screen.getByText('12')).toBeInTheDocument();
  });
});
