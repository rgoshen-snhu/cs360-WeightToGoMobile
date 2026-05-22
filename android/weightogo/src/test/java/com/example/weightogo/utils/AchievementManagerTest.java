package com.example.weightogo.utils;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.weightogo.database.AchievementDAO;
import com.example.weightogo.database.DatabaseException;
import com.example.weightogo.database.GoalWeightDAO;
import com.example.weightogo.database.UserDAO;
import com.example.weightogo.database.WeighToGoDBHelper;
import com.example.weightogo.database.WeightEntryDAO;
import com.example.weightogo.models.Achievement;
import com.example.weightogo.models.GoalWeight;
import com.example.weightogo.models.User;
import com.example.weightogo.models.WeightEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Unit tests for AchievementManager.
 * Tests FR3.1 - Achievement detection and tracking.
 */
@RunWith(RobolectricTestRunner.class)
public class AchievementManagerTest {

    private Context context;
    private WeighToGoDBHelper dbHelper;
    private SQLiteDatabase db;
    private AchievementDAO achievementDAO;
    private GoalWeightDAO goalWeightDAO;
    private WeightEntryDAO weightEntryDAO;
    private UserDAO userDAO;
    private AchievementManager achievementManager;

    private long testUserId;

    @Before
    public void setUp() throws DatabaseException {
        context = RuntimeEnvironment.getApplication();
        dbHelper = WeighToGoDBHelper.getInstance(context);
        db = dbHelper.getWritableDatabase();

        achievementDAO = new AchievementDAO(dbHelper);
        goalWeightDAO = new GoalWeightDAO(dbHelper);
        weightEntryDAO = new WeightEntryDAO(dbHelper);
        userDAO = new UserDAO(dbHelper);

        achievementManager = new AchievementManager(
                achievementDAO,
                goalWeightDAO,
                weightEntryDAO
        );

        // Create test user with unique username to avoid conflicts across tests
        User testUser = new User();
        testUser.setUsername("testuser_" + System.currentTimeMillis());
        testUser.setPasswordHash("hashedpassword");
        testUser.setSalt("salt");
        testUser.setPasswordAlgorithm("SHA256");
        testUser.setCreatedAt(LocalDateTime.now());
        testUserId = userDAO.insertUser(testUser);
    }

