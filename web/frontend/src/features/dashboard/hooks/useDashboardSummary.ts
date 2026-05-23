import { useQuery } from '@tanstack/react-query';
import { dashboardClient } from '../api/dashboard-client';

export const DASHBOARD_SUMMARY_KEY = ['dashboard-summary'] as const;

/** Return the dashboard summary for the current user. */
export function useDashboardSummary() {
  return useQuery({
    queryKey: DASHBOARD_SUMMARY_KEY,
    queryFn: () => dashboardClient.summary(),
  });
}
