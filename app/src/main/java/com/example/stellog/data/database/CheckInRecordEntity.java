package com.example.stellog.data.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.stellog.data.model.CheckInRecord;
import com.example.stellog.util.DateUtils;

/**
 * Room 中的打卡记录表结构。
 *
 * date 在业务模型中是 RecordDate；落库时拆成 year/month/day，另存 dateKey 便于范围查询和建立索引。
 */
@Entity(
        tableName = "check_in_records",
        indices = {
                @Index("habitId"),
                @Index("dateKey"),
                @Index(value = {"habitId", "dateKey"}, unique = true)
        }
)
public class CheckInRecordEntity {
    @PrimaryKey
    public long id;
    public long habitId;
    public long userId;
    public int year;
    public int month;
    public int day;
    public int dateKey;
    public long value;
    public String source;
    public long createdAt;
    public long updatedAt;

    /**
     * 将业务模型转换成数据库 Entity。
     */
    public static CheckInRecordEntity fromModel(CheckInRecord record) {
        CheckInRecordEntity entity = new CheckInRecordEntity();
        entity.id = record.id;
        entity.habitId = record.habitId;
        entity.userId = record.userId;
        entity.year = record.date.year;
        entity.month = record.date.month;
        entity.day = record.date.day;
        entity.dateKey = DateUtils.toDateKey(record.date);
        entity.value = record.value;
        entity.source = record.source;
        entity.createdAt = record.createdAt;
        entity.updatedAt = record.updatedAt;
        return entity;
    }

    /**
     * 将数据库 Entity 转回业务模型。
     */
    public CheckInRecord toModel() {
        return new CheckInRecord(
                id,
                habitId,
                userId,
                new CheckInRecord.RecordDate(year, month, day),
                value,
                source,
                createdAt,
                updatedAt
        );
    }
}
