package com.example.stellog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Locale;

/**
 * 应用主页面。
 *
 * 当前页面负责展示活动卡片、处理卡片滑动、创建活动结果、今日打卡和取消打卡。
 */
public class MainActivity extends AppCompatActivity {

    // 当前版本中每次打卡默认增加 1，后续可以改成用户填写的数量。
    private static final long DEFAULT_CHECK_IN_VALUE = 0L;

    // 内存中的活动列表；后续接入数据库时可替换为持久化查询结果。
    private final List<Habit> habits = new ArrayList<>();

    // 内存中的打卡记录列表；卡片上的本周状态和今日状态都由它推导。
    private final List<CheckInRecord> records = new ArrayList<>();

    private ViewPager2 habitPager;
    private HabitPagerAdapter habitAdapter;
    private TextView pageIndicatorText;

    // 记录当前 ViewPager2 展示的是第几张卡片，便于打卡后只刷新当前页。
    private int currentHabitPosition = 0;

    // 接收“创建活动页面”返回的数据，并将其转成 Habit 加入列表。
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

    // 接收记录详细页面返回的新数值，并同步更新今日 record 与活动累计值。
    private final ActivityResultLauncher<Intent> recordDetailLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                            return;
                        }

                        long habitId = result.getData().getLongExtra("habit_id", -1L);
                        long newValue = result.getData().getLongExtra("record_value", DEFAULT_CHECK_IN_VALUE);
                        applyRecordDetailValue(habitId, newValue);
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 开启 EdgeToEdge 后，手动给根布局添加系统栏 padding，避免内容被遮挡。
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

    /**
     * 初始化活动卡片 ViewPager。
     *
     * ViewPager2 负责连续滑动效果，RecyclerView.Adapter 负责把 Habit 渲染成单张卡片。
     */
    private void setupHabitPager() {
        habitPager = findViewById(R.id.habit_pager);
        habitAdapter = new HabitPagerAdapter(habits);
        habitPager.setAdapter(habitAdapter);

        // 预渲染相邻卡片，让左右滑动时能看到连续过渡。
        habitPager.setOffscreenPageLimit(1);
        habitPager.setClipToPadding(false);
        habitPager.setClipChildren(false);

        // 左右 padding 控制卡片视觉宽度和两侧露出范围。
        habitPager.setPadding(128, 0, 128, 0);

        // 非当前卡片略微缩小、变淡，增强层次感。
        habitPager.setPageTransformer((page, position) -> {
            float scale = 0.94f + (1 - Math.min(Math.abs(position), 1f)) * 0.06f;
            page.setScaleY(scale);
            page.setAlpha(0.28f + (1 - Math.min(Math.abs(position), 1f)) * 0.72f);
        });
        habitPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentHabitPosition = position;
                updateHeader(position);
            }
        });
    }

    /**
     * 创建一个新活动并刷新卡片列表。
     */
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

        // 创建完成后自动滑到新活动卡片。
        habitPager.setCurrentItem(habits.size() - 1, true);
        updateHeader(habits.size() - 1);
    }

    /**
     * 更新右上角页码。
     */
    private void updateHeader(int position) {
        if (habits.isEmpty()) {
            pageIndicatorText.setText(getString(R.string.page_indicator_format, 0, 0));
            return;
        }
        pageIndicatorText.setText(getString(R.string.page_indicator_format, position + 1, habits.size()));
    }

    /**
     * 查找指定活动今天的打卡记录。
     */
    private CheckInRecord getTodayRecord(long habitId) {
        CheckInRecord.RecordDate today = CheckInRecord.RecordDate.today();
        for (CheckInRecord record : records) {
            if (record.habitId == habitId && record.date.isSameDay(today)) {
                return record;
            }
        }
        return null;
    }

    /**
     * 今日打卡：新增 record，并同步更新 Habit 上的统计字段。
     */
    private void checkInToday(Habit habit) {
        if (getTodayRecord(habit.id) != null) {
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

    /**
     * 取消今日打卡：删除今天的 record，并回退 Habit 上的统计字段。
     */
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

    /**
     * 判断指定活动在某一天是否已经打卡。
     */
    private boolean hasRecordOnDate(long habitId, CheckInRecord.RecordDate date) {
        for (CheckInRecord record : records) {
            if (record.habitId == habitId && record.date.isSameDay(date)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成本周周一到周日的日期列表，用于绑定 7 个打卡圆点。
     */
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

    /**
     * 生成卡片上显示的今日日期文本，例如 2026-05-11。
     */
    private String getTodayDateString() {
        CheckInRecord.RecordDate today = CheckInRecord.RecordDate.today();
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", today.year, today.month, today.day);
    }

    /**
     * 打开记录详细页面，让用户填写今天完成的数量。
     */
    private void showRecordDetailPage(Habit habit) {
        CheckInRecord todayRecord = getTodayRecord(habit.id);

        if (todayRecord == null) {
            Toast.makeText(this, "请先完成今日打卡", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, RecordDetailActivity.class);
        intent.putExtra("habit_id", habit.id);
        intent.putExtra("habit_name", habit.name);
        intent.putExtra("habit_unit", habit.unit);
        intent.putExtra("record_value", todayRecord.value);
        recordDetailLauncher.launch(intent);
    }

    private void applyRecordDetailValue(long habitId, long newValue) {
        int habitPosition = findHabitPosition(habitId);
        if (habitPosition < 0) {
            return;
        }

        Habit habit = habits.get(habitPosition);
        CheckInRecord todayRecord = getTodayRecord(habitId);
        if (todayRecord == null) {
            return;
        }

        long oldValue = todayRecord.value;
        if (newValue == oldValue) {
            return;
        }

        long now = System.currentTimeMillis();
        todayRecord.value = newValue;
        todayRecord.updatedAt = now;
        habit.totalValue = habit.totalValue - oldValue + newValue;
        habit.updatedAt = now;

        habitAdapter.notifyItemChanged(habitPosition);
    }

    private int findHabitPosition(long habitId) {
        for (int i = 0; i < habits.size(); i++) {
            if (habits.get(i).id == habitId) {
                return i;
            }
        }
        return -1;
    }



    /**
     * ViewPager2 使用的适配器，将 Habit 列表转换为卡片页。
     */
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

        /**
         * 单张卡片的 ViewHolder。
         *
         * 缓存控件引用，并负责把 Habit 和 CheckInRecord 状态渲染到 UI 上。
         */
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
                // 找到当前 habit 今天的打卡记录
                CheckInRecord todayRecord = getTodayRecord(habit.id);
                boolean checkedInToday = todayRecord != null;

                // 此日计显示今天这条 record 的 value，而不是默认值
                long todayValue = todayRecord == null ? 0 : todayRecord.value;

                habitName.setText(habit.name);
                streakValue.setText(String.valueOf(habit.recordNum));
                streakValue.setTextColor(primaryColor);
                targetSummary.setText(getString(
                        R.string.habit_target_summary,
                        todayValue,
                        habit.unit,
                        habit.totalValue,
                        habit.unit
                ));
                todayDate.setText(getString(R.string.today_date_format, getTodayDateString()));
                todayDate.setTextColor(primaryColor);

                bindWeekDots(habit.id);

                // 未打卡时显示“打卡”按钮；已打卡后显示“记录详细 / 取消”操作区。
                checkInButton.setVisibility(checkedInToday ? View.GONE : View.VISIBLE);
                checkedActions.setVisibility(checkedInToday ? View.VISIBLE : View.GONE);
                checkInButton.setOnClickListener(v -> checkInToday(habit));
                cancelCheckInButton.setOnClickListener(v -> cancelTodayCheckIn(habit));
                // 为“记录详细”按钮设置点击监听器
                itemView.findViewById(R.id.record_detail_button).setOnClickListener(v -> showRecordDetailPage(habit));
            }

            private void bindWeekDots(long habitId) {
                List<CheckInRecord.RecordDate> weekDates = getCurrentWeekDates();
                for (int i = 0; i < weekDots.length; i++) {
                    boolean checked = hasRecordOnDate(habitId, weekDates.get(i));
                    weekDots[i].setBackgroundResource(checked ? R.drawable.bg_circle_green : R.drawable.bg_circle_outline);
                    weekDots[i].setText(checked ? "\u2713" : "");
                    weekDots[i].setTextColor(getColor(R.color.white));
                }
            }
        }
    }
}
