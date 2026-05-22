package com.example.weightogo.utils;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/**
 * Utility class for input validation (usernames, passwords, etc.).
 *
 * Provides validation methods for user inputs to ensure data integrity and security.
 * All validation methods are null-safe and return false for invalid inputs.
 *
 * **Validation Rules:**
 * - **Username**: 3-20 characters, alphanumeric and underscore only
 * - **Password**: Minimum 6 characters with at least one digit
 *
 * **Usage Example:**
 * <pre>
 * // Validate username
 * if (ValidationUtils.isValidUsername(username)) {
 *     // Proceed with registration
 * } else {
 *     // Show error message
 * }
 *
 * // Validate password
 * if (ValidationUtils.isValidPassword(password)) {
 *     // Proceed with authentication
 * } else {
 *     // Show error message
 * }
 * </pre>
 *
 * **Thread Safety:** All methods are static and thread-safe.
 */
public final class ValidationUtils {

    private static final String TAG = "ValidationUtils";

    /**
     * Username validation pattern: 3-20 characters, alphanumeric and underscore only.
     * Pattern: ^[a-zA-Z0-9_]{3,20}$
     * - ^ = start of string
     * - [a-zA-Z0-9_] = alphanumeric and underscore character class
     * - {3,20} = 3 to 20 characters
     * - $ = end of string
     */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    /**
     * Minimum password length requirement.
     */
    private static final int MIN_PASSWORD_LENGTH = 6;

    /**
     * Phone number validation pattern: E.164 international format.
     * Pattern: ^\\+?[1-9]\\d{9,14}$
     * - ^ = start of string
     * - \\+? = optional plus sign
     * - [1-9] = first digit 1-9 (not 0)
     * - \\d{9,14} = 9-14 more digits (total 10-15 digits)
     * - $ = end of string
     *
     * Examples:
     * - Valid: +12025551234, 2025551234, +447911123456
     * - Invalid: 202-555-1234, +1 202 555 1234, abc123
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{9,14}$");

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     *
     * @throws AssertionError if instantiation is attempted via reflection
     */
    private ValidationUtils() {
        throw new AssertionError("ValidationUtils is a utility class and should not be instantiated");
    }

    // =============================================================================================
    // NULL/EMPTY CHECKING
    // =============================================================================================

    /**
     * Checks if a string is null, empty, or contains only whitespace.
     * This is a centralized null-checking method to eliminate duplicate code patterns
     * throughout the codebase (DRY principle).
     *
     * **Usage Example:**
     * <pre>
     * if (ValidationUtils.isNullOrEmpty(username)) {
     *     // Show error: "Username is required"
     *     return;
     * }
     * </pre>
     *
     * @param value the string to check
     * @return true if the string is null, empty (""), or whitespace-only ("   ")
     */
    public static boolean isNullOrEmpty(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    // =============================================================================================
    // USERNAME & PASSWORD VALIDATION
    // =============================================================================================

    /**
     * Validates a username against security requirements.
     *
     * **Validation Rules:**
     * - Length: 3-20 characters
     * - Allowed characters: a-z, A-Z, 0-9, underscore (_)
     * - No spaces, special characters, or Unicode allowed
     *
     * **Security Note:** Username validation prevents SQL injection attempts
     * and ensures usernames are database-compatible.
     *
     * @param username the username to validate
     * @return true if valid, false if null, empty, or doesn't match pattern
     */
    public static boolean isValidUsername(@Nullable String username) {
        // Null or empty check
        if (isNullOrEmpty(username)) {
            Log.w(TAG, "isValidUsername: username is null or empty");
            return false;
        }

        // Pattern matching
        boolean isValid = USERNAME_PATTERN.matcher(username).matches();

        if (!isValid) {
            Log.w(TAG, "isValidUsername: username does not match pattern (3-20 chars, alphanumeric + underscore)");
        } else {
            Log.d(TAG, "isValidUsername: username is valid");
        }

        return isValid;
    }

    /**
     * Validates a password against security requirements.
     *
     * **Validation Rules:**
     * - Minimum length: 6 characters
     * - Must contain at least one digit (0-9)
     * - No maximum length (longer is better for security)
     *
     * **Security Note:** These are minimum requirements for Phase 2.
     * Future phases may add additional requirements:
     * - Uppercase letter
     * - Lowercase letter
     * - Special character
     * - Maximum length limit
     *
     * @param password the password to validate
     * @return true if valid, false if null, empty, too short, or missing digits
     */
    public static boolean isValidPassword(@Nullable String password) {
        // Null or empty check
        if (isNullOrEmpty(password)) {
            Log.w(TAG, "isValidPassword: password is null or empty");
            return false;
        }

        // Length check
        if (password.length() < MIN_PASSWORD_LENGTH) {
            Log.w(TAG, "isValidPassword: password is too short (min " + MIN_PASSWORD_LENGTH + " chars)");
            return false;
        }

        // Must contain at least one digit
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasDigit) {
            Log.w(TAG, "isValidPassword: password does not contain a digit");
            return false;
        }

        Log.d(TAG, "isValidPassword: password is valid");
        return true;
    }

