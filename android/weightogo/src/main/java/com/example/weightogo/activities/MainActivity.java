package com.example.weightogo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weightogo.R;
import com.example.weightogo.adapters.WeightEntryAdapter;
import com.example.weightogo.database.GoalWeightDAO;
import com.example.weightogo.fragments.GoalDialogFragment;
import com.example.weightogo.database.UserDAO;
import com.example.weightogo.database.WeighToGoDBHelper;
import com.example.weightogo.database.WeightEntryDAO;
import com.example.weightogo.models.GoalWeight;
import com.example.weightogo.models.User;
import com.example.weightogo.models.WeightEntry;
import com.example.weightogo.utils.DateUtils;
import com.example.weightogo.utils.SessionManager;
import com.example.weightogo.utils.ValidationUtils;
import com.example.weightogo.utils.WeightUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Main dashboard activity for WeightOgO app.
 * Displays user greeting, progress card, quick stats, and recent weight entries.
 */
public class MainActivity extends AppCompatActivity
        implements WeightEntryAdapter.OnItemClickListener,
                   GoalDialogFragment.GoalDialogListener {

    // Request Codes
    private static final int REQUEST_CODE_WEIGHT_ENTRY = 1001;

    // UI Elements
    private TextView greetingText;
    private TextView userName;
    private ImageButton settingsButton;

    // Progress Card
    private MaterialCardView progressCard;
    private TextView currentWeightValue;
    private TextView currentWeightUnit;
    private TextView startWeightValue;
    private TextView startWeightUnit;
    private TextView goalWeightValue;
    private TextView goalWeightUnit;
    private View progressBarFill;
    private TextView progressPercentage;
    private ImageButton btnEditGoalFromCard;

    // Quick Stats
    private TextView totalLostValue;
    private TextView lbsToGoalValue;
    private TextView dayStreakValue;

    // Weight List
    private RecyclerView weightRecyclerView;
    private LinearLayout emptyStateContainer;
    private WeightEntryAdapter adapter;

    // Navigation
    private FloatingActionButton addEntryFab;
    private BottomNavigationView bottomNavigation;

    // Data Layer
    private WeighToGoDBHelper dbHelper;
    private UserDAO userDAO;
    private WeightEntryDAO weightEntryDAO;
    private GoalWeightDAO goalWeightDAO;
    private SessionManager sessionManager;

    // State
    private long currentUserId;
    private List<WeightEntry> weightEntries = new ArrayList<>();  // Initialized at declaration to prevent NPE
    private GoalWeight activeGoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check authentication first
        if (!checkAuthentication()) {
            return; // Activity will finish in checkAuthentication()
        }

        setContentView(R.layout.activity_main);

        // Initialize data layer
        initDataLayer();

        // Initialize UI
        initViews();
        setupRecyclerView();
        setupFAB();
        setupBottomNavigation();

        // Load data
        loadWeightEntries();
        updateProgressCard();
        calculateQuickStats();
        updateGreeting();
        updateUserName();

        // Check if we should show goal dialog (from GoalsActivity FAB)
        if (getIntent().getBooleanExtra("SHOW_GOAL_DIALOG", false)) {
            showSetGoalDialog();
        }
    }

    /**
     * Check if user is authenticated. Redirect to login if not.
     *
     * @return true if authenticated, false otherwise
     */
    private boolean checkAuthentication() {
        if (sessionManager == null) {
            sessionManager = SessionManager.getInstance(this);
        }

        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return false;
        }

        currentUserId = sessionManager.getCurrentUserId();
        return true;
    }

    /**
     * Initialize database and DAOs.
     * Only initializes if not already set (allows test injection).
     */
    private void initDataLayer() {
        if (dbHelper == null) {
            dbHelper = WeighToGoDBHelper.getInstance(this);
        }
        if (userDAO == null) {
            userDAO = new UserDAO(dbHelper);
        }
        if (weightEntryDAO == null) {
            weightEntryDAO = new WeightEntryDAO(dbHelper);
        }
        if (goalWeightDAO == null) {
            goalWeightDAO = new GoalWeightDAO(dbHelper);
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
     * Set GoalWeightDAO instance (for testing only).
     *
     * @param goalWeightDAO the GoalWeightDAO instance to use
     * @throws IllegalArgumentException if goalWeightDAO is null
     */
    @VisibleForTesting
    void setGoalWeightDAO(GoalWeightDAO goalWeightDAO) {
        if (goalWeightDAO == null) {
            throw new IllegalArgumentException("GoalWeightDAO cannot be null");
        }
        this.goalWeightDAO = goalWeightDAO;
    }

    /**
     * Set SessionManager instance (for testing only).
     *
     * @param sessionManager the SessionManager instance to use
     * @throws IllegalArgumentException if sessionManager is null
     */
    @VisibleForTesting
    void setSessionManager(SessionManager sessionManager) {
        if (sessionManager == null) {
            throw new IllegalArgumentException("SessionManager cannot be null");
        }
        this.sessionManager = sessionManager;
    }

    /**
     * Set DBHelper instance (for testing only).
     *
     * @param dbHelper the WeighToGoDBHelper instance to use
     * @throws IllegalArgumentException if dbHelper is null
     */
    @VisibleForTesting
    void setDbHelper(WeighToGoDBHelper dbHelper) {
        if (dbHelper == null) {
            throw new IllegalArgumentException("DBHelper cannot be null");
        }
        this.dbHelper = dbHelper;
    }

    // =============================================================================================
    // VIEW INITIALIZATION
    // =============================================================================================

    /**
     * Initialize all view references.
     */
    private void initViews() {
        // Header
        greetingText = findViewById(R.id.greetingText);
        userName = findViewById(R.id.userName);
        settingsButton = findViewById(R.id.settingsButton);

        // Progress Card
        progressCard = findViewById(R.id.progressCard);
        currentWeightValue = findViewById(R.id.currentWeightValue);
        currentWeightUnit = findViewById(R.id.currentWeightUnit);
        startWeightValue = findViewById(R.id.startWeightValue);
        startWeightUnit = findViewById(R.id.startWeightUnit);
        goalWeightValue = findViewById(R.id.goalWeightValue);
        goalWeightUnit = findViewById(R.id.goalWeightUnit);
        progressBarFill = findViewById(R.id.progressBarFill);
        progressPercentage = findViewById(R.id.progressPercentage);
        btnEditGoalFromCard = findViewById(R.id.btnEditGoalFromCard);

        // Quick Stats
        totalLostValue = findViewById(R.id.totalLostValue);
        lbsToGoalValue = findViewById(R.id.lbsToGoalValue);
        dayStreakValue = findViewById(R.id.dayStreakValue);

        // Weight List
        weightRecyclerView = findViewById(R.id.weightRecyclerView);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);

        // Navigation
        addEntryFab = findViewById(R.id.addEntryFab);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    /**
     * Setup RecyclerView with adapter and layout manager.
     */
    private void setupRecyclerView() {
        adapter = new WeightEntryAdapter(weightEntries, this);
        weightRecyclerView.setAdapter(adapter);
        weightRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Setup FAB and button click listeners.
     */
    private void setupFAB() {
        addEntryFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WeightEntryActivity.class);
            intent.putExtra(WeightEntryActivity.EXTRA_USER_ID, currentUserId);
            intent.putExtra(WeightEntryActivity.EXTRA_IS_EDIT_MODE, false);
            startActivityForResult(intent, REQUEST_CODE_WEIGHT_ENTRY);
        });

        btnEditGoalFromCard.setOnClickListener(v -> handleEditGoal());

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Setup bottom navigation item selection listener.
     */
    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Already on home, do nothing
                return true;
            } else if (itemId == R.id.nav_trends) {
                // Trends disabled - future enhancement (see TODO.md Phase 11)
                return false;
            } else if (itemId == R.id.nav_goals) {
                Intent intent = new Intent(this, GoalsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Profile disabled - future enhancement (see TODO.md Phase 12)
                return false;
            }

            return false;
        });
    }

    /**
     * Load weight entries from database and update UI.
     */
    private void loadWeightEntries() {
        weightEntries.clear();
        List<WeightEntry> entries = weightEntryDAO.getWeightEntriesForUser(currentUserId);
        weightEntries.addAll(entries);
        adapter.notifyDataSetChanged();

        showEmptyState(weightEntries.isEmpty());
    }

    /**
     * Update progress card with goal data.
     * Uses cached weightEntries to avoid redundant database query.
     */
    private void updateProgressCard() {
        activeGoal = goalWeightDAO.getActiveGoal(currentUserId);

        if (activeGoal == null) {
            progressCard.setVisibility(View.GONE);
            btnEditGoalFromCard.setVisibility(View.GONE);
            return;
        }

        progressCard.setVisibility(View.VISIBLE);
        btnEditGoalFromCard.setVisibility(View.VISIBLE);

        // Get goal unit
        String goalUnit = activeGoal.getGoalUnit();

        // Get current weight from most recent entry (use cached list)
        double current = activeGoal.getStartWeight();
        if (!weightEntries.isEmpty()) {
            WeightEntry latestEntry = weightEntries.get(0);
            current = latestEntry.getWeightValue();
            String entryUnit = latestEntry.getWeightUnit();

            // Convert if units don't match
            if (!entryUnit.equals(goalUnit)) {
                if ("kg".equals(goalUnit)) {
                    current = WeightUtils.convertLbsToKg(current);
                } else {
                    current = WeightUtils.convertKgToLbs(current);
                }
            }
        }

        // Display weight values
        startWeightValue.setText(WeightUtils.formatWeight(activeGoal.getStartWeight()));
        startWeightUnit.setText(goalUnit);
        currentWeightValue.setText(WeightUtils.formatWeight(current));
        currentWeightUnit.setText(goalUnit);
        goalWeightValue.setText(WeightUtils.formatWeight(activeGoal.getGoalWeight()));
        goalWeightUnit.setText(goalUnit);

        // Update progress bar
        updateProgressBar(current, activeGoal.getStartWeight(), activeGoal.getGoalWeight());
    }

    /**
     * Update progress bar width and percentage.
     *
     * @param current current weight
     * @param start starting weight
     * @param goal goal weight
     */
    private void updateProgressBar(double current, double start, double goal) {
        double totalRange = Math.abs(start - goal);
        double progress = Math.abs(start - current);

        // Prevent division by zero if start equals goal
        int percentageValue = (totalRange == 0) ? 0 : (int) ((progress / totalRange) * 100);

        // Clamp percentage between 0 and 100
        final int percentage = Math.max(0, Math.min(100, percentageValue));

        progressPercentage.setText(percentage + "%");

        // Set progress bar width
        ViewGroup.LayoutParams params = progressBarFill.getLayoutParams();
        params.width = 0; // Will be updated after layout
        progressBarFill.setLayoutParams(params);

        // Post to update after layout
        progressBarFill.post(() -> {
            int containerWidth = progressBarFill.getParent() instanceof View ?
                    ((View) progressBarFill.getParent()).getWidth() : 0;
            ViewGroup.LayoutParams layoutParams = progressBarFill.getLayoutParams();
            layoutParams.width = (int) (containerWidth * (percentage / 100.0));
            progressBarFill.setLayoutParams(layoutParams);
        });
    }

    /**
     * Calculate and display quick stats.
     * Uses cached weightEntries to avoid redundant database query.
     */
    private void calculateQuickStats() {
        activeGoal = goalWeightDAO.getActiveGoal(currentUserId);

        if (activeGoal != null && !weightEntries.isEmpty()) {
            double current = weightEntries.get(0).getWeightValue();
            double start = activeGoal.getStartWeight();
            double goal = activeGoal.getGoalWeight();

            // Total lost
            double totalLost = start - current;
            totalLostValue.setText(String.format("%.0f", totalLost));

            // Lbs to goal
            double lbsToGoal = Math.abs(current - goal);
            lbsToGoalValue.setText(String.format("%.0f", lbsToGoal));
        }

        // Day streak (use cached list)
        int streak = DateUtils.calculateDayStreak(weightEntries);
        dayStreakValue.setText(String.valueOf(streak));
    }

    /**
     * Update greeting based on time of day.
     */
    private void updateGreeting() {
        int hour = LocalTime.now().getHour();
        String greeting = getGreetingForHour(hour);
        greetingText.setText(greeting);
    }

    /**
     * Get greeting text for a specific hour of day.
     * <p>
     * Extracted for testing time boundaries (resolves GH #50).
     *
     * @param hour the hour of day (0-23)
     * @return greeting string ("Good morning", "Good afternoon", or "Good evening")
     */
    @VisibleForTesting
    static String getGreetingForHour(int hour) {
        if (hour < 12) {
            return "Good morning";
        } else if (hour < 18) {
            return "Good afternoon";
        } else {
            return "Good evening";
        }
    }

    /**
     * Set greeting text for a specific hour (for testing only).
     * <p>
     * Allows tests to verify greeting logic at specific times without
     * waiting for actual time boundaries. Resolves GH #50.
     *
     * @param hour the hour of day (0-23) to test
     */
    @VisibleForTesting
    void setGreetingForHour(int hour) {
        String greeting = getGreetingForHour(hour);
        greetingText.setText(greeting);
    }

    /**
     * Update user name display.
     * Falls back to username if display_name is null or empty (defensive programming).
     */
    private void updateUserName() {
        User user = userDAO.getUserById(currentUserId);
        if (user != null) {
            String displayName = user.getDisplayName();
            if (ValidationUtils.isNullOrEmpty(displayName)) {
                displayName = user.getUsername();  // Fallback to username
            }
            userName.setText(displayName);
        }
    }

    /**
     * Show or hide empty state.
     *
     * @param show true to show empty state, false to hide
     */
    private void showEmptyState(boolean show) {
        emptyStateContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        weightRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Handle delete entry with confirmation dialog.
     *
     * @param entry the weight entry to delete
     */
    private void handleDeleteEntry(WeightEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this weight entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    weightEntryDAO.deleteWeightEntry(entry.getWeightId());
                    Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
                    loadWeightEntries();
                    updateProgressCard();
                    calculateQuickStats();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ============================================================
    // Goal Setting Dialog
    // ============================================================

    /**
     * Shows the goal dialog fragment.
     * Fragment handles all UI, validation, and database operations.
     */
    public void showSetGoalDialog() {
        // Get current weight (latest entry)
        double currentWeight = 0.0;
        String currentUnit = "lbs";
        if (!weightEntries.isEmpty()) {
            WeightEntry latestEntry = weightEntries.get(0);
            currentWeight = latestEntry.getWeightValue();
            currentUnit = latestEntry.getWeightUnit();
        }

        // Validate we have a weight to set goal from
        if (currentWeight <= 0.0) {
            Toast.makeText(this, "Please add a weight entry first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and show dialog fragment
        GoalDialogFragment dialog = GoalDialogFragment.newInstance(
                currentUserId,
                currentWeight,
                currentUnit
        );
        dialog.setListener(this);
        dialog.show(getSupportFragmentManager(), "GoalDialogFragment");
    }

    /**
     * Shows the goal dialog fragment in edit mode.
     * Opens dialog with existing goal data for editing.
     */
    private void handleEditGoal() {
        if (activeGoal == null) {
            Toast.makeText(this, "No active goal to edit", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current weight (latest entry)
        double currentWeight = 0.0;
        String currentUnit = "lbs";
        if (!weightEntries.isEmpty()) {
            WeightEntry latestEntry = weightEntries.get(0);
            currentWeight = latestEntry.getWeightValue();
            currentUnit = latestEntry.getWeightUnit();
        }

        // Validate we have a weight
        if (currentWeight <= 0.0) {
            Toast.makeText(this, "No weight entries found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and show dialog in edit mode
        GoalDialogFragment dialog = GoalDialogFragment.newInstanceForEdit(
                currentUserId,
                currentWeight,
                currentUnit,
                activeGoal
        );
        dialog.setListener(this);
        dialog.show(getSupportFragmentManager(), "GoalDialogFragment");
    }

    // ============================================================
    // GoalDialogFragment.GoalDialogListener Implementation
    // ============================================================

    @Override
    public void onGoalSaved(GoalWeight goal) {
        // Refresh active goal reference
        activeGoal = goal;

        // Refresh UI
        updateProgressCard();
        calculateQuickStats();
    }

    @Override
    public void onGoalSaveError(String errorMessage) {
        // Error already shown via Toast in fragment
        // Could log error or show additional UI feedback here
        android.util.Log.e("MainActivity", "Goal save failed: " + errorMessage);
    }

    // ============================================================
    // WeightEntryAdapter.OnItemClickListener Implementation
    // ============================================================

    @Override
    public void onEditClick(WeightEntry entry) {
        Intent intent = new Intent(MainActivity.this, WeightEntryActivity.class);
        intent.putExtra(WeightEntryActivity.EXTRA_USER_ID, currentUserId);
        intent.putExtra(WeightEntryActivity.EXTRA_IS_EDIT_MODE, true);
        intent.putExtra(WeightEntryActivity.EXTRA_WEIGHT_ID, entry.getWeightId());
        intent.putExtra(WeightEntryActivity.EXTRA_WEIGHT_VALUE, entry.getWeightValue());
        intent.putExtra(WeightEntryActivity.EXTRA_WEIGHT_DATE, entry.getWeightDate().toString());
        intent.putExtra(WeightEntryActivity.EXTRA_WEIGHT_UNIT, entry.getWeightUnit());
        startActivityForResult(intent, REQUEST_CODE_WEIGHT_ENTRY);
    }

    @Override
    public void onDeleteClick(WeightEntry entry) {
        handleDeleteEntry(entry);
    }

    /**
     * Refresh data when returning to MainActivity.
     * Called when navigating back from other screens (Goals, etc.).
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh all data from database
        loadWeightEntries();
        updateProgressCard();
        calculateQuickStats();
    }

    /**
     * Handle result from WeightEntryActivity.
     * Refreshes data if entry was saved or updated.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_WEIGHT_ENTRY && resultCode == RESULT_OK) {
            // Refresh all data from database
            loadWeightEntries();
            updateProgressCard();
            calculateQuickStats();
        }
    }
}
