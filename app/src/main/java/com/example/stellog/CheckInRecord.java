package com.example.stellog;

import java.util.Calendar;

public class CheckInRecord {
    public static final String SOURCE_NORMAL = "正常打卡";
    public static final String SOURCE_PATCH = "补打卡";

    public final long id;
    public final long habitId;
    public final long userId;
    public final RecordDate date;
    public final long value;
    public final String source;
    public final long createdAt;
    public final long updatedAt;

    public CheckInRecord(
            long id,
            long habitId,
            long userId,
            RecordDate date,
            long value,
            String source,
            long createdAt,
            long updatedAt
    ) {
        this.id = id;
        this.habitId = habitId;
        this.userId = userId;
        this.date = date;
        this.value = value;
        this.source = source;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static class RecordDate {
        public final int year;
        public final int month;
        public final int day;

        public RecordDate(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        public boolean isSameDay(RecordDate other) {
            return other != null
                    && year == other.year
                    && month == other.month
                    && day == other.day;
        }

        public static RecordDate today() {
            Calendar calendar = Calendar.getInstance();
            return fromCalendar(calendar);
        }

        public static RecordDate fromCalendar(Calendar calendar) {
            return new RecordDate(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
        }
    }
}
