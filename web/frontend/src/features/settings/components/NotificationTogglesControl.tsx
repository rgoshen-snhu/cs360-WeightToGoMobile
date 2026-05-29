/**
 * Notification toggle controls (DDR-0008, FR-P-3).
 *
 * Three MUI Switches: achievement, milestone, and streak alerts.
 * Streak is stored-but-inert (no producer in Phase 3) — shown disabled
 * with "(coming soon)" so the row appears without re-layout at Phase 5.
 */

import { FormControl, FormControlLabel, FormGroup, FormLabel, Switch } from '@mui/material';

interface NotificationTogglesControlProps {
  notifyAchievement: boolean;
  notifyMilestone: boolean;
  notifyStreak: boolean;
  onChange: (
    key: 'notify_achievement' | 'notify_milestone' | 'notify_streak',
    value: boolean,
  ) => void;
}

export function NotificationTogglesControl({
  notifyAchievement,
  notifyMilestone,
  notifyStreak,
  onChange,
}: NotificationTogglesControlProps) {
  return (
    <FormControl component="fieldset" fullWidth>
      <FormLabel component="legend" sx={{ mb: 1, fontWeight: 'medium' }}>
        Notifications
      </FormLabel>
      <FormGroup>
        <FormControlLabel
          control={
            <Switch
              checked={notifyAchievement}
              onChange={(e) => onChange('notify_achievement', e.target.checked)}
              slotProps={{ input: { 'aria-label': 'Achievement alerts toggle' } }}
            />
          }
          label="Achievement alerts"
        />
        <FormControlLabel
          control={
            <Switch
              checked={notifyMilestone}
              onChange={(e) => onChange('notify_milestone', e.target.checked)}
              slotProps={{ input: { 'aria-label': 'Milestone alerts toggle' } }}
            />
          }
          label="Milestone alerts"
        />
        <FormControlLabel
          disabled
          control={
            <Switch
              checked={notifyStreak}
              slotProps={{ input: { 'aria-label': 'Streak alerts toggle (coming soon)' } }}
            />
          }
          label="Streak alerts (coming soon)"
        />
      </FormGroup>
    </FormControl>
  );
}
