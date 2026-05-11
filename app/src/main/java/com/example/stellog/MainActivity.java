package com.example.stellog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final long DEFAULT_CHECK_IN_VALUE = 1L;

    private final List<Habit> habits = new ArrayList<>();
    private final List<CheckInRecord> records = new ArrayList<>();

    private HabitPagerAdapter habitAdapter;
    private TextView pageIndicatorText;
    private int currentHabitPosition = 0;

    private final ActivityResultLauncher<Intent> createHabitLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                            return;
                        }

                        String name = result.getData().getStringExtra("habit_name");
                        String unit = result.getData().getStringExtra("habit_unit");
                        if (name == null || name.trim().isEmpty()) {
                            return;
                        }
                        if (unit == null) {
                            unit = "";
                        }

                        addHabit(name.trim(), unit.trim());
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        pageIndicatorText = findViewById(R.id.page_indicator_text);
        setupHabitPager();
        updateHeader(0);

        findViewById(R.id.add_activity_button).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateHabitActivity.class);
            createHabitLauncher.launch(intent);
        });
    }

    private void setupHabitPager() {
        ViewPager2 habitPager = findViewById(R.id.habit_pager);
        habitAdapter = new HabitPagerAdapter(habits);
        habitPager.setAdapter(habitAdapter);
        habitPager.setOffscreenPageLimit(1);
        habitPager.setClipToPadding(false);
        habitPager.setClipChildren(false);
        habitPager.setPadding(128, 0, 128, 0);
        habitPager.setPageTransformer((page, position) -> {
            float scale = 0.94f + (1 - Math.min(Math.abs(position), 1f)) * 0.06f;
            page.setScaleY(scale);
            page.setAlpha(0.55f + (1 - Math.min(Math.abs(position), 1f)) * 0.45f);
        });
        habitPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentHabitPosition = position;
                updateHeader(position);
            }
        });
    }

    private void addHabit(String name, String unit) {
        long now = System.currentTimeMillis();
        long newId = habits.isEmpty() ? 1L : habits.get(habits.size() - 1).id + 1L;

        Habit newHabit = new Habit(
                newId,
                0,
                name,
                unit,
                0,
                false,
                1,
                0,
                now,
                now
        );

        habits.add(newHabit);
        habitAdapter.notifyItemInserted(habits.size() - 1);

        ViewPager2 habitPager = findViewById(R.id.habit_pager);
        habitPager.setCurrentItem(habits.size() - 1, true);
        updateHeader(habits.size() - 1);
    }

    private void updateHeader(int position) {
        if (habits.isEmpty()) {
            pageIndicatorText.setText(getString(R.string.page_indicator_format, 0, 0));
            return;
        }
        pageIndicatorText.setText(getString(R.string.page_indicator_format, position + 1, habits.size()));
    }

    private boolean hasCheckedInToday(Habit habit) {
        return getTodayRecord(habit.id) != null;
    }

    private CheckInRecord getTodayRecord(long habitId) {
        CheckInRecord.RecordDate today = CheckInRecord.RecordDate.today();
        for (CheckInRecord record : records) {
            if (record.habitId == habitId && record.date.isSameDay(today)) {
                return record;
            }
        }
        return null;
    }

    private void checkInToday(Habit habit) {
        if (hasCheckedInToday(habit)) {
            return;
        }

        long now = System.currentTimeMillis();
        CheckInRecord record = new CheckInRecord(
                generateRecordId(),
                habit.id,
                habit.userId,
                CheckInRecord.RecordDate.today(),
                DEFAULT_CHECK_IN_VALUE,
                CheckInRecord.SOURCE_NORMAL,
                now,
                now
        );

        records.add(record);
        habit.recordNum += 1;
        habit.totalValue += record.value;
        habit.updatedAt = now;
        habitAdapter.notifyItemChanged(currentHabitPosition);
    }

    private void cancelTodayCheckIn(Habit habit) {
        CheckInRecord todayRecord = getTodayRecord(habit.id);
        if (todayRecord == null) {
            return;
        }

        records.remove(todayRecord);
        habit.recordNum = Math.max(0, habit.recordNum - 1);
        habit.totalValue = Math.max(0, habit.totalValue - todayRecord.value);
        habit.updatedAt = System.currentTimeMillis();
        habitAdapter.notifyItemChanged(currentHabitPosition);
    }

    private long generateRecordId() {
        if (records.isEmpty()) {
            return 1L;
        }
        return records.get(records.size() - 1).id + 1L;
    }

    private boolean hasRecordOnDate(long habitId, CheckInRecord.RecordDate date) {
        for (CheckInRecord record : records) {
            if (record.habitId == habitId && record.date.isSameDay(date)) {
                return true;
            }
        }
        return false;
    }

    private List<CheckInRecord.RecordDate> getCurrentWeekDates() {
        List<CheckInRecord.RecordDate> weekDates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int mondayOffset = dayOfWeek == Calendar.SUNDAY ? -6 : Calendar.MONDAY - dayOfWeek;
        calendar.add(Calendar.DAY_OF_MONTH, mondayOffset);

        for (int i = 0; i < 7; i++) {
            weekDates.add(CheckInRecord.RecordDate.fromCalendar(calendar));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return weekDates;
    }

    private String formatValueWithUnit(long value, String unit) {
        if (unit == null || unit.isEmpty()) {
            return String.valueOf(value);
        }
        return value + " " + unit;
    }

    private class HabitPagerAdapter extends RecyclerView.Adapter<HabitPagerAdapter.HabitViewHolder> {
        private final List<Habit> items;

        HabitPagerAdapter(List<Habit> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_habit_card, parent, false);
            return new HabitViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class HabitViewHolder extends RecyclerView.ViewHolder {
            private final TextView habitName;
            private final TextView streakValue;
            private final TextView targetSummary;
            private final TextView todayDate;
            private final TextView checkInButton;
            private final View checkedActions;
            private final TextView cancelCheckInButton;
            private final TextView[] weekDots;

            HabitViewHolder(@NonNull View itemView) {
                super(itemView);
                habitName = itemView.findViewById(R.id.habit_name);
                streakValue = itemView.findViewById(R.id.streak_value);
                targetSummary = itemView.findViewById(R.id.target_summary);
                todayDate = itemView.findViewById(R.id.today_date);
                checkInButton = itemView.findViewById(R.id.check_in_button);
                checkedActions = itemView.findViewById(R.id.checked_actions);
                cancelCheckInButton = itemView.findViewById(R.id.cancel_check_in_button);
                weekDots = new TextView[]{
                        itemView.findViewById(R.id.week_dot_monday),
                        itemView.findViewById(R.id.week_dot_tuesday),
                        itemView.findViewById(R.id.week_dot_wednesday),
                        itemView.findViewById(R.id.week_dot_thursday),
                        itemView.findViewById(R.id.week_dot_friday),
                        itemView.findViewById(R.id.week_dot_saturday),
                        itemView.findViewById(R.id.week_dot_sunday)
                };
            }

            void bind(Habit habit) {
                int primaryColor = getColor(R.color.stellog_primary);
                boolean checkedInToday = hasCheckedInToday(habit);

                habitName.setText(habit.name);
                streakValue.setText(String.valueOf(habit.recordNum));
                streakValue.setTextColor(primaryColor);
                targetSummary.setText(getString(
                        R.string.habit_target_summary,
                        DEFAULT_CHECK_IN_VALUE,
                        habit.unit,
                        habit.totalValue,
                        habit.unit
                ));
                todayDate.setText(getString(R.string.today_date_format, "2026-05-11"));
                todayDate.setTextColor(primaryColor);

                bindWeekDots(habit.id);

                checkInButton.setVisibility(checkedInToday ? View.GONE : View.VISIBLE);
                checkedActions.setVisibility(checkedInToday ? View.VISIBLE : View.GONE);
                checkInButton.setOnClickListener(v -> checkInToday(habit));
                cancelCheckInButton.setOnClickListener(v -> cancelTodayCheckIn(habit));
            }

            private void bindWeekDots(long habitId) {
                List<CheckInRecord.RecordDate> weekDates = getCurrentWeekDates();
                for (int i = 0; i < weekDots.length; i++) {
                    boolean checked = hasRecordOnDate(habitId, weekDates.get(i));
                    weekDots[i].setBackgroundResource(checked ? R.drawable.bg_circle_green : R.drawable.bg_circle_outline);
                    weekDots[i].setText(checked ? "✓" : "");
                    weekDots[i].setTextColor(getColor(R.color.white));
                }
            }
        }
    }
}
