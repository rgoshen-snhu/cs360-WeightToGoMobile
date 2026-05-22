package com.example.weightogo.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.weightogo.R;
import com.example.weightogo.database.GoalWeightDAO;
import com.example.weightogo.database.UserPreferenceDAO;
import com.example.weightogo.database.WeighToGoDBHelper;
import com.example.weightogo.models.GoalWeight;
import com.example.weightogo.utils.DateUtils;
import com.example.weightogo.utils.GoalUtils;
import com.example.weightogo.utils.SessionManager;
import com.example.weightogo.utils.WeightUtils;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Dialog fragment for creating and editing weight goals.
 * Reusable across MainActivity and GoalsActivity.
 *
 * Usage:
 * <pre>
 * GoalDialogFragment dialog = GoalDialogFragment.newInstance(userId, currentWeight, currentUnit);
 * dialog.setListener(this);  // Activity implements GoalDialogListener
 * dialog.show(getSupportFragmentManager(), "GoalDialogFragment");
 * </pre>
 */
public class GoalDialogFragment extends DialogFragment {

    // ============================================================
    // Constants
    // ============================================================

    private static final String TAG = "GoalDialogFragment";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_CURRENT_WEIGHT = "current_weight";
    private static final String ARG_CURRENT_UNIT = "current_unit";
    private static final String ARG_EXISTING_GOAL_ID = "existing_goal_id";

    // ============================================================
    // Listener Interface
    // ============================================================

    /**
     * Callback interface for goal dialog events.
     * Activities must implement this interface to receive goal creation results.
     */
    public interface GoalDialogListener {
        /**
         * Called when a goal is successfully saved to the database.
         *
         * @param goal The newly created goal with goalId populated
         */
        void onGoalSaved(GoalWeight goal);

        /**
         * Called when goal save fails (e.g., database error, constraint violation).
         *
         * @param errorMessage Human-readable error message
         */
        void onGoalSaveError(String errorMessage);
    }

    // ============================================================
    // Instance Fields
    // ============================================================

    // Listener
    private GoalDialogListener listener;

    // DAOs
    private GoalWeightDAO goalWeightDAO;
    private UserPreferenceDAO userPreferenceDAO;

    // Arguments (parsed from Bundle)
    private long userId;
    private double currentWeight;
    private String currentUnit;
    private long existingGoalId;

    // UI Elements
    private TextView textCurrentWeight;
    private TextInputEditText inputGoalWeight;
    private TextView btnSelectTargetDate;
    private TextView textSelectedTargetDate;

    // Dialog State
    private String selectedUnit;
    private LocalDate selectedTargetDate;
    private GoalWeight existingGoal; // For edit mode

    // ============================================================
    // Public Methods
    // ============================================================

    /**
     * Creates a new instance of GoalDialogFragment for creating a new goal.
     *
     * @param userId        Current user ID for goal creation
     * @param currentWeight User's current weight for display and validation
     * @param currentUnit   Current weight unit ("lbs" or "kg")
     * @return Configured GoalDialogFragment instance
     */
    public static GoalDialogFragment newInstance(long userId, double currentWeight, String currentUnit) {
        GoalDialogFragment fragment = new GoalDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        args.putDouble(ARG_CURRENT_WEIGHT, currentWeight);
        args.putString(ARG_CURRENT_UNIT, currentUnit);
        args.putLong(ARG_EXISTING_GOAL_ID, -1); // Default: create mode
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates a new instance of GoalDialogFragment for editing an existing goal.
     *
     * @param userId        Current user ID
     * @param currentWeight User's current weight for display and validation
     * @param currentUnit   Current weight unit ("lbs" or "kg")
     * @param existingGoal  The existing goal to edit
     * @return Configured GoalDialogFragment instance in edit mode
     */
    public static GoalDialogFragment newInstanceForEdit(long userId, double currentWeight,
                                                        String currentUnit, GoalWeight existingGoal) {
        GoalDialogFragment fragment = new GoalDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        args.putDouble(ARG_CURRENT_WEIGHT, currentWeight);
        args.putString(ARG_CURRENT_UNIT, currentUnit);
        args.putLong(ARG_EXISTING_GOAL_ID, existingGoal.getGoalId());
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Sets the listener for goal dialog events.
     * Must be called before showing the dialog.
     *
     * @param listener Callback for goal save events
     * @throws IllegalArgumentException if listener is null
     */
    public void setListener(@NonNull GoalDialogListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("GoalDialogListener cannot be null");
        }
        this.listener = listener;
    }

    // ============================================================
    // Lifecycle Methods
    // ============================================================

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Parse arguments
        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalStateException("GoalDialogFragment requires arguments. Use newInstance()");
        }

