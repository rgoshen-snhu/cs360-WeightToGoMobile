package com.example.weightogo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weightogo.R;
import com.example.weightogo.models.WeightEntry;
import com.example.weightogo.utils.DateUtils;
import com.example.weightogo.utils.WeightUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying weight entry items.
 * Implements ViewHolder pattern for efficient list rendering.
 */
public class WeightEntryAdapter extends RecyclerView.Adapter<WeightEntryAdapter.ViewHolder> {

    /**
     * Interface for handling item click events.
     */
    public interface OnItemClickListener {
        /**
         * Called when the edit button is clicked.
         *
         * @param entry the weight entry to edit
         */
        void onEditClick(WeightEntry entry);

        /**
         * Called when the delete button is clicked.
         *
         * @param entry the weight entry to delete
         */
        void onDeleteClick(WeightEntry entry);
    }

    private final List<WeightEntry> entries;
    private final OnItemClickListener listener;

    /**
     * Constructor for WeightEntryAdapter.
     *
     * @param entries list of weight entries to display (must not be null)
     * @param listener listener for item click events (must not be null)
     * @throws NullPointerException if entries or listener is null
     */
    public WeightEntryAdapter(@NonNull List<WeightEntry> entries, @NonNull OnItemClickListener listener) {
        if (entries == null) {
            throw new NullPointerException("entries must not be null");
        }
        if (listener == null) {
            throw new NullPointerException("listener must not be null");
        }
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weight_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeightEntry entry = entries.get(position);

        // Bind date badge
        bindDateBadge(holder, entry.getWeightDate());

        // Bind weight value and unit
        bindWeightValue(holder, entry.getWeightValue(), entry.getWeightUnit());

        // Bind entry time
        bindEntryTime(holder, entry.getWeightDate(), entry.getCreatedAt());

        // Bind trend badge
        bindTrendBadge(holder, position);

        // Set up click listeners
        holder.editButton.setOnClickListener(v -> listener.onEditClick(entry));
        holder.deleteButton.setOnClickListener(v -> listener.onDeleteClick(entry));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    /**
     * Binds date badge (day number and month name).
     *
     * @param holder ViewHolder containing the views
     * @param date the date to display
     */
    private void bindDateBadge(ViewHolder holder, LocalDate date) {
        String shortDate = DateUtils.formatDateShort(date); // "26 Nov"
        String[] parts = shortDate.split(" ");

        if (parts.length == 2) {
            holder.dayNumber.setText(parts[0]); // "26"
            holder.monthName.setText(parts[1].toUpperCase(Locale.US)); // "NOV"
        }
    }

    /**
     * Binds weight value and unit with 1 decimal place formatting.
     *
     * @param holder ViewHolder containing the views
     * @param weight the weight value to display
     * @param unit the weight unit (lbs or kg)
     */
    private void bindWeightValue(ViewHolder holder, double weight, String unit) {
        holder.weightValue.setText(WeightUtils.formatWeight(weight));
        holder.weightUnit.setText(unit);
    }

    /**
     * Binds entry time with smart formatting (Today, Yesterday, or full date).
     *
     * @param holder ViewHolder containing the views
     * @param date the date of the entry
     * @param createdAt the timestamp when the entry was created
     */
    private void bindEntryTime(ViewHolder holder, LocalDate date, LocalDateTime createdAt) {
        String timeStr;

        if (DateUtils.isToday(date)) {
            timeStr = "Today, " + formatTime(createdAt);
        } else if (isYesterday(date)) {
            timeStr = "Yesterday, " + formatTime(createdAt);
        } else {
            timeStr = DateUtils.formatDateFull(date);
        }

        holder.entryTime.setText(timeStr);
    }

    /**
     * Binds trend badge showing weight change from previous entry.
     * Converts weights to current entry's unit before comparison.
     *
     * @param holder ViewHolder containing the views
     * @param position position of the entry in the list
     */
    private void bindTrendBadge(ViewHolder holder, int position) {
        // If this is the last entry (no previous to compare), hide trend badge
        if (position >= entries.size() - 1) {
            holder.trendBadge.setVisibility(View.GONE);
            return;
        }

        holder.trendBadge.setVisibility(View.VISIBLE);

        WeightEntry current = entries.get(position);
        WeightEntry previous = entries.get(position + 1); // List is sorted DESC (most recent first)

        // Normalize both weights to current entry's unit for accurate comparison
        double currentWeight = current.getWeightValue();
        double previousWeight = previous.getWeightValue();

        // Convert previous weight to current entry's unit if they differ
        if (!current.getWeightUnit().equals(previous.getWeightUnit())) {
            if (current.getWeightUnit().equals("lbs") && previous.getWeightUnit().equals("kg")) {
                // Convert previous kg to lbs
                previousWeight = WeightUtils.convertKgToLbs(previousWeight);
            } else if (current.getWeightUnit().equals("kg") && previous.getWeightUnit().equals("lbs")) {
                // Convert previous lbs to kg
                previousWeight = WeightUtils.convertLbsToKg(previousWeight);
            }
        }

        // Calculate trend: previous - current (positive = weight loss, negative = weight gain)
        double diff = WeightUtils.roundToOneDecimal(previousWeight - currentWeight);
        String unit = current.getWeightUnit(); // Trend shown in current entry's unit

        if (Math.abs(diff) < 0.1) {
            // No change
            holder.trendBadge.setText("− 0.0 " + unit);
            holder.trendBadge.setBackgroundResource(R.drawable.bg_badge_trend_same);
        } else if (diff > 0) {
            // Lost weight (previous was heavier)
            holder.trendBadge.setText("↓ " + WeightUtils.formatWeightWithUnit(diff, unit));
            holder.trendBadge.setBackgroundResource(R.drawable.bg_badge_trend_down);
        } else {
            // Gained weight
            holder.trendBadge.setText("↑ " + WeightUtils.formatWeightWithUnit(Math.abs(diff), unit));
            holder.trendBadge.setBackgroundResource(R.drawable.bg_badge_trend_up);
        }
    }

    /**
     * Formats time as "h:mm a" (e.g., "7:32 AM").
     *
     * @param dateTime the datetime to format
     * @return formatted time string
     */
    private String formatTime(LocalDateTime dateTime) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
        return dateTime.format(timeFormatter);
    }

    /**
     * Checks if a date is yesterday.
     *
     * @param date the date to check
     * @return true if the date is yesterday
     */
    private boolean isYesterday(LocalDate date) {
        return date.equals(LocalDate.now().minusDays(1));
    }

    /**
     * ViewHolder for weight entry items.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dayNumber;
        TextView monthName;
        TextView weightValue;
        TextView weightUnit;
        TextView entryTime;
        TextView trendBadge;
        ImageButton editButton;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.dayNumber);
            monthName = itemView.findViewById(R.id.monthName);
            weightValue = itemView.findViewById(R.id.weightValue);
            weightUnit = itemView.findViewById(R.id.weightUnit);
            entryTime = itemView.findViewById(R.id.entryTime);
            trendBadge = itemView.findViewById(R.id.trendBadge);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