    // =============================================================================================
    // PHONE NUMBER VALIDATION (Phase 7.1)
    // =============================================================================================

    /**
     * Validates a phone number against E.164 international format.
     *
     * **Validation Rules:**
     * - 10-15 digits total
     * - Optional + prefix (international format)
     * - No spaces, dashes, or letters
     * - First digit cannot be 0
     *
     * **Examples:**
     * - Valid: "2025551234", "+12025551234", "+447911123456"
     * - Invalid: "202-555-1234", "+1 202 555 1234", "123456789"
     *
     * **Note:** US numbers without country code (10 digits) are accepted.
     * Use formatPhoneE164() to convert to E.164 format before storage.
     *
     * @param phoneNumber the phone number to validate
     * @return true if valid E.164 format, false otherwise
     */
    public static boolean isValidPhoneNumber(@Nullable String phoneNumber) {
        // Null or empty check
        if (isNullOrEmpty(phoneNumber)) {
            Log.w(TAG, "isValidPhoneNumber: phone number is null or empty");
            return false;
        }

        // Remove all whitespace for validation
        String cleanPhone = phoneNumber.replaceAll("\\s+", "");

        // Pattern matching
        boolean isValid = PHONE_PATTERN.matcher(cleanPhone).matches();

        if (!isValid) {
            Log.w(TAG, "isValidPhoneNumber: phone does not match E.164 pattern (10-15 digits, optional +)");
        } else {
            Log.d(TAG, "isValidPhoneNumber: phone is valid E.164 format");
        }

        return isValid;
    }

    /**
     * Formats a phone number to E.164 international format.
     *
     * **Conversion Rules:**
     * - Already E.164 (+1...): Return unchanged
     * - 10-digit US number: Prepend +1
     * - Invalid format: Return null
     *
     * **Examples:**
     * - Input: "2025551234" → Output: "+12025551234"
     * - Input: "+12025551234" → Output: "+12025551234"
     * - Input: "202-555-1234" → Output: null (invalid format)
     *
     * **Note:** This method assumes US country code (+1) for 10-digit numbers.
     * International numbers must already include country code.
     *
     * @param phoneNumber the phone number to format
     * @return E.164 formatted phone number, or null if invalid
     */
    @Nullable
    public static String formatPhoneE164(@Nullable String phoneNumber) {
        // Validate first
        if (!isValidPhoneNumber(phoneNumber)) {
            Log.w(TAG, "formatPhoneE164: phone number is invalid, cannot format");
            return null;
        }

        // Remove whitespace
        String cleanPhone = phoneNumber.replaceAll("\\s+", "");

        // Already E.164 format (starts with +)
        if (cleanPhone.startsWith("+")) {
            Log.d(TAG, "formatPhoneE164: already E.164 format");
            return cleanPhone;
        }

        // Assume US country code for 10-digit numbers
        if (cleanPhone.length() == 10) {
            String e164 = "+1" + cleanPhone;
            Log.d(TAG, "formatPhoneE164: converted US number to E.164: " + e164);
            return e164;
        }

        // All other cases: prepend + if not present
        // (International numbers 11-15 digits should already have country code)
        String e164 = "+" + cleanPhone;
        Log.d(TAG, "formatPhoneE164: prepended + to phone number");
        return e164;
    }

    /**
     * Gets specific validation error message for phone number.
     * Returns string resource key (not localized string).
     *
     * **Error Priority:**
     * 1. Required (null/empty)
     * 2. Invalid characters (letters, special chars except +)
     * 3. Too short (< 10 digits)
     * 4. Too long (> 15 digits)
     * 5. Invalid E.164 pattern
     *
     * **Usage Example:**
     * <pre>
     * String error = ValidationUtils.getPhoneValidationError(phoneNumber);
     * if (error != null) {
     *     // Show error message from string resource
     *     phoneInput.setError(getString(R.string.valueOf(error)));
     * }
     * </pre>
     *
     * @param phoneNumber the phone number to validate
     * @return String resource key (e.g., "error_phone_required"), or null if valid
     */
    @Nullable
    public static String getPhoneValidationError(@Nullable String phoneNumber) {
        // Priority 1: Check null or empty
        if (isNullOrEmpty(phoneNumber)) {
            Log.d(TAG, "getPhoneValidationError: phone is null or empty");
            return "error_phone_required";
        }

        // Remove whitespace for validation
        String cleanPhone = phoneNumber.replaceAll("\\s+", "");

        // Priority 2: Check for invalid characters (only digits and + allowed)
        // Allow optional + at start, then only digits
        if (!cleanPhone.matches("^\\+?[0-9]+$")) {
            Log.d(TAG, "getPhoneValidationError: phone contains invalid characters");
            return "error_phone_invalid_chars";
        }

        // Count digits only (exclude + sign)
        String digitsOnly = cleanPhone.replaceAll("\\+", "");
        int digitCount = digitsOnly.length();

        // Priority 3: Check too short
        if (digitCount < 10) {
            Log.d(TAG, "getPhoneValidationError: phone is too short (" + digitCount + " digits)");
            return "error_phone_too_short";
        }

        // Priority 4: Check too long
        if (digitCount > 15) {
            Log.d(TAG, "getPhoneValidationError: phone is too long (" + digitCount + " digits)");
            return "error_phone_too_long";
        }

        // Priority 5: Check E.164 pattern (validates first digit cannot be 0)
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            Log.d(TAG, "getPhoneValidationError: phone does not match E.164 pattern");
            return "error_phone_invalid";
        }

