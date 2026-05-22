package com.example.weightogo.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.example.weightogo.models.GoalWeight;
import com.example.weightogo.models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for GoalWeightDAO.
 * Tests all CRUD operations using Robolectric for in-memory database.
 */
@RunWith(RobolectricTestRunner.class)
public class GoalWeightDAOTest {

    private WeighToGoDBHelper dbHelper;
    private GoalWeightDAO goalWeightDAO;
    private UserDAO userDAO;
    private long testUserId;

    @Before
    public void setUp() throws DatabaseException {
        Context context = RuntimeEnvironment.getApplication();
        dbHelper = WeighToGoDBHelper.getInstance(context);
        goalWeightDAO = new GoalWeightDAO(dbHelper);
        userDAO = new UserDAO(dbHelper);

        // Delete any existing test user from previous runs (database is singleton)
        User existingUser = userDAO.getUserByUsername("testuser");
        if (existingUser != null) {
            userDAO.deleteUser(existingUser.getUserId());
        }

        // Create a test user for foreign key relationships
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hash123");
        testUser.setSalt("salt123");
        testUser.setPasswordAlgorithm("SHA256");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser.setActive(true);

        testUserId = userDAO.insertUser(testUser);
        assertTrue("Test user should be created", testUserId > 0);
    }

    @After
    public void tearDown() {
        // Clean up test data
        if (testUserId > 0) {
            userDAO.deleteUser(testUserId);
        }

        // Properly clean up singleton database instance
        try {
            if (dbHelper != null) {
                dbHelper.close();
            }
        } finally {
            dbHelper = null;
            goalWeightDAO = null;
            userDAO = null;
            RuntimeEnvironment.getApplication().deleteDatabase("weigh_to_go.db");
            WeighToGoDBHelper.resetInstance();
        }
    }

    @Test
    public void test_insertGoal_withValidData_returnsGoalId() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        goal.setUserId(testUserId);
        goal.setGoalWeight(150.0);
        goal.setGoalUnit("lbs");
        goal.setStartWeight(180.0);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
        goal.setActive(true);
        goal.setAchieved(false);

        // ACT
        long goalId = goalWeightDAO.insertGoal(goal);

