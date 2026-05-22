package com.example.weightogo.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.example.weightogo.models.Achievement;
import com.example.weightogo.models.GoalWeight;
import com.example.weightogo.models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Unit tests for AchievementDAO.
 * Tests all CRUD operations using Robolectric for in-memory database.
 * Following strict TDD: Red-Green-Refactor cycle.
 */
@RunWith(RobolectricTestRunner.class)
public class AchievementDAOTest {

    private WeighToGoDBHelper dbHelper;
    private AchievementDAO achievementDAO;
    private UserDAO userDAO;
    private GoalWeightDAO goalWeightDAO;
    private long testUserId;
    private long testGoalId;

    @Before
    public void setUp() throws DuplicateUsernameException, DatabaseException {
        Context context = RuntimeEnvironment.getApplication();
        dbHelper = WeighToGoDBHelper.getInstance(context);
        achievementDAO = new AchievementDAO(dbHelper);
        userDAO = new UserDAO(dbHelper);
        goalWeightDAO = new GoalWeightDAO(dbHelper);

        // Create test user for foreign key relationships
        User testUser = new User();
        testUser.setUsername("testuser_achievements_" + System.currentTimeMillis()); // Unique username
        testUser.setPasswordHash("hash123");
        testUser.setSalt("salt123");
        testUser.setPasswordAlgorithm("SHA256");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser.setActive(true);

        testUserId = userDAO.insertUser(testUser);
        assertTrue("Test user should be created", testUserId > 0);

        // Create test goal for foreign key relationships
        GoalWeight testGoal = new GoalWeight();
        testGoal.setUserId(testUserId);
        testGoal.setGoalWeight(150.0);
        testGoal.setGoalUnit("lbs");
        testGoal.setStartWeight(180.0);
        testGoal.setCreatedAt(LocalDateTime.now());
        testGoal.setUpdatedAt(LocalDateTime.now());
        testGoal.setActive(true);
        testGoal.setAchieved(false);

        testGoalId = goalWeightDAO.insertGoal(testGoal);
        assertTrue("Test goal should be created", testGoalId > 0);
    }

    @After
    public void tearDown() {
        // Clean up test data (cascade delete handles achievements)
        if (testUserId > 0) {
            userDAO.deleteUser(testUserId);
        }
        // Don't close dbHelper - it's a singleton
    }

    // ========== CRUD Operations Tests ==========

    @Test
    public void test_insertAchievement_withValidData_returnsAchievementId() {
        // ARRANGE
        Achievement achievement = new Achievement();
        achievement.setUserId(testUserId);
        achievement.setGoalId(testGoalId);
        achievement.setAchievementType("GOAL_REACHED");
        achievement.setTitle("Goal Reached!");
        achievement.setDescription("You reached your goal of 150 lbs");
        achievement.setValue(150.0);
        achievement.setAchievedAt(LocalDateTime.now());
        achievement.setNotified(false);

        // ACT
        long achievementId = achievementDAO.insertAchievement(achievement);

        // ASSERT
        assertTrue("Achievement ID should be greater than 0", achievementId > 0);
    }

    @Test
    public void test_getAchievementsForUser_withMultiple_returnsAllUserAchievements() {
        // ARRANGE
        Achievement achievement1 = createTestAchievement("FIRST_ENTRY", "First Step!", false);
        Achievement achievement2 = createTestAchievement("MILESTONE_5", "5 lbs Lost!", false);
        Achievement achievement3 = createTestAchievement("STREAK_7", "7-Day Streak!", false);

        long id1 = achievementDAO.insertAchievement(achievement1);
        long id2 = achievementDAO.insertAchievement(achievement2);
        long id3 = achievementDAO.insertAchievement(achievement3);

        assertTrue("All achievements should be inserted", id1 > 0 && id2 > 0 && id3 > 0);

        // ACT
        List<Achievement> achievements = achievementDAO.getAchievementsForUser(testUserId);

        // ASSERT
        assertNotNull("Achievements list should not be null", achievements);
        assertEquals("Should have 3 achievements", 3, achievements.size());
    }

