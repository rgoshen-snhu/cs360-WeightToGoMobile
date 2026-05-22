package com.example.weightogo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.example.weightogo.adapters.GoalHistoryAdapter;
import com.example.weightogo.database.GoalWeightDAO;
import com.example.weightogo.fragments.GoalDialogFragment;
import com.example.weightogo.database.UserDAO;
import com.example.weightogo.database.WeighToGoDBHelper;
import com.example.weightogo.database.WeightEntryDAO;
import com.example.weightogo.models.GoalWeight;
import com.example.weightogo.models.WeightEntry;
import com.example.weightogo.utils.DateUtils;
import com.example.weightogo.utils.SessionManager;
import com.example.weightogo.utils.WeightUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing weight goals.
 * Displays current goal with expanded stats and goal history.
 */
public class GoalsActivity extends AppCompatActivity
        implements GoalDialogFragment.GoalDialogListener {

    // UI Elements - Header
    private ImageButton btnBack;

    // UI Elements - Current Goal Card
    private MaterialCardView cardCurrentGoal;
    private ImageButton btnEditGoal;
    private ImageButton btnDeleteGoal;
    private TextView textStartWeight;
    private TextView textStartUnit;
    private TextView textCurrentWeightValue;
    private TextView textCurrentUnit;
    private TextView textGoalWeight;
    private TextView textGoalUnit;
    private LinearLayout targetDateContainer;
    private TextView textTargetDate;
    private TextView textDaysSinceStart;
    private TextView textPace;
    private TextView textProjection;
    private TextView textAvgWeeklyLoss;

    // UI Elements - Empty State
    private LinearLayout emptyStateContainer;

    // UI Elements - Goal History
    private LinearLayout goalHistorySection;
    private RecyclerView recyclerGoalHistory;
    private GoalHistoryAdapter adapter;

    // UI Elements - FAB
    private FloatingActionButton fabAddGoal;

    // Data Layer
    private WeighToGoDBHelper dbHelper;
    private GoalWeightDAO goalWeightDAO;
    private WeightEntryDAO weightEntryDAO;
    private UserDAO userDAO;
    private SessionManager sessionManager;

    // State
    private long currentUserId;
    private GoalWeight activeGoal;
    private List<GoalWeight> goalHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check authentication first
        if (!checkAuthentication()) {
            return; // Activity will finish in checkAuthentication()
        }

        setContentView(R.layout.activity_goals);

        // Initialize data layer
        initDataLayer();

        // Initialize UI
        initViews();
        setupRecyclerView();
        setupFAB();

        // Load data
        loadGoalData();
    }

    /**
     * Check if user is authenticated. Redirect to login if not.
     *
     * @return true if authenticated, false otherwise
     */
    private boolean checkAuthentication() {
        sessionManager = SessionManager.getInstance(this);

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
        if (goalWeightDAO == null) {
            goalWeightDAO = new GoalWeightDAO(dbHelper);
        }
        if (weightEntryDAO == null) {
            weightEntryDAO = new WeightEntryDAO(dbHelper);
        }
        if (userDAO == null) {
            userDAO = new UserDAO(dbHelper);
        }
        if (goalHistory == null) {
            goalHistory = new ArrayList<>();
        }
    }

    // =============================================================================================
    // TESTING SETTERS (Package-Private)
    // =============================================================================================

    /**
     * Set GoalWeightDAO instance (for testing only).
     *
     * @param goalWeightDAO the GoalWeightDAO instance to use
     */
    @VisibleForTesting
    void setGoalWeightDAO(GoalWeightDAO goalWeightDAO) {
        this.goalWeightDAO = goalWeightDAO;
    }

    /**
     * Set WeightEntryDAO instance (for testing only).
     *
     * @param weightEntryDAO the WeightEntryDAO instance to use
     */
    @VisibleForTesting
    void setWeightEntryDAO(WeightEntryDAO weightEntryDAO) {
        this.weightEntryDAO = weightEntryDAO;
    }

    /**
     * Set UserDAO instance (for testing only).
     *
     * @param userDAO the UserDAO instance to use
     */
    @VisibleForTesting
    void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Set SessionManager instance (for testing only).
     *
     * @param sessionManager the SessionManager instance to use
     */
    @VisibleForTesting
    void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * Initialize all view references.
     */
    private void initViews() {
        // Header
        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Current Goal Card
        cardCurrentGoal = findViewById(R.id.card_current_goal);
        btnEditGoal = findViewById(R.id.btn_edit_goal);
        btnDeleteGoal = findViewById(R.id.btn_delete_goal);
        textStartWeight = findViewById(R.id.text_start_weight);
        textStartUnit = findViewById(R.id.text_start_unit);
        textCurrentWeightValue = findViewById(R.id.text_current_weight_value);
        textCurrentUnit = findViewById(R.id.text_current_unit);
        textGoalWeight = findViewById(R.id.text_goal_weight);
        textGoalUnit = findViewById(R.id.text_goal_unit);
        targetDateContainer = findViewById(R.id.target_date_container);
        textTargetDate = findViewById(R.id.text_target_date);
        textDaysSinceStart = findViewById(R.id.text_days_since_start);
        textPace = findViewById(R.id.text_pace);
        textProjection = findViewById(R.id.text_projection);
        textAvgWeeklyLoss = findViewById(R.id.text_avg_weekly_loss);

        // Empty State
        emptyStateContainer = findViewById(R.id.empty_state_container);

        // Goal History
        goalHistorySection = findViewById(R.id.goal_history_section);
        recyclerGoalHistory = findViewById(R.id.recycler_goal_history);

        // FAB
        fabAddGoal = findViewById(R.id.fab_add_goal);

        // Button listeners
        btnEditGoal.setOnClickListener(v -> handleEditGoal());
        btnDeleteGoal.setOnClickListener(v -> handleDeleteGoal());
    }

    /**
     * Setup RecyclerView for goal history.
     */
    private void setupRecyclerView() {
        adapter = new GoalHistoryAdapter(goalHistory);
        recyclerGoalHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerGoalHistory.setAdapter(adapter);
    }

    /**
     * Setup FAB click listener.
     */
    private void setupFAB() {
        fabAddGoal.setOnClickListener(v -> showSetGoalDialog());
    }

    /**
     * Shows the goal dialog fragment.
     * Fragment handles all UI, validation, and database operations.
     */
    private void showSetGoalDialog() {
        // Get current weight (latest entry)
        WeightEntry latestEntry = weightEntryDAO.getLatestWeightEntry(currentUserId);

        if (latestEntry == null) {
            Toast.makeText(this, "Please add a weight entry first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and show dialog fragment
        GoalDialogFragment dialog = GoalDialogFragment.newInstance(
                currentUserId,
                latestEntry.getWeightValue(),
                latestEntry.getWeightUnit()
        );
        dialog.setListener(this);
        dialog.show(getSupportFragmentManager(), "GoalDialogFragment");
    }

    /**
     * Load goal data from database (active goal + history).
     */
    private void loadGoalData() {
        // Load active goal
        activeGoal = goalWeightDAO.getActiveGoal(currentUserId);

        // Load goal history (inactive goals)
        List<GoalWeight> allGoals = goalWeightDAO.getGoalHistory(currentUserId);
        goalHistory.clear();
        for (GoalWeight goal : allGoals) {
            if (!goal.isActive()) {
                goalHistory.add(goal);
            }
        }

        // Update UI based on data
        if (activeGoal != null) {
            // Show current goal card
            cardCurrentGoal.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
            fabAddGoal.setVisibility(View.GONE);

            updateCurrentGoalCard();
            updateExpandedStats();
        } else {
            // Show empty state
            cardCurrentGoal.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
            fabAddGoal.setVisibility(View.VISIBLE);
        }

        // Show/hide goal history section
        if (!goalHistory.isEmpty()) {
            goalHistorySection.setVisibility(View.VISIBLE);
            adapter.updateGoals(goalHistory);  // Use adapter method instead of notifyDataSetChanged()
        } else {
            goalHistorySection.setVisibility(View.GONE);
        }
    }

    /**
     * Update current goal card with start/current/goal weights.
     */
    private void updateCurrentGoalCard() {
        if (activeGoal == null) {
            return;
        }

        // Start weight
        textStartWeight.setText(WeightUtils.formatWeight(activeGoal.getStartWeight()));
        textStartUnit.setText(activeGoal.getGoalUnit());

        // Current weight (already converted to goal's unit by getCurrentWeight())
        double currentWeight = getCurrentWeight();
        textCurrentWeightValue.setText(WeightUtils.formatWeight(currentWeight));
        textCurrentUnit.setText(activeGoal.getGoalUnit());

        // Goal weight
        textGoalWeight.setText(WeightUtils.formatWeight(activeGoal.getGoalWeight()));
        textGoalUnit.setText(activeGoal.getGoalUnit());

        // Target date (optional)
        if (activeGoal.getTargetDate() != null) {
            textTargetDate.setText(DateUtils.formatDateFull(activeGoal.getTargetDate()));
            targetDateContainer.setVisibility(View.VISIBLE);
        } else {
            targetDateContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Update expanded stats (days since start, pace, projection, avg weekly loss).
     */
    private void updateExpandedStats() {
        if (activeGoal == null) {
            return;
        }

        LocalDate startDate = activeGoal.getCreatedAt().toLocalDate();
        LocalDate today = LocalDate.now();
        long daysSinceStart = ChronoUnit.DAYS.between(startDate, today);

        // Days since start
        String daysText = String.format(getString(R.string.days_format), daysSinceStart);
        textDaysSinceStart.setText(daysText);

        // Calculate pace and projection
        double currentWeight = getCurrentWeight();
        double startWeight = activeGoal.getStartWeight();
        double goalWeight = activeGoal.getGoalWeight();

        // Check if user is making progress in the right direction
        boolean isLossGoal = goalWeight < startWeight;
        double weightChange = startWeight - currentWeight; // Positive = weight lost, Negative = weight gained
        double weightLost = Math.abs(weightChange);
        double weightRemaining = Math.abs(currentWeight - goalWeight);

        // Validate that user is making progress in correct direction
        boolean makingProgress = (isLossGoal && weightChange > 0) || (!isLossGoal && weightChange < 0);

        if (daysSinceStart > 0 && makingProgress && weightLost > 0) {
            // Pace (lbs/week)
            double pace = (weightLost / daysSinceStart) * 7;
            String paceText = String.format(getString(R.string.pace_format), pace);
            textPace.setText(paceText);

            // Projection (estimated completion date)
            if (pace > 0.01) { // Avoid division by zero
                long daysToGoal = (long) ((weightRemaining / pace) * 7);
                LocalDate projectedDate = today.plusDays(daysToGoal);
                String projectionText = DateUtils.formatDateFull(projectedDate);
                textProjection.setText(projectionText);
            } else {
                textProjection.setText("N/A");
            }

            // Avg weekly loss/gain
            double avgWeeklyChange = (weightLost / daysSinceStart) * 7;
            String avgText = String.format(getString(R.string.avg_weekly_format), avgWeeklyChange);
            textAvgWeeklyLoss.setText(avgText);
        } else {
            textPace.setText("N/A");
            textProjection.setText("N/A");
            textAvgWeeklyLoss.setText("N/A");
        }
    }

    // ============================================================
    // GoalDialogFragment.GoalDialogListener Implementation
    // ============================================================

    @Override
    public void onGoalSaved(GoalWeight goal) {
        // Reload all goal data (active goal + history)
        loadGoalData();
    }

    @Override
    public void onGoalSaveError(String errorMessage) {
        // Error already shown via Toast in fragment
        android.util.Log.e("GoalsActivity", "Goal save failed: " + errorMessage);
    }

    // ============================================================
    // Goal Actions
    // ============================================================

    /**
     * Handle edit goal button click.
     * Opens GoalDialogFragment in edit mode with existing goal data.
     */
    private void handleEditGoal() {
        if (activeGoal == null) {
            Toast.makeText(this, "No active goal to edit", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current weight for validation
        WeightEntry latestEntry = weightEntryDAO.getLatestWeightEntry(currentUserId);
        if (latestEntry == null) {
            Toast.makeText(this, "No weight entries found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show dialog in edit mode
        GoalDialogFragment dialog = GoalDialogFragment.newInstanceForEdit(
                currentUserId,
                latestEntry.getWeightValue(),
                latestEntry.getWeightUnit(),
                activeGoal
        );
        dialog.setListener(this);
        dialog.show(getSupportFragmentManager(), "GoalDialogFragment");
    }

    /**
     * Handle delete goal button click.
     * Shows confirmation dialog and deactivates goal if confirmed.
     */
    private void handleDeleteGoal() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_goal_title)
                .setMessage(R.string.confirm_delete_goal_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (activeGoal != null) {
                        int rowsUpdated = goalWeightDAO.deactivateGoal(activeGoal.getGoalId());
                        if (rowsUpdated > 0) {
                            Toast.makeText(this, R.string.success_goal_deleted, Toast.LENGTH_SHORT).show();
                            loadGoalData(); // Refresh UI
                        } else {
                            Toast.makeText(this, "Failed to delete goal", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Get current weight from latest weight entry, converted to match goal's unit.
     *
     * @return current weight in goal's unit, or 0.0 if no entries exist
     */
    private double getCurrentWeight() {
        WeightEntry latestEntry = weightEntryDAO.getLatestWeightEntry(currentUserId);
        if (latestEntry != null) {
            double currentWeight = latestEntry.getWeightValue();

            // Convert if goal exists and units don't match
            if (activeGoal != null) {
                String entryUnit = latestEntry.getWeightUnit();
                String goalUnit = activeGoal.getGoalUnit();

                if (!entryUnit.equals(goalUnit)) {
                    if ("kg".equals(goalUnit)) {
                        currentWeight = WeightUtils.convertLbsToKg(currentWeight);
                    } else {
                        currentWeight = WeightUtils.convertKgToLbs(currentWeight);
                    }
                }
            }

            return currentWeight;
        }
        return 0.0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from MainActivity
        loadGoalData();
    }
}
