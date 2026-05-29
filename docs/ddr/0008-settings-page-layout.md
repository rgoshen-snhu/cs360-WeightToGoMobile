# DDR-0008: Settings Page Layout

- **Date**: 2026-05-29
- **Status**: Accepted

## Context

Issue #55 (Phase 3 — User Preferences) replaces the `SettingsPlaceholderPage` with a
functional settings page exposing:

- **FR-P-1**: Global weight-unit preference (`lbs` | `kg`) — drives the default unit on
  new weight entries and goal forms (display label only; no stored-value conversion, per
  design decision §1.1#1 and ADR-0020).
- **FR-P-3**: Notification toggles — on/off for achievement, milestone, and streak alerts.

The page must meet NFR-A-3 (keyboard navigability) and NFR-A-6 (reduced-motion) per the
SRS accessibility requirements. The `colorScheme` / FR-P-2 theme toggle is deferred to
the Final enhancement.

## Decision

**Two-section card layout within the existing `<PageLayout>` shell.**

### Page structure

```
/settings
└── PageLayout (existing shell — AppBar + nav)
    └── Container maxWidth="sm"
        ├── Typography variant="h4"  "Settings"
        ├── Card  "Units"
        │   └── UnitPreferenceControl    (segmented radio: lbs | kg)
        └── Card  "Notifications"
            └── NotificationTogglesControl
                ├── FormControlLabel  Switch  "Achievement alerts"
                ├── FormControlLabel  Switch  "Milestone alerts"
                └── FormControlLabel  Switch  "Streak alerts (coming soon)"
```

### Unit control — segmented radio (`UnitPreferenceControl`)

Two MUI `Radio` buttons rendered as a `ToggleButtonGroup`-style pair (using `RadioGroup`
with styled `Radio` inside `Paper` segments), one per unit. The selected unit is highlighted
with `primary.main` background. Accessible via `<FormLabel>` + `<RadioGroup>` with
`aria-labelledby`.

### Notification toggles (`NotificationTogglesControl`)

Three MUI `Switch` components in a `FormGroup`, each wrapped in a `FormControlLabel`.
Labels: "Achievement alerts", "Milestone alerts", "Streak alerts". The streak toggle is
rendered as `disabled` with "(coming soon)" appended to its label, communicating the
inert state to all users without hiding it.

### Save feedback — live region

Changes are persisted immediately on interaction (no explicit Save button). An
`aria-live="polite"` region at the bottom of the page announces "Preferences saved"
for 2 seconds after each successful mutation, then clears. This gives screen reader
users confirmation without a modal interruption (WCAG 2.1 AA SC 4.1.3).

### Route wiring

`SettingsPlaceholderPage` is replaced with `SettingsPage` at the `/settings` route in
`App.tsx`. The `routes.tsx` `RouteConfig` data file is unchanged.

## Rationale

**Card-per-section vs single flat form:** Two `Card` components (Units, Notifications)
group semantically distinct concerns. This matches the MUI pattern used on `GoalsPage`
and `WeightEntryFormPage` and keeps sections independently scannable for keyboard users.

**Segmented radio over `<Select>`:** A two-option unit preference is a small-N
mutually-exclusive choice. A segmented radio exposes both options at a glance with no
dropdown; it communicates the full option space without requiring an interaction to
reveal it. Equivalent accessibility to a `<select>` when wrapped in a `RadioGroup`
with a visible `FormLabel`.

**MUI Switch for toggles over checkbox:** MUI `Switch` communicates "this feature is
on/off" through a binary indicator that users associate with system settings. A
`Checkbox` implies "this item is included in a collection". For notification
preferences, the toggle mental model is more intuitive.

**Immediate persistence over explicit Save:** The number of fields is small (one radio,
three toggles); each mutation is reversible. Immediate persistence removes the
"forgot to save" failure mode, reduces UI state management complexity, and matches
the Android app's preference behavior. The `aria-live` announcement compensates for
the absence of a Save button that would otherwise confirm submission.

**Streak toggle disabled, not hidden:** FR-P-3 requires the toggle to exist; streak
detection ships in Step 5. Showing the control as `disabled` with a "(coming soon)"
label informs users the feature is planned, avoids a re-layout at Step 5, and keeps
the preference value persisted (stored-but-inert per ADR-0020).

**No `colorScheme` control:** FR-P-2 (theme) is deferred to the Final enhancement.
Rendering a non-functional theme toggle would mislead users. The field is omitted from
`SettingsPage` and removed from `PreferencesContext` to eliminate dead code (design
decision §1.1#5).

## Impact

- `web/frontend/src/features/settings/pages/SettingsPage.tsx` — new page; replaces
  `SettingsPlaceholderPage`.
- `web/frontend/src/features/settings/components/UnitPreferenceControl.tsx` — new.
- `web/frontend/src/features/settings/components/NotificationTogglesControl.tsx` — new.
- `web/frontend/src/contexts/PreferencesContext.tsx` — `colorScheme` removed; context
  backed by `usePreferencesQuery`; `setPreference` wired to `useUpdatePreference` mutation.
- `web/frontend/src/App.tsx` — route `/settings` swapped from placeholder to `SettingsPage`.
- `web/frontend/src/features/weight/components/WeightEntryForm.tsx` — default `weight_unit`
  from `preferences.weightUnit` (no longer hardcoded `'lbs'`).
- `web/frontend/src/features/goals/...` — default `target_unit` from `preferences.weightUnit`.

## Visual Reference

```
┌─────────────────────────────────────────────────────┐
│  Settings                                            │
├─────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────┐   │
│  │  Units                                       │   │
│  │  ┌──────────────┐  ┌──────────────┐         │   │
│  │  │   ● lbs      │  │   ○ kg       │         │   │
│  │  └──────────────┘  └──────────────┘         │   │
│  └──────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────┐   │
│  │  Notifications                               │   │
│  │  Achievement alerts          ●──○            │   │
│  │  Milestone alerts            ●──○            │   │
│  │  Streak alerts (coming soon) ○──●  (disabled)│   │
│  └──────────────────────────────────────────────┘   │
│                                                     │
│  aria-live region: "Preferences saved" (2 s)        │
└─────────────────────────────────────────────────────┘
```
