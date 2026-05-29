/**
 * Tests for preferences schemas and the snake↔camel mapper [G12].
 */
import { describe, expect, it } from 'vitest';

import {
  apiToPreferences,
  preferencesToApi,
  preferencesApiSchema,
  type Preferences,
  type PreferencesApiResponse,
} from './preferences-schemas';

const validApi: PreferencesApiResponse = {
  weight_unit: 'lbs',
  notify_achievement: true,
  notify_milestone: true,
  notify_streak: false,
};

describe('preferencesApiSchema', () => {
  it('parses a valid API response', () => {
    const result = preferencesApiSchema.safeParse(validApi);
    expect(result.success).toBe(true);
  });

  it('rejects an invalid weight_unit', () => {
    const result = preferencesApiSchema.safeParse({ ...validApi, weight_unit: 'oz' });
    expect(result.success).toBe(false);
  });

  it('rejects a non-boolean notify field', () => {
    const result = preferencesApiSchema.safeParse({ ...validApi, notify_achievement: 'yes' });
    expect(result.success).toBe(false);
  });

  it('requires all four fields', () => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { notify_streak, ...partial } = validApi;
    const result = preferencesApiSchema.safeParse(partial);
    expect(result.success).toBe(false);
  });
});

describe('apiToPreferences', () => {
  it('maps snake_case to camelCase', () => {
    const prefs: Preferences = apiToPreferences(validApi);
    expect(prefs).toEqual({
      weightUnit: 'lbs',
      notifyAchievement: true,
      notifyMilestone: true,
      notifyStreak: false,
    });
  });

  it('preserves kg weight unit', () => {
    const prefs = apiToPreferences({ ...validApi, weight_unit: 'kg' });
    expect(prefs.weightUnit).toBe('kg');
  });

  it('maps notify_streak false correctly', () => {
    const prefs = apiToPreferences({ ...validApi, notify_streak: false });
    expect(prefs.notifyStreak).toBe(false);
  });
});

describe('preferencesToApi (round-trip)', () => {
  it('maps camelCase back to snake_case for the PUT body', () => {
    const prefs: Preferences = {
      weightUnit: 'kg',
      notifyAchievement: false,
      notifyMilestone: true,
      notifyStreak: true,
    };
    expect(preferencesToApi(prefs)).toEqual({
      weight_unit: 'kg',
      notify_achievement: false,
      notify_milestone: true,
      notify_streak: true,
    });
  });
});
