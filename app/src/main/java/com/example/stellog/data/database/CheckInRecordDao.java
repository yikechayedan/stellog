package com.example.stellog.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

/**
 * check_in_records 表的数据访问接口。
 */
@Dao
public interface CheckInRecordDao {
    /**
     * 查询某个习惯在指定日期是否已有打卡记录。
     */
    @Query("SELECT * FROM check_in_records WHERE habitId = :habitId AND year = :year AND month = :month AND day = :day LIMIT 1")
    CheckInRecordEntity findOnDate(long habitId, int year, int month, int day);

    /**
     * 生成下一条打卡记录 id。
     */
    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM check_in_records")
    long nextId();

    /**
     * 插入新打卡记录。
     */
    @Insert
    void insert(CheckInRecordEntity record);

    /**
     * 更新打卡记录，例如记录详情页修改完成数量。
     */
    @Update
    void update(CheckInRecordEntity record);

    /**
     * 删除打卡记录，例如取消今日打卡。
     */
    @Delete
    void delete(CheckInRecordEntity record);
}
