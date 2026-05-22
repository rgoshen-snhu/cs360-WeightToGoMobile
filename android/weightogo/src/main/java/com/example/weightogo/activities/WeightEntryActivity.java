package com.example.weightogo.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.weightogo.R;
import com.example.weightogo.database.AchievementDAO;
import com.example.weightogo.database.GoalWeightDAO;
import com.example.weightogo.database.UserDAO;
import com.example.weightogo.database.UserPreferenceDAO;
import com.example.weightogo.database.WeighToGoDBHelper;
import com.example.weightogo.database.WeightEntryDAO;
import com.example.weightogo.models.Achievement;
import com.example.weightogo.models.WeightEntry;
import com.example.weightogo.utils.AchievementManager;
import com.example.weightogo.utils.DateUtils;
import com.example.weightogo.utils.SMSNotificationManager;
import com.example.weightogo.utils.SessionManager;
import com.example.weightogo.utils.WeightUtils;

import java.util.List;
import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WeightEntryActivity handles adding and editing weight entries.
 *
 * Features:
 * - Number pad input (0-9, decimal, backspace)
 * - Quick adjust buttons (-1, -0.5, +0.5, +1)
 * - Unit toggle (lbs ↔ kg)
 * - Date navigation (previous/next with today detection)
 * - Validation (50-700 lbs or 22.7-317.5 kg)
 * - Duplicate entry detection (one entry per user per date)
 *
 * Navigation:
 * - From MainActivity FAB (add mode)
 * - From MainActivity edit button (edit mode)
 * - Returns RESULT_OK on successful save/update
 */
public class WeightEntryActivity extends AppCompatActivity {

    private static final String TAG = "WeightEntryActivity";
    private static final int MAX_DIGITS = 5;
    private static final String DECIMAL_POINT = ".";

    // =============================================================================================
    // INTENT EXTRAS CONSTANTS
    // =============================================================================================

    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_IS_EDIT_MODE = "isEditMode";
    public static final String EXTRA_WEIGHT_ID = "weightId";
    public static final String EXTRA_WEIGHT_VALUE = "weightValue";
    public static final String EXTRA_WEIGHT_DATE = "weightDate";
    public static final String EXTRA_WEIGHT_UNIT = "weightUnit";
    public static final String EXTRA_NOTES = "notes";

    // =============================================================================================
    // UI COMPONENTS
    // =============================================================================================

    // Navigation
    private ImageButton backButton;
    private ImageButton prevDateButton;
    private ImageButton nextDateButton;

    // Date Display
    private TextView dayNumber;
    private TextView fullDate;
    private TextView todayBadge;

    // Weight Display
    private TextView weightValue;
    private TextView weightUnit;

    // Number Pad (12 buttons: 0-9, decimal, backspace)
    private TextView numpad0;
    private TextView numpad1;
    private TextView numpad2;
    private TextView numpad3;
    private TextView numpad4;
    private TextView numpad5;
    private TextView numpad6;
    private TextView numpad7;
    private TextView numpad8;
    private TextView numpad9;
    private TextView numpadDecimal;
    private TextView numpadBackspace;

    // Quick Adjust Buttons
    private TextView adjustMinusOne;
    private TextView adjustMinusHalf;
    private TextView adjustPlusHalf;
    private TextView adjustPlusOne;

    // Save Button
    private MaterialButton saveButton;

    // Previous Entry Hint
    private TextView lastEntryValue;
    private TextView lastEntryDate;

    // =============================================================================================
    // DATA LAYER
    // =============================================================================================

    private WeighToGoDBHelper dbHelper;
    private WeightEntryDAO weightEntryDAO;
    private UserPreferenceDAO userPreferenceDAO;
    private AchievementManager achievementManager;
    private SMSNotificationManager smsManager;

    // =============================================================================================
    // STATE
    // =============================================================================================

    private long userId;
    private boolean isEditMode;
    private long editWeightId;
    private WeightEntry currentEntry;  // Cached entry for edit mode
    private LocalDate currentDate;
    private String currentUnit = "lbs";
    private StringBuilder weightInput;

    // =============================================================================================
    // LIFECYCLE METHODS
    // =============================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: WeightEntryActivity started");

