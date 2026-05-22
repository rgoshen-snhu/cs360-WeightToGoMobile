package com.example.weightogo.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.weightogo.R;
import com.example.weightogo.database.AchievementDAO;
import com.example.weightogo.database.UserDAO;
import com.example.weightogo.database.UserPreferenceDAO;
import com.example.weightogo.models.Achievement;
import com.example.weightogo.models.User;

/**
 * Singleton manager for sending SMS notifications.
 *
 * Handles SMS sending for:
 * - Goal achievements
 * - Milestone alerts (5, 10, 25 lbs lost)
 * - Daily reminders
 *
 * **Permissions Required:**
 * - android.permission.SEND_SMS (all Android versions)
 * - android.permission.POST_NOTIFICATIONS (Android 13+)
 *
 * **User Preferences:**
 * - sms_notifications_enabled (master toggle)
 * - sms_goal_alerts (goal reached alerts)
 * - sms_milestone_alerts (milestone alerts)
 * - sms_reminder_enabled (daily reminder)
 *
 * **Thread Safety:** Singleton pattern with synchronized getInstance()
 */
public class SMSNotificationManager {

    private static final String TAG = "SMSNotificationManager";

    // Singleton instance
    private static SMSNotificationManager instance;

    // Preference keys (used by SettingsActivity and UserPreferenceDAO)
    public static final String KEY_SMS_ENABLED = "sms_notifications_enabled";
    public static final String KEY_GOAL_ALERTS = "sms_goal_alerts";
    public static final String KEY_MILESTONE_ALERTS = "sms_milestone_alerts";
    public static final String KEY_REMINDER_ENABLED = "sms_reminder_enabled";

    // Dependencies
    private final Context context;
    private final UserDAO userDAO;
    private final UserPreferenceDAO userPreferenceDAO;
    private final AchievementDAO achievementDAO;

    /**
     * Private constructor for singleton pattern.
     *
     * @param context Application context
     * @param userDAO UserDAO instance
     * @param userPreferenceDAO UserPreferenceDAO instance
     * @param achievementDAO AchievementDAO instance
     */
    private SMSNotificationManager(@NonNull Context context,
                                    @NonNull UserDAO userDAO,
                                    @NonNull UserPreferenceDAO userPreferenceDAO,
                                    @NonNull AchievementDAO achievementDAO) {
        this.context = context.getApplicationContext();
        this.userDAO = userDAO;
        this.userPreferenceDAO = userPreferenceDAO;
        this.achievementDAO = achievementDAO;
    }

    /**
     * Gets singleton instance of SMSNotificationManager.
     * Thread-safe with synchronized block.
     *
     * @param context Application context
     * @param userDAO UserDAO instance
     * @param userPreferenceDAO UserPreferenceDAO instance
     * @param achievementDAO AchievementDAO instance
     * @return Singleton instance
     */
    public static synchronized SMSNotificationManager getInstance(@NonNull Context context,
                                                                   @NonNull UserDAO userDAO,
                                                                   @NonNull UserPreferenceDAO userPreferenceDAO,
                                                                   @NonNull AchievementDAO achievementDAO) {
        if (instance == null) {
            instance = new SMSNotificationManager(context, userDAO, userPreferenceDAO, achievementDAO);
            Log.d(TAG, "getInstance: Created new SMSNotificationManager instance");
        }
        return instance;
    }

    /**
     * Checks if SEND_SMS permission is granted.
     *
     * @return true if permission granted, false otherwise
     */
    public boolean hasSmsSendPermission() {
        boolean hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;

        Log.d(TAG, "hasSmsSendPermission: " + hasPermission);
        return hasPermission;
    }

    /**
     * Checks if POST_NOTIFICATIONS permission is granted (Android 13+).
     * Returns true on Android 12 and below (permission not required).
     *
     * @return true if permission granted or not required, false otherwise
     */
    public boolean hasPostNotificationsPermission() {
        // Android 12 and below don't require POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "hasPostNotificationsPermission: true (Android < 13, permission not required)");
            return true;
        }

        // Android 13+ requires POST_NOTIFICATIONS
        boolean hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;

