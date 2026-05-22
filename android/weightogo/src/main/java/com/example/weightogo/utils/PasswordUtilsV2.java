package com.example.weightogo.utils;

import android.util.Log;

import at.favre.lib.crypto.bcrypt.BCrypt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Password utilities with bcrypt support (Phase 8.6).
 * Provides backward compatibility with SHA256 during migration.
 *
 * <p><strong>Migration Strategy:</strong>
 * - Legacy users: SHA256 + salt verification
 * - New users: bcrypt (salt handled internally)
 * - Lazy migration: SHA256 users migrated to bcrypt on login
 * </p>
 *
 * <p><strong>Security:</strong>
 * - bcrypt cost factor: 12 (2^12 = 4096 iterations)
 * - Resistant to brute-force and GPU attacks
 * - Industry standard for password hashing
 * </p>
 */
public class PasswordUtilsV2 {

    private static final String TAG = "PasswordUtilsV2";
    private static final int BCRYPT_COST = 12;  // 2^12 iterations (~300ms on modern hardware)

    // Password algorithm constants (public for use in LoginActivity and other components)
    public static final String ALGORITHM_SHA256 = "SHA256";
    public static final String ALGORITHM_BCRYPT = "BCRYPT";

    /**
     * Hash password using bcrypt.
     * Salt is generated and embedded automatically by bcrypt.
     *
     * @param password Plain text password (never logged)
     * @return bcrypt hash string (starts with $2a$12$), or null if hashing fails
     */
    @Nullable
    public static String hashPasswordBcrypt(@NonNull String password) {
        if (password == null || password.isEmpty()) {
            Log.w(TAG, "hashPasswordBcrypt: Empty password provided");
            return null;
        }

        try {
            String hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
            Log.d(TAG, "hashPasswordBcrypt: Successfully hashed password with bcrypt");
            return hash;

        } catch (Exception e) {
            Log.e(TAG, "hashPasswordBcrypt: Failed to hash password", e);
            return null;
        }
    }

    /**
     * Verify password against bcrypt hash.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param password Plain text password (never logged)
     * @param bcryptHash bcrypt hash from database (starts with $2a$12$)
     * @return true if password matches hash
     */
    public static boolean verifyPasswordBcrypt(@NonNull String password, @NonNull String bcryptHash) {
        if (password == null || password.isEmpty()) {
            Log.w(TAG, "verifyPasswordBcrypt: Empty password provided");
            return false;
        }

        if (bcryptHash == null || bcryptHash.isEmpty()) {
            Log.w(TAG, "verifyPasswordBcrypt: Empty hash provided");
            return false;
        }

        try {
            BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), bcryptHash);
            boolean verified = result.verified;

            if (verified) {
                Log.d(TAG, "verifyPasswordBcrypt: Password verified successfully");
            } else {
                Log.w(TAG, "verifyPasswordBcrypt: Password verification failed");
            }

            return verified;

        } catch (Exception e) {
            Log.e(TAG, "verifyPasswordBcrypt: Exception during verification", e);
            return false;
        }
    }

    /**
     * Verify password using algorithm-specific verification.
     * Supports both SHA256 (legacy) and BCRYPT.
     *
     * <p><strong>Migration Support:</strong>
     * This method enables lazy migration from SHA256 to bcrypt.
     * Legacy users authenticate with SHA256, then are migrated to bcrypt on next login.
     * </p>
     *
     * @param password Plain text password (never logged)
     * @param algorithm 'SHA256' or 'BCRYPT'
     * @param storedHash Hash from database
     * @param salt Salt from database (only used for SHA256, empty for bcrypt)
     * @return true if password matches using the specified algorithm
     */
    public static boolean verifyPassword(@NonNull String password,
                                         @NonNull String algorithm,
                                         @NonNull String storedHash,
                                         @NonNull String salt) {
        Log.d(TAG, "verifyPassword: Verifying password with algorithm: " + algorithm);

        if (ALGORITHM_BCRYPT.equals(algorithm)) {
            return verifyPasswordBcrypt(password, storedHash);

        } else if (ALGORITHM_SHA256.equals(algorithm)) {
            // Delegate to legacy SHA256 verification
            return PasswordUtils.verifyPassword(password, salt, storedHash);

        } else {
            Log.w(TAG, "verifyPassword: Unknown algorithm: " + algorithm);
            return false;
        }
    }
}