    @After
    public void tearDown() {
        try {
            if (db != null && db.isOpen()) {
                db.close();
            }
            if (dbHelper != null) {
                dbHelper.close();
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Tests FR3.1.1 - GOAL_REACHED achievement
     * Verify that GOAL_REACHED achievement is awarded when user reaches goal weight.
     */
    @Test
    public void test_checkAchievements_goalReached_awardsGoalReachedAchievement() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        goal.setUserId(testUserId);
        goal.setGoalWeight(150.0);
        goal.setStartWeight(180.0);
        goal.setGoalUnit("lbs");
        goal.setActive(true);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());

        goalWeightDAO.setNewActiveGoal(goal);

        // ACT
        List<Achievement> achievements = achievementManager.checkAchievements(testUserId, 150.0);

        // ASSERT
        assertNotNull("Should return list of achievements", achievements);
        boolean hasGoalReached = achievements.stream()
                .anyMatch(a -> "GOAL_REACHED".equals(a.getAchievementType()));
        assertTrue("Should award GOAL_REACHED achievement", hasGoalReached);
    }

    /**
     * Tests FR3.1.2 - FIRST_ENTRY achievement
     * Verify that FIRST_ENTRY achievement is awarded on first weight entry.
     */
    @Test
    public void test_checkAchievements_firstEntry_awardsFirstEntryAchievement() {
        // ARRANGE - No existing weight entries

        // ACT
        List<Achievement> achievements = achievementManager.checkAchievements(testUserId, 180.0);

        // ASSERT
        boolean hasFirstEntry = achievements.stream()
                .anyMatch(a -> "FIRST_ENTRY".equals(a.getAchievementType()));
        assertTrue("Should award FIRST_ENTRY achievement", hasFirstEntry);
    }

    /**
     * Tests FR3.1.3 - STREAK_7 achievement
     * Verify that STREAK_7 achievement is awarded after 7 consecutive days.
     */
    @Test
    public void test_checkAchievements_sevenDayStreak_awardsStreak7Achievement() {
        // ARRANGE
        LocalDate today = LocalDate.now();

        // Create 7 consecutive entries (checkAchievements called after entry saved)
        for (int i = 0; i < 7; i++) {
            WeightEntry entry = new WeightEntry();
            entry.setUserId(testUserId);
            entry.setWeightValue(180.0 - i);
            entry.setWeightUnit("lbs");
            entry.setWeightDate(today.minusDays(i));
            entry.setCreatedAt(LocalDateTime.now());
            entry.setUpdatedAt(LocalDateTime.now());
            weightEntryDAO.insertWeightEntry(entry);
        }

        // ACT
        List<Achievement> achievements = achievementManager.checkAchievements(testUserId, 180.0);

        // ASSERT
        boolean hasStreak7 = achievements.stream()
                .anyMatch(a -> "STREAK_7".equals(a.getAchievementType()));
        assertTrue("Should award STREAK_7 achievement", hasStreak7);
    }

    /**
     * Tests FR3.1.4 - STREAK_30 achievement
     * Verify that STREAK_30 achievement is awarded after 30 consecutive days.
     */
    @Test
    public void test_checkAchievements_thirtyDayStreak_awardsStreak30Achievement() {
        // ARRANGE
        LocalDate today = LocalDate.now();

        // Create 30 consecutive entries (checkAchievements called after entry saved)
        for (int i = 0; i < 30; i++) {
            WeightEntry entry = new WeightEntry();
            entry.setUserId(testUserId);
            entry.setWeightValue(180.0 - i * 0.1);
            entry.setWeightUnit("lbs");
            entry.setWeightDate(today.minusDays(i));
            entry.setCreatedAt(LocalDateTime.now());
            entry.setUpdatedAt(LocalDateTime.now());
            weightEntryDAO.insertWeightEntry(entry);
        }

        // ACT
        List<Achievement> achievements = achievementManager.checkAchievements(testUserId, 180.0);

        // ASSERT
        boolean hasStreak30 = achievements.stream()
                .anyMatch(a -> "STREAK_30".equals(a.getAchievementType()));
        assertTrue("Should award STREAK_30 achievement", hasStreak30);
    }

    /**
     * Tests FR3.1.5 - MILESTONE_5 achievement
     * Verify that MILESTONE_5 achievement is awarded after losing 5 lbs.
     */
    @Test
    public void test_checkAchievements_fivePoundsLost_awardsMilestone5Achievement() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        goal.setUserId(testUserId);
        goal.setGoalWeight(170.0);
        goal.setStartWeight(180.0);
        goal.setGoalUnit("lbs");
        goal.setActive(true);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());

        goalWeightDAO.setNewActiveGoal(goal);

        // ACT - Lost 5 lbs (180 → 175)
        List<Achievement> achievements = achievementManager.checkAchievements(testUserId, 175.0);

