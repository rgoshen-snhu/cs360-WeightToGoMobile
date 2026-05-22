package com.example.weightogo.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for WeightUtils.
 * Tests weight conversion (lbs ↔ kg), validation, and formatting.
 */
public class WeightUtilsTest {

    private static final double DELTA = 0.01; // Tolerance for double comparison

    /**
     * Test 1: Convert pounds to kilograms with valid input
     */
    @Test
    public void test_convertLbsToKg_withValidInput_returnsCorrectValue() {
        // ARRANGE
        double weightLbs = 150.0;
        double expectedKg = 68.0; // 150 * 0.453592 ≈ 68.0

        // ACT
        double actualKg = WeightUtils.convertLbsToKg(weightLbs);

        // ASSERT
        assertEquals("150 lbs should convert to approximately 68 kg",
                expectedKg, actualKg, DELTA);
    }

    /**
     * Test 2: Convert kilograms to pounds with valid input
     */
    @Test
    public void test_convertKgToLbs_withValidInput_returnsCorrectValue() {
        // ARRANGE
        double weightKg = 68.0;
        double expectedLbs = 149.9; // 68 / 0.453592 = 149.914... ≈ 149.9 (rounded to 1 decimal)

        // ACT
        double actualLbs = WeightUtils.convertKgToLbs(weightKg);

        // ASSERT
        assertEquals("68 kg should convert to approximately 149.9 lbs",
                expectedLbs, actualLbs, DELTA);
    }

    /**
     * Test 3: Convert zero pounds to kilograms
     */
    @Test
    public void test_convertLbsToKg_withZero_returnsZero() {
        // ARRANGE
        double weightLbs = 0.0;
        double expectedKg = 0.0;

        // ACT
        double actualKg = WeightUtils.convertLbsToKg(weightLbs);

        // ASSERT
        assertEquals("0 lbs should convert to 0 kg",
                expectedKg, actualKg, DELTA);
    }

    /**
     * Test 4: Round weight to one decimal place
     */
    @Test
    public void test_roundToOneDecimal_withPreciseValue_roundsCorrectly() {
        // ARRANGE
        double preciseValue = 68.0388; // 150 lbs * 0.453592 exact
        double expected = 68.0;

        // ACT
        double actual = WeightUtils.roundToOneDecimal(preciseValue);

        // ASSERT
        assertEquals("68.0388 should round to 68.0",
                expected, actual, 0.01);
    }

    /**
     * Test 5: Validate weight within valid range (lbs)
     */
    @Test
    public void test_isValidWeight_withValidRange_returnsTrue() {
        // ARRANGE
        double validWeightLbs = 150.0;
        String unitLbs = "lbs";
        double validWeightKg = 68.0;
        String unitKg = "kg";

        // ACT
        boolean resultLbs = WeightUtils.isValidWeight(validWeightLbs, unitLbs);
        boolean resultKg = WeightUtils.isValidWeight(validWeightKg, unitKg);

        // ASSERT
        assertTrue("150 lbs is within valid range (0-700)", resultLbs);
        assertTrue("68 kg is within valid range (0-317.5)", resultKg);
    }

    /**
     * Test 6: Validate weight outside valid range
     */
    @Test
    public void test_isValidWeight_withInvalidRange_returnsFalse() {
        // ARRANGE
        double tooHighLbs = 701.0;
        String unitLbs = "lbs";
        double tooHighKg = 318.0;
        String unitKg = "kg";
        double negativeLbs = -1.0;

        // ACT
        boolean resultTooHighLbs = WeightUtils.isValidWeight(tooHighLbs, unitLbs);
        boolean resultTooHighKg = WeightUtils.isValidWeight(tooHighKg, unitKg);
        boolean resultNegative = WeightUtils.isValidWeight(negativeLbs, unitLbs);

        // ASSERT
        assertFalse("701 lbs exceeds max (700)", resultTooHighLbs);
        assertFalse("318 kg exceeds max (317.5)", resultTooHighKg);
        assertFalse("Negative weight is invalid", resultNegative);
    }

