import { Card, CardContent, Skeleton, Typography } from '@mui/material';

interface TotalEntriesCardProps {
  total: number;
  isLoading: boolean;
}

/** Card displaying the total number of weight entries logged. */
export function TotalEntriesCard({ total, isLoading }: TotalEntriesCardProps) {
  return (
    <Card>
      <CardContent>
        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
          Total Entries
        </Typography>
        {isLoading ? (
          <Skeleton variant="text" width="40%" />
        ) : (
          <>
            <Typography variant="h5">{total}</Typography>
            <Typography variant="body2" color="text.secondary">
              entries logged
            </Typography>
          </>
        )}
      </CardContent>
    </Card>
  );
}