    @Test
    public void test_getAchievementsByType_filtersCorrectly() {
        // ARRANGE
        Achievement milestone5 = createTestAchievement("MILESTONE_5", "5 lbs Lost!", false);
        Achievement milestone10 = createTestAchievement("MILESTONE_10", "10 lbs Lost!", false);
        Achievement streak7 = createTestAchievement("STREAK_7", "7-Day Streak!", false);

        achievementDAO.insertAchievement(milestone5);
        achievementDAO.insertAchievement(milestone10);
        achievementDAO.insertAchievement(streak7);

        // ACT
        List<Achievement> milestoneAchievements = achievementDAO.getAchievementsByType(
                testUserId, "MILESTONE_5");

        // ASSERT
        assertNotNull("Filtered list should not be null", milestoneAchievements);
        assertEquals("Should have 1 MILESTONE_5 achievement", 1, milestoneAchievements.size());
        assertEquals("Should be MILESTONE_5 type", "MILESTONE_5",
                milestoneAchievements.get(0).getAchievementType());
    }

    @Test
    public void test_updateIsNotified_setsFlag() {
        // ARRANGE
        Achievement achievement = createTestAchievement("GOAL_REACHED", "Goal Reached!", false);
        long achievementId = achievementDAO.insertAchievement(achievement);
        assertTrue("Achievement should be inserted", achievementId > 0);

        // ACT
        int rowsUpdated = achievementDAO.updateIsNotified(achievementId, true);

        // ASSERT
        assertEquals("Should update 1 row", 1, rowsUpdated);

        // Verify flag was updated
        List<Achievement> achievements = achievementDAO.getAchievementsForUser(testUserId);
        assertFalse("Achievements list should not be empty", achievements.isEmpty());
        assertTrue("isNotified should be true", achievements.get(0).isNotified());
    }

    @Test
    public void test_getUnnotifiedAchievements_returnsOnlyUnnotified() {
        // ARRANGE
        Achievement notified = createTestAchievement("FIRST_ENTRY", "First!", true);
        Achievement unnotified1 = createTestAchievement("MILESTONE_5", "5 lbs!", false);
        Achievement unnotified2 = createTestAchievement("STREAK_7", "Streak!", false);

        long id1 = achievementDAO.insertAchievement(notified);
        long id2 = achievementDAO.insertAchievement(unnotified1);
        long id3 = achievementDAO.insertAchievement(unnotified2);

        assertTrue("All should be inserted", id1 > 0 && id2 > 0 && id3 > 0);

        // ACT
        List<Achievement> unnotifiedAchievements = achievementDAO.getUnnotifiedAchievements(testUserId);

        // ASSERT
        assertNotNull("Unnotified list should not be null", unnotifiedAchievements);
        assertEquals("Should have 2 unnotified achievements", 2, unnotifiedAchievements.size());
        for (Achievement achievement : unnotifiedAchievements) {
            assertFalse("All should be unnotified", achievement.isNotified());
        }
    }

    // ========== Edge Cases Tests ==========

    @Test
    public void test_insertAchievement_withInvalidUserId_returnsMinus1() {
        // ARRANGE
        Achievement achievement = new Achievement();
        achievement.setUserId(99999); // Non-existent user
        achievement.setAchievementType("GOAL_REACHED");
        achievement.setTitle("Test");
        achievement.setAchievedAt(LocalDateTime.now());
        achievement.setNotified(false);

        // ACT
        long result = achievementDAO.insertAchievement(achievement);

        // ASSERT
        assertEquals("Should return -1 for foreign key violation", -1, result);
    }

    @Test
    public void test_insertAchievement_withNullGoalId_insertsSuccessfully() {
        // ARRANGE - goalId is optional for achievements like FIRST_ENTRY
        Achievement achievement = new Achievement();
        achievement.setUserId(testUserId);
        achievement.setGoalId(null); // NULL goal ID
        achievement.setAchievementType("FIRST_ENTRY");
        achievement.setTitle("First Step!");
        achievement.setDescription("You logged your first weight entry");
        achievement.setAchievedAt(LocalDateTime.now());
        achievement.setNotified(false);

        // ACT
        long achievementId = achievementDAO.insertAchievement(achievement);

        // ASSERT
        assertTrue("Should insert with null goalId", achievementId > 0);

        // Verify goalId is null when retrieved
        List<Achievement> achievements = achievementDAO.getAchievementsForUser(testUserId);
        assertFalse("List should not be empty", achievements.isEmpty());
        assertNull("GoalId should be null", achievements.get(0).getGoalId());
    }

    @Test
    public void test_getAchievementsForUser_withNoAchievements_returnsEmptyList() {
        // ARRANGE - no achievements inserted for testUserId

        // ACT
        List<Achievement> achievements = achievementDAO.getAchievementsForUser(testUserId);

        // ASSERT
        assertNotNull("List should not be null", achievements);
        assertTrue("List should be empty", achievements.isEmpty());
        assertEquals("Size should be 0", 0, achievements.size());
    }

