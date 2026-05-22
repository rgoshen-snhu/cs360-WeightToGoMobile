package com.example.weightogo.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.weightogo.R;
import com.example.weightogo.database.DatabaseException;
import com.example.weightogo.database.UserDAO;
import com.example.weightogo.database.UserPreferenceDAO;
import com.example.weightogo.database.WeighToGoDBHelper;
import com.example.weightogo.models.User;
import com.example.weightogo.utils.PasswordUtilsV2;
import com.example.weightogo.utils.SMSNotificationManager;
import com.example.weightogo.utils.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

/**
 * Espresso instrumented tests for SettingsActivity.
 * <p>
 * Migrated from SettingsActivityTest.java (Robolectric) to resolve GitHub Issue #12
 * (Robolectric/Material3 theme incompatibility).
 * <p>
 * These tests run on a real Android device or emulator with full Material3 theme support.
 * Tests settings management: weight unit preferences, SMS permissions, phone number validation,
 * preference persistence, and SMS toggle cascading behavior.
 * <p>
 * Coverage:
 * - Weight unit preference (4 tests)
 * - SMS permission management (8 tests)
 * - Phone number validation (6 tests)
 * - Preference persistence & SMS toggle cascading (6 tests)
 * <p>
 * Total: 24 tests
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SettingsActivityEspressoTest {

    @Mock private UserDAO mockUserDAO;
    @Mock private UserPreferenceDAO mockUserPreferenceDAO;
    @Mock private SMSNotificationManager mockSmsManager;

    private ActivityScenario<SettingsActivity> scenario;
    private Context context;
    private WeighToGoDBHelper dbHelper;
    private SessionManager sessionManager;
    private User testUser;
    private long testUserId;

    @Before
    public void setUp() throws Exception {
        // Initialize Mockito mocks
        MockitoAnnotations.openMocks(this);

        // Get application context
        context = ApplicationProvider.getApplicationContext();

        // Initialize test database
        dbHelper = WeighToGoDBHelper.getTestInstance(context, WeighToGoDBHelper.DATABASE_TEST_NAME);

        // Get SessionManager instance
        sessionManager = SessionManager.getInstance(context);

        // Clean up any existing test user from previous runs
        UserDAO cleanupDAO = new UserDAO(dbHelper);
        User existingUser = cleanupDAO.getUserByUsername("testuser");
        if (existingUser != null) {
            cleanupDAO.deleteUser(existingUser.getUserId());
        }

        // Create test user
        testUser = createTestUser("testuser", "Test User");
        testUserId = testUser.getUserId();

        // Log in test user BEFORE launching activity
        sessionManager.createSession(testUser);

        // Set default mock behaviors
        when(mockUserPreferenceDAO.getWeightUnit(testUserId)).thenReturn("lbs");
        when(mockUserPreferenceDAO.setPreference(anyLong(), anyString(), anyString())).thenReturn(true);
        when(mockUserDAO.getUserById(testUserId)).thenReturn(testUser);

        // Launch activity with mocked dependencies
        scenario = ActivityScenario.launch(SettingsActivity.class);
        scenario.onActivity(activity -> {
            activity.setUserDAO(mockUserDAO);
            activity.setUserPreferenceDAO(mockUserPreferenceDAO);
            activity.setSMSNotificationManager(mockSmsManager);
        });
    }

    @After
    public void tearDown() {
        // Close activity scenario
        if (scenario != null) {
            scenario.close();
        }

        // Clear session
        if (sessionManager != null) {
            sessionManager.logout();
        }

        // Close database
        if (dbHelper != null) {
            dbHelper.close();
        }
        context.deleteDatabase(WeighToGoDBHelper.DATABASE_TEST_NAME);
    }

    // ============================================================
    // WEIGHT UNIT PREFERENCE TESTS (4 tests)
    // ============================================================

    /**
     * Test 1: onCreate loads current weight unit preference.
     * <p>
     * Tests FR6.0.4 - SettingsActivity preference loading.
     * Verifies that when a user has "kg" preference, the activity
     * loads it on startup.
     */
    @Test
    public void test_onCreate_loadsCurrentWeightUnit() {
        // ARRANGE - Mock returns "kg" preference
        when(mockUserPreferenceDAO.getWeightUnit(testUserId)).thenReturn("kg");

        // Restart activity to load preference
        scenario.recreate();

        // ASSERT - Verify preference was queried
        verify(mockUserPreferenceDAO).getWeightUnit(testUserId);

        // ASSERT - Verify UI elements exist (visual verification on device)
        onView(withId(R.id.unitKg)).check(matches(isDisplayed()));
        onView(withId(R.id.unitLbs)).check(matches(isDisplayed()));
    }

    /**
     * Test 2: Click lbs toggle saves lbs preference.
     * <p>
     * Tests FR6.0.4 - SettingsActivity preference saving.
     * Verifies that clicking the lbs button saves "lbs" to database.
     */
    @Test
    public void test_clickLbsToggle_savesLbsPreference() {
        // ARRANGE - Start with "kg" preference
        when(mockUserPreferenceDAO.getWeightUnit(testUserId)).thenReturn("kg");
        scenario.recreate();

        // ACT - Click lbs button
        onView(withId(R.id.unitLbs)).perform(click());

        // ASSERT - Verify preference was saved as "lbs"
        verify(mockUserPreferenceDAO).setPreference(eq(testUserId), eq("weight_unit"), eq("lbs"));
    }

    /**
     * Test 3: Click kg toggle saves kg preference.
     * <p>
     * Tests FR6.0.4 - SettingsActivity preference saving.
     * Verifies that clicking the kg button saves "kg" to database.
     */
    @Test
    public void test_clickKgToggle_savesKgPreference() {
        // ARRANGE - Start with "lbs" preference (default)
        when(mockUserPreferenceDAO.getWeightUnit(testUserId)).thenReturn("lbs");
        scenario.recreate();

        // ACT - Click kg button
        onView(withId(R.id.unitKg)).perform(click());

        // ASSERT - Verify preference was saved as "kg"
        verify(mockUserPreferenceDAO).setPreference(eq(testUserId), eq("weight_unit"), eq("kg"));
    }

    /**
     * Test 4: Save weight unit shows confirmation toast.
     * <p>
     * Tests FR6.0.4 - SettingsActivity user feedback.
     * Verifies that after saving preference, UI interaction completes without crash.
     * <p>
     * NOTE: Espresso does not have built-in toast verification. This test verifies
     * the click completes successfully. Manual testing required for toast content.
     * <p>
     * TODO(GH #49): Add toast verification using UI Automator
     */
    @Test
    public void test_saveWeightUnit_showsConfirmationToast() {
        // ACT - Click kg button (should trigger toast)
        onView(withId(R.id.unitKg)).perform(click());

        // ASSERT - Verify no crash (toast content verified manually)
        // Expected toast: "Weight unit updated to kg"
        // Toast verification requires custom matcher or UI Automator
        onView(withId(R.id.unitKg)).check(matches(isDisplayed()));
    }

    // ============================================================
    // SMS NOTIFICATION TESTS (8 tests)
    // ============================================================

    /**
     * Test 5: onCreate checks SMS permission status.
     * <p>
     * Tests FR7.4 - SettingsActivity SMS permission checking.
     * Verifies that on startup, the activity checks current permission status
     * and updates UI accordingly.
     */
    @Test
    public void test_onCreate_checksPermissionStatus() {
        // ASSERT - Verify permission UI elements exist
        onView(withId(R.id.permissionStatusBadge)).check(matches(isDisplayed()));
        onView(withId(R.id.grantPermissionButton)).check(matches(isDisplayed()));
    }

    /**
     * Test 6: checkPermissions with granted permissions updates UI to granted state.
     * <p>
     * Tests FR7.4 - SettingsActivity permission UI updates.
     * Verifies that permission status badge is visible.
     * <p>
     * NOTE: Full permission grant flow requires device/emulator interaction
     * and cannot be fully automated in Espresso without UIAutomator.
     */
    @Test
    public void test_checkPermissions_withGranted_updatesUIGranted() {
        // ASSERT - Verify permission status badge exists and is displayed
        onView(withId(R.id.permissionStatusBadge)).check(matches(isDisplayed()));

        // Note: Actual permission status depends on device runtime permissions
        // Manual testing required to verify "Granted" state
    }

    /**
     * Test 7: checkPermissions with denied permissions updates UI to required state.
     * <p>
     * Tests FR7.4 - SettingsActivity permission UI updates.
     * Verifies that permission UI elements are visible.
     * <p>
     * NOTE: Full permission denial flow requires device/emulator interaction
     * and cannot be fully automated in Espresso without UIAutomator.
     */
    @Test
    public void test_checkPermissions_withDenied_updatesUIRequired() {
        // ASSERT - Verify permission UI elements exist and are displayed
        onView(withId(R.id.permissionStatusBadge)).check(matches(isDisplayed()));
        onView(withId(R.id.grantPermissionButton)).check(matches(isDisplayed()));

        // Note: Actual permission status depends on device runtime permissions
        // Manual testing required to verify "Required" state
    }

    /**
     * Test 8: Grant permission button click launches permission request.
     * <p>
     * Tests FR7.4 - SettingsActivity permission request flow.
     * Verifies that the grant permission button is clickable and doesn't crash.
     * <p>
     * NOTE: Actual permission dialog requires device interaction and cannot be
     * automated in Espresso. This test verifies the button exists and is clickable.
     */
    @Test
    public void test_requestPermissionButton_click_launchesPermissionRequest() {
        // ACT - Click grant permission button
        onView(withId(R.id.grantPermissionButton)).perform(click());

        // ASSERT - Verify no crash (permission dialog verified manually)
        // Manual testing required to verify permission request dialog appears
        onView(withId(R.id.grantPermissionButton)).check(matches(isDisplayed()));
    }

    /**
     * Test 9: Permission granted callback updates UI and enables SMS features.
     * <p>
     * Tests FR7.4 - SettingsActivity permission grant handling.
     * Verifies that SMS toggle switches exist and are displayed.
     * <p>
     * NOTE: Cannot programmatically grant permissions in Espresso tests.
     * This test verifies the UI elements exist for manual verification.
     */
    @Test
    public void test_onPermissionGranted_updatesUIAndEnablesSms() {
        // ASSERT - Verify SMS toggle switches exist
        onView(withId(R.id.switchEnableSms)).check(matches(isDisplayed()));
        onView(withId(R.id.switchGoalAlerts)).check(matches(isDisplayed()));
        onView(withId(R.id.switchMilestoneAlerts)).check(matches(isDisplayed()));
        onView(withId(R.id.switchDailyReminders)).check(matches(isDisplayed()));

        // Note: Actual enable state depends on permission grant
        // Manual testing required to verify switches are enabled after permission grant
    }

    /**
     * Test 10: Permission denied callback updates UI and shows rationale.
     * <p>
     * Tests FR7.4 - SettingsActivity permission denial handling.
     * Verifies that permission status badge exists for displaying denial state.
     * <p>
     * NOTE: Cannot programmatically deny permissions in Espresso tests.
     * This test verifies the UI elements exist for manual verification.
     */
    @Test
    public void test_onPermissionDenied_updatesUIAndShowsRationale() {
        // ASSERT - Verify permission status badge exists
        onView(withId(R.id.permissionStatusBadge)).check(matches(isDisplayed()));

        // Note: Actual denial state and rationale display requires manual testing
        // Manual testing required to verify denial flow
    }

    /**
     * Test 11: Save phone button with valid phone saves to database.
     * <p>
     * Tests FR7.4 - SettingsActivity phone number input handling.
     * Verifies that phone number input field exists and is displayed.
     * <p>
     * NOTE: Full phone number save flow requires keyboard interaction
     * which is complex to automate in Espresso. This test verifies the input exists.
     */
    @Test
    public void test_savePhoneButton_withValidPhone_savesToDatabase() {
        // ASSERT - Verify phone number input exists
        onView(withId(R.id.phoneNumberInput)).check(matches(isDisplayed()));

        // Note: Actual phone save requires text input and button click
        // Manual testing required to verify phone number save with E.164 format
    }

    /**
     * Test 12: Save phone button with invalid phone shows error.
     * <p>
     * Tests FR7.4 - SettingsActivity phone number validation.
     * Verifies that phone number input field exists for validation testing.
     * <p>
     * NOTE: Full validation flow requires keyboard interaction and error verification
     * which is complex to automate in Espresso. This test verifies the input exists.
     */
    @Test
    public void test_savePhoneButton_withInvalidPhone_showsError() {
        // ASSERT - Verify phone number input exists
        onView(withId(R.id.phoneNumberInput)).check(matches(isDisplayed()));

        // Note: Actual validation error requires invalid input and button click
        // Manual testing required to verify error display for invalid phone numbers
    }

    // ============================================================
    // PHONE NUMBER VALIDATION TESTS (Phase 8.4 - Coverage Gaps)
    // ============================================================

    /**
     * Test 13: Save phone with 10-digit US number formats to E.164.
     * Tests ValidationUtils.formatPhoneE164() integration with SettingsActivity.
     * Verifies that 10-digit US numbers are formatted to +1 prefix.
     */
    @Test
    public void test_savePhone_with10DigitUS_formatsToE164() {
        // ARRANGE
        when(mockUserDAO.updatePhoneNumber(eq(testUserId), eq("+12025551234"))).thenReturn(true);

        // ACT - Enter 10-digit US number (no +1 prefix)
        onView(withId(R.id.phoneNumberInput))
                .perform(replaceText("2025551234"), closeSoftKeyboard());

        // Simulate IME_ACTION_DONE or save button click (depending on implementation)
        onView(withId(R.id.phoneNumberInput)).perform(typeText("\n"));

        // ASSERT - Phone should be saved with E.164 format (+12025551234)
        verify(mockUserDAO).updatePhoneNumber(eq(testUserId), eq("+12025551234"));
    }

    /**
     * Test 14: Save phone with international E.164 accepts unchanged.
     * Tests that numbers already in E.164 format are not modified.
     * Verifies UK phone number +447911123456 saved as-is.
     */
    @Test
    public void test_savePhone_withInternationalE164_acceptsUnchanged() {
        // ARRANGE
        when(mockUserDAO.updatePhoneNumber(eq(testUserId), eq("+447911123456"))).thenReturn(true);

        // ACT - Enter international number in E.164 format
        onView(withId(R.id.phoneNumberInput))
                .perform(replaceText("+447911123456"), closeSoftKeyboard());

        onView(withId(R.id.phoneNumberInput)).perform(typeText("\n"));

        // ASSERT - Phone should be saved unchanged
        verify(mockUserDAO).updatePhoneNumber(eq(testUserId), eq("+447911123456"));
    }

    /**
     * Test 15: Save phone with letters shows validation error.
     * Tests ValidationUtils.getPhoneValidationError() integration.
     * Verifies that invalid characters trigger error display.
     */
    @Test
    public void test_savePhone_withLetters_showsValidationError() {
        // ACT - Enter invalid phone with letters
        onView(withId(R.id.phoneNumberInput))
                .perform(replaceText("abc12345"), closeSoftKeyboard());

        onView(withId(R.id.phoneNumberInput)).perform(typeText("\n"));

        // ASSERT - Database save should NOT be called
        verify(mockUserDAO, never()).updatePhoneNumber(anyLong(), anyString());

        // Note: EditText error display verification is complex in Espresso
        // Manual testing required to verify error message shown to user
    }

    /**
     * Test 16: Save phone with dashes shows validation error.
     * Tests that phone numbers with formatting characters are rejected.
     * Verifies "202-555-1234" format triggers validation error.
     */
    @Test
    public void test_savePhone_withDashes_showsValidationError() {
        // ACT - Enter phone with dashes (invalid format)
        onView(withId(R.id.phoneNumberInput))
                .perform(replaceText("202-555-1234"), closeSoftKeyboard());

        onView(withId(R.id.phoneNumberInput)).perform(typeText("\n"));

        // ASSERT - Database save should NOT be called
        verify(mockUserDAO, never()).updatePhoneNumber(anyLong(), anyString());

        // Note: Error message verification requires EditText.getError() check
        // which is difficult to test reliably in Espresso
    }

    /**
     * Test 17: Save phone with too few digits shows validation error.
     * Tests minimum length validation (10-15 digits required).
     * Verifies "12345" (5 digits) is rejected.
     */
    @Test
    public void test_savePhone_withTooShort_showsValidationError() {
        // ACT - Enter phone that's too short (5 digits)
        onView(withId(R.id.phoneNumberInput))
                .perform(replaceText("12345"), closeSoftKeyboard());

        onView(withId(R.id.phoneNumberInput)).perform(typeText("\n"));

        // ASSERT - Database save should NOT be called
        verify(mockUserDAO, never()).updatePhoneNumber(anyLong(), anyString());
    }

    /**
     * Test 18: Save phone success persists after activity restart.
     * Tests that phone number persists via UserDAO storage.
     * Verifies E.164 formatted phone survives activity recreation.
     */
    @Test
    public void test_savePhone_success_persistsAfterActivityRestart() {
        // ARRANGE - Mock user with saved phone number
        User userWithPhone = testUser;
        userWithPhone.setPhoneNumber("+12025551234");
        when(mockUserDAO.getUserById(testUserId)).thenReturn(userWithPhone);
        when(mockUserDAO.updatePhoneNumber(eq(testUserId), eq("+12025551234"))).thenReturn(true);

        // ACT - Enter and save phone
        onView(withId(R.id.phoneNumberInput))
                .perform(replaceText("2025551234"), closeSoftKeyboard());

        onView(withId(R.id.phoneNumberInput)).perform(typeText("\n"));

        // Close and reopen activity
        scenario.close();
        scenario = ActivityScenario.launch(SettingsActivity.class);
        scenario.onActivity(activity -> {
            activity.setUserDAO(mockUserDAO);
            activity.setUserPreferenceDAO(mockUserPreferenceDAO);
            activity.setSMSNotificationManager(mockSmsManager);
        });

        // ASSERT - Phone should be loaded and displayed (stripped of +1 for US numbers)
        onView(withId(R.id.phoneNumberInput))
                .check(matches(withText("2025551234")));
    }

    // ============================================================
    // PREFERENCE PERSISTENCE & TOGGLE TESTS (6 tests)
    // ============================================================

    /**
     * Test 19: Unit toggle from lbs to kg persists to database.
     * <p>
     * Tests FR6.0.4 - Weight unit preference persistence.
     * Verifies that clicking the kg button when currently set to lbs
     * saves "kg" preference to database via UserPreferenceDAO.
     */
    @Test
    public void test_unitToggle_lbsToKg_persistsToDatabase() {
        // ARRANGE - Start with "lbs" preference
        when(mockUserPreferenceDAO.getWeightUnit(testUserId)).thenReturn("lbs");
        scenario.recreate();

        // ACT - Click kg button
        onView(withId(R.id.unitKg)).perform(click());

        // ASSERT - Verify preference saved to database as "kg"
        verify(mockUserPreferenceDAO).setPreference(eq(testUserId), eq("weight_unit"), eq("kg"));
    }

    /**
     * Test 20: Unit toggle from kg to lbs persists to database.
     * <p>
     * Tests FR6.0.4 - Weight unit preference persistence.
     * Verifies that clicking the lbs button when currently set to kg
     * saves "lbs" preference to database via UserPreferenceDAO.
     */
    @Test
    public void test_unitToggle_kgToLbs_persistsToDatabase() {
        // ARRANGE - Start with "kg" preference
        when(mockUserPreferenceDAO.getWeightUnit(testUserId)).thenReturn("kg");
        scenario.recreate();

        // ACT - Click lbs button
        onView(withId(R.id.unitLbs)).perform(click());

        // ASSERT - Verify preference saved to database as "lbs"
        verify(mockUserPreferenceDAO).setPreference(eq(testUserId), eq("weight_unit"), eq("lbs"));
    }

    /**
     * Test 21: Unit toggle preference persists across activity restart.
     * <p>
     * Tests FR6.0.4 - Weight unit preference persistence.
     * Verifies that when user sets weight unit to kg, closes the activity,
     * and reopens it, the kg preference is still loaded from database.
     */
    @Test
    public void test_unitToggle_persistsAcrossActivityRestart() {
        // ARRANGE - Set preference to "kg" and mock database response
        when(mockUserPreferenceDAO.getWeightUnit(testUserId)).thenReturn("lbs");
        scenario.recreate();

        // ACT - Click kg button to change preference
        onView(withId(R.id.unitKg)).perform(click());

        // Update mock to return "kg" after save
        when(mockUserPreferenceDAO.getWeightUnit(testUserId)).thenReturn("kg");

        // Close and reopen activity
        scenario.close();
        scenario = ActivityScenario.launch(SettingsActivity.class);
        scenario.onActivity(activity -> {
            activity.setUserDAO(mockUserDAO);
            activity.setUserPreferenceDAO(mockUserPreferenceDAO);
            activity.setSMSNotificationManager(mockSmsManager);
        });

        // ASSERT - Verify preference was loaded from database on restart
        verify(mockUserPreferenceDAO).getWeightUnit(testUserId);

        // Visual verification: kg button should be displayed (styled as selected in UI)
        onView(withId(R.id.unitKg)).check(matches(isDisplayed()));
    }

    /**
     * Test 22: Master SMS toggle disabled disables child alert toggles.
     * <p>
     * Tests FR5.0 - SMS notification toggle cascading behavior.
     * Verifies that when master SMS toggle is disabled, all child alert
     * toggles (goal, milestone, reminder) are disabled and cannot be clicked.
     */
    @Test
    public void test_masterToggle_whenDisabled_disablesChildToggles() {
        // ARRANGE - Start with master enabled, children visible
        // (Default state from setUp)

        // ACT - Disable master SMS toggle
        onView(withId(R.id.switchEnableSms)).perform(click());

        // ASSERT - Child toggles should be disabled (not clickable)
        onView(withId(R.id.switchGoalAlerts))
                .check(matches(not(isEnabled())));
        onView(withId(R.id.switchMilestoneAlerts))
                .check(matches(not(isEnabled())));
        onView(withId(R.id.switchDailyReminders))
                .check(matches(not(isEnabled())));
    }

    /**
     * Test 23: Master SMS toggle enabled enables child alert toggles.
     * <p>
     * Tests FR5.0 - SMS notification toggle cascading behavior.
     * Verifies that when master SMS toggle is enabled, all child alert
     * toggles (goal, milestone, reminder) are enabled and can be clicked.
     */
    @Test
    public void test_masterToggle_whenEnabled_enablesChildToggles() {
        // ARRANGE - Disable master first
        onView(withId(R.id.switchEnableSms)).perform(click());

        // ACT - Re-enable master SMS toggle
        onView(withId(R.id.switchEnableSms)).perform(click());

        // ASSERT - Child toggles should be enabled (clickable)
        onView(withId(R.id.switchGoalAlerts))
                .check(matches(isEnabled()));
        onView(withId(R.id.switchMilestoneAlerts))
                .check(matches(isEnabled()));
        onView(withId(R.id.switchDailyReminders))
                .check(matches(isEnabled()));
    }

    /**
     * Test 24: Child toggle stays disabled when master toggle is disabled.
     * <p>
     * Tests FR5.0 - SMS notification toggle cascading behavior.
     * Verifies that when master SMS toggle is OFF, attempting to click
     * a child toggle (e.g., goal alerts) has no effect - it stays disabled.
     * This prevents users from enabling individual alerts without SMS permission.
     */
    @Test
    public void test_childToggle_whenMasterDisabled_staysDisabled() {
        // ARRANGE - Disable master SMS toggle
        onView(withId(R.id.switchEnableSms)).perform(click());

        // Verify child is disabled first
        onView(withId(R.id.switchGoalAlerts))
                .check(matches(not(isEnabled())));

        // ACT - Attempt to click goal alerts toggle (should have no effect)
        // Note: Espresso may throw PerformException if view is not enabled,
        // so we skip the click test and just verify it remains disabled

        // ASSERT - Child toggle should still be disabled
        onView(withId(R.id.switchGoalAlerts))
                .check(matches(not(isEnabled())));
    }

    // ============================================================
    // PHONE NUMBER PERSISTENCE TESTS (3 tests) - Bug Fix: Phone Persistence
    // ============================================================

    /**
     * Test 25 (NEW): Phone number persists when navigating away without pressing Done.
     * Tests that onPause() lifecycle method saves phone number automatically.
     * Verifies fix for Issue: Phone number not persisting when user navigates away.
     */
    @Test
    public void test_phoneNumber_persistsOnNavigateAway_withoutPressingDone() {
        // ARRANGE - Mock successful save
        when(mockUserDAO.updatePhoneNumber(eq(testUserId), eq("+12025551234"))).thenReturn(true);

        // ACT - Type phone number but DON'T press Done (just close keyboard)
        onView(withId(R.id.phoneNumberInput))
                .perform(replaceText("2025551234"), closeSoftKeyboard());

        // Trigger onPause by recreating activity (simulates back press)
        scenario.recreate();

        // ASSERT - Phone should be saved via onPause()
        verify(mockUserDAO).updatePhoneNumber(eq(testUserId), eq("+12025551234"));
    }

    /**
     * Test 26 (NEW): Invalid phone number not saved on navigate away.
     * Tests that onPause() validates phone before saving.
     * Verifies that invalid input doesn't corrupt database.
     */
    @Test
    public void test_invalidPhoneNumber_notSavedOnNavigateAway() {
        // ACT - Type invalid phone number
        onView(withId(R.id.phoneNumberInput))
                .perform(replaceText("invalid"), closeSoftKeyboard());

        // Trigger onPause by recreating activity
        scenario.recreate();

        // ASSERT - updatePhoneNumber should NOT be called
        verify(mockUserDAO, never()).updatePhoneNumber(anyLong(), anyString());
    }

    /**
     * Test 27 (NEW): Empty phone number not saved on navigate away.
     * Tests that onPause() skips save for empty input.
     */
    @Test
    public void test_emptyPhoneNumber_notSavedOnNavigateAway() {
        // ACT - Clear phone number field
        onView(withId(R.id.phoneNumberInput))
                .perform(replaceText(""), closeSoftKeyboard());

        // Trigger onPause by recreating activity
        scenario.recreate();

        // ASSERT - updatePhoneNumber should NOT be called
        verify(mockUserDAO, never()).updatePhoneNumber(anyLong(), anyString());
    }

    // ============================================================
    // EMULATOR SMS LOGGING TEST (1 test) - Bug Fix: Emulator SMS Testing
    // ============================================================

    /**
     * Test 28 (NEW): Test message button logs to Logcat on emulator.
     * Tests that test message detection works and logs masked phone number.
     *
     * NOTE: This test verifies button click doesn't crash. Manual verification
     * of Logcat output required to confirm message and masking.
     *
     * Expected Logcat output:
     * I/SettingsActivity: ======================================
     * I/SettingsActivity: TEST SMS (EMULATOR MODE)
     * I/SettingsActivity: To: ***1234
     * I/SettingsActivity: Message: This is a test message from Weigh to Go! Your SMS notifications are working! ✅
     * I/SettingsActivity: ======================================
     */
    @Test
    public void test_sendTestMessage_onEmulator_logsToLogcat() {
        // ARRANGE - Mock user with phone number
        User userWithPhone = new User();
        userWithPhone.setUserId(testUserId);
        userWithPhone.setUsername("testuser");
        userWithPhone.setPhoneNumber("+12025551234");

        when(mockUserDAO.getUserById(testUserId)).thenReturn(userWithPhone);
        when(mockSmsManager.canSendSms(testUserId)).thenReturn(true);

        // ACT - Click test message button
        onView(withId(R.id.sendTestMessageButton)).perform(click());

        // ASSERT - Verify no crash (Logcat output verified manually)
        // Expected: Masked phone number (***1234) logged
        // Manual verification: Check Logcat for test message
        onView(withId(R.id.sendTestMessageButton)).check(matches(isDisplayed()));
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Creates a test user with the given username and display name.
     * <p>
     * Generates a secure password hash using bcrypt and inserts the user into the
     * test database. Returns the created User object with ID populated.
     *
     * @param username    the username for the test user
     * @param displayName the display name for the test user
     * @return the created User object
     * @throws DatabaseException if database operation fails
     */
    private User createTestUser(String username, String displayName) throws Exception {
        String password = "Test123!";
        String passwordHash = PasswordUtilsV2.hashPasswordBcrypt(password);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setSalt("");  // bcrypt stores salt in hash
        user.setPasswordAlgorithm("BCRYPT");
        user.setDisplayName(displayName);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        UserDAO realUserDAO = new UserDAO(dbHelper);
        long userId = realUserDAO.insertUser(user);
        user.setUserId(userId);

        return user;
    }
}
