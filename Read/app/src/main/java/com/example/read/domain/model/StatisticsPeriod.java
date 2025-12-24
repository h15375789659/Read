package com.example.read.domain.model;

import java.util.Calendar;

/**
 * 统计周期枚举
 */
public enum StatisticsPeriod {
    DAY(1),
    WEEK(7),
    MONTH(30);

    private final int days;

    StatisticsPeriod(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }

    /**
     * 获取周期开始时间戳
     */
    public long getStartTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1));
        return calendar.getTimeInMillis();
    }

    /**
     * 获取周期结束时间戳
     */
    public long getEndTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }
}
