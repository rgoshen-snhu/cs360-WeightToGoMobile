package com.example.weightogo.utils;

import android.util.Log;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for converting between Java 8 date/time types and SQLite-compatible strings.
 *
 * SQLite stores dates and times as TEXT in ISO-8601 format:
 * - Timestamps: "yyyy-MM-dd HH:mm:ss"
 * - Dates: "yyyy-MM-dd"
 *
 * This class provides bidirectional conversion with proper validation and error handling.
 *
 * This is a utility class with only static methods. It is declared final and has a private
 * constructor to prevent instantiation and inheritance.
 */
public final class DateTimeConverter {

    private static final String TAG = "DateTimeConverter";

    /**
     * SQLite-compatible timestamp format (ISO-8601 compliant).
     * Public for DAO layer documentation and validation.
     *
     * Format: "yyyy-MM-dd HH:mm:ss" (24-hour time, no timezone)
     * Example: "2025-12-10 14:30:00"
     */
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);

    /**
     * SQLite-compatible date format (ISO-8601 compliant).
     * Public for DAO layer documentation and validation.
     *
     * Format: "yyyy-MM-dd"
     * Example: "2025-12-10"
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     *
     * @throws AssertionError if instantiation is attempted
     */
    private DateTimeConverter() {
        throw new AssertionError("DateTimeConverter is a utility class and should not be instantiated");
    }

    /**
     * Converts a LocalDateTime to SQLite-compatible timestamp string.
     *
     * @param dateTime the LocalDateTime to convert
     * @return timestamp string in "yyyy-MM-dd HH:mm:ss" format, or null if input is null
     */
    public static String toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            Log.w(TAG, "toTimestamp: called with null LocalDateTime");
            return null;
        }

        try {
            return dateTime.format(TIMESTAMP_FORMATTER);
        } catch (DateTimeException e) {
            Log.e(TAG, "toTimestamp: error formatting LocalDateTime '" + dateTime + "': " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts a SQLite timestamp string to LocalDateTime.
     *
     * @param timestamp the timestamp string in "yyyy-MM-dd HH:mm:ss" format
     * @return LocalDateTime object, or null if input is null/invalid
     */
    public static LocalDateTime fromTimestamp(String timestamp) {
        if (ValidationUtils.isNullOrEmpty(timestamp)) {
            Log.w(TAG, "fromTimestamp: called with null or empty string");
            return null;
        }

        try {
            return LocalDateTime.parse(timestamp, TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException e) {
            Log.e(TAG, "fromTimestamp: error parsing timestamp string '" + timestamp + "': " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts a LocalDate to SQLite-compatible date string.
     *
     * @param date the LocalDate to convert
     * @return date string in "yyyy-MM-dd" format, or null if input is null
     */
    public static String toDateString(LocalDate date) {
        if (date == null) {
            Log.w(TAG, "toDateString: called with null LocalDate");
            return null;
        }

        try {
            return date.format(DATE_FORMATTER);
        } catch (DateTimeException e) {
            Log.e(TAG, "toDateString: error formatting LocalDate '" + date + "': " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts a SQLite date string to LocalDate.
     *
     * @param dateString the date string in "yyyy-MM-dd" format
     * @return LocalDate object, or null if input is null/invalid
     */
    public static LocalDate fromDateString(String dateString) {
        if (ValidationUtils.isNullOrEmpty(dateString)) {
            Log.w(TAG, "fromDateString: called with null or empty string");
            return null;
        }

        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            Log.e(TAG, "fromDateString: error parsing date string '" + dateString + "': " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Validates if a string matches the expected timestamp format.
     * Useful for DAO layer to quickly check format before parsing.
     *
     * @param timestamp the timestamp string to validate
     * @return true if timestamp matches "yyyy-MM-dd HH:mm:ss" format, false otherwise
     */
    public static boolean isValidTimestamp(String timestamp) {
        if (ValidationUtils.isNullOrEmpty(timestamp)) {
            return false;
        }

        try {
            LocalDateTime.parse(timestamp, TIMESTAMP_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Validates if a string matches the expected date format.
     * Useful for DAO layer to quickly check format before parsing.
     *
     * @param dateString the date string to validate
     * @return true if dateString matches "yyyy-MM-dd" format, false otherwise
     */
    public static boolean isValidDateString(String dateString) {
        if (ValidationUtils.isNullOrEmpty(dateString)) {
            return false;
        }

        try {
            LocalDate.parse(dateString, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}