        // ASSERT
        boolean hasMilestone5 = achievements.stream()
                .anyMatch(a -> "MILESTONE_5".equals(a.getAchievementType()));
        assertTrue("Should award MILESTONE_5 achievement", hasMilestone5);
    }

    /**
     * Tests FR3.1.6 - MILESTONE_10 achievement
     * Verify that MILESTONE_10 achievement is awarded after losing 10 lbs.
     */
    @Test
    public void test_checkAchievements_tenPoundsLost_awardsMilestone10Achievement() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        goal.setUserId(testUserId);
        goal.setGoalWeight(165.0);
        goal.setStartWeight(180.0);
        goal.setGoalUnit("lbs");
        goal.setActive(true);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());

        goalWeightDAO.setNewActiveGoal(goal);

        // ACT - Lost 10 lbs (180 → 170)
        List<Achievement> achievements = achievementManager.checkAchievements(testUserId, 170.0);

        // ASSERT
        boolean hasMilestone10 = achievements.stream()
                .anyMatch(a -> "MILESTONE_10".equals(a.getAchievementType()));
        assertTrue("Should award MILESTONE_10 achievement", hasMilestone10);
    }

    /**
     * Tests FR3.1.7 - MILESTONE_25 achievement
     * Verify that MILESTONE_25 achievement is awarded after losing 25 lbs.
     */
    @Test
    public void test_checkAchievements_twentyFivePoundsLost_awardsMilestone25Achievement() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        goal.setUserId(testUserId);
        goal.setGoalWeight(150.0);
        goal.setStartWeight(180.0);
        goal.setGoalUnit("lbs");
        goal.setActive(true);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());

        goalWeightDAO.setNewActiveGoal(goal);

        // ACT - Lost 25 lbs (180 → 155)
        List<Achievement> achievements = achievementManager.checkAchievements(testUserId, 155.0);

        // ASSERT
        boolean hasMilestone25 = achievements.stream()
                .anyMatch(a -> "MILESTONE_25".equals(a.getAchievementType()));
        assertTrue("Should award MILESTONE_25 achievement", hasMilestone25);
    }

    /**
     * Tests FR3.1.8 - NEW_LOW achievement
     * Verify that NEW_LOW achievement is awarded when reaching new lowest weight.
     */
    @Test
    public void test_checkAchievements_newLowestWeight_awardsNewLowAchievement() {
        // ARRANGE
        LocalDate yesterday = LocalDate.now().minusDays(1);

        WeightEntry previousEntry = new WeightEntry();
        previousEntry.setUserId(testUserId);
        previousEntry.setWeightValue(180.0);
        previousEntry.setWeightUnit("lbs");
        previousEntry.setWeightDate(yesterday);
        previousEntry.setCreatedAt(LocalDateTime.now());
        previousEntry.setUpdatedAt(LocalDateTime.now());
        weightEntryDAO.insertWeightEntry(previousEntry);

        // ACT - New lower weight
        List<Achievement> achievements = achievementManager.checkAchievements(testUserId, 175.0);

        // ASSERT
        boolean hasNewLow = achievements.stream()
                .anyMatch(a -> "NEW_LOW".equals(a.getAchievementType()));
        assertTrue("Should award NEW_LOW achievement", hasNewLow);
    }

    /**
     * Tests FR3.1.9 - Duplicate prevention for GOAL_REACHED
     * Verify that duplicate achievements are not awarded.
     */
    @Test
    public void test_checkAchievements_goalReachedAlreadyAwarded_doesNotAwardDuplicate() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        goal.setUserId(testUserId);
        goal.setGoalWeight(150.0);
        goal.setStartWeight(180.0);
        goal.setGoalUnit("lbs");
        goal.setActive(true);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());

        goalWeightDAO.setNewActiveGoal(goal);

        // First check - should award
        List<Achievement> firstCheck = achievementManager.checkAchievements(testUserId, 150.0);
        long firstCount = firstCheck.stream()
                .filter(a -> "GOAL_REACHED".equals(a.getAchievementType()))
                .count();

        // ACT - Second check with same weight
        List<Achievement> secondCheck = achievementManager.checkAchievements(testUserId, 150.0);

        // ASSERT
        long secondCount = secondCheck.stream()
                .filter(a -> "GOAL_REACHED".equals(a.getAchievementType()))
                .count();

        assertEquals("Should award GOAL_REACHED once", 1, firstCount);
        assertEquals("Should not award duplicate GOAL_REACHED", 0, secondCount);
    }

    /**
     * Tests FR3.1.10 - Duplicate prevention for STREAK_7
     * Verify that streak achievements are not duplicated.
     */
    @Test
    public void test_checkAchievements_streak7AlreadyAwarded_doesNotAwardDuplicate() {
        // ARRANGE
        LocalDate today = LocalDate.now();

        // Create 7 consecutive entries (checkAchievements called after entry saved)
        for (int i = 0; i < 7; i++) {
            WeightEntry entry = new WeightEntry();
            entry.setUserId(testUserId);
            entry.setWeightValue(180.0 - i);
            entry.setWeightUnit("lbs");
            entry.setWeightDate(today.minusDays(i));
            entry.setCreatedAt(LocalDateTime.now());
            entry.setUpdatedAt(LocalDateTime.now());
            weightEntryDAO.insertWeightEntry(entry);
        }

        // First check - should award
        List<Achievement> firstCheck = achievementManager.checkAchievements(testUserId, 180.0);
        long firstCount = firstCheck.stream()
                .filter(a -> "STREAK_7".equals(a.getAchievementType()))
                .count();

        // ACT - Second check on same day
        List<Achievement> secondCheck = achievementManager.checkAchievements(testUserId, 179.0);

        // ASSERT
        long secondCount = secondCheck.stream()
                .filter(a -> "STREAK_7".equals(a.getAchievementType()))
                .count();

        assertEquals("Should award STREAK_7 once", 1, firstCount);
        assertEquals("Should not award duplicate STREAK_7", 0, secondCount);
    }

    /**
     * Tests FR3.1.11 - Duplicate prevention for milestones
     * Verify that milestone achievements are not duplicated.
     */
    @Test
    public void test_checkAchievements_milestone5AlreadyAwarded_doesNotAwardDuplicate() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        goal.setUserId(testUserId);
        goal.setGoalWeight(170.0);
        goal.setStartWeight(180.0);
        goal.setGoalUnit("lbs");
        goal.setActive(true);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());

        goalWeightDAO.setNewActiveGoal(goal);

        // First check - should award
        List<Achievement> firstCheck = achievementManager.checkAchievements(testUserId, 175.0);
        long firstCount = firstCheck.stream()
                .filter(a -> "MILESTONE_5".equals(a.getAchievementType()))
                .count();

        // ACT - Second check with same weight
        List<Achievement> secondCheck = achievementManager.checkAchievements(testUserId, 175.0);

        // ASSERT
        long secondCount = secondCheck.stream()
                .filter(a -> "MILESTONE_5".equals(a.getAchievementType()))
                .count();

        assertEquals("Should award MILESTONE_5 once", 1, firstCount);
        assertEquals("Should not award duplicate MILESTONE_5", 0, secondCount);
    }

    /**
     * Tests FR3.1.12 - Multiple achievements in one check
     * Verify that multiple achievements can be awarded simultaneously.
     */
    @Test
    public void test_checkAchievements_multipleConditionsMet_awardsMultipleAchievements() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        goal.setUserId(testUserId);
        goal.setGoalWeight(170.0);
        goal.setStartWeight(180.0);
        goal.setGoalUnit("lbs");
        goal.setActive(true);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());

        goalWeightDAO.setNewActiveGoal(goal);

        // No existing entries = FIRST_ENTRY + MILESTONE_5 met (NEW_LOW not awarded on first entry)

        // ACT
        List<Achievement> achievements = achievementManager.checkAchievements(testUserId, 175.0);

        // ASSERT
        assertTrue("Should award multiple achievements", achievements.size() >= 2);

        boolean hasFirstEntry = achievements.stream()
                .anyMatch(a -> "FIRST_ENTRY".equals(a.getAchievementType()));
        boolean hasMilestone5 = achievements.stream()
                .anyMatch(a -> "MILESTONE_5".equals(a.getAchievementType()));

        assertTrue("Should award FIRST_ENTRY", hasFirstEntry);
        assertTrue("Should award MILESTONE_5", hasMilestone5);
    }
}
