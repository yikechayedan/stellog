package com.example.stellog.data.model;

import java.util.Calendar;

/**
 * 日历中单个日期格子的展示数据。
 */
public class CalendarDaySpec {
    public final Calendar date;
    public final String label;
    public final boolean today;
    public final boolean selected;
    public final boolean outsideMonth;
    public final int recordCount;

    public CalendarDaySpec(
            Calendar date,
            String label,
            boolean today,
            boolean selected,
            boolean outsideMonth,
            int recordCount
    ) {
        this.date = date;
        this.label = label;
        this.today = today;
        this.selected = selected;
        this.outsideMonth = outsideMonth;
        this.recordCount = recordCount;
    }
}
