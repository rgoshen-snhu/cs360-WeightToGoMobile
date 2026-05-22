package com.example.weightogo.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.weightogo.R;
import com.example.weightogo.database.AchievementDAO;
import com.example.weightogo.database.UserDAO;
import com.example.weightogo.database.UserPreferenceDAO;
import com.example.weightogo.database.WeighToGoDBHelper;
import com.example.weightogo.models.User;
import com.example.weightogo.utils.SMSNotificationManager;
import com.example.weightogo.utils.SessionManager;
import com.example.weightogo.utils.ValidationUtils;
import com.example.weightogo.workers.DailyReminderWorker;
import androidx.appcompat.widget.SwitchCompat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SettingsActivity - Centralized settings management screen
 *
 * Features:
 * - Weight unit preference (lbs/kg) toggle
 * - SMS notification settings and permissions
 * - Phone number management
 * - SMS preference toggles (master, goal alerts, milestone alerts, daily reminders)
 * - Test message functionality
 *
 * Part of Phase 6.0.4: Global Weight Unit Preference System
 * Part of Phase 7.4: SMS Notification Management
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    // UI Elements - Weight Unit
    private ImageButton backButton;
    private TextView unitLbs;
    private TextView unitKg;

    // UI Elements - SMS Permissions
    private TextView permissionStatusBadge;
    private Button grantPermissionButton;

    // UI Elements - Phone Number
    private EditText phoneNumberInput;

    // UI Elements - SMS Preferences
    private SwitchCompat switchEnableSms;
    private SwitchCompat switchGoalAlerts;
    private SwitchCompat switchMilestoneAlerts;
    private SwitchCompat switchDailyReminders;
    private Button sendTestMessageButton;

    // Permission Launcher
    private ActivityResultLauncher<String[]> permissionLauncher;

    // Data Layer
    private WeighToGoDBHelper dbHelper;
    private UserPreferenceDAO userPreferenceDAO;
    private UserDAO userDAO;
    private SMSNotificationManager smsManager;

    // Background Thread Executor (for database operations in onPause)
    private ExecutorService executorService;

    // State
    private String currentUnit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize background thread executor
        executorService = Executors.newSingleThreadExecutor();

        // Initialize data layer and SMS manager
        initDataLayer();

        // Setup permission launcher BEFORE creating activity
        setupPermissionLauncher();

        // Initialize UI views
        initViews();

        // Load current preferences
        loadCurrentPreference();

        // Load SMS-related preferences
        loadPhoneNumber();
        loadSmsPreferences();
        checkPermissions();

        // Setup click listeners last
        setupClickListeners();
    }

    /**
     * Lifecycle callback when activity is paused (user navigates away).
     * Automatically saves phone number if valid to prevent data loss.
     *
     * **Fix for Issue:** Phone number only saved when pressing keyboard "Done" button.
     * Users expect phone number to persist when navigating away via back button,
     * home button, or opening another app.
     *
     * **Implementation:**
     * - Validates phone number using existing ValidationUtils
     * - Formats to E.164 using existing formatPhoneE164()
     * - Saves via existing UserDAO.updatePhoneNumber()
     * - Silent save (no toast notification to avoid interrupting navigation)
     *
     * @see ValidationUtils#getPhoneValidationError(String)
     * @see ValidationUtils#formatPhoneE164(String)
     * @see UserDAO#updatePhoneNumber(long, String)
     */
    @Override
    protected void onPause() {
        super.onPause();

        // Auto-save phone number if present
        if (phoneNumberInput != null) {
            String phoneInput = phoneNumberInput.getText().toString().trim();

            // Only save if non-empty
            if (!phoneInput.isEmpty()) {
                // Validate phone number
                String error = ValidationUtils.getPhoneValidationError(phoneInput);
                if (error == null) {
                    // Valid phone - format and save on background thread
                    String e164Phone = ValidationUtils.formatPhoneE164(phoneInput);
                    if (e164Phone != null) {
                        long userId = SessionManager.getInstance(this).getCurrentUserId();

                        // Execute database write on background thread to avoid ANR
                        executorService.execute(() -> {
                            boolean success = userDAO.updatePhoneNumber(userId, e164Phone);

                            if (success) {
                                Log.i(TAG, "onPause: Auto-saved phone number for user " + userId);
                            } else {
                                Log.w(TAG, "onPause: Failed to auto-save phone number");

                                // Notify user on UI thread if save fails
                                runOnUiThread(() -> {
                                    Toast.makeText(SettingsActivity.this,
                                            "Failed to save phone number",
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    }
                } else {
                    // Invalid phone - log but don't block navigation
                    Log.d(TAG, "onPause: Skipping save for invalid phone number (error: " + error + ")");
                }
            }
        }
    }

    /**
     * Cleanup executor service when activity is destroyed.
     * Prevents thread leaks by shutting down background executor.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Shutdown executor service to prevent thread leaks
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                // Wait up to 1 second for pending tasks to complete
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    Log.w(TAG, "onDestroy: Executor service did not terminate in time");
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
                Log.e(TAG, "onDestroy: Interrupted while shutting down executor", e);
            }
        }
    }

    /**
     * Initialize database helper and DAOs.
     * Only initializes if not already set (allows test injection).
     */
    private void initDataLayer() {
        if (dbHelper == null) {
            dbHelper = WeighToGoDBHelper.getInstance(this);
        }
        if (userPreferenceDAO == null) {
            userPreferenceDAO = new UserPreferenceDAO(dbHelper);
        }
        if (userDAO == null) {
            userDAO = new UserDAO(dbHelper);
        }

        // Initialize shared DAO (avoid inline instantiation for clarity)
        AchievementDAO achievementDAO = new AchievementDAO(dbHelper);

        if (smsManager == null) {
            smsManager = SMSNotificationManager.getInstance(this, userDAO, userPreferenceDAO, achievementDAO);
        }
    }

    // =============================================================================================
    // TESTING SETTERS (Package-Private)
    // =============================================================================================

    /**
     * Set UserDAO instance (for testing only).
     *
     * @param userDAO the UserDAO instance to use
     * @throws IllegalArgumentException if userDAO is null
     */
    @VisibleForTesting
    void setUserDAO(UserDAO userDAO) {
        if (userDAO == null) {
            throw new IllegalArgumentException("UserDAO cannot be null");
        }
        this.userDAO = userDAO;
    }

    /**
     * Set UserPreferenceDAO instance (for testing only).
     *
     * @param userPreferenceDAO the UserPreferenceDAO instance to use
     * @throws IllegalArgumentException if userPreferenceDAO is null
     */
    @VisibleForTesting
    void setUserPreferenceDAO(UserPreferenceDAO userPreferenceDAO) {
        if (userPreferenceDAO == null) {
            throw new IllegalArgumentException("UserPreferenceDAO cannot be null");
        }
        this.userPreferenceDAO = userPreferenceDAO;
    }

    /**
     * Set SMSNotificationManager instance (for testing only).
     *
     * @param smsManager the SMSNotificationManager instance to use
     * @throws IllegalArgumentException if smsManager is null
     */
    @VisibleForTesting
    void setSMSNotificationManager(SMSNotificationManager smsManager) {
        if (smsManager == null) {
            throw new IllegalArgumentException("SMSNotificationManager cannot be null");
        }
        this.smsManager = smsManager;
    }

    // =============================================================================================
    // VIEW INITIALIZATION
    // =============================================================================================

    /**
     * Initialize view references
     */
    private void initViews() {
        // Weight unit toggle
        backButton = findViewById(R.id.backButton);
        unitLbs = findViewById(R.id.unitLbs);
        unitKg = findViewById(R.id.unitKg);

        // SMS UI elements
        permissionStatusBadge = findViewById(R.id.permissionStatusBadge);
        grantPermissionButton = findViewById(R.id.grantPermissionButton);
        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        switchEnableSms = findViewById(R.id.switchEnableSms);
        switchGoalAlerts = findViewById(R.id.switchGoalAlerts);
        switchMilestoneAlerts = findViewById(R.id.switchMilestoneAlerts);
        switchDailyReminders = findViewById(R.id.switchDailyReminders);
        sendTestMessageButton = findViewById(R.id.sendTestMessageButton);
    }

    /**
     * Load user's current weight unit preference from database
     */
    private void loadCurrentPreference() {
        long userId = SessionManager.getInstance(this).getCurrentUserId();
        currentUnit = userPreferenceDAO.getWeightUnit(userId);
        updateUnitButtonUI();
    }

    /**
     * Setup click listeners for all interactive elements
     */
    private void setupClickListeners() {
        // Back button navigation
        backButton.setOnClickListener(v -> finish());

        // Weight unit toggle
        unitLbs.setOnClickListener(v -> saveWeightUnit("lbs"));
        unitKg.setOnClickListener(v -> saveWeightUnit("kg"));

        // SMS click listeners
        if (grantPermissionButton != null) {
            grantPermissionButton.setOnClickListener(v -> requestPermissions());
        }
        if (phoneNumberInput != null) {
            phoneNumberInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    handleSavePhone();
                    return true;
                }
                return false;
            });
        }
        if (switchEnableSms != null) {
            switchEnableSms.setOnCheckedChangeListener((buttonView, isChecked) -> handleMasterToggle(isChecked));
        }
        if (switchGoalAlerts != null) {
            switchGoalAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> handleGoalAlertsToggle(isChecked));
        }
        if (switchMilestoneAlerts != null) {
            switchMilestoneAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> handleMilestoneAlertsToggle(isChecked));
        }
        if (switchDailyReminders != null) {
            switchDailyReminders.setOnCheckedChangeListener((buttonView, isChecked) -> handleReminderToggle(isChecked));
        }
        if (sendTestMessageButton != null) {
            sendTestMessageButton.setOnClickListener(v -> handleSendTestMessage());
        }
    }

    /**
     * Save selected weight unit to database and update UI
     *
     * @param unit Weight unit to save ("lbs" or "kg")
     */
    private void saveWeightUnit(String unit) {
        long userId = SessionManager.getInstance(this).getCurrentUserId();

        boolean success = userPreferenceDAO.setWeightUnit(userId, unit);
        if (success) {
            currentUnit = unit;
            updateUnitButtonUI();
            Toast.makeText(this,
                    getString(R.string.weight_unit_updated, unit),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,
                    R.string.weight_unit_update_failed,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Update toggle button UI to reflect current unit selection
     */
    private void updateUnitButtonUI() {
        if ("lbs".equals(currentUnit)) {
            // Lbs active, kg inactive
            unitLbs.setBackgroundResource(R.drawable.bg_unit_toggle_active);
            unitLbs.setTextColor(ContextCompat.getColor(this, R.color.text_on_primary));
            unitKg.setBackgroundResource(R.drawable.bg_unit_toggle_inactive);
            unitKg.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            // Kg active, lbs inactive
            unitKg.setBackgroundResource(R.drawable.bg_unit_toggle_active);
            unitKg.setTextColor(ContextCompat.getColor(this, R.color.text_on_primary));
            unitLbs.setBackgroundResource(R.drawable.bg_unit_toggle_inactive);
            unitLbs.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    // =============================================================================================
    // SMS PERMISSION METHODS (Phase 7.4 - Commit 18)
    // =============================================================================================

    /**
     * Setup permission launcher for SMS and notifications.
     * Must be called BEFORE setContentView() in onCreate().
     */
    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean smsGranted = result.get(Manifest.permission.SEND_SMS);
                    Boolean notifGranted = result.get(Manifest.permission.POST_NOTIFICATIONS);

                    // Check if both permissions granted (POST_NOTIFICATIONS not required on Android < 13)
                    boolean allGranted = (smsGranted != null && smsGranted) &&
                            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                    (notifGranted != null && notifGranted));

                    if (allGranted) {
                        onPermissionsGranted();
                    } else {
                        onPermissionsDenied();
                    }
                }
        );
    }

    /**
     * Check current SMS permission status and update UI.
     */
    private void checkPermissions() {
        boolean hasSms = smsManager.hasSmsSendPermission();
        boolean hasNotif = smsManager.hasPostNotificationsPermission();

        if (hasSms && hasNotif) {
            updatePermissionUI("granted");
        } else {
            updatePermissionUI("required");
        }
    }

    /**
     * Update permission UI based on status.
     *
     * @param status "granted", "required", or "denied"
     */
    private void updatePermissionUI(String status) {
        if (permissionStatusBadge == null || grantPermissionButton == null || switchEnableSms == null) {
            Log.d(TAG, "updatePermissionUI: UI elements not initialized (stub)");
            return;
        }

        if ("granted".equals(status)) {
            permissionStatusBadge.setText(R.string.permission_granted);
            permissionStatusBadge.setBackgroundResource(R.drawable.bg_permission_granted);
            grantPermissionButton.setVisibility(android.view.View.GONE);

            // Enable SMS toggles
            switchEnableSms.setEnabled(true);
            updateSmsTogglesEnabled(switchEnableSms.isChecked());

        } else {
            permissionStatusBadge.setText(R.string.permission_required);
            permissionStatusBadge.setBackgroundResource(R.drawable.bg_permission_required);
            grantPermissionButton.setVisibility(android.view.View.VISIBLE);

            // Disable SMS toggles
            switchEnableSms.setEnabled(false);
            updateSmsTogglesEnabled(false);
        }
    }

    /**
     * Enable/disable SMS child toggles based on master toggle state.
     */
    private void updateSmsTogglesEnabled(boolean enabled) {
        if (switchGoalAlerts != null) {
            switchGoalAlerts.setEnabled(enabled);
        }
        if (switchMilestoneAlerts != null) {
            switchMilestoneAlerts.setEnabled(enabled);
        }
        if (switchDailyReminders != null) {
            switchDailyReminders.setEnabled(enabled);
        }
    }

    /**
     * Request SMS and notification permissions.
     */
    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.SEND_SMS);

        // Android 13+ requires POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        Log.d(TAG, "requestPermissions: Requesting " + permissions.size() + " permissions");
        permissionLauncher.launch(permissions.toArray(new String[0]));
    }

    /**
     * Callback when permissions are granted.
     */
    private void onPermissionsGranted() {
        Log.i(TAG, "onPermissionsGranted: SMS permissions granted");
        updatePermissionUI("granted");
        Toast.makeText(this, "SMS permissions granted", Toast.LENGTH_SHORT).show();
    }

    /**
     * Callback when permissions are denied.
     */
    private void onPermissionsDenied() {
        Log.w(TAG, "onPermissionsDenied: SMS permissions denied");
        updatePermissionUI("required");
        Toast.makeText(this,
                "SMS permissions required for notifications",
                Toast.LENGTH_LONG).show();
    }

    // =============================================================================================
    // PHONE NUMBER METHODS (Phase 7.4 - Commit 19)
    // =============================================================================================

    /**
     * Load user's phone number from database.
     */
    private void loadPhoneNumber() {
        if (phoneNumberInput == null) {
            Log.d(TAG, "loadPhoneNumber: phoneNumberInput not initialized (stub)");
            return;
        }

        long userId = SessionManager.getInstance(this).getCurrentUserId();
        User user = userDAO.getUserById(userId);

        if (user != null && user.getPhoneNumber() != null) {
            // Display phone (strip +1 for US display)
            String displayPhone = user.getPhoneNumber().replace("+1", "");
            phoneNumberInput.setText(displayPhone);
        }
    }

    /**
     * Handle phone number save (triggered by keyboard done/enter).
     */
    private void handleSavePhone() {
        String phoneInput = phoneNumberInput.getText().toString().trim();

        // Validate
        String error = ValidationUtils.getPhoneValidationError(phoneInput);
        if (error != null) {
            int errorResId = getResources().getIdentifier(error, "string", getPackageName());
            phoneNumberInput.setError(getString(errorResId));
            return;
        }

        // Format to E.164
        String e164Phone = ValidationUtils.formatPhoneE164(phoneInput);
        if (e164Phone == null) {
            phoneNumberInput.setError(getString(R.string.error_phone_invalid));
            return;
        }

        // Save to database
        long userId = SessionManager.getInstance(this).getCurrentUserId();
        boolean success = userDAO.updatePhoneNumber(userId, e164Phone);

        if (success) {
            Toast.makeText(this, "Phone number saved", Toast.LENGTH_SHORT).show();
            phoneNumberInput.setError(null);
            phoneNumberInput.clearFocus();
            Log.i(TAG, "handleSavePhone: Saved phone number for user " + userId);
        } else {
            Toast.makeText(this, "Failed to save phone number", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "handleSavePhone: Failed to save phone number");
        }
    }

    // =============================================================================================
    // SMS PREFERENCE METHODS (Phase 7.4 - Commit 20)
    // =============================================================================================

    /**
     * Load SMS preferences from database.
     */
    private void loadSmsPreferences() {
        if (switchEnableSms == null || switchGoalAlerts == null ||
                switchMilestoneAlerts == null || switchDailyReminders == null) {
            Log.d(TAG, "loadSmsPreferences: SMS toggles not initialized (stub)");
            return;
        }

        long userId = SessionManager.getInstance(this).getCurrentUserId();

        String smsEnabled = userPreferenceDAO.getPreference(userId,
                SMSNotificationManager.KEY_SMS_ENABLED, "false");
        switchEnableSms.setChecked("true".equals(smsEnabled));

        String goalAlerts = userPreferenceDAO.getPreference(userId,
                SMSNotificationManager.KEY_GOAL_ALERTS, "true");
        switchGoalAlerts.setChecked("true".equals(goalAlerts));

        String milestoneAlerts = userPreferenceDAO.getPreference(userId,
                SMSNotificationManager.KEY_MILESTONE_ALERTS, "true");
        switchMilestoneAlerts.setChecked("true".equals(milestoneAlerts));

        String reminderEnabled = userPreferenceDAO.getPreference(userId,
                SMSNotificationManager.KEY_REMINDER_ENABLED, "false");
        switchDailyReminders.setChecked("true".equals(reminderEnabled));

        // Update child toggle enabled state based on master
        updateSmsTogglesEnabled(switchEnableSms.isChecked());
    }

    /**
     * Handle master SMS toggle (enable/disable all SMS notifications).
     */
    private void handleMasterToggle(boolean isChecked) {
        long userId = SessionManager.getInstance(this).getCurrentUserId();
        userPreferenceDAO.setPreference(userId,
                SMSNotificationManager.KEY_SMS_ENABLED,
                isChecked ? "true" : "false");

        // Enable/disable child toggles
        updateSmsTogglesEnabled(isChecked);

        Toast.makeText(this,
                "SMS notifications " + (isChecked ? "enabled" : "disabled"),
                Toast.LENGTH_SHORT).show();

        Log.d(TAG, "handleMasterToggle: SMS notifications " + (isChecked ? "enabled" : "disabled"));
    }

    /**
     * Handle goal alerts toggle.
     */
    private void handleGoalAlertsToggle(boolean isChecked) {
        long userId = SessionManager.getInstance(this).getCurrentUserId();
        userPreferenceDAO.setPreference(userId,
                SMSNotificationManager.KEY_GOAL_ALERTS,
                isChecked ? "true" : "false");

        Log.d(TAG, "handleGoalAlertsToggle: Goal alerts " + (isChecked ? "enabled" : "disabled"));
    }

    /**
     * Handle milestone alerts toggle.
     */
    private void handleMilestoneAlertsToggle(boolean isChecked) {
        long userId = SessionManager.getInstance(this).getCurrentUserId();
        userPreferenceDAO.setPreference(userId,
                SMSNotificationManager.KEY_MILESTONE_ALERTS,
                isChecked ? "true" : "false");

        Log.d(TAG, "handleMilestoneAlertsToggle: Milestone alerts " + (isChecked ? "enabled" : "disabled"));
    }

    /**
     * Handle daily reminder toggle.
     */
    private void handleReminderToggle(boolean isChecked) {
        long userId = SessionManager.getInstance(this).getCurrentUserId();
        userPreferenceDAO.setPreference(userId,
                SMSNotificationManager.KEY_REMINDER_ENABLED,
                isChecked ? "true" : "false");

        // Schedule or cancel WorkManager daily reminder (Phase 7.6 - Commit 28)
        if (isChecked) {
            scheduleDailyReminder();
        } else {
            WorkManager.getInstance(this).cancelUniqueWork("daily_reminder");
            Log.d(TAG, "handleReminderToggle: Daily reminder work cancelled");
        }

        Toast.makeText(this,
                "Daily reminders " + (isChecked ? "enabled" : "disabled"),
                Toast.LENGTH_SHORT).show();

        Log.d(TAG, "handleReminderToggle: Daily reminders " + (isChecked ? "enabled" : "disabled"));
    }

    /**
     * Schedule daily reminder WorkManager periodic work.
     * Runs every 24 hours at 9:00 AM.
     *
     * Phase 7.6 - Commit 28: Daily Reminder Scheduling
     * Fixed: Pass userId via WorkManager Data to avoid thread safety issues
     */
    private void scheduleDailyReminder() {
        // Cancel existing work first
        WorkManager.getInstance(this).cancelUniqueWork("daily_reminder");

        // Get current user ID from SessionManager (safe on UI thread)
        long userId = SessionManager.getInstance(this).getCurrentUserId();

        // Pass userId to worker via input data (thread-safe)
        androidx.work.Data inputData = new androidx.work.Data.Builder()
                .putLong("USER_ID", userId)
                .build();

        // Create constraints (requires battery not low)
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        // Calculate initial delay to next 9:00 AM
        long initialDelayMillis = calculateInitialDelay();

        // Create periodic work request (24 hours interval, 1 hour flex)
        PeriodicWorkRequest reminderWork = new PeriodicWorkRequest.Builder(
                DailyReminderWorker.class,
                24, TimeUnit.HOURS,
                1, TimeUnit.HOURS  // Flex interval allows execution within 1 hour window
        )
        .setConstraints(constraints)
        .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
        .setInputData(inputData)
        .build();

        // Enqueue work
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_reminder",
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderWork
        );

        Log.i(TAG, "scheduleDailyReminder: Daily reminder scheduled for user " + userId +
                " with " + initialDelayMillis / 1000 / 60 + " minutes initial delay");
    }

    /**
     * Calculate initial delay to next 9:00 AM.
     * If current time is after 9:00 AM today, schedules for 9:00 AM tomorrow.
     *
     * @return delay in milliseconds
     */
    private long calculateInitialDelay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReminder = now.withHour(9).withMinute(0).withSecond(0).withNano(0);

        // If we've passed 9:00 AM today, schedule for tomorrow
        if (now.isAfter(nextReminder)) {
            nextReminder = nextReminder.plusDays(1);
        }

        long delayMillis = Duration.between(now, nextReminder).toMillis();
        Log.d(TAG, "calculateInitialDelay: Next reminder at " + nextReminder +
                " (delay: " + delayMillis / 1000 / 60 + " minutes)");

        return delayMillis;
    }

    // =============================================================================================
    // TEST MESSAGE METHOD (Phase 7.4 - Commit 21)
    // =============================================================================================

    /**
     * Handle send test message button click.
     *
     * **Emulator Support (NEW):**
     * Detects if running on emulator and logs message to Logcat instead of
     * sending real SMS (emulator SMS sending doesn't work reliably).
     *
     * **Security:**
     * Phone numbers are masked in logs (show only last 4 digits) to prevent
     * PII exposure in production logs.
     *
     * **Flow:**
     * 1. Check if SMS can be sent (permissions + phone number configured)
     * 2. Detect emulator vs real device
     * 3. Emulator: Log test message to Logcat with masked phone
     * 4. Real device: Send actual SMS via SmsManager
     */
    private void handleSendTestMessage() {
        long userId = SessionManager.getInstance(this).getCurrentUserId();

        // Check if can send SMS
        if (!smsManager.canSendSms(userId)) {
            Toast.makeText(this,
                    "Cannot send SMS. Check permissions and phone number.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Get user phone
        User user = userDAO.getUserById(userId);
        if (user == null || user.getPhoneNumber() == null) {
            Toast.makeText(this, "No phone number configured", Toast.LENGTH_SHORT).show();
            return;
        }

        String testMessage = getString(R.string.sms_test_message);

        // EMULATOR DETECTION: Log to Logcat instead of sending SMS
        if (ValidationUtils.isRunningOnEmulator()) {
            // Mask phone number for secure logging
            String maskedPhone = ValidationUtils.maskPhoneNumber(user.getPhoneNumber());

            // Log test message with clear visual separation
            Log.i(TAG, "======================================");
            Log.i(TAG, "TEST SMS (EMULATOR MODE)");
            Log.i(TAG, "To: " + maskedPhone);
            Log.i(TAG, "Message: " + testMessage);
            Log.i(TAG, "======================================");

            Toast.makeText(this,
                    "Test message logged to Logcat (emulator mode)",
                    Toast.LENGTH_LONG).show();

            Log.i(TAG, "handleSendTestMessage: Logged test SMS to Logcat (emulator mode)");
            return;
        }

        // REAL DEVICE: Send actual SMS
        try {
            android.telephony.SmsManager smsManagerSystem = android.telephony.SmsManager.getDefault();
            smsManagerSystem.sendTextMessage(user.getPhoneNumber(), null, testMessage, null, null);

            Toast.makeText(this, "Test message sent!", Toast.LENGTH_SHORT).show();

            // Log with masked phone number for security
            String maskedPhone = ValidationUtils.maskPhoneNumber(user.getPhoneNumber());
            Log.i(TAG, "handleSendTestMessage: Test SMS sent to " + maskedPhone);

        } catch (SecurityException e) {
            Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "handleSendTestMessage: SecurityException", e);

        } catch (Exception e) {
            Toast.makeText(this, "Failed to send test message", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "handleSendTestMessage: Exception", e);
        }
    }
}
