package com.example.weightogo.utils;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.LocalDate;

import static org.junit.Assert.*;

/**
 * Unit tests for DateTimeConverter utility class.
 * Tests LocalDateTime and LocalDate conversions to/from SQLite-compatible strings.
 *
 * Following strict TDD: one failing test at a time.
 */
public class DateTimeConverterTest {

    /**
     * Test 1: toTimestamp with valid LocalDateTime returns ISO-8601 formatted string
     * Expected format: "yyyy-MM-dd HH:mm:ss"
     */
    @Test
    public void test_toTimestamp_withValidLocalDateTime_returnsISO8601String() {
        // ARRANGE
        LocalDateTime dateTime = LocalDateTime.of(2025, 12, 10, 14, 30, 45);
        String expected = "2025-12-10 14:30:45";

        // ACT
        String actual = DateTimeConverter.toTimestamp(dateTime);

        // ASSERT
        assertEquals("Timestamp should be in yyyy-MM-dd HH:mm:ss format", expected, actual);
    }

    /**
     * Test 2: fromTimestamp with valid string returns LocalDateTime
     */
    @Test
    public void test_fromTimestamp_withValidString_returnsLocalDateTime() {
        // ARRANGE
        String timestamp = "2025-12-10 14:30:45";
        LocalDateTime expected = LocalDateTime.of(2025, 12, 10, 14, 30, 45);

        // ACT
        LocalDateTime actual = DateTimeConverter.fromTimestamp(timestamp);

        // ASSERT
        assertEquals("Should parse timestamp string to LocalDateTime", expected, actual);
    }

    /**
     * Test 3: toDateString with valid LocalDate returns ISO-8601 formatted string
     * Expected format: "yyyy-MM-dd"
     */
    @Test
    public void test_toDateString_withValidLocalDate_returnsISO8601String() {
        // ARRANGE
        LocalDate date = LocalDate.of(2025, 12, 10);
        String expected = "2025-12-10";

        // ACT
        String actual = DateTimeConverter.toDateString(date);

        // ASSERT
        assertEquals("Date should be in yyyy-MM-dd format", expected, actual);
    }

    /**
     * Test 4: fromDateString with valid string returns LocalDate
     */
    @Test
    public void test_fromDateString_withValidString_returnsLocalDate() {
        // ARRANGE
        String dateString = "2025-12-10";
        LocalDate expected = LocalDate.of(2025, 12, 10);

        // ACT
        LocalDate actual = DateTimeConverter.fromDateString(dateString);

        // ASSERT
        assertEquals("Should parse date string to LocalDate", expected, actual);
    }

    /**
     * Test 5: Round-trip conversion preserves DateTime values
     * Tests that converting LocalDateTime -> String -> LocalDateTime returns the same value
     */
    @Test
    public void test_roundTrip_preservesDateTime() {
        // ARRANGE
        LocalDateTime original = LocalDateTime.of(2025, 12, 10, 14, 30, 45);

        // ACT
        String timestamp = DateTimeConverter.toTimestamp(original);
        LocalDateTime result = DateTimeConverter.fromTimestamp(timestamp);

        // ASSERT
        assertEquals("Round-trip conversion should preserve LocalDateTime", original, result);
    }

    // ========== EDGE CASE TESTS ==========

    /**
     * Test 6: toTimestamp with null LocalDateTime returns null
     * Defensive code should handle null input gracefully
     */
    @Test
    public void test_toTimestamp_withNullDateTime_returnsNull() {
        // ARRANGE
        LocalDateTime nullDateTime = null;

        // ACT
        String actual = DateTimeConverter.toTimestamp(nullDateTime);

        // ASSERT
        assertNull("toTimestamp should return null for null input", actual);
    }

    /**
     * Test 7: fromTimestamp with null string returns null
     * Defensive code should handle null input gracefully
     */
    @Test
    public void test_fromTimestamp_withNullString_returnsNull() {
        // ARRANGE
        String nullString = null;

        // ACT
        LocalDateTime actual = DateTimeConverter.fromTimestamp(nullString);

        // ASSERT
        assertNull("fromTimestamp should return null for null input", actual);
    }

    /**
     * Test 8: fromTimestamp with empty string returns null
     * Defensive code should handle empty string gracefully
     */
    @Test
    public void test_fromTimestamp_withEmptyString_returnsNull() {
        // ARRANGE
        String emptyString = "";

        // ACT
        LocalDateTime actual = DateTimeConverter.fromTimestamp(emptyString);

        // ASSERT
        assertNull("fromTimestamp should return null for empty string", actual);
    }

