/**
 * Segmented radio control for the global weight-unit preference (DDR-0008, FR-P-1).
 *
 * Exposes both options at a glance without a dropdown.
 * Accessible via FormLabel + RadioGroup (WCAG 2.1 AA SC 1.3.1).
 */

import { FormControl, FormLabel, Radio, RadioGroup, FormControlLabel } from '@mui/material';

interface UnitPreferenceControlProps {
  value: 'lbs' | 'kg';
  onChange: (unit: 'lbs' | 'kg') => void;
}

export function UnitPreferenceControl({ value, onChange }: UnitPreferenceControlProps) {
  return (
    <FormControl component="fieldset" fullWidth>
      <FormLabel component="legend" sx={{ mb: 1, fontWeight: 'medium' }}>
        Weight unit
      </FormLabel>
      <RadioGroup
        row
        name="weight-unit"
        value={value}
        onChange={(e) => onChange(e.target.value as 'lbs' | 'kg')}
        aria-label="Weight unit preference"
      >
        {(['lbs', 'kg'] as const).map((unit) => (
          <FormControlLabel
            key={unit}
            value={unit}
            control={<Radio />}
            label={unit}
            sx={{
              mr: 2,
              px: 2,
              py: 0.5,
              borderRadius: 1,
              bgcolor: value === unit ? 'primary.main' : 'transparent',
              color: value === unit ? 'primary.contrastText' : 'text.primary',
              '& .MuiRadio-root': { color: value === unit ? 'primary.contrastText' : undefined },
            }}
          />
        ))}
      </RadioGroup>
    </FormControl>
  );
}
