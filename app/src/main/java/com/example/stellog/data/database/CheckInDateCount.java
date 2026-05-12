package com.example.stellog.data.database;

/**
 * 日历页按日期聚合后的打卡数量。
 *
 * 这是 Room 查询结果使用的轻量对象，不对应单独的数据表。
 */
public class CheckInDateCount {
    public int dateKey;
    public int count;
}