        // Valid phone number
        Log.d(TAG, "getPhoneValidationError: phone is valid");
        return null;
    }

    // =============================================================================================
    // PHONE NUMBER MASKING (Bug Fix: Phone Persistence & Emulator SMS Testing)
    // =============================================================================================

    /**
     * Masking constants for secure phone number logging.
     */
    private static final String MASK_PREFIX = "***";
    private static final String MASK_NONE = MASK_PREFIX + "NONE";

    /**
     * Masks phone number for secure logging.
     * Shows only last 4 digits for security compliance (PII protection).
     *
     * **Masking Rules:**
     * - Valid phone: Show last 4 digits (e.g., "+12025551234" → "***1234")
     * - Null/empty: Return "***NONE"
     * - Short (<4 chars): Return "***"
     *
     * **Usage Example:**
     * <pre>
     * String masked = ValidationUtils.maskPhoneNumber("+12025551234");
     * Log.i(TAG, "Sending SMS to: " + masked);  // Logs: "Sending SMS to: ***1234"
     * </pre>
     *
     * **Security Note:** Always use this method when logging phone numbers
     * to prevent PII exposure in logs (GDPR/compliance requirement).
     *
     * @param phoneNumber the phone number to mask
     * @return masked phone number (last 4 digits visible), or "***NONE" if null/empty
     */
    @NonNull
    public static String maskPhoneNumber(@Nullable String phoneNumber) {
        // Null or empty check
        if (isNullOrEmpty(phoneNumber)) {
            return MASK_NONE;
        }

        // Remove all non-digit characters for masking
        String digitsOnly = phoneNumber.replaceAll("\\D", "");

        // Short number (less than 4 digits): mask all
        if (digitsOnly.length() < 4) {
            return MASK_PREFIX;
        }

        // Standard masking: show last 4 digits
        String last4 = digitsOnly.substring(digitsOnly.length() - 4);
        return MASK_PREFIX + last4;
    }

    // =============================================================================================
    // EMULATOR DETECTION (Bug Fix: Emulator SMS Testing)
    // =============================================================================================

    /**
     * Detects if app is running on Android emulator vs real device.
     *
     * **Detection Strategy:**
     * Checks Build.FINGERPRINT for emulator signatures like "generic", "unknown",
     * and Build.MODEL for common emulator model names.
     *
     * **Common Emulator Signatures:**
     * - Android Studio Emulator: FINGERPRINT contains "generic"
     * - Genymotion: PRODUCT contains "vbox" (VirtualBox)
     * - Generic AVD: MODEL contains "google_sdk" or "Emulator"
     *
     * **Usage Example:**
     * <pre>
     * if (ValidationUtils.isRunningOnEmulator()) {
     *     // Log to Logcat instead of sending real SMS
     *     Log.i(TAG, "Test SMS (emulator): " + message);
     * } else {
     *     // Send real SMS on device
     *     SmsManager.getDefault().sendTextMessage(...);
     * }
     * </pre>
     *
     * **Limitations:** Not 100% reliable (some custom emulators may be missed),
     * but sufficient for development/testing purposes.
     *
     * @return true if running on emulator, false if real device
     */
    public static boolean isRunningOnEmulator() {
        // Null-safe checks for Build properties (can be null in test environments)
        String fingerprint = Build.FINGERPRINT != null ? Build.FINGERPRINT : "";
        String model = Build.MODEL != null ? Build.MODEL : "";
        String product = Build.PRODUCT != null ? Build.PRODUCT : "";

        boolean isEmulator = fingerprint.contains("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || product.contains("sdk")
                || product.contains("vbox");  // VirtualBox (Genymotion)

        // Log result only (not device details for security)
        Log.d(TAG, "isRunningOnEmulator: " + isEmulator);

        return isEmulator;
    }
}
