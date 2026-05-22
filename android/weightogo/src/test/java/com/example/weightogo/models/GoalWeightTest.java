package com.example.weightogo.models;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * Unit tests for GoalWeight model class.
 * Tests FR1.2 - Goal Weight data model.
 */
public class GoalWeightTest {

    @Test
    public void test_defaultConstructor_createsGoalWeightObject() {
        // ARRANGE & ACT
        GoalWeight goal = new GoalWeight();

        // ASSERT
        assertNotNull("GoalWeight object should not be null", goal);
    }

    @Test
    public void test_getGoalId_defaultValue_returnsZero() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();

        // ACT
        long goalId = goal.getGoalId();

        // ASSERT
        assertEquals("Default goalId should be 0", 0L, goalId);
    }

    @Test
    public void test_setGoalId_withValidId_setsValue() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        long expectedId = 42L;

        // ACT
        goal.setGoalId(expectedId);

        // ASSERT
        assertEquals("GoalId should be set correctly", expectedId, goal.getGoalId());
    }

    @Test
    public void test_setUserId_withValidId_setsValue() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        long expectedUserId = 123L;

        // ACT
        goal.setUserId(expectedUserId);

        // ASSERT
        assertEquals("UserId should be set correctly", expectedUserId, goal.getUserId());
    }

    @Test
    public void test_setGoalWeight_withValidValue_setsValue() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        double expectedGoalWeight = 150.0;

        // ACT
        goal.setGoalWeight(expectedGoalWeight);

        // ASSERT
        assertEquals("GoalWeight should be set correctly", expectedGoalWeight, goal.getGoalWeight(), 0.01);
    }

    @Test
    public void test_setGoalUnit_withValidUnit_setsValue() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        String expectedUnit = "lbs";

        // ACT
        goal.setGoalUnit(expectedUnit);

        // ASSERT
        assertEquals("GoalUnit should be set correctly", expectedUnit, goal.getGoalUnit());
    }

    @Test
    public void test_setStartWeight_withValidValue_setsValue() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        double expectedStartWeight = 200.0;

        // ACT
        goal.setStartWeight(expectedStartWeight);

        // ASSERT
        assertEquals("StartWeight should be set correctly", expectedStartWeight, goal.getStartWeight(), 0.01);
    }

    @Test
    public void test_setTargetDate_withValidDate_setsValue() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        LocalDate expectedDate = LocalDate.of(2025, 12, 31);

        // ACT
        goal.setTargetDate(expectedDate);

        // ASSERT
        assertEquals("TargetDate should be set correctly", expectedDate, goal.getTargetDate());
    }

    @Test
    public void test_setIsAchieved_withValidFlag_setsValue() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        boolean expectedFlag = true;

        // ACT
        goal.setAchieved(expectedFlag);

        // ASSERT
        assertEquals("IsAchieved should be set correctly", expectedFlag, goal.isAchieved());
    }

    @Test
    public void test_setAchievedDate_withValidDate_setsValue() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        LocalDate expectedDate = LocalDate.of(2025, 12, 10);

        // ACT
        goal.setAchievedDate(expectedDate);

        // ASSERT
        assertEquals("AchievedDate should be set correctly", expectedDate, goal.getAchievedDate());
    }

    @Test
    public void test_setCreatedAt_withValidTimestamp_setsValue() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        LocalDateTime expectedTimestamp = LocalDateTime.of(2025, 12, 10, 8, 0, 0);

        // ACT
        goal.setCreatedAt(expectedTimestamp);

        // ASSERT
        assertEquals("CreatedAt should be set correctly", expectedTimestamp, goal.getCreatedAt());
    }

    @Test
    public void test_setUpdatedAt_withValidTimestamp_setsValue() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        LocalDateTime expectedTimestamp = LocalDateTime.of(2025, 12, 10, 9, 0, 0);

        // ACT
        goal.setUpdatedAt(expectedTimestamp);

        // ASSERT
        assertEquals("UpdatedAt should be set correctly", expectedTimestamp, goal.getUpdatedAt());
    }

    @Test
    public void test_setIsActive_withValidFlag_setsValue() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        boolean expectedFlag = true;

        // ACT
        goal.setActive(expectedFlag);

        // ASSERT
        assertEquals("IsActive should be set correctly", expectedFlag, goal.isActive());
    }

    @Test
    public void test_toString_returnsNonNullString() {
        // ARRANGE
        GoalWeight goal = new GoalWeight();
        goal.setGoalId(1L);
        goal.setUserId(123L);
        goal.setGoalWeight(150.0);
        goal.setGoalUnit("lbs");
        goal.setActive(true);

        // ACT
        String result = goal.toString();

        // ASSERT
        assertNotNull("toString should return non-null value", result);
        assertTrue("toString should contain goalId", result.contains("1"));
        assertTrue("toString should contain userId", result.contains("123"));
        assertTrue("toString should contain goalWeight", result.contains("150.0"));
        assertTrue("toString should contain goalUnit", result.contains("lbs"));
    }

    @Test
    public void test_equals_withSameGoalId_returnsTrue() {
        // ARRANGE
        GoalWeight goal1 = new GoalWeight();
        goal1.setGoalId(42L);
        goal1.setGoalWeight(150.0);

        GoalWeight goal2 = new GoalWeight();
        goal2.setGoalId(42L);
        goal2.setGoalWeight(160.0);

        // ACT & ASSERT
        assertEquals("Goals with same goalId should be equal", goal1, goal2);
    }

    @Test
    public void test_equals_withDifferentGoalId_returnsFalse() {
        // ARRANGE
        GoalWeight goal1 = new GoalWeight();
        goal1.setGoalId(42L);

        GoalWeight goal2 = new GoalWeight();
        goal2.setGoalId(99L);

        // ACT & ASSERT
        assertNotEquals("Goals with different goalId should not be equal", goal1, goal2);
    }

    @Test
    public void test_hashCode_withSameGoalId_returnsSameHash() {
        // ARRANGE
        GoalWeight goal1 = new GoalWeight();
        goal1.setGoalId(42L);

        GoalWeight goal2 = new GoalWeight();
        goal2.setGoalId(42L);

        // ACT & ASSERT
        assertEquals("Goals with same goalId should have same hashCode", goal1.hashCode(), goal2.hashCode());
    }

    @Test
    public void test_equals_withUninitializedGoals_returnsFalse() {
        // ARRANGE
        GoalWeight goal1 = new GoalWeight();  // goalId = 0 (uninitialized)
        GoalWeight goal2 = new GoalWeight();  // goalId = 0 (uninitialized)

        // ACT & ASSERT
        assertNotEquals("Uninitialized goals (goalId=0) should not be equal", goal1, goal2);
    }
}
