package com.example.stellog;

public class Habit {
    public final long id;
    public final long userId;
    public final String name;
    public final String unit;
    public int recordNum;
    public final boolean reminderEnabled;
    public final int sortWeight;
    public long totalValue;
    public final long createdAt;
    public long updatedAt;

    public Habit(
            long id,
            long userId,
            String name,
            String unit,
            int recordNum,
            boolean reminderEnabled,
            int sortWeight,
            long totalValue,
            long createdAt,
            long updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.unit = unit;
        this.recordNum = recordNum;
        this.reminderEnabled = reminderEnabled;
        this.sortWeight = sortWeight;
        this.totalValue = totalValue;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
