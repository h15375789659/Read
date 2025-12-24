package com.example.read.domain.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 阅读统计领域模型
 */
public class ReadingStatistics {
    private long date;
    private long totalDuration;
    private int totalCharCount;
    private Map<Integer, Long> hourDistribution; // 时段 -> 阅读时长

    public ReadingStatistics() {
        this.hourDistribution = new HashMap<>();
    }

    public ReadingStatistics(long date, long totalDuration, int totalCharCount) {
        this.date = date;
        this.totalDuration = totalDuration;
        this.totalCharCount = totalCharCount;
        this.hourDistribution = new HashMap<>();
    }

    // Getters
    public long getDate() { return date; }
    public long getTotalDuration() { return totalDuration; }
    public int getTotalCharCount() { return totalCharCount; }
    public Map<Integer, Long> getHourDistribution() { return hourDistribution; }

    // Setters
    public void setDate(long date) { this.date = date; }
    public void setTotalDuration(long totalDuration) { this.totalDuration = totalDuration; }
    public void setTotalCharCount(int totalCharCount) { this.totalCharCount = totalCharCount; }
    public void setHourDistribution(Map<Integer, Long> hourDistribution) { this.hourDistribution = hourDistribution; }

    /**
     * 添加时段阅读时长
     */
    public void addHourDuration(int hour, long duration) {
        long existing = hourDistribution.getOrDefault(hour, 0L);
        hourDistribution.put(hour, existing + duration);
    }

    /**
     * 获取格式化的阅读时长字符串
     */
    public String getFormattedDuration() {
        long hours = totalDuration / (1000 * 60 * 60);
        long minutes = (totalDuration % (1000 * 60 * 60)) / (1000 * 60);
        
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        } else {
            return minutes + "分钟";
        }
    }
}