    /**
     * Test 9: fromTimestamp with whitespace-only string returns null
     * Defensive code should handle whitespace-only string gracefully
     */
    @Test
    public void test_fromTimestamp_withWhitespaceString_returnsNull() {
        // ARRANGE
        String whitespaceString = "   ";

        // ACT
        LocalDateTime actual = DateTimeConverter.fromTimestamp(whitespaceString);

        // ASSERT
        assertNull("fromTimestamp should return null for whitespace-only string", actual);
    }

    /**
     * Test 10: fromTimestamp with invalid format returns null
     * Defensive code should handle malformed timestamp strings
     */
    @Test
    public void test_fromTimestamp_withInvalidFormat_returnsNull() {
        // ARRANGE
        String invalidFormat = "2025/12/10 14:30:45"; // Wrong separator (/ instead of -)

        // ACT
        LocalDateTime actual = DateTimeConverter.fromTimestamp(invalidFormat);

        // ASSERT
        assertNull("fromTimestamp should return null for invalid format", actual);
    }

    /**
     * Test 11: fromTimestamp with malformed date returns null
     * Defensive code should handle invalid date values (e.g., month 13)
     */
    @Test
    public void test_fromTimestamp_withMalformedDate_returnsNull() {
        // ARRANGE
        String malformedDate = "2025-13-32 25:61:99"; // Invalid month, day, hour, minute, second

        // ACT
        LocalDateTime actual = DateTimeConverter.fromTimestamp(malformedDate);

        // ASSERT
        assertNull("fromTimestamp should return null for malformed date values", actual);
    }

    /**
     * Test 12: toDateString with null LocalDate returns null
     * Defensive code should handle null input gracefully
     */
    @Test
    public void test_toDateString_withNullDate_returnsNull() {
        // ARRANGE
        LocalDate nullDate = null;

        // ACT
        String actual = DateTimeConverter.toDateString(nullDate);

        // ASSERT
        assertNull("toDateString should return null for null input", actual);
    }

    /**
     * Test 13: fromDateString with null string returns null
     * Defensive code should handle null input gracefully
     */
    @Test
    public void test_fromDateString_withNullString_returnsNull() {
        // ARRANGE
        String nullString = null;

        // ACT
        LocalDate actual = DateTimeConverter.fromDateString(nullString);

        // ASSERT
        assertNull("fromDateString should return null for null input", actual);
    }

    /**
     * Test 14: fromDateString with empty string returns null
     * Defensive code should handle empty string gracefully
     */
    @Test
    public void test_fromDateString_withEmptyString_returnsNull() {
        // ARRANGE
        String emptyString = "";

        // ACT
        LocalDate actual = DateTimeConverter.fromDateString(emptyString);

        // ASSERT
        assertNull("fromDateString should return null for empty string", actual);
    }

    /**
     * Test 15: fromDateString with whitespace-only string returns null
     * Defensive code should handle whitespace-only string gracefully
     */
    @Test
    public void test_fromDateString_withWhitespaceString_returnsNull() {
        // ARRANGE
        String whitespaceString = "   ";

        // ACT
        LocalDate actual = DateTimeConverter.fromDateString(whitespaceString);

        // ASSERT
        assertNull("fromDateString should return null for whitespace-only string", actual);
    }

    /**
     * Test 16: fromDateString with invalid format returns null
     * Defensive code should handle malformed date strings
     */
    @Test
    public void test_fromDateString_withInvalidFormat_returnsNull() {
        // ARRANGE
        String invalidFormat = "12/10/2025"; // Wrong format (MM/DD/YYYY instead of YYYY-MM-DD)

        // ACT
        LocalDate actual = DateTimeConverter.fromDateString(invalidFormat);

        // ASSERT
        assertNull("fromDateString should return null for invalid format", actual);
    }

    /**
     * Test 17: fromDateString with malformed date returns null
     * Defensive code should handle invalid date values (e.g., month 13)
     */
    @Test
    public void test_fromDateString_withMalformedDate_returnsNull() {
        // ARRANGE
        String malformedDate = "2025-13-32"; // Invalid month and day

        // ACT
        LocalDate actual = DateTimeConverter.fromDateString(malformedDate);

        // ASSERT
        assertNull("fromDateString should return null for malformed date values", actual);
    }
}