    @Test
    public void test_hasAchievementType_withExisting_returnsTrue() {
        // ARRANGE
        Achievement achievement = createTestAchievement("GOAL_REACHED", "Goal!", false);
        long id = achievementDAO.insertAchievement(achievement);
        assertTrue("Achievement should be inserted", id > 0);

        // ACT
        boolean exists = achievementDAO.hasAchievementType(testUserId, "GOAL_REACHED");

        // ASSERT
        assertTrue("GOAL_REACHED should exist for user", exists);
    }

    @Test
    public void test_getAchievementsForUser_orderedByAchievedAtDesc() {
        // ARRANGE - Insert 3 achievements with different timestamps
        LocalDateTime now = LocalDateTime.now();

        Achievement old = createTestAchievement("FIRST_ENTRY", "First!", false);
        old.setAchievedAt(now.minusDays(2));

        Achievement middle = createTestAchievement("MILESTONE_5", "5 lbs!", false);
        middle.setAchievedAt(now.minusDays(1));

        Achievement recent = createTestAchievement("STREAK_7", "Streak!", false);
        recent.setAchievedAt(now);

        // Insert in random order
        achievementDAO.insertAchievement(middle);
        achievementDAO.insertAchievement(recent);
        achievementDAO.insertAchievement(old);

        // ACT
        List<Achievement> achievements = achievementDAO.getAchievementsForUser(testUserId);

        // ASSERT
        assertEquals("Should have 3 achievements", 3, achievements.size());
        assertEquals("First should be most recent", "STREAK_7",
                achievements.get(0).getAchievementType());
        assertEquals("Second should be middle", "MILESTONE_5",
                achievements.get(1).getAchievementType());
        assertEquals("Third should be oldest", "FIRST_ENTRY",
                achievements.get(2).getAchievementType());
    }

    // ========== Business Logic Tests ==========

    @Test
    public void test_hasAchievementType_withNonExisting_returnsFalse() {
        // ARRANGE - no achievements inserted

        // ACT
        boolean exists = achievementDAO.hasAchievementType(testUserId, "GOAL_REACHED");

        // ASSERT
        assertFalse("GOAL_REACHED should not exist", exists);
    }

    @Test
    public void test_getLatestAchievement_returnsMostRecent() {
        // ARRANGE
        LocalDateTime now = LocalDateTime.now();

        Achievement old = createTestAchievement("FIRST_ENTRY", "First!", false);
        old.setAchievedAt(now.minusDays(5));

        Achievement latest = createTestAchievement("GOAL_REACHED", "Goal!", false);
        latest.setAchievedAt(now);

        achievementDAO.insertAchievement(old);
        achievementDAO.insertAchievement(latest);

        // ACT
        Achievement latestAchievement = achievementDAO.getLatestAchievement(testUserId);

        // ASSERT
        assertNotNull("Latest achievement should not be null", latestAchievement);
        assertEquals("Should be GOAL_REACHED", "GOAL_REACHED",
                latestAchievement.getAchievementType());
    }

    @Test
    public void test_insertAchievement_withValue_savesCorrectly() {
        // ARRANGE
        Achievement achievement = createTestAchievement("MILESTONE_10", "10 lbs Lost!", false);
        achievement.setValue(10.0); // Milestone value

        // ACT
        long id = achievementDAO.insertAchievement(achievement);

        // ASSERT
        assertTrue("Achievement should be inserted", id > 0);

        // Verify value was saved
        List<Achievement> achievements = achievementDAO.getAchievementsForUser(testUserId);
        assertFalse("List should not be empty", achievements.isEmpty());
        assertNotNull("Value should not be null", achievements.get(0).getValue());
        assertEquals("Value should be 10.0", 10.0, achievements.get(0).getValue(), 0.001);
    }

    @Test
    public void test_getLatestAchievement_withNoAchievements_returnsNull() {
        // ARRANGE - no achievements

        // ACT
        Achievement latest = achievementDAO.getLatestAchievement(testUserId);

        // ASSERT
        assertNull("Latest should be null when no achievements exist", latest);
    }

    // ========== Helper Methods ==========

    /**
     * Helper method to create a test achievement with common defaults.
     */
    private Achievement createTestAchievement(String type, String title, boolean isNotified) {
        Achievement achievement = new Achievement();
        achievement.setUserId(testUserId);
        achievement.setGoalId(testGoalId);
        achievement.setAchievementType(type);
        achievement.setTitle(title);
        achievement.setDescription("Test description for " + title);
        achievement.setAchievedAt(LocalDateTime.now());
        achievement.setNotified(isNotified);
        return achievement;
    }
}
