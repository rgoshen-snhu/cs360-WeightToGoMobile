package com.example.weightogo.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;

/**
 * Model class representing a user account.
 */
public class User {

    /** Primary key - unique identifier for user account */
    private long userId;

    /** Unique username for authentication - cannot be null */
    @NonNull private String username;

    /** Optional email address for account recovery and notifications */
    @Nullable private String email;

    /**
     * Optional phone number for SMS notifications in E.164 format (e.g., +15551234567).
     * Required for SMS notification features (FR-5).
     */
    @Nullable private String phoneNumber;

    /** Optional display name shown in UI */
    @Nullable private String displayName;

    /**
     * Hashed password for authentication.
     * Algorithm used is specified in passwordAlgorithm field.
     * NEVER store, log, or transmit plain text passwords.
     */
    @NonNull private String passwordHash;

    /**
     * Cryptographic salt used for password hashing (for SHA256 only).
     * bcrypt handles salts internally.
     * NEVER store, log, or expose this value.
     */
    @NonNull private String salt;

    /**
     * Password hashing algorithm used: 'SHA256', 'BCRYPT', or 'ARGON2'.
     * Phase 8.6: Enables lazy migration from SHA256 to bcrypt on login.
     */
    @NonNull private String passwordAlgorithm;

    /** Timestamp when user account was created */
    @NonNull private LocalDateTime createdAt;

    /** Timestamp when user account was last updated */
    @NonNull private LocalDateTime updatedAt;

    /** Timestamp of last successful login (null if never logged in) */
    @Nullable private LocalDateTime lastLogin;

    /** Account status flag - true if account is active, false if deactivated */
    private boolean isActive;

    /**
     * Default constructor.
     */
    public User() {
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    public void setEmail(@Nullable String email) {
        this.email = email;
    }

    @Nullable
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(@Nullable String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
    }

    @NonNull
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(@NonNull String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @NonNull
    public String getSalt() {
        return salt;
    }

    public void setSalt(@NonNull String salt) {
        this.salt = salt;
    }

    @NonNull
    public String getPasswordAlgorithm() {
        return passwordAlgorithm;
    }

    public void setPasswordAlgorithm(@NonNull String passwordAlgorithm) {
        this.passwordAlgorithm = passwordAlgorithm;
    }

    @NonNull
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @NonNull
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@NonNull LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Nullable
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(@Nullable LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * Equality based on userId (primary key).
     * Note: This implementation assumes User will not be subclassed.
     * Two users are equal if they have the same non-zero userId.
     * Uninitialized users (userId=0) are never equal to prevent false matches.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return userId != 0 && userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(userId);
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", createdAt=" + createdAt +
                ", lastLogin=" + lastLogin +
                '}';
    }
}
