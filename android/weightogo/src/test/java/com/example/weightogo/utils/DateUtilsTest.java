package com.example.weightogo.utils;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.weightogo.models.WeightEntry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for DateUtils utility class.
 * Tests date formatting and day streak calculation functionality.
 */
public class DateUtilsTest {

    /**
     * Test 1: formatDateShort() returns short format "d MMM"
     * Example: November 26 → "26 Nov"
     */
    @Test
    public void test_formatDateShort_withDate_returnsShortFormat() {
        // ARRANGE
        LocalDate date = LocalDate.of(2025, 11, 26);
        String expected = "26 Nov";

        // ACT
        String actual = DateUtils.formatDateShort(date);

        // ASSERT
        assertEquals("Should format date as 'd MMM'", expected, actual);
    }

    /**
     * Test 2: formatDateShort() with single-digit day returns no leading zero
     * Example: January 5 → "5 Jan" (not "05 Jan")
     */
    @Test
    public void test_formatDateShort_withSingleDigitDay_returnsNoLeadingZero() {
        // ARRANGE
        LocalDate date = LocalDate.of(2025, 1, 5);
        String expected = "5 Jan";

        // ACT
        String actual = DateUtils.formatDateShort(date);

        // ASSERT
        assertEquals("Should not pad single-digit day with leading zero", expected, actual);
    }

    /**
     * Test 3: formatDateFull() returns full format "EEEE, MMMM d, yyyy"
     * Example: November 26, 2025 → "Tuesday, November 26, 2025"
     */
    @Test
    public void test_formatDateFull_withDate_returnsFullFormat() {
        // ARRANGE
        LocalDate date = LocalDate.of(2025, 11, 26);
        String expected = "Wednesday, November 26, 2025";

        // ACT
        String actual = DateUtils.formatDateFull(date);

        // ASSERT
        assertEquals("Should format date as 'EEEE, MMMM d, yyyy'", expected, actual);
    }

    /**
     * Test 4: isToday() returns true for today's date
     */
    @Test
    public void test_isToday_withTodaysDate_returnsTrue() {
        // ARRANGE
        LocalDate today = LocalDate.now();

        // ACT
        boolean result = DateUtils.isToday(today);

        // ASSERT
        assertTrue("Today's date should return true", result);
    }

    /**
     * Test 5: isToday() returns false for yesterday's date
     */
    @Test
    public void test_isToday_withYesterdaysDate_returnsFalse() {
        // ARRANGE
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // ACT
        boolean result = DateUtils.isToday(yesterday);

        // ASSERT
        assertFalse("Yesterday's date should return false", result);
    }

    /**
     * Test 6: calculateDayStreak() with consecutive days returns correct streak
     */
    @Test
    public void test_calculateDayStreak_withConsecutiveDays_returnsCorrectStreak() {
        // ARRANGE
        List<WeightEntry> entries = new ArrayList<>();

        // Create 3 consecutive days of entries (sorted DESC)
        WeightEntry entry1 = new WeightEntry();
        entry1.setWeightDate(LocalDate.now());
        entry1.setWeightValue(170.0);
        entries.add(entry1);

        WeightEntry entry2 = new WeightEntry();
        entry2.setWeightDate(LocalDate.now().minusDays(1));
        entry2.setWeightValue(171.0);
        entries.add(entry2);

        WeightEntry entry3 = new WeightEntry();
        entry3.setWeightDate(LocalDate.now().minusDays(2));
        entry3.setWeightValue(172.0);
        entries.add(entry3);

        // ACT
        int streak = DateUtils.calculateDayStreak(entries);

        // ASSERT
        assertEquals("Should return 3 for 3 consecutive days", 3, streak);
    }

    /**
     * Test 7: calculateDayStreak() with gap in dates returns streak until gap
     */
    @Test
    public void test_calculateDayStreak_withGapInDates_returnsStreakUntilGap() {
        // ARRANGE
        List<WeightEntry> entries = new ArrayList<>();

        // Create entries: today, yesterday, then 4 days ago (gap at day 3)
        WeightEntry entry1 = new WeightEntry();
        entry1.setWeightDate(LocalDate.now());
        entry1.setWeightValue(170.0);
        entries.add(entry1);

        WeightEntry entry2 = new WeightEntry();
        entry2.setWeightDate(LocalDate.now().minusDays(1));
        entry2.setWeightValue(171.0);
        entries.add(entry2);

        WeightEntry entry3 = new WeightEntry();
        entry3.setWeightDate(LocalDate.now().minusDays(4)); // Gap here
        entry3.setWeightValue(172.0);
        entries.add(entry3);

        // ACT
        int streak = DateUtils.calculateDayStreak(entries);

        // ASSERT
        assertEquals("Should return 2 (stops at gap)", 2, streak);
    }

    /**
     * Test 8: calculateDayStreak() with empty list returns zero
     */
    @Test
    public void test_calculateDayStreak_withEmptyList_returnsZero() {
        // ARRANGE
        List<WeightEntry> emptyList = new ArrayList<>();

        // ACT
        int streak = DateUtils.calculateDayStreak(emptyList);

        // ASSERT
        assertEquals("Empty list should return 0 streak", 0, streak);
    }

    /**
     * Test 9: calculateDayStreak() with null list returns zero
     */
    @Test
    public void test_calculateDayStreak_withNullList_returnsZero() {
        // ARRANGE
        List<WeightEntry> nullList = null;

        // ACT
        int streak = DateUtils.calculateDayStreak(nullList);

        // ASSERT
        assertEquals("Null list should return 0 streak", 0, streak);
    }
}