    /**
     * Test 7: Convert between units with same unit (no conversion needed)
     */
    @Test
    public void test_convertBetweenUnits_withSameUnit_returnsOriginalValue() {
        // ARRANGE
        double weightLbs = 150.0;
        double weightKg = 68.0;

        // ACT
        double resultLbs = WeightUtils.convertBetweenUnits(weightLbs, "lbs", "lbs");
        double resultKg = WeightUtils.convertBetweenUnits(weightKg, "kg", "kg");

        // ASSERT
        assertEquals("150 lbs to lbs should return 150.0",
                weightLbs, resultLbs, DELTA);
        assertEquals("68 kg to kg should return 68.0",
                weightKg, resultKg, DELTA);
    }

    /**
     * Test 8: Convert pounds to kilograms
     */
    @Test
    public void test_convertBetweenUnits_withLbsToKg_returnsCorrectValue() {
        // ARRANGE
        double weightLbs = 150.0;
        double expectedKg = 68.0; // 150 * 0.453592 ≈ 68.0

        // ACT
        double actualKg = WeightUtils.convertBetweenUnits(weightLbs, "lbs", "kg");

        // ASSERT
        assertEquals("150 lbs should convert to approximately 68.0 kg",
                expectedKg, actualKg, DELTA);
    }

    /**
     * Test 9: Convert kilograms to pounds
     */
    @Test
    public void test_convertBetweenUnits_withKgToLbs_returnsCorrectValue() {
        // ARRANGE
        double weightKg = 68.0;
        double expectedLbs = 149.9; // 68 / 0.453592 ≈ 149.9

        // ACT
        double actualLbs = WeightUtils.convertBetweenUnits(weightKg, "kg", "lbs");

        // ASSERT
        assertEquals("68 kg should convert to approximately 149.9 lbs",
                expectedLbs, actualLbs, DELTA);
    }

    /**
     * Test 10: Convert with invalid units returns zero
     */
    @Test
    public void test_convertBetweenUnits_withInvalidUnits_returnsZero() {
        // ARRANGE
        double weight = 150.0;

        // ACT
        double result1 = WeightUtils.convertBetweenUnits(weight, "pounds", "kg");
        double result2 = WeightUtils.convertBetweenUnits(weight, "lbs", "kilograms");

        // ASSERT
        assertEquals("Invalid 'pounds' unit should return 0.0",
                0.0, result1, DELTA);
        assertEquals("Invalid 'kilograms' unit should return 0.0",
                0.0, result2, DELTA);
    }

    /**
     * Test 11: Convert negative weight returns zero
     */
    @Test
    public void test_convertBetweenUnits_withNegativeValue_returnsZero() {
        // ARRANGE
        double negativeWeight = -150.0;

        // ACT
        double result1 = WeightUtils.convertBetweenUnits(negativeWeight, "lbs", "kg");
        double result2 = WeightUtils.convertBetweenUnits(negativeWeight, "kg", "lbs");

        // ASSERT
        assertEquals("Negative lbs to kg should return 0.0",
                0.0, result1, DELTA);
        assertEquals("Negative kg to lbs should return 0.0",
                0.0, result2, DELTA);
    }

    /**
     * Test 12: Format weight to 1 decimal place
     */
    @Test
    public void test_formatWeight_withValidValue_returnsFormattedString() {
        // ARRANGE
        double weight1 = 150.0;
        double weight2 = 68.0388; // Should round to 68.0
        double weight3 = 0.0;

        // ACT
        String result1 = WeightUtils.formatWeight(weight1);
        String result2 = WeightUtils.formatWeight(weight2);
        String result3 = WeightUtils.formatWeight(weight3);

        // ASSERT
        assertEquals("150.0 should format to '150.0'",
                "150.0", result1);
        assertEquals("68.0388 should format to '68.0'",
                "68.0", result2);
        assertEquals("0.0 should format to '0.0'",
                "0.0", result3);
    }

    /**
     * Test 13: Format weight with unit
     */
    @Test
    public void test_formatWeightWithUnit_withValidValues_returnsFormattedString() {
        // ARRANGE
        double weightLbs = 150.0;
        double weightKg = 68.0;

        // ACT
        String resultLbs = WeightUtils.formatWeightWithUnit(weightLbs, "lbs");
        String resultKg = WeightUtils.formatWeightWithUnit(weightKg, "kg");

        // ASSERT
        assertEquals("150.0 lbs should format to '150.0 lbs'",
                "150.0 lbs", resultLbs);
        assertEquals("68.0 kg should format to '68.0 kg'",
                "68.0 kg", resultKg);
    }
}
