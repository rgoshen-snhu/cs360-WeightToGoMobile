package com.example.weightogo.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;

/**
 * Utility class for goal weight validation.
 *
 * Provides validation methods for goal weights to ensure they are achievable and safe.
 * All validation methods are null-safe and return false for invalid inputs.
 *
 * **Validation Rules:**
 * - **Goal Weight**: Must differ from current weight and be within valid range (0-700 lbs / 0-317.5 kg)
 * - **Target Date**: Optional, but if provided must be in the future
 *
 * **Usage Example:**
 * <pre>
 * // Validate goal weight
 * double currentWeight = 180.0;
 * double goalWeight = 150.0;
 * if (GoalUtils.isValidGoal(currentWeight, goalWeight, "lbs")) {
 *     // Proceed with goal creation
 * } else {
 *     // Show error message
 * }
 *
 * // Validate target date (optional)
 * LocalDate targetDate = LocalDate.now().plusDays(90);
 * if (GoalUtils.isValidTargetDate(targetDate)) {
 *     // Target date is valid
 * }
 * </pre>
 *
 * **Thread Safety:** All methods are static and thread-safe.
 */
public final class GoalUtils {

    private static final String TAG = "GoalUtils";

    /**
     * Minimum difference threshold between current and goal weight (to prevent same weight goals).
     * Using a small epsilon to account for floating point precision.
     */
    private static final double MIN_WEIGHT_DIFFERENCE = 0.1;

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     *
     * @throws AssertionError if instantiation is attempted via reflection
     */
    private GoalUtils() {
        throw new AssertionError("GoalUtils is a utility class and should not be instantiated");
    }

    /**
     * Validates a goal weight against the current weight and valid weight ranges.
     *
     * **Validation Rules:**
     * 1. Goal must differ from current weight (by at least 0.1 units)
     * 2. Goal must be within valid range for the unit:
     *    - Pounds: 0.0 - 700.0 lbs
     *    - Kilograms: 0.0 - 317.5 kg
     * 3. Goal must be non-negative
     *
     * **Note:** This method allows both weight loss (goal < current) and weight gain (goal > current) goals.
     *
     * @param currentWeight the user's current weight
     * @param goalWeight    the desired goal weight
     * @param unit          the weight unit ("lbs" or "kg")
     * @return true if goal is valid, false otherwise
     */
    public static boolean isValidGoal(double currentWeight, double goalWeight, @NonNull String unit) {
        // Runtime null check for unit parameter
        if (unit == null) {
            Log.e(TAG, "isValidGoal: unit parameter cannot be null");
            return false;
        }

        Log.d(TAG, "isValidGoal: current=" + currentWeight + ", goal=" + goalWeight + ", unit=" + unit);

        // Rule 1: Goal must differ from current weight
        double difference = Math.abs(goalWeight - currentWeight);
        if (difference < MIN_WEIGHT_DIFFERENCE) {
            Log.w(TAG, "isValidGoal: goal weight is too close to current weight (difference: " + difference + ")");
            return false;
        }

        // Rule 2: Goal must be within valid range for the unit
        if (!WeightUtils.isValidWeight(goalWeight, unit)) {
            Log.w(TAG, "isValidGoal: goal weight is outside valid range for " + unit);
            return false;
        }

        Log.d(TAG, "isValidGoal: goal is valid");
        return true;
    }

    /**
     * Validates a target date for goal achievement.
     *
     * **Validation Rules:**
     * 1. Target date must be in the future (not today or past)
     * 2. Null dates are considered invalid (use this method only if target date is provided)
     *
     * **Note:** Target date is optional in goal creation. Only call this method if the user
     * provides a target date.
     *
     * @param targetDate the desired target date for goal achievement (nullable)
     * @return true if date is in the future, false if null, today, or in the past
     */
    public static boolean isValidTargetDate(@Nullable LocalDate targetDate) {
        // Null check
        if (targetDate == null) {
            Log.w(TAG, "isValidTargetDate: target date is null");
            return false;
        }

        LocalDate today = LocalDate.now();

        // Must be after today (not today or past)
        boolean isValid = targetDate.isAfter(today);

        if (!isValid) {
            Log.w(TAG, "isValidTargetDate: target date must be in the future (provided: " + targetDate + ", today: " + today + ")");
        } else {
            Log.d(TAG, "isValidTargetDate: target date is valid (" + targetDate + ")");
        }

        return isValid;
    }
}
