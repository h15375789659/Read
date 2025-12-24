package com.example.read.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 阅读统计实体类 - 存储用户阅读统计数据
 */
@Entity(tableName = "reading_statistics")
public class ReadingStatisticsEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long date; // 日期时间戳（精确到天）
    private long novelId;
    private long readingDuration; // 阅读时长（毫秒）
    private int readingCharCount; // 阅读字数
    private int hourOfDay; // 阅读时段（0-23）

    public ReadingStatisticsEntity(long date, long novelId, long readingDuration, int readingCharCount, int hourOfDay) {
        this.date = date;
        this.novelId = novelId;
        this.readingDuration = readingDuration;
        this.readingCharCount = readingCharCount;
        this.hourOfDay = hourOfDay;
    }

    // Getters
    public long getId() { return id; }
    public long getDate() { return date; }
    public long getNovelId() { return novelId; }
    public long getReadingDuration() { return readingDuration; }
    public int getReadingCharCount() { return readingCharCount; }
    public int getHourOfDay() { return hourOfDay; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setDate(long date) { this.date = date; }
    public void setNovelId(long novelId) { this.novelId = novelId; }
    public void setReadingDuration(long readingDuration) { this.readingDuration = readingDuration; }
    public void setReadingCharCount(int readingCharCount) { this.readingCharCount = readingCharCount; }
    public void setHourOfDay(int hourOfDay) { this.hourOfDay = hourOfDay; }
}