        setContentView(R.layout.activity_weight_entry);

        // Initialize weight input to "0.0" (allows saving immediately in add mode)
        weightInput = new StringBuilder("0.0");

        // Get intent extras
        getIntentExtras();

        // Initialize data layer
        initDataLayer();

        // Load user preferences (must be after initDataLayer, before initViews)
        loadUserPreferences();

        // Initialize UI
        initViews();
        setupClickListeners();

        // Load data if edit mode
        if (isEditMode) {
            loadExistingEntry();
        } else {
            loadPreviousEntry();
            // In add mode, ensure display shows 0.0 (overrides XML default of 172.0)
            updateWeightDisplay();
        }

        // Initialize date to today
        if (currentDate == null) {
            currentDate = LocalDate.now();
        }
        updateDateDisplay(currentDate);
    }

    // =============================================================================================
    // INITIALIZATION
    // =============================================================================================

    /**
     * Extract intent extras for userId, edit mode, and entry data.
     */
    private void getIntentExtras() {
        userId = getIntent().getLongExtra(EXTRA_USER_ID, -1);
        isEditMode = getIntent().getBooleanExtra(EXTRA_IS_EDIT_MODE, false);

        if (isEditMode) {
            editWeightId = getIntent().getLongExtra(EXTRA_WEIGHT_ID, -1);
            double weightValue = getIntent().getDoubleExtra(EXTRA_WEIGHT_VALUE, 0.0);
            String dateStr = getIntent().getStringExtra(EXTRA_WEIGHT_DATE);
            currentUnit = getIntent().getStringExtra(EXTRA_WEIGHT_UNIT);

            if (dateStr != null) {
                currentDate = LocalDate.parse(dateStr);
            }

            if (weightValue > 0) {
                weightInput = new StringBuilder(WeightUtils.formatWeight(weightValue));
            }
        }

        Log.d(TAG, "getIntentExtras: userId=" + userId + ", isEditMode=" + isEditMode);
    }

    /**
     * Initialize database helper and DAOs.
     * Only initializes if not already set (allows test injection).
     */
    private void initDataLayer() {
        if (dbHelper == null) {
            dbHelper = WeighToGoDBHelper.getInstance(this);
        }
        if (weightEntryDAO == null) {
            weightEntryDAO = new WeightEntryDAO(dbHelper);
        }
        if (userPreferenceDAO == null) {
            userPreferenceDAO = new UserPreferenceDAO(dbHelper);
        }

        // Initialize shared DAOs (avoid duplicate instantiation)
        AchievementDAO achievementDAO = new AchievementDAO(dbHelper);
        GoalWeightDAO goalWeightDAO = new GoalWeightDAO(dbHelper);
        UserDAO userDAO = new UserDAO(dbHelper);

        // Initialize achievement system (Phase 7.5)
        if (achievementManager == null) {
            achievementManager = new AchievementManager(achievementDAO, goalWeightDAO, weightEntryDAO);
        }

        // Initialize SMS notification manager (Phase 7.5)
        if (smsManager == null) {
            smsManager = SMSNotificationManager.getInstance(this, userDAO, userPreferenceDAO, achievementDAO);
        }

        Log.d(TAG, "initDataLayer: Data layer initialized with achievement and SMS managers");
    }

    // =============================================================================================
    // TESTING SETTERS (Package-Private)
    // =============================================================================================

    /**
     * Set WeightEntryDAO instance (for testing only).
     *
     * @param weightEntryDAO the WeightEntryDAO instance to use
     * @throws IllegalArgumentException if weightEntryDAO is null
     */
    @VisibleForTesting
    void setWeightEntryDAO(WeightEntryDAO weightEntryDAO) {
        if (weightEntryDAO == null) {
            throw new IllegalArgumentException("WeightEntryDAO cannot be null");
        }
        this.weightEntryDAO = weightEntryDAO;
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
     * Set AchievementManager instance (for testing only).
     *
     * @param achievementManager the AchievementManager instance to use
     * @throws IllegalArgumentException if achievementManager is null
     */
    @VisibleForTesting
    void setAchievementManager(AchievementManager achievementManager) {
        if (achievementManager == null) {
            throw new IllegalArgumentException("AchievementManager cannot be null");
        }
        this.achievementManager = achievementManager;
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
    // USER PREFERENCES
    // =============================================================================================

    /**
     * Load user preferences from database.
     * Only loads in add mode; edit mode uses unit from existing entry.
     */
    private void loadUserPreferences() {
        if (isEditMode) {
            Log.d(TAG, "loadUserPreferences: Skipping (edit mode uses entry's unit)");
            return; // In edit mode, unit comes from existing entry via intent
        }

        long currentUserId = SessionManager.getInstance(this).getCurrentUserId();
        currentUnit = userPreferenceDAO.getWeightUnit(currentUserId);
        Log.d(TAG, "loadUserPreferences: Loaded weight unit preference: " + currentUnit);
    }

    /**
     * Initialize all UI components via findViewById.
     */
    private void initViews() {
        // Navigation
        backButton = findViewById(R.id.backButton);
        prevDateButton = findViewById(R.id.prevDateButton);
        nextDateButton = findViewById(R.id.nextDateButton);

        // Date Display
        dayNumber = findViewById(R.id.dayNumber);
        fullDate = findViewById(R.id.fullDate);
        todayBadge = findViewById(R.id.todayBadge);

        // Weight Display
        weightValue = findViewById(R.id.weightValue);
        weightUnit = findViewById(R.id.weightUnit);

        // Number Pad
        numpad0 = findViewById(R.id.numpad0);
        numpad1 = findViewById(R.id.numpad1);
        numpad2 = findViewById(R.id.numpad2);
        numpad3 = findViewById(R.id.numpad3);
        numpad4 = findViewById(R.id.numpad4);
        numpad5 = findViewById(R.id.numpad5);
        numpad6 = findViewById(R.id.numpad6);
        numpad7 = findViewById(R.id.numpad7);
        numpad8 = findViewById(R.id.numpad8);
        numpad9 = findViewById(R.id.numpad9);
        numpadDecimal = findViewById(R.id.numpadDecimal);
        numpadBackspace = findViewById(R.id.numpadBackspace);

        // Quick Adjust
        adjustMinusOne = findViewById(R.id.adjustMinusOne);
        adjustMinusHalf = findViewById(R.id.adjustMinusHalf);
        adjustPlusHalf = findViewById(R.id.adjustPlusHalf);
        adjustPlusOne = findViewById(R.id.adjustPlusOne);

        // Save Button
        saveButton = findViewById(R.id.saveButton);

        // Previous Entry Hint
        lastEntryValue = findViewById(R.id.lastEntryValue);
        lastEntryDate = findViewById(R.id.lastEntryDate);

        Log.d(TAG, "initViews: All views initialized");
    }

    /**
     * Setup click listeners for all buttons.
     */
    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Date navigation
        setupDateNavigationListeners();

        // Number pad
        setupNumberPadListeners();

        // Quick adjust
        setupQuickAdjustListeners();

        // Save button
        setupSaveButton();

        Log.d(TAG, "setupClickListeners: Click listeners configured");
    }

    // =============================================================================================
    // DATE NAVIGATION (Commit 5)
    // =============================================================================================

    /**
     * Setup date navigation button click listeners (previous/next).
     */
    private void setupDateNavigationListeners() {
        prevDateButton.setOnClickListener(v -> navigateToPreviousDay());
        nextDateButton.setOnClickListener(v -> navigateToNextDay());

        Log.d(TAG, "setupDateNavigationListeners: Date navigation configured");
    }

    /**
     * Navigate to previous day.
     */
    private void navigateToPreviousDay() {
        if (currentDate == null) {
            currentDate = LocalDate.now();
            Log.w(TAG, "navigateToPreviousDay: currentDate was null, initialized to today");
        }

        currentDate = currentDate.minusDays(1);
        updateDateDisplay(currentDate);
        Log.d(TAG, "navigateToPreviousDay: Moved to " + currentDate);
    }

    /**
     * Navigate to next day.
     * Prevents moving beyond today's date.
     */
    private void navigateToNextDay() {
        if (currentDate == null) {
            currentDate = LocalDate.now();
            Log.w(TAG, "navigateToNextDay: currentDate was null, initialized to today");
            updateDateDisplay(currentDate);
            return;
        }

        LocalDate tomorrow = currentDate.plusDays(1);
        LocalDate today = LocalDate.now();

        if (!tomorrow.isAfter(today)) {
            currentDate = tomorrow;
            updateDateDisplay(currentDate);
            Log.d(TAG, "navigateToNextDay: Moved to " + currentDate);
        } else {
            Log.d(TAG, "navigateToNextDay: Cannot move to future date");
        }
    }

    // =============================================================================================
    // NUMBER PAD INPUT (Commit 2)
    // =============================================================================================

    /**
     * Setup number pad button click listeners (0-9, decimal, backspace).
     */
    private void setupNumberPadListeners() {
        // Array of number pad button IDs
        int[] numpadIds = {
                R.id.numpad0, R.id.numpad1, R.id.numpad2, R.id.numpad3, R.id.numpad4,
                R.id.numpad5, R.id.numpad6, R.id.numpad7, R.id.numpad8, R.id.numpad9
        };

        // Wire up digit buttons (0-9)
        for (int i = 0; i < numpadIds.length; i++) {
            final int digit = i;
            findViewById(numpadIds[i]).setOnClickListener(v -> handleNumberInput(String.valueOf(digit)));
        }

        // Wire up decimal and backspace
        numpadDecimal.setOnClickListener(v -> handleDecimalPoint());
        numpadBackspace.setOnClickListener(v -> handleBackspace());

        Log.d(TAG, "setupNumberPadListeners: Number pad configured");
    }

    /**
     * Handle digit button press (0-9).
     * Prevents leading zeros (except "0.") and enforces max digit limit.
     * Replaces "0" or "0.0" when user starts typing a new number.
     *
     * @param digit the digit to append (0-9)
     */
    private void handleNumberInput(String digit) {
        String current = weightInput.toString();

        // Replace "0" or "0.0" when user types a non-zero digit (start fresh)
        if ((current.equals("0") || current.equals("0.0")) && !digit.equals("0")) {
            weightInput = new StringBuilder(digit);
        } else if ((current.equals("0") || current.equals("0.0")) && digit.equals("0")) {
            return; // Don't allow multiple leading zeros
        } else {
            // Count digits only (excluding decimal point)
            long digitCount = current.chars().filter(c -> c != '.').count();
            if (digitCount < MAX_DIGITS) {
                weightInput.append(digit);
            }
        }

        updateWeightDisplay();
        Log.d(TAG, "handleNumberInput: Input = " + weightInput.toString());
    }

    /**
     * Handle decimal point button press.
     * Only adds decimal if not already present and input is not empty.
     */
    private void handleDecimalPoint() {
        String current = weightInput.toString();

        // Only add decimal if not already present and not empty
        if (!current.contains(DECIMAL_POINT) && current.length() > 0) {
            weightInput.append(DECIMAL_POINT);
            updateWeightDisplay();
            Log.d(TAG, "handleDecimalPoint: Input = " + weightInput.toString());
        }
    }

    /**
     * Handle backspace button press.
     * Removes the last character from input.
     */
    private void handleBackspace() {
        if (weightInput.length() > 0) {
            weightInput.deleteCharAt(weightInput.length() - 1);
            updateWeightDisplay();
            Log.d(TAG, "handleBackspace: Input = " + weightInput.toString());
        }
    }

    // =============================================================================================
    // QUICK ADJUST BUTTONS (Commit 3)
    // =============================================================================================

    /**
     * Setup quick adjust button click listeners (-1, -0.5, +0.5, +1).
     */
    private void setupQuickAdjustListeners() {
        adjustMinusOne.setOnClickListener(v -> adjustWeight(-1.0));
        adjustMinusHalf.setOnClickListener(v -> adjustWeight(-0.5));
        adjustPlusHalf.setOnClickListener(v -> adjustWeight(0.5));
        adjustPlusOne.setOnClickListener(v -> adjustWeight(1.0));

        Log.d(TAG, "setupQuickAdjustListeners: Quick adjust buttons configured");
    }

    /**
     * Adjust weight value by specified amount.
     * Allows building up from 0 (no min validation), but enforces max limit.
     * Final validation happens in handleSave().
     *
     * @param amount the amount to add/subtract (positive or negative)
     */
    private void adjustWeight(double amount) {
        String current = weightInput.toString();
        double currentValue;

        try {
            currentValue = current.isEmpty() ? 0.0 : Double.parseDouble(current);
        } catch (NumberFormatException e) {
            Log.w(TAG, "adjustWeight: Invalid number format: " + current);
            Toast.makeText(this, "Invalid weight format", Toast.LENGTH_SHORT).show();
            return;
        }

        double newValue = currentValue + amount;

        // Only validate max (allow building up from 0 with quick adjust)
        double max = currentUnit.equals("lbs") ? WeightUtils.MAX_WEIGHT_LBS : WeightUtils.MAX_WEIGHT_KG;

        if (newValue >= WeightUtils.MIN_WEIGHT && newValue <= max) {
            weightInput = new StringBuilder(WeightUtils.formatWeight(newValue));
            updateWeightDisplay();
            Log.d(TAG, "adjustWeight: Adjusted by " + amount + " to " + weightInput.toString());
        } else if (newValue < 0.0) {
            // Don't allow negative weights
            weightInput = new StringBuilder("0.0");
            updateWeightDisplay();
            Log.w(TAG, "adjustWeight: Cannot go below 0.0, reset to 0.0");
        } else {
            // Exceeds max
            Log.w(TAG, "adjustWeight: Value " + newValue + " exceeds max " + max);
            Toast.makeText(this, "Maximum weight is " + (int)max + " " + currentUnit, Toast.LENGTH_SHORT).show();
        }
    }

    // =============================================================================================
    // DATA LOADING
    // =============================================================================================

    /**
     * Load existing entry data for edit mode.
     * Caches the entry to avoid redundant database queries.
     */
    private void loadExistingEntry() {
        currentEntry = weightEntryDAO.getWeightEntryById(editWeightId);

        if (currentEntry != null) {
            weightInput = new StringBuilder(WeightUtils.formatWeight(currentEntry.getWeightValue()));
            currentUnit = currentEntry.getWeightUnit();
            currentDate = currentEntry.getWeightDate();

            updateWeightDisplay();
            Log.d(TAG, "loadExistingEntry: Loaded and cached entry " + editWeightId);
        } else {
            Log.w(TAG, "loadExistingEntry: Entry not found for weightId=" + editWeightId);
        }
    }

    /**
     * Load previous entry to show as hint in add mode.
     */
    private void loadPreviousEntry() {
        WeightEntry lastEntry = weightEntryDAO.getLatestWeightEntry(userId);

        if (lastEntry != null) {
            String value = WeightUtils.formatWeightWithUnit(
                    lastEntry.getWeightValue(), lastEntry.getWeightUnit());
            lastEntryValue.setText(value);

            // Format date using DateUtils
            String date = "on " + DateUtils.formatDateShort(lastEntry.getWeightDate());
            lastEntryDate.setText(date);

            Log.d(TAG, "loadPreviousEntry: Loaded previous entry");
        } else {
            lastEntryValue.setVisibility(View.GONE);
            lastEntryDate.setVisibility(View.GONE);
            Log.d(TAG, "loadPreviousEntry: No previous entry found");
        }
    }

    /**
     * Update date display components.
     *
     * @param date the date to display
     */
    private void updateDateDisplay(LocalDate date) {
        currentDate = date;

        // Update day number
        dayNumber.setText(String.valueOf(date.getDayOfMonth()));

        // Update full date using DateUtils
        fullDate.setText(DateUtils.formatDateFull(date));

        // Update "Today" badge visibility
        boolean isToday = DateUtils.isToday(date);
        todayBadge.setVisibility(isToday ? View.VISIBLE : View.GONE);

        // Disable next button if today
        nextDateButton.setEnabled(!isToday);
        nextDateButton.setAlpha(isToday ? 0.3f : 1.0f);

        Log.d(TAG, "updateDateDisplay: Date set to " + date);
    }

    /**
     * Update weight value display.
     */
    private void updateWeightDisplay() {
        String value = weightInput.toString();
        if (value.isEmpty()) {
            value = "0.0";
        }
        weightValue.setText(value);
        weightUnit.setText(currentUnit);
    }

    // =============================================================================================
    // SAVE FUNCTIONALITY (Commit 6)
    // =============================================================================================

    /**
     * Setup save button click listener.
     */
    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> handleSave());
        Log.d(TAG, "setupSaveButton: Save button configured");
    }

    /**
     * Handle save button click.
     * Validates input and calls createNewEntry or updateExistingEntry.
     * Allows 0 as a valid weight (can be deleted later via edit).
     */
    private void handleSave() {
        String weightStr = weightInput.toString();

        // Validate non-empty (allow "0" or "0.0" as valid)
        if (weightStr.isEmpty()) {
            Toast.makeText(this, "Please enter a weight value", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "handleSave: Empty weight value");
            return;
        }

        double weight;

        try {
            weight = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid weight format", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "handleSave: Invalid number format: " + weightStr);
            return;
        }

        // Validate range based on current unit (allow 0 for placeholder/deletion scenario)
        double min = WeightUtils.MIN_WEIGHT;
        double max = currentUnit.equals("lbs") ? WeightUtils.MAX_WEIGHT_LBS : WeightUtils.MAX_WEIGHT_KG;

        if (weight < min || weight > max) {
            String message = String.format("Weight must be between %s and %s",
                    WeightUtils.formatWeightWithUnit(min, currentUnit),
                    WeightUtils.formatWeightWithUnit(max, currentUnit));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.w(TAG, "handleSave: Weight out of range: " + weight);
            return;
        }

        // Call appropriate method based on mode
        if (isEditMode) {
            updateExistingEntry(weight);
        } else {
            createNewEntry(weight);
        }
    }

    /**
     * Create new weight entry in database.
     *
     * @param weight the weight value to save
     */
    private void createNewEntry(double weight) {
        WeightEntry entry = new WeightEntry();
        entry.setUserId(userId);
        entry.setWeightValue(weight);
        entry.setWeightUnit(currentUnit);
        entry.setWeightDate(currentDate);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        entry.setDeleted(false);

        long weightId = weightEntryDAO.insertWeightEntry(entry);

        if (weightId > 0) {
            Log.i(TAG, "createNewEntry: Successfully created weight entry: " + weightId);

            // Check for achievements (Phase 7.5)
            List<Achievement> newAchievements = achievementManager.checkAchievements(userId, weight);

            // Send SMS for each new achievement
            for (Achievement achievement : newAchievements) {
                boolean sent = smsManager.sendAchievementSms(achievement);
                if (sent) {
                    Log.i(TAG, "createNewEntry: Achievement SMS sent: " + achievement.getAchievementType());
                }
            }

            Toast.makeText(this, "Entry saved successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            // Likely duplicate entry for this date
            String message = String.format("You already have an entry for %s",
                    DateUtils.formatDateFull(currentDate));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.w(TAG, "createNewEntry: Duplicate entry detected for date: " + currentDate);
        }
    }

    /**
     * Update existing weight entry in database.
     * Uses cached currentEntry to avoid redundant database query.
     *
     * @param weight the weight value to save
     */
    private void updateExistingEntry(double weight) {
        if (currentEntry == null) {
            Toast.makeText(this, "Entry not found", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "updateExistingEntry: Cached entry is null for weightId=" + editWeightId);
            return;
        }

        currentEntry.setWeightValue(weight);
        currentEntry.setWeightUnit(currentUnit);
        currentEntry.setWeightDate(currentDate);
        currentEntry.setUpdatedAt(LocalDateTime.now());

        int rowsUpdated = weightEntryDAO.updateWeightEntry(currentEntry);

        if (rowsUpdated == 1) {
            Log.i(TAG, "updateExistingEntry: Successfully updated weight entry: " + editWeightId);

            // Check for achievements (Phase 7.5)
            List<Achievement> newAchievements = achievementManager.checkAchievements(userId, weight);

            // Send SMS for each new achievement
            for (Achievement achievement : newAchievements) {
                boolean sent = smsManager.sendAchievementSms(achievement);
                if (sent) {
                    Log.i(TAG, "updateExistingEntry: Achievement SMS sent: " + achievement.getAchievementType());
                }
            }

            Toast.makeText(this, "Entry updated successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to update entry", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "updateExistingEntry: Update failed for weightId=" + editWeightId);
        }
    }
}
