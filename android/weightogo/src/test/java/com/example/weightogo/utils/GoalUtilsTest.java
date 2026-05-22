package com.example.weightogo.utils;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for GoalUtils.
 * Tests goal weight validation against current weight and valid ranges.
 * Tests target date validation (optional field).
 */
public class GoalUtilsTest {

    private static final double DELTA = 0.01; // Tolerance for double comparison

    // =============================================================================================
    // GOAL WEIGHT VALIDATION TESTS (6 tests)
    // =============================================================================================

    /**
     * Test 1: Valid goal above current weight
     * Goal must differ from current weight and be within valid range.
     */
    @Test
    public void test_isValidGoal_withValidGoalAboveCurrent_returnsTrue() {
        // ARRANGE
        double currentWeight = 150.0;
        double goalWeight = 180.0; // Gain weight goal
        String unit = "lbs";

        // ACT
        boolean result = GoalUtils.isValidGoal(currentWeight, goalWeight, unit);

        // ASSERT
        assertTrue("Goal above current weight should be valid", result);
    }

    /**
     * Test 2: Valid goal below current weight
     * Goal must differ from current weight and be within valid range.
     */
    @Test
    public void test_isValidGoal_withValidGoalBelowCurrent_returnsTrue() {
        // ARRANGE
        double currentWeight = 180.0;
        double goalWeight = 150.0; // Lose weight goal
        String unit = "lbs";

        // ACT
        boolean result = GoalUtils.isValidGoal(currentWeight, goalWeight, unit);

        // ASSERT
        assertTrue("Goal below current weight should be valid", result);
    }

    /**
     * Test 3: Goal same as current weight (invalid)
     * Goal must differ from current weight.
     */
    @Test
    public void test_isValidGoal_withSameAsCurrentWeight_returnsFalse() {
        // ARRANGE
        double currentWeight = 150.0;
        double goalWeight = 150.0; // Same as current (invalid)
        String unit = "lbs";

        // ACT
        boolean result = GoalUtils.isValidGoal(currentWeight, goalWeight, unit);

        // ASSERT
        assertFalse("Goal same as current weight should be invalid", result);
    }

    /**
     * Test 4: Goal exceeds maximum weight (lbs)
     * Goal must be within valid range (0-700 lbs).
     */
    @Test
    public void test_isValidGoal_withOutOfRangeLbs_returnsFalse() {
        // ARRANGE
        double currentWeight = 150.0;
        double goalWeight = 701.0; // Exceeds max (700 lbs)
        String unit = "lbs";

        // ACT
        boolean result = GoalUtils.isValidGoal(currentWeight, goalWeight, unit);

        // ASSERT
        assertFalse("Goal exceeding 700 lbs should be invalid", result);
    }

    /**
     * Test 5: Goal exceeds maximum weight (kg)
     * Goal must be within valid range (0-317.5 kg).
     */
    @Test
    public void test_isValidGoal_withOutOfRangeKg_returnsFalse() {
        // ARRANGE
        double currentWeight = 68.0;
        double goalWeight = 318.0; // Exceeds max (317.5 kg)
        String unit = "kg";

        // ACT
        boolean result = GoalUtils.isValidGoal(currentWeight, goalWeight, unit);

        // ASSERT
        assertFalse("Goal exceeding 317.5 kg should be invalid", result);
    }

    /**
     * Test 6: Goal is negative weight
     * Goal must be non-negative.
     */
    @Test
    public void test_isValidGoal_withNegativeWeight_returnsFalse() {
        // ARRANGE
        double currentWeight = 150.0;
        double goalWeight = -10.0; // Negative (invalid)
        String unit = "lbs";

        // ACT
        boolean result = GoalUtils.isValidGoal(currentWeight, goalWeight, unit);

        // ASSERT
        assertFalse("Negative goal weight should be invalid", result);
    }

    // =============================================================================================
    // TARGET DATE VALIDATION TESTS (2 tests)
    // =============================================================================================

    /**
     * Test 7: Valid target date in future
     * Target date is optional, but if provided must be in future.
     */
    @Test
    public void test_isValidTargetDate_withFutureDate_returnsTrue() {
        // ARRANGE
        LocalDate futureDate = LocalDate.now().plusDays(30); // 30 days from now

        // ACT
        boolean result = GoalUtils.isValidTargetDate(futureDate);

        // ASSERT
        assertTrue("Future target date should be valid", result);
    }

    /**
     * Test 8: Invalid target date in past
     * Target date must not be in the past.
     */
    @Test
    public void test_isValidTargetDate_withPastDate_returnsFalse() {
        // ARRANGE
        LocalDate pastDate = LocalDate.now().minusDays(1); // Yesterday

        // ACT
        boolean result = GoalUtils.isValidTargetDate(pastDate);

        // ASSERT
        assertFalse("Past target date should be invalid", result);
    }
}