        Log.d(TAG, "hasPostNotificationsPermission: " + hasPermission + " (Android 13+)");
        return hasPermission;
    }

    /**
     * Checks if SMS can be sent for a user.
     *
     * Checks:
     * 1. User has phone number
     * 2. SMS notifications enabled in preferences
     * 3. SEND_SMS permission granted
     * 4. POST_NOTIFICATIONS permission granted (Android 13+)
     *
     * @param userId User ID to check
     * @return true if all conditions met, false otherwise
     */
    public boolean canSendSms(long userId) {
        Log.d(TAG, "canSendSms: Checking for user_id=" + userId);

        // Check user has phone number
        User user = userDAO.getUserById(userId);
        if (user == null || user.getPhoneNumber() == null) {
            Log.w(TAG, "canSendSms: User or phone number not found");
            return false;
        }

        // Check SMS notifications enabled
        String smsEnabled = userPreferenceDAO.getPreference(userId, KEY_SMS_ENABLED, "false");
        if (!"true".equals(smsEnabled)) {
            Log.d(TAG, "canSendSms: SMS notifications disabled in preferences");
            return false;
        }

        // Check permissions
        if (!hasSmsSendPermission()) {
            Log.w(TAG, "canSendSms: SEND_SMS permission not granted");
            return false;
        }

        if (!hasPostNotificationsPermission()) {
            Log.w(TAG, "canSendSms: POST_NOTIFICATIONS permission not granted");
            return false;
        }

        Log.d(TAG, "canSendSms: All conditions met, can send SMS");
        return true;
    }

    /**
     * Sends SMS for goal achievement.
     *
     * @param userId User ID
     * @param goalWeight Goal weight achieved
     * @param unit Weight unit (lbs/kg)
     * @return true if SMS sent successfully, false otherwise
     */
    public boolean sendGoalAchievedSms(long userId, double goalWeight, String unit) {
        Log.d(TAG, "sendGoalAchievedSms: Checking conditions for user_id=" + userId);

        // Check if we can send SMS
        if (!canSendSms(userId)) {
            Log.d(TAG, "sendGoalAchievedSms: Cannot send SMS (canSendSms returned false)");
            return false;
        }

        // Check if goal alerts are enabled
        String goalAlertsEnabled = userPreferenceDAO.getPreference(userId, KEY_GOAL_ALERTS, "true");
        if (!"true".equals(goalAlertsEnabled)) {
            Log.d(TAG, "sendGoalAchievedSms: Goal alerts disabled in preferences");
            return false;
        }

        // Get user phone number
        User user = userDAO.getUserById(userId);
        if (user == null || user.getPhoneNumber() == null) {
            Log.w(TAG, "sendGoalAchievedSms: User or phone number not found");
            return false;
        }

        // Get message template and format
        String messageTemplate = context.getString(R.string.sms_goal_achieved);
        String message = String.format(messageTemplate, goalWeight, unit);

        // Send SMS
        return sendSms(user.getPhoneNumber(), message, "Goal achieved");
    }

    /**
     * Sends SMS for milestone achievement.
     *
     * @param userId User ID
     * @param milestone Milestone amount (e.g., 5, 10, 25)
     * @param unit Weight unit (lbs/kg)
     * @return true if SMS sent successfully, false otherwise
     */
    public boolean sendMilestoneSms(long userId, int milestone, String unit) {
        Log.d(TAG, "sendMilestoneSms: Checking conditions for user_id=" + userId + ", milestone=" + milestone);

        // Check if we can send SMS
        if (!canSendSms(userId)) {
            Log.d(TAG, "sendMilestoneSms: Cannot send SMS (canSendSms returned false)");
            return false;
        }

        // Check if milestone alerts are enabled
        String milestoneAlertsEnabled = userPreferenceDAO.getPreference(userId, KEY_MILESTONE_ALERTS, "true");
        if (!"true".equals(milestoneAlertsEnabled)) {
            Log.d(TAG, "sendMilestoneSms: Milestone alerts disabled in preferences");
            return false;
        }

        // Get user phone number
        User user = userDAO.getUserById(userId);
        if (user == null || user.getPhoneNumber() == null) {
            Log.w(TAG, "sendMilestoneSms: User or phone number not found");
            return false;
        }

        // Get message template and format
        // Use sms_milestone_5 for all milestones (generic message)
        String messageTemplate = context.getString(R.string.sms_milestone_5);
        String message = String.format(messageTemplate, milestone, unit);

        // Send SMS
        return sendSms(user.getPhoneNumber(), message, "Milestone " + milestone + " " + unit);
    }

    /**
     * Sends daily reminder SMS.
     *
     * @param userId User ID
     * @return true if SMS sent successfully, false otherwise
     */
    public boolean sendDailyReminderSms(long userId) {
        Log.d(TAG, "sendDailyReminderSms: Checking conditions for user_id=" + userId);

        // Check if we can send SMS
        if (!canSendSms(userId)) {
            Log.d(TAG, "sendDailyReminderSms: Cannot send SMS (canSendSms returned false)");
            return false;
        }

        // Check if daily reminders are enabled
        String reminderEnabled = userPreferenceDAO.getPreference(userId, KEY_REMINDER_ENABLED, "false");
        if (!"true".equals(reminderEnabled)) {
            Log.d(TAG, "sendDailyReminderSms: Daily reminders disabled in preferences");
            return false;
        }

        // Get user phone number
        User user = userDAO.getUserById(userId);
        if (user == null || user.getPhoneNumber() == null) {
            Log.w(TAG, "sendDailyReminderSms: User or phone number not found");
            return false;
        }

        // Get message template
        String message = context.getString(R.string.sms_daily_reminder);

        // Send SMS
        return sendSms(user.getPhoneNumber(), message, "Daily reminder");
    }

    /**
     * Sends SMS for achievement and marks as notified.
     *
     * Supports all achievement types: GOAL_REACHED, FIRST_ENTRY, STREAK_7, STREAK_30,
     * MILESTONE_5, MILESTONE_10, MILESTONE_25, MILESTONE_50, NEW_LOW
     *
     * @param achievement Achievement to notify about
     * @return true if SMS sent successfully, false if skipped/failed
     */
    public boolean sendAchievementSms(@NonNull Achievement achievement) {
        Log.d(TAG, "sendAchievementSms: Processing achievement type=" + achievement.getAchievementType());

        long userId = achievement.getUserId();
        String achievementType = achievement.getAchievementType();
        boolean smsSent = false;

        // Handle different achievement types
        switch (achievementType) {
            case "GOAL_REACHED":
                // Check goal alerts preference
                String goalAlertsEnabled = userPreferenceDAO.getPreference(userId, KEY_GOAL_ALERTS, "true");
                if ("true".equals(goalAlertsEnabled)) {
                    Double goalWeight = achievement.getValue();
                    if (goalWeight != null) {
                        // Get user's weight unit preference (default to lbs)
                        String unit = userPreferenceDAO.getPreference(userId, "weight_unit_preference", "lbs");
                        smsSent = sendGoalAchievedSms(userId, goalWeight, unit);
                    }
                }
                break;

            case "MILESTONE_5":
            case "MILESTONE_10":
            case "MILESTONE_25":
            case "MILESTONE_50":
                // Check milestone alerts preference
                String milestoneAlertsEnabled = userPreferenceDAO.getPreference(userId, KEY_MILESTONE_ALERTS, "true");
                if ("true".equals(milestoneAlertsEnabled)) {
                    Double milestoneValue = achievement.getValue();
                    if (milestoneValue != null) {
                        String unit = userPreferenceDAO.getPreference(userId, "weight_unit_preference", "lbs");
                        smsSent = sendMilestoneSms(userId, milestoneValue.intValue(), unit);
                    }
                }
                break;

            case "FIRST_ENTRY":
            case "STREAK_7":
            case "STREAK_30":
            case "NEW_LOW":
                // These use milestone alerts preference for now
                // Could add separate preferences in future
                String streakAlertsEnabled = userPreferenceDAO.getPreference(userId, KEY_MILESTONE_ALERTS, "true");
                if ("true".equals(streakAlertsEnabled)) {
                    // For now, skip SMS for these types (would need new message templates)
                    Log.d(TAG, "sendAchievementSms: Skipping SMS for " + achievementType + " (no template)");
                }
                break;

            default:
                Log.w(TAG, "sendAchievementSms: Unknown achievement type: " + achievementType);
                break;
        }

        // Mark achievement as notified if SMS was sent
        if (smsSent) {
            int updated = achievementDAO.updateIsNotified(achievement.getAchievementId(), true);
            if (updated > 0) {
                Log.i(TAG, "sendAchievementSms: Marked achievement as notified: " + achievement.getAchievementId());
            } else {
                Log.w(TAG, "sendAchievementSms: Failed to mark achievement as notified");
            }
        }

        return smsSent;
    }

    /**
     * Sends SMS for multiple achievements in batch.
     * Useful for sending all unnotified achievements at once.
     *
     * @param achievements List of achievements to notify
     * @return Number of SMS sent successfully
     */
    public int sendAchievementBatch(@NonNull java.util.List<Achievement> achievements) {
        Log.d(TAG, "sendAchievementBatch: Processing " + achievements.size() + " achievements");

        int successCount = 0;

        for (Achievement achievement : achievements) {
            boolean sent = sendAchievementSms(achievement);
            if (sent) {
                successCount++;
            }
        }

        Log.i(TAG, "sendAchievementBatch: Sent " + successCount + " of " + achievements.size() + " SMS");
        return successCount;
    }

    /**
     * Private helper method to send SMS using Android SmsManager.
     *
     * @param phoneNumber E.164 formatted phone number
     * @param message SMS message text
     * @param messageType Type of message for logging (e.g., "Goal achieved", "Milestone 5 lbs")
     * @return true if SMS sent successfully, false on error
     */
    private boolean sendSms(@NonNull String phoneNumber, @NonNull String message, @NonNull String messageType) {
        try {
            String maskedPhone = ValidationUtils.maskPhoneNumber(phoneNumber);
            Log.d(TAG, "sendSms: Attempting to send " + messageType + " SMS to " + maskedPhone);

            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            smsManager.sendTextMessage(
                    phoneNumber,    // destination phone number
                    null,           // service center address (null = use default)
                    message,        // message text
                    null,           // sentIntent (null = no notification)
                    null            // deliveryIntent (null = no delivery confirmation)
            );

            Log.i(TAG, "sendSms: Successfully sent " + messageType + " SMS to " + maskedPhone);
            return true;

        } catch (SecurityException e) {
            // Thrown if SEND_SMS permission not granted
            Log.e(TAG, "sendSms: SecurityException - SEND_SMS permission not granted", e);
            return false;

        } catch (IllegalArgumentException e) {
            // Thrown if phone number or message is invalid
            Log.e(TAG, "sendSms: IllegalArgumentException - Invalid phone or message", e);
            return false;

        } catch (Exception e) {
            // Catch any other unexpected exceptions
            Log.e(TAG, "sendSms: Unexpected exception while sending " + messageType + " SMS", e);
            return false;
        }
    }
}
