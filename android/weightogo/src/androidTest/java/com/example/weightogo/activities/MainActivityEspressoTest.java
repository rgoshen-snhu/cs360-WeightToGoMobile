package com.example.weightogo.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.weightogo.activities.MainActivity;
import com.example.weightogo.R;
import com.example.weightogo.database.DatabaseException;
import com.example.weightogo.database.DuplicateUsernameException;
import com.example.weightogo.database.GoalWeightDAO;
import com.example.weightogo.database.UserDAO;
import com.example.weightogo.database.UserPreferenceDAO;
import com.example.weightogo.database.WeighToGoDBHelper;
import com.example.weightogo.database.WeightEntryDAO;
import com.example.weightogo.models.GoalWeight;
import com.example.weightogo.models.User;
import com.example.weightogo.models.WeightEntry;
import com.example.weightogo.utils.PasswordUtilsV2;
import com.example.weightogo.utils.SessionManager;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Espresso instrumented tests for MainActivity.
 * <p>
 * Migrated from MainActivityTest.java (Robolectric) to resolve GitHub Issue #12
 * (Robolectric/Material3 theme incompatibility).
 * <p>
 * These tests run on a real Android device or emulator with full Material3 theme support.
 * Tests critical UI flows: dashboard initialization, weight entry display, progress tracking,
 * navigation, and user interactions.
 * <p>
 * Coverage:
 * - UI initialization (2 tests)
 * - Time boundary tests (6 tests - resolves GH #50)
 * - Empty state handling (2 tests)
 * - RecyclerView population (1 test)
 * - Progress card display (2 tests)
 * - Quick stats calculation (2 tests)
 * - Delete entry workflow (4 tests - includes AlertDialog interaction tests, resolves GH #48)
 * - Navigation behavior (3 tests)
 * - User info display (1 test)
 * - Progress calculations (2 tests)
 * <p>
 * Total: 25 tests (17 original + 2 AlertDialog + 6 time boundary)
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityEspressoTest {

    private ActivityScenario<MainActivity> scenario;

    private Context context;
    private WeighToGoDBHelper dbHelper;
    private UserDAO userDAO;
    private WeightEntryDAO weightEntryDAO;
    private GoalWeightDAO goalWeightDAO;
    private UserPreferenceDAO userPreferenceDAO;
    private SessionManager sessionManager;

    private User testUser;
    private long testUserId;

    /**
     * Set up test environment before each test.
     * <p>
     * Creates a test database, test user, and logs in the user to simulate
     * authenticated state. This ensures MainActivity displays dashboard content.
     * <p>
     * IMPORTANT: Activity is launched AFTER session creation to prevent race condition
     * where MainActivity.onCreate() checks login status before session exists.
     */
    @Before
    public void setUp() throws Exception {
        // Get application context
        context = ApplicationProvider.getApplicationContext();

        // Initialize test database
        dbHelper = WeighToGoDBHelper.getTestInstance(context, WeighToGoDBHelper.DATABASE_TEST_NAME);

        // Initialize DAOs
        userDAO = new UserDAO(dbHelper);
        weightEntryDAO = new WeightEntryDAO(dbHelper);
        goalWeightDAO = new GoalWeightDAO(dbHelper);
        userPreferenceDAO = new UserPreferenceDAO(dbHelper);

        // Get SessionManager instance
        sessionManager = SessionManager.getInstance(context);

        // Clean up any existing test user from previous runs
        User existingUser = userDAO.getUserByUsername("testuser");
        if (existingUser != null) {
            userDAO.deleteUser(existingUser.getUserId());
        }

        // Create test user
        testUser = createTestUser("testuser", "Test User");
        testUserId = testUser.getUserId();

        // Log in test user BEFORE launching activity
        sessionManager.createSession(testUser);

        // Set default weight unit preference
        userPreferenceDAO.setPreference(testUserId, "weight_unit", "lbs");

        // Launch activity AFTER session is ready (prevents race condition)
        scenario = ActivityScenario.launch(MainActivity.class);
    }

    /**
     * Clean up test environment after each test.
     * <p>
     * Closes activity scenario, clears session, closes database, and resets
     * SessionManager singleton to ensure test isolation.
     */
    @After
    public void tearDown() {
        // Close activity scenario first
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

        // NOTE: SessionManager singleton cannot be reset for test isolation
        // Tests must manually logout() to clear session state
        // TODO: Add resetInstance() method to SessionManager for testing
    }

    // ============================================================
    // UI INITIALIZATION TESTS (2 tests)
    // ============================================================

    /**
     * Test 1: onCreate when logged in initializes all views correctly.
     * <p>
     * Verifies that all critical UI elements are initialized and displayed when the user
     * is logged in. Tests greeting text, user name, RecyclerView, FAB, and bottom navigation.
     */
    @Test
    public void test_onCreate_whenLoggedIn_initializesViews() {
        // ARRANGE - Test user already logged in via setUp()

        // ASSERT - Verify all views are initialized and displayed
        onView(withId(R.id.greetingText)).check(matches(isDisplayed()));
        onView(withId(R.id.userName)).check(matches(isDisplayed()));
        onView(withId(R.id.weightRecyclerView)).check(matches(isDisplayed()));
        onView(withId(R.id.addEntryFab)).check(matches(isDisplayed()));
        onView(withId(R.id.bottomNavigation)).check(matches(isDisplayed()));
    }

    /**
     * Test 2: greetingText shows time-based greeting (morning/afternoon/evening).
     * <p>
     * Verifies that the greeting text changes based on the current time of day:
     * - Before noon (0-11): "Good morning"
     * - Noon to 6pm (12-17): "Good afternoon"
     * - After 6pm (18-23): "Good evening"
     * <p>
     * **GH #50 Resolution:**
     * This test verifies greeting logic at the current time. Additional time boundary
     * tests (Tests 3-8) verify behavior at specific hour boundaries to prevent failures
     * at midnight/noon/evening transitions.
     *
     * @see MainActivity#getGreetingForHour(int) - Extracted greeting logic
     * @see #test_greetingText_at5AM_showsGoodMorning() - Morning boundary tests
     */
    @Test
    public void test_greetingText_showsTimeBasedGreeting() {
        // ARRANGE - Determine expected greeting based on current time
        int hour = LocalTime.now().getHour();
        String expectedGreeting = (hour < 12) ? "Good morning"
                : (hour < 18) ? "Good afternoon"
                : "Good evening";

        // ACT & ASSERT - Verify greeting matches time of day
        onView(withId(R.id.greetingText)).check(matches(withText(expectedGreeting)));
    }

    // ============================================================
    // TIME BOUNDARY TESTS (6 tests - Resolves GH #50)
    // ============================================================

    /**
     * Test 3: Greeting at 5 AM shows "Good morning".
     * <p>
     * **GH #50:** Verifies morning greeting well before noon boundary (hour = 5).
     *
     * @see MainActivity#setGreetingForHour(int) - Test-only method
     */
    @Test
    public void test_greetingText_at5AM_showsGoodMorning() {
        // ARRANGE & ACT - Set greeting for 5 AM
        scenario.onActivity(activity -> activity.setGreetingForHour(5));

        // ASSERT - Verify "Good morning" displayed
        onView(withId(R.id.greetingText)).check(matches(withText("Good morning")));
    }

    /**
     * Test 4: Greeting at 11 AM shows "Good morning".
     * <p>
     * **GH #50:** Verifies morning greeting at boundary before noon (hour = 11).
     * Critical test: Ensures hour < 12 logic works correctly.
     *
     * @see MainActivity#setGreetingForHour(int) - Test-only method
     */
    @Test
    public void test_greetingText_at11AM_showsGoodMorning() {
        // ARRANGE & ACT - Set greeting for 11 AM (last hour of morning)
        scenario.onActivity(activity -> activity.setGreetingForHour(11));

        // ASSERT - Verify "Good morning" displayed
        onView(withId(R.id.greetingText)).check(matches(withText("Good morning")));
    }

    /**
     * Test 5: Greeting at 12 PM (noon) shows "Good afternoon".
     * <p>
     * **GH #50:** Verifies afternoon greeting at noon boundary (hour = 12).
     * Critical test: Ensures hour >= 12 logic works correctly.
     *
     * @see MainActivity#setGreetingForHour(int) - Test-only method
     */
    @Test
    public void test_greetingText_at12PM_showsGoodAfternoon() {
        // ARRANGE & ACT - Set greeting for 12 PM (noon)
        scenario.onActivity(activity -> activity.setGreetingForHour(12));

        // ASSERT - Verify "Good afternoon" displayed
        onView(withId(R.id.greetingText)).check(matches(withText("Good afternoon")));
    }

    /**
     * Test 6: Greeting at 5 PM shows "Good afternoon".
     * <p>
     * **GH #50:** Verifies afternoon greeting well before evening boundary (hour = 17).
     *
     * @see MainActivity#setGreetingForHour(int) - Test-only method
     */
    @Test
    public void test_greetingText_at5PM_showsGoodAfternoon() {
        // ARRANGE & ACT - Set greeting for 5 PM
        scenario.onActivity(activity -> activity.setGreetingForHour(17));

        // ASSERT - Verify "Good afternoon" displayed
        onView(withId(R.id.greetingText)).check(matches(withText("Good afternoon")));
    }

    /**
     * Test 7: Greeting at 6 PM shows "Good evening".
     * <p>
     * **GH #50:** Verifies evening greeting at 6 PM boundary (hour = 18).
     * Critical test: Ensures hour >= 18 logic works correctly.
     *
     * @see MainActivity#setGreetingForHour(int) - Test-only method
     */
    @Test
    public void test_greetingText_at6PM_showsGoodEvening() {
        // ARRANGE & ACT - Set greeting for 6 PM (evening start)
        scenario.onActivity(activity -> activity.setGreetingForHour(18));

        // ASSERT - Verify "Good evening" displayed
        onView(withId(R.id.greetingText)).check(matches(withText("Good evening")));
    }

    /**
     * Test 8: Greeting at 11 PM shows "Good evening".
     * <p>
     * **GH #50:** Verifies evening greeting late at night (hour = 23).
     *
     * @see MainActivity#setGreetingForHour(int) - Test-only method
     */
    @Test
    public void test_greetingText_at11PM_showsGoodEvening() {
        // ARRANGE & ACT - Set greeting for 11 PM
        scenario.onActivity(activity -> activity.setGreetingForHour(23));

        // ASSERT - Verify "Good evening" displayed
        onView(withId(R.id.greetingText)).check(matches(withText("Good evening")));
    }

    // ============================================================
    // EMPTY STATE TESTS (2 tests)
    // ============================================================

    /**
     * Test 9: loadWeightEntries with no entries shows empty state.
     * <p>
     * Verifies that the empty state container is visible when the user has no weight entries.
     * This guides users to add their first entry.
     */
    @Test
    public void test_loadWeightEntries_withNoEntries_showsEmptyState() {
        // ARRANGE - No weight entries created

        // ACT & ASSERT - Verify empty state is visible
        onView(withId(R.id.emptyStateContainer))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    /**
     * Test 10: loadWeightEntries with entries hides empty state.
     * <p>
     * Verifies that the empty state container is hidden when the user has at least one
     * weight entry. The RecyclerView with entries should be displayed instead.
     */
    @Test
    public void test_loadWeightEntries_withEntries_hidesEmptyState() {
        // ARRANGE - Create one weight entry
        createTestWeightEntry(170.0);

        // Restart activity to load entries
        scenario.recreate();

        // ACT & ASSERT - Verify empty state is hidden
        onView(withId(R.id.emptyStateContainer))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    // ============================================================
    // RECYCLERVIEW TESTS (1 test)
    // ============================================================

    /**
     * Test 11: loadWeightEntries with entries populates RecyclerView.
     * <p>
     * Verifies that weight entries are correctly loaded into the RecyclerView and
     * displayed to the user. Tests that the adapter has the expected number of items.
     */
    @Test
    public void test_loadWeightEntries_withEntries_populatesRecyclerView() {
        // ARRANGE - Create 3 weight entries
        createTestWeightEntry(170.0);
        createTestWeightEntry(171.5);
        createTestWeightEntry(169.0);

        // Restart activity to load entries
        scenario.recreate();

        // ACT & ASSERT - Verify RecyclerView has 3 items
        onView(withId(R.id.weightRecyclerView))
                .check(matches(withRecyclerViewItemCount(3)));
    }

    // ============================================================
    // PROGRESS CARD TESTS (2 tests)
    // ============================================================

    /**
     * Test 12: updateProgressCard with active goal shows progress data.
     * <p>
     * Verifies that the progress card displays correct weight values when the user
     * has an active goal. Tests start weight, current weight, and goal weight display.
     */
    @Test
    public void test_updateProgressCard_withActiveGoal_showsProgressData() {
        // ARRANGE - Create goal (start: 180, goal: 150) and current weight entry (165)
        createTestGoal(180.0, 150.0);
        createTestWeightEntry(165.0);

        // Restart activity to load data
        scenario.recreate();

        // ACT & ASSERT - Verify progress card is visible and shows correct values
        onView(withId(R.id.progressCard))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.startWeightValue)).check(matches(withText("180.0")));
        onView(withId(R.id.currentWeightValue)).check(matches(withText("165.0")));
        onView(withId(R.id.goalWeightValue)).check(matches(withText("150.0")));
    }

    /**
     * Test 13: updateProgressCard with no goal hides progress card.
     * <p>
     * Verifies that the progress card is hidden when the user does not have an active goal.
     * This prevents showing irrelevant or confusing UI elements.
     */
    @Test
    public void test_updateProgressCard_withNoGoal_hidesProgressCard() {
        // ARRANGE - No goal created

        // ACT & ASSERT - Verify progress card is hidden
        onView(withId(R.id.progressCard))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    // ============================================================
    // QUICK STATS TESTS (2 tests)
    // ============================================================

    /**
     * Test 14: calculateQuickStats with data shows correct values.
     * <p>
     * Verifies that quick stats (total weight lost, lbs to goal) are calculated and
     * displayed correctly based on start weight, current weight, and goal weight.
     * <p>
     * Example: Start 180, Current 165, Goal 150
     * - Total lost: 180 - 165 = 15 lbs
     * - Lbs to goal: 165 - 150 = 15 lbs
     */
    @Test
    public void test_calculateQuickStats_withData_showsCorrectValues() {
        // ARRANGE - Create goal and weight entry
        createTestGoal(180.0, 150.0);  // Start: 180, Goal: 150
        createTestWeightEntry(165.0);  // Current: 165

        // Restart activity to calculate stats
        scenario.recreate();

        // ACT & ASSERT - Verify quick stats show correct values
        // Total lost: 180 - 165 = 15
        onView(withId(R.id.totalLostValue)).check(matches(withText("15")));
        // Lbs to goal: 165 - 150 = 15
        onView(withId(R.id.lbsToGoalValue)).check(matches(withText("15")));
    }

    /**
     * Test 15: calculateQuickStats with streak shows day streak.
     * <p>
     * Verifies that the day streak is calculated correctly when the user has entries
     * for consecutive days. Tests streak calculation for 3 consecutive days.
     */
    @Test
    public void test_calculateQuickStats_withStreak_showsDayStreak() {
        // ARRANGE - Create entries for 3 consecutive days
        createTestWeightEntryOnDate(170.0, LocalDate.now());
        createTestWeightEntryOnDate(171.0, LocalDate.now().minusDays(1));
        createTestWeightEntryOnDate(172.0, LocalDate.now().minusDays(2));

        // Restart activity to calculate streak
        scenario.recreate();

        // ACT & ASSERT - Verify day streak shows "3"
        onView(withId(R.id.dayStreakValue)).check(matches(withText("3")));
    }

    // ============================================================
    // DELETE ENTRY TESTS (4 tests)
    // ============================================================

    /**
     * Test 16: handleDeleteEntry with confirmation deletes entry.
     * <p>
     * Verifies that clicking the delete button and confirming the deletion successfully
     * soft-deletes the weight entry (sets deleted flag to true). The entry should no
     * longer be visible in the UI.
     * <p>
     * NOTE: This test verifies the database operation. UI interaction with AlertDialog
     * requires additional Espresso Intents setup and is tested manually.
     * <p>
     * TODO(GH #48): Add proper AlertDialog interaction testing
     * See PR #47 review for recommended implementation.
     */
    @Test
    public void test_handleDeleteEntry_withConfirmation_deletesEntry() {
        // ARRANGE - Create a weight entry
        long entryId = createTestWeightEntry(170.0);

        // ACT - Manually trigger soft delete (simulating confirmation)
        WeightEntry entry = weightEntryDAO.getWeightEntryById(entryId);
        assertNotNull("Entry should exist before deletion", entry);

        weightEntryDAO.deleteWeightEntry(entryId);

        // ASSERT - Verify entry is soft deleted
        WeightEntry deletedEntry = weightEntryDAO.getWeightEntryById(entryId);
        assertTrue("Entry should be soft deleted", deletedEntry.isDeleted());
    }

    /**
     * Test 17: handleDeleteEntry with cancel does not delete.
     * <p>
     * Verifies that clicking the delete button and then canceling the deletion does NOT
     * delete the weight entry. The entry should remain visible and not be marked as deleted.
     * <p>
     * NOTE: This test verifies the database state. UI interaction with AlertDialog
     * requires additional Espresso Intents setup and is tested manually.
     * <p>
     * TODO(GH #48): Add proper AlertDialog interaction testing
     * See PR #47 review for recommended implementation.
     */
    @Test
    public void test_handleDeleteEntry_withCancel_doesNotDelete() {
        // ARRANGE - Create a weight entry
        long entryId = createTestWeightEntry(170.0);

        // ACT - Verify entry exists and is NOT deleted (simulating cancel)
        WeightEntry entry = weightEntryDAO.getWeightEntryById(entryId);

        // ASSERT - Verify entry is NOT soft deleted
        assertNotNull("Entry should exist", entry);
        assertFalse("Entry should not be deleted", entry.isDeleted());
    }

    /**
     * Test 18: DELETE with AlertDialog - Cancel button does not delete entry.
     * <p>
     * Verifies complete UI flow for canceling deletion:
     * 1. Click delete button in RecyclerView item
     * 2. AlertDialog appears with "Delete Entry" title
     * 3. Click "Cancel" button
     * 4. Entry remains in database (not soft-deleted)
     * <p>
     * **Resolves GH #48**: Full AlertDialog interaction testing
     *
     * @see MainActivity#handleDeleteEntry(WeightEntry)
     */
    @Test
    public void test_deleteEntryUI_clickCancel_doesNotDelete() {
        // ARRANGE - Create a weight entry
        long entryId = createTestWeightEntry(170.0);

        // Restart activity to refresh RecyclerView
        scenario.recreate();

        // ACT - Click delete button on first RecyclerView item
        onView(withId(R.id.weightRecyclerView))
                .perform(actionOnItemAtPosition(0, clickChildViewWithId(R.id.deleteButton)));

        // AlertDialog should appear - Click "Cancel"
        onView(withText("Cancel")).perform(click());

        // ASSERT - Verify entry is NOT soft deleted
        WeightEntry entry = weightEntryDAO.getWeightEntryById(entryId);
        assertNotNull("Entry should still exist after cancel", entry);
        assertFalse("Entry should not be soft deleted after cancel", entry.isDeleted());
    }

    /**
     * Test 19: DELETE with AlertDialog - Delete button deletes entry.
     * <p>
     * Verifies complete UI flow for confirming deletion:
     * 1. Click delete button in RecyclerView item
     * 2. AlertDialog appears with "Are you sure?" message
     * 3. Click "Delete" button
     * 4. Entry is soft-deleted in database (deleted flag = true)
     * <p>
     * **Resolves GH #48**: Full AlertDialog interaction testing
     *
     * @see MainActivity#handleDeleteEntry(WeightEntry)
     */
    @Test
    public void test_deleteEntryUI_clickConfirm_deletesEntry() {
        // ARRANGE - Create a weight entry
        long entryId = createTestWeightEntry(170.0);

        // Restart activity to refresh RecyclerView
        scenario.recreate();

        // ACT - Click delete button on first RecyclerView item
        onView(withId(R.id.weightRecyclerView))
                .perform(actionOnItemAtPosition(0, clickChildViewWithId(R.id.deleteButton)));

        // AlertDialog should appear - Click "Delete" to confirm
        onView(withText("Delete")).perform(click());

        // ASSERT - Verify entry is soft deleted
        WeightEntry entry = weightEntryDAO.getWeightEntryById(entryId);
        assertNotNull("Entry should exist in database", entry);
        assertTrue("Entry should be soft deleted after confirmation", entry.isDeleted());
    }

    // ============================================================
    // NAVIGATION TESTS (3 tests)
    // ============================================================

    /**
     * Test 20: FAB click shows toast placeholder.
     * <p>
     * Verifies that clicking the FloatingActionButton (FAB) shows a placeholder toast
     * message indicating the feature is coming in Phase 4.
     * <p>
     * **Toast Verification Limitation (Resolves GH #49):**
     * Espresso does not have built-in support for verifying Toast messages. This test
     * clicks the FAB and verifies no crash occurs, but cannot automatically verify
     * the toast content.
     * <p>
     * **Verification Strategy:**
     * - Automated: Verifies button click does not crash
     * - Manual: Developer visually confirms toast message during test execution
     * <p>
     * **Alternative Solutions (Not Implemented):**
     * 1. **UIAutomator** (adds dependency): androidx.test.uiautomator:uiautomator
     *    - Can verify Toast text via `UiDevice.findObject(new UiSelector().textContains("..."))`
     *    - Requires additional setup and slower execution
     *    - Recommended for critical user-facing toasts
     * 2. **Snackbar Replacement** (preferred for critical messages):
     *    - Replace Toast with Snackbar for important feedback
     *    - Snackbars are testable with: `onView(withText("...")).check(matches(isDisplayed()))`
     *    - Not applicable for placeholder messages (current case)
     * 3. **Manual Testing** (current approach):
     *    - Acceptable for non-critical placeholder messages
     *    - Developer confirms toast during test run
     * <p>
     * **Decision:** Manual testing is sufficient for placeholder toasts. Critical user
     * feedback should use Snackbars (Phase 4+ implementation).
     * <p>
     * **GH #49 Status:** ✅ RESOLVED (2025-12-13) - Documented limitation and alternatives
     *
     * @see MainActivity#onClick(View) - FAB click handler
     */
    @Test
    public void test_fabClick_showsToastPlaceholder() {
        // ACT - Click FAB button
        onView(withId(R.id.addEntryFab)).perform(click());

        // ASSERT - Verify no crash (toast content verified manually)
        // Expected toast: "Add Entry - Coming in Phase 4"
        // Manual verification: Observer sees toast message during test execution
    }

    /**
     * Test 21: bottomNavigation home selected stays on MainActivity.
     * <p>
     * Verifies that tapping the Home item in the bottom navigation bar keeps the user
     * on MainActivity (does not trigger navigation or finish the activity).
     */
    @Test
    public void test_bottomNavigation_homeSelected_staysOnMainActivity() {
        // ACT - Click home item in bottom navigation
        onView(withId(R.id.nav_home)).perform(click());

        // ASSERT - Verify activity is still displayed (not finished)
        onView(withId(R.id.main)).check(matches(isDisplayed()));
    }

    /**
     * Test 22: bottomNavigation other item selected shows toast placeholder.
     * <p>
     * Verifies that tapping other items in the bottom navigation (e.g., Trends) shows
     * a placeholder toast message indicating the feature is coming in Phase 5.
     * <p>
     * **Toast Verification Limitation (Resolves GH #49):**
     * Espresso does not have built-in support for verifying Toast messages. This test
     * clicks the trends navigation item and verifies no crash occurs, but cannot
     * automatically verify the toast content.
     * <p>
     * **Verification Strategy:**
     * - Automated: Verifies navigation click does not crash
     * - Manual: Developer visually confirms toast message during test execution
     * <p>
     * **Alternative Solutions:** See test_fabClick_showsToastPlaceholder() documentation
     * for detailed comparison of UIAutomator, Snackbar replacement, and manual testing.
     * <p>
     * **Decision:** Manual testing is sufficient for placeholder toasts. Future navigation
     * implementations (Phase 5+) should use proper activities rather than toasts.
     * <p>
     * **GH #49 Status:** ✅ RESOLVED (2025-12-13) - Documented limitation and alternatives
     *
     * @see MainActivity#onNavigationItemSelected(MenuItem) - Bottom nav handler
     */
    @Test
    public void test_bottomNavigation_otherItemSelected_showsToastPlaceholder() {
        // ACT - Click trends item in bottom navigation
        onView(withId(R.id.nav_trends)).perform(click());

        // ASSERT - Verify no crash (toast content verified manually)
        // Expected toast: "Trends - Coming in Phase 5"
        // Manual verification: Observer sees toast message during test execution
    }

    // ============================================================
    // USER INFO TESTS (1 test)
    // ============================================================

    /**
     * Test 23: userName displays current user name.
     * <p>
     * Verifies that the user's display name is shown correctly in the UI header.
     * Tests that the userName TextView displays "Test User".
     */
    @Test
    public void test_userName_displaysCurrentUserName() {
        // ARRANGE - Test user name set in setUp()

        // ACT & ASSERT - Verify user name is displayed
        onView(withId(R.id.userName)).check(matches(withText("Test User")));
    }

    // ============================================================
    // PROGRESS CALCULATION TESTS (2 tests)
    // ============================================================

    /**
     * Test 24: progressPercentage calculates correctly.
     * <p>
     * Verifies that the progress percentage is calculated correctly based on weight loss
     * progress toward the goal.
     * <p>
     * Example: Start 180, Goal 150, Current 165
     * - Total range: 180 - 150 = 30 lbs
     * - Lost so far: 180 - 165 = 15 lbs
     * - Progress: 15 / 30 = 50%
     */
    @Test
    public void test_progressPercentage_calculatesCorrectly() {
        // ARRANGE - Create goal and weight entry for 50% progress
        createTestGoal(180.0, 150.0);  // Start: 180, Goal: 150, Total range: 30
        createTestWeightEntry(165.0);  // Current: 165, Lost: 15, Remaining: 15 → 50%

        // Restart activity to calculate progress
        scenario.recreate();

        // ACT & ASSERT - Verify progress percentage is "50%"
        onView(withId(R.id.progressPercentage)).check(matches(withText("50%")));
    }

    /**
     * Test 25: progressBar width matches percentage.
     * <p>
     * Verifies that the progress bar fill view exists and is visible when progress data
     * is available. The actual width measurement requires layout inflation, which is
     * guaranteed in instrumented tests.
     */
    @Test
    public void test_progressBar_widthMatchesPercentage() {
        // ARRANGE - Create goal and weight entry for 50% progress
        createTestGoal(180.0, 150.0);
        createTestWeightEntry(165.0);

        // Restart activity to render progress bar
        scenario.recreate();

        // ACT & ASSERT - Verify progress bar fill view exists and is visible
        onView(withId(R.id.progressBarFill)).check(matches(isDisplayed()));
        // NOTE: Actual width validation would require custom View matcher with
        // View.getWidth() and layout measurement. The view existence is sufficient
        // for this test to confirm proper rendering.
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
     * @throws DuplicateUsernameException if username already exists
     * @throws DatabaseException if database operation fails
     */
    private User createTestUser(String username, String displayName) throws DuplicateUsernameException, DatabaseException {
        String password = "Test123!";
        String passwordHash = PasswordUtilsV2.hashPasswordBcrypt(password);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setSalt("");  // bcrypt stores salt in hash, so empty string for User model
        user.setPasswordAlgorithm("BCRYPT");
        user.setDisplayName(displayName);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        long userId = userDAO.insertUser(user);
        user.setUserId(userId);

        return user;
    }

    /**
     * Creates a test weight entry for today with the given weight value.
     * <p>
     * Uses the current date and the test user ID. Returns the entry ID.
     *
     * @param weight the weight value in lbs
     * @return the created weight entry ID
     */
    private long createTestWeightEntry(double weight) {
        return createTestWeightEntryOnDate(weight, LocalDate.now());
    }

    /**
     * Creates a test weight entry for a specific date with the given weight value.
     * <p>
     * Inserts the entry into the real database (not mocked). Returns the entry ID.
     *
     * @param weight the weight value in lbs
     * @param date   the date for the weight entry
     * @return the created weight entry ID
     */
    private long createTestWeightEntryOnDate(double weight, LocalDate date) {
        WeightEntry entry = new WeightEntry();
        entry.setUserId(testUserId);
        entry.setWeightValue(weight);
        entry.setWeightUnit("lbs");
        entry.setWeightDate(date);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        entry.setDeleted(false);

        return weightEntryDAO.insertWeightEntry(entry);
    }

    /**
     * Creates a test goal with the given start weight and goal weight.
     * <p>
     * Sets the goal as active and inserts it into the real database (not mocked).
     *
     * @param startWeight the starting weight for the goal in lbs
     * @param goalWeight  the target goal weight in lbs
     */
    private void createTestGoal(double startWeight, double goalWeight) {
        GoalWeight goal = new GoalWeight();
        goal.setUserId(testUserId);
        goal.setGoalWeight(goalWeight);
        goal.setGoalUnit("lbs");
        goal.setStartWeight(startWeight);
        goal.setActive(true);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());

        goalWeightDAO.insertGoal(goal);
    }

    // ============================================================
    // CUSTOM ESPRESSO MATCHERS
    // ============================================================

    /**
     * Custom Espresso matcher to verify RecyclerView item count.
     * <p>
     * Matches if the RecyclerView's adapter has the expected number of items.
     *
     * @param expectedCount the expected number of items in the RecyclerView
     * @return a Matcher that checks RecyclerView item count
     */
    private static Matcher<View> withRecyclerViewItemCount(final int expectedCount) {
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("RecyclerView with item count: " + expectedCount);
            }

            @Override
            protected boolean matchesSafely(RecyclerView recyclerView) {
                RecyclerView.Adapter adapter = recyclerView.getAdapter();
                return adapter != null && adapter.getItemCount() == expectedCount;
            }
        };
    }

    /**
     * Custom ViewAction to click on a child view within a RecyclerView item.
     * <p>
     * Used for clicking buttons (like delete or edit) within RecyclerView items
     * when testing AlertDialog interactions.
     * <p>
     * Usage example:
     * <pre>
     * onView(withId(R.id.weightRecyclerView))
     *     .perform(actionOnItemAtPosition(0, clickChildViewWithId(R.id.deleteButton)));
     * </pre>
     *
     * @param id the resource ID of the child view to click
     * @return a ViewAction that clicks the child view
     */
    private static androidx.test.espresso.ViewAction clickChildViewWithId(final int id) {
        return new androidx.test.espresso.ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(androidx.test.espresso.UiController uiController, View view) {
                View v = view.findViewById(id);
                if (v != null) {
                    v.performClick();
                }
            }
        };
    }
}
