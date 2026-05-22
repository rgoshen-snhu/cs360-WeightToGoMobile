package com.example.weightogo.utils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for BooleanConverter utility class.
 * Verifies correct conversion between Java boolean and SQLite INTEGER (0/1).
 *
 * Following strict TDD: tests written before implementation.
 */
public class BooleanConverterTest {

    /**
     * Test 1: toInteger converts true to 1
     */
    @Test
    public void test_toInteger_withTrue_returns1() {
        // ACT
        int result = BooleanConverter.toInteger(true);

        // ASSERT
        assertEquals("true should convert to 1", 1, result);
    }

    /**
     * Test 2: toInteger converts false to 0
     */
    @Test
    public void test_toInteger_withFalse_returns0() {
        // ACT
        int result = BooleanConverter.toInteger(false);

        // ASSERT
        assertEquals("false should convert to 0", 0, result);
    }

    /**
     * Test 3: fromInteger converts 1 to true
     */
    @Test
    public void test_fromInteger_with1_returnsTrue() {
        // ACT
        boolean result = BooleanConverter.fromInteger(1);

        // ASSERT
        assertTrue("1 should convert to true", result);
    }

    /**
     * Test 4: fromInteger converts 0 to false
     */
    @Test
    public void test_fromInteger_with0_returnsFalse() {
        // ACT
        boolean result = BooleanConverter.fromInteger(0);

        // ASSERT
        assertFalse("0 should convert to false", result);
    }

    /**
     * Test 5: fromInteger handles non-zero values as true (SQLite convention)
     */
    @Test
    public void test_fromInteger_withNonZero_returnsTrue() {
        // ACT & ASSERT
        assertTrue("Any non-zero value should convert to true", BooleanConverter.fromInteger(5));
        assertTrue("Negative values should convert to true", BooleanConverter.fromInteger(-1));
        assertTrue("Large values should convert to true", BooleanConverter.fromInteger(999));
    }

    // ========== EDGE CASE TESTS ==========

    /**
     * Test 6: fromInteger handles Integer.MAX_VALUE as true
     */
    @Test
    public void test_fromInteger_withMaxValue_returnsTrue() {
        // ACT
        boolean result = BooleanConverter.fromInteger(Integer.MAX_VALUE);

        // ASSERT
        assertTrue("Integer.MAX_VALUE should convert to true", result);
    }

    /**
     * Test 7: fromInteger handles Integer.MIN_VALUE as true
     */
    @Test
    public void test_fromInteger_withMinValue_returnsTrue() {
        // ACT
        boolean result = BooleanConverter.fromInteger(Integer.MIN_VALUE);

        // ASSERT
        assertTrue("Integer.MIN_VALUE should convert to true", result);
    }
}