        userId = args.getLong(ARG_USER_ID, -1);
        currentWeight = args.getDouble(ARG_CURRENT_WEIGHT, 0.0);
        currentUnit = args.getString(ARG_CURRENT_UNIT, "lbs");
        existingGoalId = args.getLong(ARG_EXISTING_GOAL_ID, -1);

        // Validate required arguments
        if (userId <= 0 || currentWeight <= 0.0) {
            throw new IllegalArgumentException("Invalid arguments: userId and currentWeight must be positive");
        }

        // Initialize DAOs
        WeighToGoDBHelper dbHelper = WeighToGoDBHelper.getInstance(requireContext());
        goalWeightDAO = new GoalWeightDAO(dbHelper);
        userPreferenceDAO = new UserPreferenceDAO(dbHelper);

        // Load existing goal if in edit mode
        if (existingGoalId > 0) {
            existingGoal = goalWeightDAO.getGoalById(existingGoalId);
            if (existingGoal != null) {
                selectedUnit = existingGoal.getGoalUnit();
                selectedTargetDate = existingGoal.getTargetDate();
            } else {
                // Goal not found, treat as create mode
                existingGoalId = -1;
                selectedUnit = loadUserPreferenceUnit();
                selectedTargetDate = null;
            }
        } else {
            // Create mode - load from user preference
            selectedUnit = loadUserPreferenceUnit();
            selectedTargetDate = null;
        }
    }

    /**
     * Loads the user's preferred weight unit from UserPreferenceDAO.
     * Used in create mode to initialize selectedUnit.
     *
     * @return the user's preferred unit ("lbs" or "kg"), defaults to "lbs"
     */
    private String loadUserPreferenceUnit() {
        long currentUserId = SessionManager.getInstance(requireContext()).getCurrentUserId();
        return userPreferenceDAO.getWeightUnit(currentUserId);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Inflate layout
        View dialogView = requireActivity().getLayoutInflater()
                .inflate(R.layout.dialog_set_goal, null);

        // Initialize views
        initViews(dialogView);

        // Setup interactions
        setupDatePicker();

        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Button listeners
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> handleSaveGoal());

        return dialog;
    }

    /**
     * Called when the fragment's view hierarchy is being removed.
     * Clear listener to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listener = null;  // Clear listener to prevent memory leak
    }

    // ============================================================
    // Private Helper Methods
    // ============================================================

    /**
     * Initialize all view references and set current weight display.
     *
     * @param dialogView The inflated dialog view
     */
    private void initViews(View dialogView) {
        textCurrentWeight = dialogView.findViewById(R.id.text_current_weight);
        inputGoalWeight = dialogView.findViewById(R.id.input_goal_weight);
        btnSelectTargetDate = dialogView.findViewById(R.id.btn_select_target_date);
        textSelectedTargetDate = dialogView.findViewById(R.id.text_selected_target_date);

        // Set current weight display
        textCurrentWeight.setText(WeightUtils.formatWeightWithUnit(currentWeight, currentUnit));

        // Pre-fill goal weight if in edit mode
        if (existingGoal != null) {
            inputGoalWeight.setText(WeightUtils.formatWeight(existingGoal.getGoalWeight()));

            // Show target date if exists
            if (selectedTargetDate != null) {
                textSelectedTargetDate.setText(String.format(getString(R.string.target_date_format),
                        DateUtils.formatDateFull(selectedTargetDate)));
                textSelectedTargetDate.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Setup target date picker button listener.
     */
    private void setupDatePicker() {
        btnSelectTargetDate.setOnClickListener(v -> showDatePicker());
    }

    /**
     * Shows a Material Date Picker for selecting the optional target date.
     */
    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Target Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Convert epoch milliseconds to LocalDate
            Instant instant = Instant.ofEpochMilli(selection);
            LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();

            selectedTargetDate = date;
            textSelectedTargetDate.setText(String.format(getString(R.string.target_date_format),
                    DateUtils.formatDateFull(date)));
            textSelectedTargetDate.setVisibility(View.VISIBLE);
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    /**
     * Handles saving the goal after validation.
     * Validates inputs, creates GoalWeight object, and saves to database.
     */
    private void handleSaveGoal() {
        // Get goal weight input
        String goalWeightStr = inputGoalWeight.getText() != null ?
                inputGoalWeight.getText().toString().trim() : "";

        // Validation: goal weight required
        if (goalWeightStr.isEmpty()) {
            inputGoalWeight.setError(getString(R.string.error_goal_required));
            inputGoalWeight.requestFocus();
            return;
        }

        // Parse goal weight
        double goalWeight;
        try {
            goalWeight = Double.parseDouble(goalWeightStr);
        } catch (NumberFormatException e) {
            inputGoalWeight.setError(getString(R.string.error_invalid_weight));
            inputGoalWeight.requestFocus();
            return;
        }

        // Validation: goal must differ from current weight and be in valid range
        if (!GoalUtils.isValidGoal(currentWeight, goalWeight, selectedUnit)) {
            // Determine specific error message
            if (Math.abs(goalWeight - currentWeight) < 0.1) {
                inputGoalWeight.setError(getString(R.string.error_goal_same_as_current));
            } else if ("lbs".equals(selectedUnit)) {
                inputGoalWeight.setError(getString(R.string.error_goal_out_of_range_lbs));
            } else {
                inputGoalWeight.setError(getString(R.string.error_goal_out_of_range_kg));
            }
            inputGoalWeight.requestFocus();
            return;
        }

        // Validation: target date must be in future (if provided)
        if (selectedTargetDate != null && !GoalUtils.isValidTargetDate(selectedTargetDate)) {
            Toast.makeText(requireContext(), R.string.error_target_date_past, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and save goal
        validateAndSaveGoal(goalWeight);
    }

    /**
     * Creates or updates GoalWeight object and saves to database via DAO.
     * Calls listener callback on success or failure.
     *
     * @param goalWeight The validated goal weight value
     */
    private void validateAndSaveGoal(double goalWeight) {
        GoalWeight goal;
        boolean isEditMode = (existingGoal != null);

        if (isEditMode) {
            // Edit mode: Update existing goal
            goal = existingGoal;
            goal.setGoalWeight(goalWeight);

            // Convert start weight if unit was changed
            String oldUnit = existingGoal.getGoalUnit();
            if (!oldUnit.equals(selectedUnit)) {
                double currentStartWeight = existingGoal.getStartWeight();
                goal.setStartWeight(WeightUtils.convertBetweenUnits(currentStartWeight, oldUnit, selectedUnit));
            }

            goal.setGoalUnit(selectedUnit);
            goal.setTargetDate(selectedTargetDate); // Optional, can be null
            goal.setUpdatedAt(LocalDateTime.now());

            // Update in database
            int rowsUpdated = goalWeightDAO.updateGoal(goal);

            if (rowsUpdated > 0) {
                Toast.makeText(requireContext(), "Goal updated successfully", Toast.LENGTH_SHORT).show();

                // Callback to activity BEFORE dismiss
                if (listener != null) {
                    listener.onGoalSaved(goal);
                }

                dismiss();
            } else {
                String errorMsg = "Failed to update goal. Please try again.";
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();

                if (listener != null) {
                    listener.onGoalSaveError(errorMsg);
                }
            }
        } else {
            // Create mode: Create new goal
            goal = new GoalWeight();
            goal.setUserId(userId);
            goal.setGoalWeight(goalWeight);
            goal.setGoalUnit(selectedUnit);

            // Convert start weight if unit was changed
            double startWeight = WeightUtils.convertBetweenUnits(currentWeight, currentUnit, selectedUnit);
            goal.setStartWeight(startWeight);

            goal.setTargetDate(selectedTargetDate); // Optional, can be null
            goal.setActive(true);
            goal.setAchieved(false);
            goal.setCreatedAt(LocalDateTime.now());
            goal.setUpdatedAt(LocalDateTime.now());

            // Save to database (deactivates previous goal automatically)
            long goalId = goalWeightDAO.setNewActiveGoal(goal);

            if (goalId > 0) {
                goal.setGoalId(goalId); // Set ID for callback
                Toast.makeText(requireContext(), R.string.success_goal_created, Toast.LENGTH_SHORT).show();

                // Callback to activity BEFORE dismiss
                if (listener != null) {
                    listener.onGoalSaved(goal);
                }

                dismiss();
            } else {
                String errorMsg = "Failed to create goal. Please try again.";
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();

                if (listener != null) {
                    listener.onGoalSaveError(errorMsg);
                }
            }
        }
    }

}
