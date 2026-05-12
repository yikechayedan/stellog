package com.example.stellog.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.stellog.R;
import com.example.stellog.data.model.Habit;
import com.example.stellog.data.repository.HabitRepository;
import com.example.stellog.util.DimensionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * 日历活动筛选页面。
 *
 * 当前只负责展示和维护页面内的选择状态，暂不把筛选结果返回日历页。
 */
public class HabitFilterActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTED_HABIT_IDS = "selected_habit_ids";

    private LinearLayout optionContainer;
    private List<Habit> habits;
    private final HashSet<Long> selectedHabitIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_habit_filter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.habit_filter_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        optionContainer = findViewById(R.id.habit_filter_options);
        habits = new HabitRepository(getApplicationContext()).getHabits();
        loadInitialSelection();

        findViewById(R.id.habit_filter_close_button).setOnClickListener(v -> finish());
        findViewById(R.id.habit_filter_confirm_button).setOnClickListener(v -> confirmSelection());
        renderOptions();
    }

    @SuppressWarnings("unchecked")
    private void loadInitialSelection() {
        Object extra = getIntent().getSerializableExtra(EXTRA_SELECTED_HABIT_IDS);
        if (extra instanceof HashSet<?>) {
            selectedHabitIds.addAll((HashSet<Long>) extra);
            return;
        }

        for (Habit habit : habits) {
            selectedHabitIds.add(habit.id);
        }
    }

    private void renderOptions() {
        optionContainer.removeAllViews();
        optionContainer.addView(createOptionRow("全部", getAllSubtitle(), areAllHabitsSelected(), this::toggleAllHabits));

        for (Habit habit : habits) {
            boolean selected = selectedHabitIds.contains(habit.id);
            optionContainer.addView(createOptionRow(
                    habit.name,
                    getHabitSubtitle(habit),
                    selected,
                    () -> toggleHabit(habit.id)
            ));
        }
    }

    private void toggleAllHabits() {
        if (areAllHabitsSelected()) {
            selectedHabitIds.clear();
        } else {
            for (Habit habit : habits) {
                selectedHabitIds.add(habit.id);
            }
        }
        renderOptions();
    }

    private void toggleHabit(long habitId) {
        if (selectedHabitIds.contains(habitId)) {
            selectedHabitIds.remove(habitId);
        } else {
            selectedHabitIds.add(habitId);
        }
        renderOptions();
    }

    private void confirmSelection() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_SELECTED_HABIT_IDS, selectedHabitIds);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private boolean areAllHabitsSelected() {
        return !habits.isEmpty() && selectedHabitIds.size() == habits.size();
    }

    private String getAllSubtitle() {
        return String.format(Locale.CHINA, "已选择 %d / %d", selectedHabitIds.size(), habits.size());
    }

    private String getHabitSubtitle(Habit habit) {
        if (habit.unit == null || habit.unit.isEmpty()) {
            return "活动";
        }
        return String.format(Locale.CHINA, "单位：%s", habit.unit);
    }

    private View createOptionRow(String title, String subtitle, boolean selected, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_calendar_summary);
        row.setPadding(
                DimensionUtils.dpToPx(getResources(), 16),
                0,
                DimensionUtils.dpToPx(getResources(), 14),
                0
        );
        row.setOnClickListener(v -> onClick.run());

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                DimensionUtils.dpToPx(getResources(), 60)
        );
        if (optionContainer.getChildCount() > 0) {
            rowParams.topMargin = DimensionUtils.dpToPx(getResources(), 10);
        }
        row.setLayoutParams(rowParams);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
        );
        textColumn.setLayoutParams(textParams);

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextColor(getColor(selected ? R.color.stellog_ink : R.color.stellog_muted));
        titleText.setTextSize(17);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setSingleLine(true);

        TextView subtitleText = new TextView(this);
        subtitleText.setText(subtitle);
        subtitleText.setTextColor(getColor(R.color.stellog_muted));
        subtitleText.setTextSize(12);
        subtitleText.setSingleLine(true);

        textColumn.addView(titleText);
        textColumn.addView(subtitleText);

        TextView checkView = new TextView(this);
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                DimensionUtils.dpToPx(getResources(), 24),
                DimensionUtils.dpToPx(getResources(), 24)
        );
        checkView.setLayoutParams(checkParams);
        checkView.setGravity(Gravity.CENTER);
        checkView.setBackgroundResource(selected ? R.drawable.bg_filter_check_selected : R.drawable.bg_filter_check_unselected);
        checkView.setText(selected ? "\u2713" : "");
        checkView.setTextColor(getColor(R.color.white));
        checkView.setTextSize(14);
        checkView.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(textColumn);
        row.addView(checkView);
        return row;
    }
}
