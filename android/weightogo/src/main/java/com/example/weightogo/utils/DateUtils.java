package com.example.weightogo.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.example.weightogo.models.WeightEntry;

/**
 * Utility class for date formatting and day streak calculations.
 * Provides helper methods for displaying dates in user-friendly formats.
 */
public final class DateUtils {

    private static final DateTimeFormatter SHORT_FORMAT = DateTimeFormatter.ofPattern("d MMM");
    private static final DateTimeFormatter FULL_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private DateUtils() {
        throw new AssertionError("DateUtils is a utility class and should not be instantiated");
    }

    /**
     * Formats a LocalDate into short format: "d MMM" (e.g., "26 Nov").
     *
     * @param date the date to format
     * @return formatted date string in "d MMM" format, or empty string if date is null
     */
    public static String formatDateShort(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(SHORT_FORMAT);
    }

    /**
     * Formats a LocalDate into full format: "EEEE, MMMM d, yyyy" (e.g., "Wednesday, November 26, 2025").
     *
     * @param date the date to format
     * @return formatted date string in "EEEE, MMMM d, yyyy" format, or empty string if date is null
     */
    public static String formatDateFull(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(FULL_FORMAT);
    }

    /**
     * Checks if a given date is today's date.
     *
     * @param date the date to check
     * @return true if the date is today, false otherwise (including null)
     */
    public static boolean isToday(LocalDate date) {
        if (date == null) {
            return false;
        }
        return date.equals(LocalDate.now());
    }

    /**
     * Calculates the current day streak from a list of weight entries.
     * A streak is the number of consecutive days with entries, starting from the most recent entry.
     * The list is expected to be sorted by date descending (newest first).
     *
     * @param entries list of weight entries (sorted by date DESC)
     * @return number of consecutive days with entries, or 0 if list is null/empty
     */
    public static int calculateDayStreak(List<WeightEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }

        int streak = 0;
        LocalDate expectedDate = null;

        for (WeightEntry entry : entries) {
            LocalDate entryDate = entry.getWeightDate();

            if (expectedDate == null) {
                // First entry - start the streak
                streak = 1;
                expectedDate = entryDate.minusDays(1);
            } else {
                // Check if this entry is consecutive (expected date)
                if (entryDate.equals(expectedDate)) {
                    streak++;
                    expectedDate = entryDate.minusDays(1);
                } else {
                    // Gap found - stop counting
                    break;
                }
            }
        }

        return streak;
    }
}
