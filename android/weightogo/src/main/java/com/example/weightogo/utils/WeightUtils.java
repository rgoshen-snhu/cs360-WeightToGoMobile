package com.example.weightogo.utils;

import android.util.Log;

/**
 * Utility class for weight conversion (lbs ↔ kg) and validation.
 *
 * Provides centralized logic for converting between imperial (pounds) and metric (kilograms)
 * weight measurements. All conversions use standard conversion factors:
 * - 1 pound = 0.453592 kilograms
 * - 1 kilogram = 2.20462 pounds
 *
 * **Usage Example:**
 * <pre>
 * // Convert pounds to kilograms
 * double weightKg = WeightUtils.convertLbsToKg(150.0);  // 68.0 kg
 *
 * // Convert kilograms to pounds
 * double weightLbs = WeightUtils.convertKgToLbs(68.0);  // 150.0 lbs
 *
 * // Validate weight range
 * boolean isValid = WeightUtils.isValidWeight(150.0, "lbs");  // true
 * </pre>
 *
 * **Thread Safety:** All methods are static and thread-safe.
 */
public final class WeightUtils {

    private static final String TAG = "WeightUtils";

    /**
     * Conversion factor: 1 pound = 0.453592 kilograms (precise)
     */
    public static final double LBS_TO_KG_CONVERSION = 0.453592;

    /**
     * Maximum weight in pounds (700.0 lbs)
     */
    public static final double MAX_WEIGHT_LBS = 700.0;

    /**
     * Maximum weight in kilograms (317.5 kg ≈ 700 lbs)
     */
    public static final double MAX_WEIGHT_KG = 317.5;

    /**
     * Minimum weight (0.0 for both units)
     * Allows 0 as placeholder value, can be deleted later
     */
    public static final double MIN_WEIGHT = 0.0;

    /**
     * Decimal places for rounding (1 decimal place for precision/readability)
     */
    private static final int DECIMAL_PLACES = 1;

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     *
     * @throws AssertionError if instantiation is attempted via reflection
     */
    private WeightUtils() {
        throw new AssertionError("WeightUtils is a utility class and should not be instantiated");
    }

    /**
     * Converts weight from pounds to kilograms.
     *
     * @param weightLbs weight in pounds (positive value expected)
     * @return weight in kilograms, rounded to 1 decimal place
     */
    public static double convertLbsToKg(double weightLbs) {
        if (weightLbs < 0) {
            Log.w(TAG, "convertLbsToKg: negative weight provided: " + weightLbs);
            return 0.0;
        }

        double weightKg = weightLbs * LBS_TO_KG_CONVERSION;
        double rounded = roundToOneDecimal(weightKg);

        Log.d(TAG, "Converted " + weightLbs + " lbs to " + rounded + " kg");
        return rounded;
    }

    /**
     * Converts weight from kilograms to pounds.
     *
     * @param weightKg weight in kilograms (positive value expected)
     * @return weight in pounds, rounded to 1 decimal place
     */
    public static double convertKgToLbs(double weightKg) {
        if (weightKg < 0) {
            Log.w(TAG, "convertKgToLbs: negative weight provided: " + weightKg);
            return 0.0;
        }

        double weightLbs = weightKg / LBS_TO_KG_CONVERSION;
        double rounded = roundToOneDecimal(weightLbs);

        Log.d(TAG, "Converted " + weightKg + " kg to " + rounded + " lbs");
        return rounded;
    }

    /**
     * Rounds a double value to one decimal place.
     *
     * @param value the value to round
     * @return value rounded to 1 decimal place
     */
    public static double roundToOneDecimal(double value) {
        double multiplier = Math.pow(10, DECIMAL_PLACES);
        return Math.round(value * multiplier) / multiplier;
    }

    /**
     * Formats weight value to 1 decimal place.
     *
     * @param weight the weight value to format
     * @return formatted string (e.g., "150.0")
     */
    public static String formatWeight(double weight) {
        return String.format("%.1f", weight);
    }

    /**
     * Formats weight value with unit to 1 decimal place.
     *
     * @param weight the weight value to format
     * @param unit   the weight unit ("lbs" or "kg")
     * @return formatted string with unit (e.g., "150.0 lbs")
     */
    public static String formatWeightWithUnit(double weight, String unit) {
        return String.format("%.1f %s", weight, unit);
    }

    /**
     * Converts weight between units (lbs ↔ kg).
     *
     * @param value      the weight value to convert
     * @param fromUnit   source unit ("lbs" or "kg")
     * @param toUnit     target unit ("lbs" or "kg")
     * @return converted weight, rounded to 1 decimal place; 0.0 if invalid
     */
    public static double convertBetweenUnits(double value, String fromUnit, String toUnit) {
        // Validate input - negative values not allowed
        if (value < 0) {
            Log.w(TAG, "convertBetweenUnits: negative weight provided: " + value);
            return 0.0;
        }

        // Same unit - no conversion needed
        if (fromUnit.equals(toUnit)) {
            return value;
        }

        // Convert lbs to kg
        if ("lbs".equals(fromUnit) && "kg".equals(toUnit)) {
            return convertLbsToKg(value);
        }

        // Convert kg to lbs
        if ("kg".equals(fromUnit) && "lbs".equals(toUnit)) {
            return convertKgToLbs(value);
        }

        // Invalid unit combination
        Log.w(TAG, "convertBetweenUnits: invalid unit combination: " + fromUnit + " to " + toUnit);
        return 0.0;
    }

    /**
     * Validates whether a weight is within acceptable range for the given unit.
     *
     * Valid ranges:
     * - Pounds: 0.0 - 700.0 lbs
     * - Kilograms: 0.0 - 317.5 kg
     *
     * @param weight the weight value to validate
     * @param unit   the weight unit ("lbs" or "kg")
     * @return true if weight is within valid range, false otherwise
     */
    public static boolean isValidWeight(double weight, String unit) {
        if (weight < MIN_WEIGHT) {
            Log.w(TAG, "isValidWeight: negative weight: " + weight);
            return false;
        }

        double max;
        if ("lbs".equals(unit)) {
            max = MAX_WEIGHT_LBS;
        } else if ("kg".equals(unit)) {
            max = MAX_WEIGHT_KG;
        } else {
            Log.w(TAG, "isValidWeight: unknown unit: " + unit);
            return false;
        }

        boolean isValid = weight >= MIN_WEIGHT && weight <= max;
        if (!isValid) {
            Log.w(TAG, "isValidWeight: " + weight + " " + unit + " exceeds range [" + MIN_WEIGHT + ", " + max + "]");
        }

        return isValid;
    }
}