        // ASSERT
        assertTrue("Goal ID should be greater than 0", goalId > 0);
    }

    @Test
    public void test_insertGoal_withTargetDate_savesTargetDate() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        goal.setUserId(testUserId);
        goal.setGoalWeight(150.0);
        goal.setGoalUnit("lbs");
        goal.setStartWeight(180.0);
        goal.setTargetDate(LocalDate.of(2026, 6, 1));
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
        goal.setActive(true);
        goal.setAchieved(false);

        // ACT
        long goalId = goalWeightDAO.insertGoal(goal);
        GoalWeight activeGoal = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertNotNull("Active goal should be retrieved", activeGoal);
        assertEquals("Target date should match", LocalDate.of(2026, 6, 1), activeGoal.getTargetDate());
    }

    @Test
    public void test_getActiveGoal_withActiveGoal_returnsGoal() {
        // ARRANGE
        GoalWeight goal = createTestGoal(testUserId, 150.0, 180.0, true, false);
        goalWeightDAO.insertGoal(goal);

        // ACT
        GoalWeight activeGoal = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertNotNull("Active goal should be found", activeGoal);
        assertEquals("Goal weight should match", 150.0, activeGoal.getGoalWeight(), 0.01);
        assertEquals("Start weight should match", 180.0, activeGoal.getStartWeight(), 0.01);
        assertTrue("Goal should be active", activeGoal.isActive());
        assertFalse("Goal should not be achieved", activeGoal.isAchieved());
    }

    @Test
    public void test_getActiveGoal_withNoActiveGoal_returnsNull() {
        // ARRANGE
        GoalWeight inactiveGoal = createTestGoal(testUserId, 150.0, 180.0, false, false);
        goalWeightDAO.insertGoal(inactiveGoal);

        // ACT
        GoalWeight activeGoal = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertNull("Should return null when no active goal exists", activeGoal);
    }

    @Test
    public void test_getActiveGoal_withMultipleGoals_returnsMostRecent() {
        try {
            // ARRANGE - Insert two active goals (edge case)
            GoalWeight goal1 = createTestGoal(testUserId, 150.0, 180.0, true, false);
            GoalWeight goal2 = createTestGoal(testUserId, 145.0, 175.0, true, false);

            Thread.sleep(10); // Ensure different created_at timestamps
            goalWeightDAO.insertGoal(goal1);
            Thread.sleep(10);
            goalWeightDAO.insertGoal(goal2);

            // ACT
            GoalWeight activeGoal = goalWeightDAO.getActiveGoal(testUserId);

            // ASSERT
            assertNotNull("Active goal should be found", activeGoal);
            assertEquals("Should return most recent active goal", 145.0, activeGoal.getGoalWeight(), 0.01);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_getGoalHistory_withMultipleGoals_returnsAllGoals() {
        // ARRANGE
        GoalWeight goal1 = createTestGoal(testUserId, 150.0, 180.0, false, true);
        GoalWeight goal2 = createTestGoal(testUserId, 145.0, 175.0, true, false);
        GoalWeight goal3 = createTestGoal(testUserId, 140.0, 170.0, false, false);

        goalWeightDAO.insertGoal(goal1);
        goalWeightDAO.insertGoal(goal2);
        goalWeightDAO.insertGoal(goal3);

        // ACT
        List<GoalWeight> history = goalWeightDAO.getGoalHistory(testUserId);

        // ASSERT
        assertEquals("Should return all 3 goals", 3, history.size());
        // Should be ordered by created_at DESC (most recent first)
    }

    @Test
    public void test_getGoalHistory_withNoGoals_returnsEmptyList() throws DatabaseException {
        // Create a second user with no goals
        User user2 = new User();
        user2.setUsername("user2");
        user2.setPasswordHash("hash");
        user2.setSalt("salt");
        user2.setPasswordAlgorithm("SHA256");
        user2.setCreatedAt(LocalDateTime.now());
        user2.setUpdatedAt(LocalDateTime.now());
        user2.setActive(true);
        long user2Id = userDAO.insertUser(user2);

        // ACT
        List<GoalWeight> history = goalWeightDAO.getGoalHistory(user2Id);

        // ASSERT
        assertNotNull("List should not be null", history);
        assertEquals("List should be empty", 0, history.size());
    }

    @Test
    public void test_updateGoal_withValidData_updatesGoal() {
        // ARRANGE
        GoalWeight goal = createTestGoal(testUserId, 150.0, 180.0, true, false);
        long goalId = goalWeightDAO.insertGoal(goal);

        goal.setGoalId(goalId);
        goal.setGoalWeight(145.0);
        goal.setAchieved(true);
        goal.setAchievedDate(LocalDate.now());

        // ACT
        int rowsUpdated = goalWeightDAO.updateGoal(goal);
        GoalWeight activeGoal = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertEquals("Should update 1 row", 1, rowsUpdated);
        assertNotNull("Goal should still exist", activeGoal);
        assertEquals("Goal weight should be updated", 145.0, activeGoal.getGoalWeight(), 0.01);
        assertTrue("Goal should be marked as achieved", activeGoal.isAchieved());
        assertNotNull("Achieved date should be set", activeGoal.getAchievedDate());
    }

    @Test
    public void test_deactivateGoal_setsIsActiveFalse() {
        // ARRANGE
        GoalWeight goal = createTestGoal(testUserId, 150.0, 180.0, true, false);
        long goalId = goalWeightDAO.insertGoal(goal);

        // ACT
        int rowsUpdated = goalWeightDAO.deactivateGoal(goalId);
        GoalWeight activeGoal = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertEquals("Should update 1 row", 1, rowsUpdated);
        assertNull("Should have no active goal after deactivation", activeGoal);
    }

    @Test
    public void test_deactivateAllGoalsForUser_deactivatesMultipleGoals() {
        // ARRANGE
        GoalWeight goal1 = createTestGoal(testUserId, 150.0, 180.0, true, false);
        GoalWeight goal2 = createTestGoal(testUserId, 145.0, 175.0, true, false);
        GoalWeight goal3 = createTestGoal(testUserId, 140.0, 170.0, false, false); // Already inactive

        goalWeightDAO.insertGoal(goal1);
        goalWeightDAO.insertGoal(goal2);
        goalWeightDAO.insertGoal(goal3);

        // ACT
        int rowsUpdated = goalWeightDAO.deactivateAllGoalsForUser(testUserId);
        GoalWeight activeGoal = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertEquals("Should update 2 rows (2 active goals)", 2, rowsUpdated);
        assertNull("Should have no active goal after deactivation", activeGoal);
    }

    @Test
    public void test_deactivateAllGoalsForUser_withNoActiveGoals_updatesZeroRows() {
        // ARRANGE
        GoalWeight inactiveGoal = createTestGoal(testUserId, 150.0, 180.0, false, false);
        goalWeightDAO.insertGoal(inactiveGoal);

        // ACT
        int rowsUpdated = goalWeightDAO.deactivateAllGoalsForUser(testUserId);

        // ASSERT
        assertEquals("Should update 0 rows when no active goals exist", 0, rowsUpdated);
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    public void test_insertGoal_withInvalidUserId_violatesForeignKey() {
        // ARRANGE
        long invalidUserId = 99999; // Non-existent user
        GoalWeight goal = createTestGoal(invalidUserId, 150.0, 180.0, true, false);

        // ACT
        long goalId = goalWeightDAO.insertGoal(goal);

        // ASSERT
        assertEquals("Insert with invalid user_id should fail due to foreign key constraint", -1, goalId);
    }

    @Test
    public void test_updateGoal_withNonExistentGoal_returnsZero() {
        // ARRANGE
        GoalWeight nonExistent = createTestGoal(testUserId, 150.0, 180.0, true, false);
        nonExistent.setGoalId(99999); // Non-existent ID

        // ACT
        int rowsUpdated = goalWeightDAO.updateGoal(nonExistent);

        // ASSERT
        assertEquals("Updating non-existent goal should return 0 rows updated", 0, rowsUpdated);
    }

    @Test
    public void test_deactivateGoal_withNonExistentGoal_returnsZero() {
        // ACT
        int rowsUpdated = goalWeightDAO.deactivateGoal(99999);

        // ASSERT
        assertEquals("Deactivating non-existent goal should return 0 rows updated", 0, rowsUpdated);
    }

    @Test
    public void test_insertGoal_withNegativeGoalWeight_insertsSuccessfully() {
        // ARRANGE - Database allows it, app should validate
        GoalWeight goal = createTestGoal(testUserId, -10.0, 180.0, true, false);

        // ACT
        long goalId = goalWeightDAO.insertGoal(goal);
        GoalWeight retrieved = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertTrue("Database should allow negative goal weight (app should validate)", goalId > 0);
        assertNotNull("Goal should be retrieved", retrieved);
        assertEquals("Negative goal weight should be preserved", -10.0, retrieved.getGoalWeight(), 0.01);
    }

    @Test
    public void test_insertGoal_withZeroGoalWeight_insertsSuccessfully() {
        // ARRANGE
        GoalWeight goal = createTestGoal(testUserId, 0.0, 180.0, true, false);

        // ACT
        long goalId = goalWeightDAO.insertGoal(goal);
        GoalWeight retrieved = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertTrue("Database should allow zero goal weight", goalId > 0);
        assertNotNull("Goal should be retrieved", retrieved);
        assertEquals("Zero goal weight should be preserved", 0.0, retrieved.getGoalWeight(), 0.01);
    }

    @Test
    public void test_insertGoal_withSameStartAndGoalWeight_insertsSuccessfully() {
        // ARRANGE - Edge case: no actual goal (start == goal)
        GoalWeight goal = createTestGoal(testUserId, 150.0, 150.0, true, false);

        // ACT
        long goalId = goalWeightDAO.insertGoal(goal);
        GoalWeight retrieved = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertTrue("Database should allow same start and goal weight", goalId > 0);
        assertNotNull("Goal should be retrieved", retrieved);
        assertEquals("Goal weight should match start weight", 150.0, retrieved.getGoalWeight(), 0.01);
        assertEquals("Start weight should match goal weight", 150.0, retrieved.getStartWeight(), 0.01);
    }

    @Test
    public void test_insertGoal_withTargetDateInPast_insertsSuccessfully() {
        // ARRANGE
        GoalWeight goal = createTestGoal(testUserId, 150.0, 180.0, true, false);
        goal.setTargetDate(LocalDate.of(2020, 1, 1)); // Past date

        // ACT
        long goalId = goalWeightDAO.insertGoal(goal);
        GoalWeight retrieved = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertTrue("Database should allow past target dates", goalId > 0);
        assertNotNull("Goal should be retrieved", retrieved);
        assertEquals("Past target date should be preserved", LocalDate.of(2020, 1, 1), retrieved.getTargetDate());
    }

    @Test
    public void test_insertGoal_withVeryFarFutureTargetDate_insertsSuccessfully() {
        // ARRANGE
        GoalWeight goal = createTestGoal(testUserId, 150.0, 180.0, true, false);
        goal.setTargetDate(LocalDate.of(2099, 12, 31)); // Far future

        // ACT
        long goalId = goalWeightDAO.insertGoal(goal);
        GoalWeight retrieved = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertTrue("Database should allow far future target dates", goalId > 0);
        assertNotNull("Goal should be retrieved", retrieved);
        assertEquals("Far future target date should be preserved", LocalDate.of(2099, 12, 31), retrieved.getTargetDate());
    }

    @Test
    public void test_insertGoal_withAchievedDateButNotAchieved_insertsSuccessfully() {
        // ARRANGE - Data inconsistency: achievedDate set but isAchieved=false
        GoalWeight goal = createTestGoal(testUserId, 150.0, 180.0, true, false);
        goal.setAchievedDate(LocalDate.now()); // Set achieved date
        // isAchieved is false

        // ACT
        long goalId = goalWeightDAO.insertGoal(goal);
        GoalWeight retrieved = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertTrue("Database should allow inconsistent achieved data", goalId > 0);
        assertNotNull("Goal should be retrieved", retrieved);
        assertFalse("Goal should not be marked as achieved", retrieved.isAchieved());
        assertNotNull("Achieved date should still be present (inconsistent state)", retrieved.getAchievedDate());
    }

    @Test
    public void test_insertGoal_withAchievedTrueButNoDate_insertsSuccessfully() {
        // ARRANGE - Data inconsistency: isAchieved=true but no achievedDate
        GoalWeight goal = createTestGoal(testUserId, 150.0, 180.0, true, true);
        // achievedDate is null

        // ACT
        long goalId = goalWeightDAO.insertGoal(goal);
        GoalWeight retrieved = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertTrue("Database should allow inconsistent achieved data", goalId > 0);
        assertNotNull("Goal should be retrieved", retrieved);
        assertTrue("Goal should be marked as achieved", retrieved.isAchieved());
        assertNull("Achieved date should be null (inconsistent state)", retrieved.getAchievedDate());
    }

    @Test
    public void test_getGoalHistory_orderedByCreatedAtDesc() {
        // ARRANGE - Insert goals in specific order
        GoalWeight goal1 = createTestGoal(testUserId, 150.0, 180.0, false, false);
        GoalWeight goal2 = createTestGoal(testUserId, 145.0, 175.0, false, false);
        GoalWeight goal3 = createTestGoal(testUserId, 140.0, 170.0, true, false);

        try {
            long id1 = goalWeightDAO.insertGoal(goal1);
            Thread.sleep(10); // Ensure different timestamps
            long id2 = goalWeightDAO.insertGoal(goal2);
            Thread.sleep(10);
            long id3 = goalWeightDAO.insertGoal(goal3);

            // ACT
            List<GoalWeight> history = goalWeightDAO.getGoalHistory(testUserId);

            // ASSERT
            assertEquals("Should return 3 goals", 3, history.size());
            // Most recent (id3) should be first
            assertEquals("First goal should be most recently created", id3, history.get(0).getGoalId());
            assertEquals("Second goal should be middle", id2, history.get(1).getGoalId());
            assertEquals("Third goal should be oldest", id1, history.get(2).getGoalId());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_deactivateAllGoalsForUser_withNonExistentUser_returnsZero() {
        // ACT
        int rowsUpdated = goalWeightDAO.deactivateAllGoalsForUser(99999);

        // ASSERT
        assertEquals("Deactivating goals for non-existent user should return 0", 0, rowsUpdated);
    }

    @Test
    public void test_insertGoal_withExtremelyLargeWeights_insertsSuccessfully() {
        // ARRANGE
        GoalWeight goal = createTestGoal(testUserId, 999999.99, 999999.99, true, false);

        // ACT
        long goalId = goalWeightDAO.insertGoal(goal);
        GoalWeight retrieved = goalWeightDAO.getActiveGoal(testUserId);

        // ASSERT
        assertTrue("Database should allow extremely large weights", goalId > 0);
        assertNotNull("Goal should be retrieved", retrieved);
        assertEquals("Large goal weight should be preserved", 999999.99, retrieved.getGoalWeight(), 0.01);
        assertEquals("Large start weight should be preserved", 999999.99, retrieved.getStartWeight(), 0.01);
    }

    @Test
    public void test_updateGoal_canSetGoalToAchievedAndInactive() {
        // ARRANGE
        GoalWeight goal = createTestGoal(testUserId, 150.0, 180.0, true, false);
        long goalId = goalWeightDAO.insertGoal(goal);

        goal.setGoalId(goalId);
        goal.setAchieved(true);
        goal.setActive(false); // Achieved and inactive
        goal.setAchievedDate(LocalDate.now());

        // ACT
        int rowsUpdated = goalWeightDAO.updateGoal(goal);
        GoalWeight activeGoal = goalWeightDAO.getActiveGoal(testUserId);
        List<GoalWeight> history = goalWeightDAO.getGoalHistory(testUserId);

        // ASSERT
        assertEquals("Should update 1 row", 1, rowsUpdated);
        assertNull("Should have no active goal", activeGoal);
        assertEquals("Should still be in history", 1, history.size());
        assertTrue("Goal should be marked as achieved", history.get(0).isAchieved());
        assertFalse("Goal should be inactive", history.get(0).isActive());
    }

    // Helper method to create test goals
    private GoalWeight createTestGoal(long userId, double goalWeight, double startWeight, boolean isActive, boolean isAchieved) {
        GoalWeight goal = new GoalWeight();
        goal.setUserId(userId);
        goal.setGoalWeight(goalWeight);
        goal.setGoalUnit("lbs");
        goal.setStartWeight(startWeight);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
        goal.setActive(isActive);
        goal.setAchieved(isAchieved);
        return goal;
    }
}
