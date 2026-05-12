package com.example.stellog.util;

import com.example.stellog.data.model.CheckInRecord;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 日期工具类。
 *
 * 当前主要服务于“今日打卡”和“本周打卡圆点”的日期计算。
 */
public final class DateUtils {
    private DateUtils() {
    }

    public static List<CheckInRecord.RecordDate> getCurrentWeekDates() {
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

    public static String getTodayDateString() {
        CheckInRecord.RecordDate today = CheckInRecord.RecordDate.today();
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", today.year, today.month, today.day);
    }

    /**
     * 生成按日期排序和范围查询使用的整数键，例如 2026-05-12 会变成 20260512。
     */
    public static int toDateKey(CheckInRecord.RecordDate date) {
        return toDateKey(date.year, date.month, date.day);
    }

    /**
     * Calendar 的月份从 0 开始，业务日期键需要转成 1 到 12。
     */
    public static int toDateKey(Calendar calendar) {
        return toDateKey(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    public static int toDateKey(int year, int month, int day) {
        return year * 10000 + month * 100 + day;
    }

    public static boolean isSameDate(Calendar left, Calendar right) {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR)
                && left.get(Calendar.MONTH) == right.get(Calendar.MONTH)
                && left.get(Calendar.DAY_OF_MONTH) == right.get(Calendar.DAY_OF_MONTH);
    }

    public static void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
