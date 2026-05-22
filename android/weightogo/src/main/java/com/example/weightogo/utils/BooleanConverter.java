package com.example.weightogo.utils;

import android.util.Log;

/**
 * Utility class for converting between Java boolean and SQLite INTEGER (0/1).
 *
 * SQLite does not have a native BOOLEAN type. By convention, INTEGER values are used:
 * - 0 represents false
 * - 1 (or any non-zero value) represents true
 *
 * This converter ensures consistent boolean ↔ INTEGER conversion across the application.
 *
 * Usage:
 * - DAO layer: Convert boolean to INTEGER before database operations
 * - Model layer: Convert INTEGER to boolean after database queries
 *
 * This is a utility class with only static methods. It is declared final and has a private
 * constructor to prevent instantiation and inheritance.
 *
 * @see <a href="https://www.sqlite.org/datatype3.html">SQLite Datatypes</a>
 */
public final class BooleanConverter {

    private static final String TAG = "BooleanConverter";

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     *
     * @throws AssertionError if instantiation is attempted
     */
    private BooleanConverter() {
        throw new AssertionError("BooleanConverter is a utility class and should not be instantiated");
    }

    /**
     * Converts a Java boolean to SQLite INTEGER representation.
     *
     * @param value the boolean value to convert
     * @return 1 if true, 0 if false
     */
    public static int toInteger(boolean value) {
        return value ? 1 : 0;
    }

    /**
     * Converts a SQLite INTEGER to Java boolean.
     *
     * Follows SQLite convention: 0 is false, any non-zero value is true.
     * Logs a warning if value is not the expected 0 or 1 (defensive validation).
     *
     * @param value the INTEGER value from database (typically 0 or 1)
     * @return false if value is 0, true otherwise
     */
    public static boolean fromInteger(int value) {
        if (value < 0 || value > 1) {
            Log.w(TAG, "fromInteger: unexpected value '" + value + "' (expected 0 or 1), treating as boolean");
        }
        return value != 0;
    }
}
