# WeightToGo - Manual Testing Checklist

**Phase 9.6: Manual Device & Scenario Testing**
**Purpose**: Validate app behavior across different devices, configurations, and user scenarios
**Test Date**: _____________
**Tester**: _____________
**App Version**: v1.0.0-testing-complete
**Branch**: feature/FR7.0-final-testing

---

## How to Use This Checklist

1. **Test on at least 2 different configurations** (different API levels, screen sizes, or orientations)
2. **Mark each test**: ✅ Pass | ❌ Fail | ⚠️ Issue Found | ⏭️ Skipped
3. **Document issues** in the "Actual Result / Notes" column
4. **Take screenshots** for any failures or unexpected behavior
5. **Update project_summary.md** with findings after completing all tests

---

## Test Environment Configuration

### Configuration 1
- [ ] **Device/Emulator**: _________________________ (e.g., Pixel 6 Emulator)
- [ ] **Android Version**: _________________________ (e.g., API 34 / Android 14)
- [ ] **Screen Size**: _________________________ (e.g., 6.1" / 1080x2400)
- [ ] **Orientation**: Portrait ☐ | Landscape ☐ | Both ☐

### Configuration 2
- [ ] **Device/Emulator**: _________________________ (e.g., Physical Pixel 4a)
- [ ] **Android Version**: _________________________ (e.g., API 28 / Android 9.0)
- [ ] **Screen Size**: _________________________ (e.g., 5.8" / 1080x2340)
- [ ] **Orientation**: Portrait ☐ | Landscape ☐ | Both ☐

### Configuration 3 (Optional)
- [ ] **Device/Emulator**: _________________________ (e.g., 7" Tablet Emulator)
- [ ] **Android Version**: _________________________ (e.g., API 31 / Android 12)
- [ ] **Screen Size**: _________________________ (e.g., 7.0" / 800x1280)
- [ ] **Orientation**: Portrait ☐ | Landscape ☐ | Both ☐

---

## Section 9.6.1: Device Testing Checklist

### Test D1: App Installation & Launch
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| D1.1 | Install APK via `./gradlew installDebug` or Android Studio | App installs successfully without errors | ☐ | |
| D1.2 | Launch app from app drawer | LoginActivity displays (first launch) | ☐ | |
| D1.3 | Check app icon display | Teal icon visible in app drawer | ☐ | |
| D1.4 | Force stop app and relaunch | App reopens to LoginActivity (no crash) | ☐ | |

### Test D2: Different API Levels
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| D2.1 | Install on API 28 (min SDK) emulator/device | App installs and runs without crashes | ☐ | |
| D2.2 | Install on API 34+ (target SDK) emulator/device | App installs and runs without crashes | ☐ | |
| D2.3 | Test all critical features on both API levels | All features work (registration, login, weight entry, goals) | ☐ | |
| D2.4 | Check Material Design 3 components | Components render correctly on both API levels | ☐ | |

### Test D3: Screen Orientations
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| D3.1 | Launch app in **portrait**, complete registration | Registration succeeds, navigates to MainActivity | ☐ | |
| D3.2 | Rotate to **landscape** on MainActivity | Layout adjusts, no data loss, no crash | ☐ | |
| D3.3 | Add weight entry in **landscape** | Entry saves successfully, displays in list | ☐ | |
| D3.4 | Rotate to **portrait** during weight entry creation | Input fields retain values, no data loss | ☐ | |
| D3.5 | Create goal in **landscape** mode | Goal dialog displays correctly, saves successfully | ☐ | |
| D3.6 | Navigate to SettingsActivity, rotate device | Settings UI adjusts, preferences preserved | ☐ | |

### Test D4: Different Screen Sizes
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| D4.1 | Test on **phone** (5-6.5" screen) | UI elements fit screen, 48dp touch targets met | ☐ | |
| D4.2 | Test on **7" tablet** emulator | UI scales appropriately, no clipping | ☐ | |
| D4.3 | Test on **10" tablet** emulator (optional) | UI uses available space effectively | ☐ | |
| D4.4 | Verify RecyclerView scrolling on all sizes | Smooth scrolling, no performance issues | ☐ | |

### Test D5: Performance & Responsiveness
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| D5.1 | Add 100+ weight entries to database | App remains responsive, no lag | ☐ | |
| D5.2 | Scroll through large weight history list | Smooth 60fps scrolling | ☐ | |
| D5.3 | Navigate between activities rapidly | No crashes, transitions smooth | ☐ | |
| D5.4 | Monitor LogCat for errors during testing | No critical errors or ANRs | ☐ | |

---

## Section 9.6.2: Authentication Scenario Testing

### Test A1: New User Registration (Happy Path)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| A1.1 | Launch app (clean install) | LoginActivity displays with "Sign In" tab active | ☐ | |
| A1.2 | Switch to "Create Account" tab | Registration form displays (username, password, confirm password) | ☐ | |
| A1.3 | Enter username: `testuser1` | Username field accepts input | ☐ | |
| A1.4 | Enter password: `TestPass123!` | Password field shows masked characters | ☐ | |
| A1.5 | Enter confirm password: `TestPass123!` | Confirm password matches | ☐ | |
| A1.6 | Tap "Create Account" button | Registration succeeds, navigates to MainActivity | ☐ | |
| A1.7 | Verify greeting displays username | "Welcome, testuser1!" or similar message shown | ☐ | |

### Test A2: Existing User Login (Happy Path)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| A2.1 | On LoginActivity "Sign In" tab | Login form displays (username, password) | ☐ | |
| A2.2 | Enter username: `testuser1` (from A1) | Username field accepts input | ☐ | |
| A2.3 | Enter password: `TestPass123!` | Password field shows masked characters | ☐ | |
| A2.4 | Tap "Sign In" button | Login succeeds, navigates to MainActivity | ☐ | |
| A2.5 | Verify last login timestamp | Last login displays in SettingsActivity | ☐ | |

### Test A3: Invalid Credentials (Error Handling)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| A3.1 | On "Sign In" tab, enter username: `testuser1` | Username field accepts input | ☐ | |
| A3.2 | Enter **wrong** password: `WrongPassword!` | Password field accepts input | ☐ | |
| A3.3 | Tap "Sign In" button | Error message displays (generic, no username enumeration) | ☐ | |
| A3.4 | Verify error message content | "Invalid username or password" (not "Wrong password") | ☐ | |
| A3.5 | Enter non-existent username: `ghostuser` | Username field accepts input | ☐ | |
| A3.6 | Tap "Sign In" button | Same generic error message displays | ☐ | |

### Test A4: Duplicate Username Registration
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| A4.1 | Switch to "Create Account" tab | Registration form displays | ☐ | |
| A4.2 | Enter **existing** username: `testuser1` | Username field accepts input | ☐ | |
| A4.3 | Enter valid password: `NewPass456!` | Password fields accept input | ☐ | |
| A4.4 | Tap "Create Account" button | Error message: "Username already exists" or similar | ☐ | |
| A4.5 | Verify user is NOT created | Login with new password fails | ☐ | |

### Test A5: Session Persistence (App Restart)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| A5.1 | Login as `testuser1` successfully | MainActivity displays | ☐ | |
| A5.2 | Close app (swipe away from recent apps) | App terminates | ☐ | |
| A5.3 | Relaunch app from app drawer | App auto-navigates to MainActivity (session restored) | ☐ | |
| A5.4 | Verify username displayed correctly | Username matches logged-in user | ☐ | |

### Test A6: Logout & Session Clearing
| Step | Action | expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| A6.1 | From MainActivity, navigate to SettingsActivity | Settings screen displays | ☐ | |
| A6.2 | Tap "Logout" button | Confirmation dialog appears | ☐ | |
| A6.3 | Tap "Cancel" in confirmation dialog | Dialog dismisses, remains on SettingsActivity | ☐ | |
| A6.4 | Tap "Logout" button again | Confirmation dialog appears | ☐ | |
| A6.5 | Tap "Logout" (confirm) in dialog | Session cleared, navigates to LoginActivity | ☐ | |
| A6.6 | Close and relaunch app | App opens to LoginActivity (session NOT restored) | ☐ | |

---

## Section 9.6.3: Weight Entry Scenario Testing

### Test W1: Add First Weight Entry (Empty Database)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| W1.1 | Login as new user (empty database) | MainActivity displays empty state message | ☐ | |
| W1.2 | Tap "Add Weight" FAB | WeightEntryActivity opens in add mode | ☐ | |
| W1.3 | Verify date defaults to today | Date display shows current date | ☐ | |
| W1.4 | Tap number buttons to enter weight: `170.5` | Weight display shows `170.5` | ☐ | |
| W1.5 | Verify unit toggle (lbs/kg) | Toggle displays current unit (e.g., lbs) | ☐ | |
| W1.6 | Tap "Save" button | Entry saves, returns to MainActivity | ☐ | |
| W1.7 | Verify entry displays in RecyclerView | Entry shows: 170.5 lbs, today's date, no trend badge | ☐ | |

### Test W2: Add Subsequent Entry (Trend Calculation)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| W2.1 | From MainActivity with 1 entry, tap FAB | WeightEntryActivity opens | ☐ | |
| W2.2 | Enter weight: `168.0` | Weight display shows `168.0` | ☐ | |
| W2.3 | Tap "Save" | Entry saves, returns to MainActivity | ☐ | |
| W2.4 | Verify trend badge on new entry | Badge shows "↓ 2.5 lbs" (or kg equivalent) | ☐ | |
| W2.5 | Verify trend badge color | Green for weight loss | ☐ | |
| W2.6 | Add another entry with higher weight: `170.0` | Entry saves | ☐ | |
| W2.7 | Verify trend badge for weight gain | Badge shows "↑ 2.0 lbs" in red | ☐ | |

### Test W3: Edit Existing Entry
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| W3.1 | On MainActivity, tap "Edit" button on an entry | WeightEntryActivity opens in edit mode | ☐ | |
| W3.2 | Verify weight field pre-populated | Existing weight value displayed | ☐ | |
| W3.3 | Verify date pre-populated | Existing date displayed | ☐ | |
| W3.4 | Change weight to `165.5` | Weight display updates | ☐ | |
| W3.5 | Tap "Save" | Entry updates, returns to MainActivity | ☐ | |
| W3.6 | Verify updated weight in RecyclerView | Entry shows new weight `165.5` | ☐ | |
| W3.7 | Verify trend recalculated | Trend badge updates based on new weight | ☐ | |

### Test W4: Delete Entry (Confirmation Dialog)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| W4.1 | On MainActivity, tap "Delete" button on an entry | Confirmation dialog appears | ☐ | |
| W4.2 | Verify dialog message | "Are you sure you want to delete this entry?" | ☐ | |
| W4.3 | Tap "Cancel" | Dialog dismisses, entry remains in list | ☐ | |
| W4.4 | Tap "Delete" button again | Confirmation dialog appears | ☐ | |
| W4.5 | Tap "Delete" (confirm) | Entry removed from RecyclerView | ☐ | |
| W4.6 | Verify soft delete (check database if possible) | Entry marked deleted, not permanently removed | ☐ | |

### Test W5: Unit Conversion (lbs ↔ kg)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| W5.1 | Add entry: `100 kg` | Entry saves with kg unit | ☐ | |
| W5.2 | Verify display shows "100 kg" | Correct unit displayed | ☐ | |
| W5.3 | Add entry: `220 lbs` | Entry saves with lbs unit | ☐ | |
| W5.4 | Verify mixed units in RecyclerView | Both entries display with correct units | ☐ | |
| W5.5 | Verify trend calculation with mixed units | Trend converts units correctly (100kg ≈ 220lbs) | ☐ | |
| W5.6 | Navigate to SettingsActivity | Settings screen displays | ☐ | |
| W5.7 | Toggle weight unit preference (kg → lbs) | Preference updates, toast confirmation | ☐ | |
| W5.8 | Return to MainActivity | All entries converted to lbs | ☐ | |
| W5.9 | Toggle back to kg in SettingsActivity | All entries converted to kg | ☐ | |

### Test W6: Empty State Handling
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| W6.1 | Delete all weight entries (or login as new user) | MainActivity shows empty state message | ☐ | |
| W6.2 | Verify empty state UI | Message: "No weight entries yet. Tap + to add one!" | ☐ | |
| W6.3 | Verify FAB still accessible | FAB displays and is tappable | ☐ | |

### Test W7: Large Dataset (100+ Entries)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| W7.1 | Add 100+ weight entries (script or manual) | All entries save successfully | ☐ | |
| W7.2 | Scroll through RecyclerView | Smooth scrolling, no lag or jank | ☐ | |
| W7.3 | Edit an entry in the middle of the list | Entry updates correctly | ☐ | |
| W7.4 | Delete an entry in the middle of the list | Entry removes, RecyclerView updates | ☐ | |
| W7.5 | Check app memory usage in Profiler (optional) | No memory leaks, acceptable usage | ☐ | |

---

## Section 9.6.4: SMS Permissions Scenario Testing

### Test S1: Grant Permission Flow (Happy Path)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| S1.1 | Fresh install, login, navigate to SettingsActivity | SMS notification toggle disabled | ☐ | |
| S1.2 | Tap SMS notification toggle | Permission rationale dialog appears | ☐ | |
| S1.3 | Verify rationale message | Explains why SMS permission needed | ☐ | |
| S1.4 | Tap "OK" in rationale dialog | System permission dialog appears | ☐ | |
| S1.5 | Tap "Allow" in system dialog | Permission granted | ☐ | |
| S1.6 | Verify SMS toggle now enabled | Toggle switches to ON state | ☐ | |
| S1.7 | Enter phone number in settings | Phone number field accepts input (E.164 format) | ☐ | |
| S1.8 | Save phone number | Preference saved, toast confirmation | ☐ | |

### Test S2: Deny Permission Flow
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| S2.1 | Fresh install (or clear app data), login | Navigate to SettingsActivity | ☐ | |
| S2.2 | Tap SMS notification toggle | Rationale dialog appears | ☐ | |
| S2.3 | Tap "OK" in rationale dialog | System permission dialog appears | ☐ | |
| S2.4 | Tap "Deny" in system dialog | Permission denied | ☐ | |
| S2.5 | Verify SMS toggle remains disabled | Toggle stays OFF, cannot be enabled | ☐ | |
| S2.6 | Verify phone number field disabled | Field grayed out or uneditable | ☐ | |
| S2.7 | Tap toggle again | Rationale dialog appears again (can request permission again) | ☐ | |

### Test S3: "Don't Ask Again" Flow
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| S3.1 | Fresh install, login, navigate to SettingsActivity | SMS toggle disabled | ☐ | |
| S3.2 | Tap SMS toggle → Tap "OK" → Tap "Deny" | Permission denied (first time) | ☐ | |
| S3.3 | Tap SMS toggle again → Tap "OK" → Check "Don't ask again" → Tap "Deny" | Permission permanently denied | ☐ | |
| S3.4 | Tap SMS toggle again | Settings prompt dialog appears (not system dialog) | ☐ | |
| S3.5 | Tap "Open Settings" in prompt | App settings screen opens | ☐ | |
| S3.6 | Manually grant SMS permission in app settings | Permission granted | ☐ | |
| S3.7 | Return to app SettingsActivity | SMS toggle now enabled | ☐ | |

### Test S4: App Functions Without SMS Permission
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| S4.1 | Login without granting SMS permission | App allows login | ☐ | |
| S4.2 | Add weight entries | Entries save successfully (no SMS dependency) | ☐ | |
| S4.3 | Create goal weight | Goal saves successfully | ☐ | |
| S4.4 | Achieve goal (if applicable) | Achievement recorded (no SMS sent, no crash) | ☐ | |
| S4.5 | Navigate all screens | No crashes or SMS-related errors | ☐ | |

### Test S5: SMS Notification Sending (If Permission Granted)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| S5.1 | Grant SMS permission, enable toggle | Toggle ON | ☐ | |
| S5.2 | Enter valid phone number (yours or test number) | Phone number saved | ☐ | |
| S5.3 | Create goal weight | Goal saved | ☐ | |
| S5.4 | Add weight entry that achieves goal | Achievement triggered | ☐ | |
| S5.5 | Check phone for SMS | SMS received with goal achievement message | ☐ | |
| S5.6 | Verify SMS content | Message includes username, goal weight, achievement details | ☐ | |

---

## Section 9.6.5: Edge Case Testing

### Test E1: Special Characters in Input
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| E1.1 | Register with username: `test<script>alert("xss")</script>` | Input sanitized, no XSS | ☐ | |
| E1.2 | Register with username: `test'; DROP TABLE users;--` | Input sanitized, no SQL injection | ☐ | |
| E1.3 | Add weight entry notes: `Special chars: @#$%^&*()` | Notes saved correctly | ☐ | |
| E1.4 | Add weight entry notes: `Emoji: 🏋️‍♂️💪🎯` | Emoji saved and displayed correctly | ☐ | |
| E1.5 | Enter phone number: `+1 (555) 123-4567` | Format normalized to E.164 (+15551234567) | ☐ | |

### Test E2: Screen Rotation During Input
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| E2.1 | On LoginActivity, enter username/password | Fields accept input | ☐ | |
| E2.2 | Rotate device to landscape | Input fields retain values | ☐ | |
| E2.3 | Complete registration | Registration succeeds | ☐ | |
| E2.4 | On WeightEntryActivity, enter weight: `180.5` | Weight displays | ☐ | |
| E2.5 | Rotate device | Weight value retained, no data loss | ☐ | |
| E2.6 | Save entry | Entry saves with correct weight | ☐ | |

### Test E3: App Kill and Restart (Process Death)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| E3.1 | Login as `testuser1` | MainActivity displays | ☐ | |
| E3.2 | Kill app process (ADB: `adb shell am kill weightogo`) | App terminates | ☐ | |
| E3.3 | Relaunch app from launcher | App opens to MainActivity (session restored) | ☐ | |
| E3.4 | Verify user data intact | Weight entries, goals, preferences preserved | ☐ | |

### Test E4: Back Button Navigation
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| E4.1 | From MainActivity, navigate to SettingsActivity | Settings displays | ☐ | |
| E4.2 | Press back button | Returns to MainActivity | ☐ | |
| E4.3 | Press back button on MainActivity | App exits (does not navigate to LoginActivity) | ☐ | |
| E4.4 | From WeightEntryActivity (add mode), press back | Returns to MainActivity without saving | ☐ | |
| E4.5 | Open WeightEntryActivity (edit mode), make changes, press back | Unsaved changes warning dialog (if implemented) or returns without saving | ☐ | |

### Test E5: Fast Clicking (Duplicate Submissions)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| E5.1 | On LoginActivity, enter valid credentials | Fields filled | ☐ | |
| E5.2 | Rapidly tap "Sign In" button 10 times | Only 1 login request processed (button disabled after first tap) | ☐ | |
| E5.3 | On WeightEntryActivity, enter weight | Weight displayed | ☐ | |
| E5.4 | Rapidly tap "Save" button 10 times | Only 1 entry saved (no duplicates) | ☐ | |
| E5.5 | Verify database has single entry | Query database or check RecyclerView | ☐ | |

### Test E6: Network State Changes (If Applicable)
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| E6.1 | Enable airplane mode | App continues to function (offline-first design) | ☐ | |
| E6.2 | Add weight entries while offline | Entries save to local database | ☐ | |
| E6.3 | Disable airplane mode | App resumes normal operation | ☐ | |
| E6.4 | Verify offline entries still present | Data persisted correctly | ☐ | |

### Test E7: Date Edge Cases
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| E7.1 | Add weight entry on Feb 28 (non-leap year) | Entry saves | ☐ | |
| E7.2 | Add weight entry on Feb 29 (leap year, if applicable) | Entry saves | ☐ | |
| E7.3 | Add weight entry on Dec 31 | Entry saves | ☐ | |
| E7.4 | Add weight entry on Jan 1 (next year) | Entry saves, year transition handled | ☐ | |
| E7.5 | Navigate weight entries across year boundary | Correct chronological ordering | ☐ | |

### Test E8: Boundary Values
| Step | Action | Expected Result | Status | Actual Result / Notes |
|------|--------|----------------|--------|----------------------|
| E8.1 | Enter weight: `0.0` | Entry saves (zero weight allowed per plan) | ☐ | |
| E8.2 | Enter weight: `999.9` | Entry saves (max reasonable weight) | ☐ | |
| E8.3 | Try to enter weight: `1000.0` or more | Input validation (if implemented) or displays correctly | ☐ | |
| E8.4 | Enter password: `Pass1!` (minimum length) | Validation passes (or fails if too short per requirements) | ☐ | |
| E8.5 | Enter username: `ab` (2 chars) | Validation fails (minimum 3 chars) | ☐ | |

---

## Test Summary

### Overall Results
- **Total Tests Executed**: _____ / _____
- **Tests Passed**: _____ (✅)
- **Tests Failed**: _____ (❌)
- **Issues Found**: _____ (⚠️)
- **Tests Skipped**: _____ (⏭️)

### Critical Issues Found
1. ________________________________________________________________
2. ________________________________________________________________
3. ________________________________________________________________

### Non-Critical Issues Found
1. ________________________________________________________________
2. ________________________________________________________________
3. ________________________________________________________________

### Performance Observations
________________________________________________________________
________________________________________________________________
________________________________________________________________

### Accessibility Observations
________________________________________________________________
________________________________________________________________
________________________________________________________________

### Recommendations
________________________________________________________________
________________________________________________________________
________________________________________________________________

---

## Next Steps After Manual Testing

1. **Document Findings**:
   - [ ] Update `project_summary.md` with manual testing results
   - [ ] Create GitHub issues for any bugs found
   - [ ] Add screenshots to `docs/testing/screenshots/` (if applicable)

2. **Fix Critical Issues**:
   - [ ] Prioritize and fix any blocking bugs
   - [ ] Re-test affected scenarios

3. **Proceed to Subsection 9.7**:
   - [ ] Validate test coverage with coverage reports
   - [ ] Execute full test suite (`./gradlew test connectedAndroidTest`)

---

**Testing completed on**: __________________
**Sign-off**: ______________________________
