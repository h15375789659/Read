package com.example.read.presentation.statistics;

import com.example.read.domain.model.NovelReadingStats;
import com.example.read.domain.model.ReadingStatistics;
import com.example.read.domain.model.StatisticsPeriod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计界面UI状态
 * 
 * 验证需求：12.2, 12.6
 */
public class StatisticsUiState {

    // 当前选择的统计周期
    private StatisticsPeriod currentPeriod;

    // 总计数据
    private long totalDuration;      // 总阅读时长（毫秒）
    private long totalCharCount;     // 总阅读字数

    // 每日统计数据（用于图表）
    private List<ReadingStatistics> dailyStatistics;

    // 时段分布数据（用于图表）
    private Map<Integer, Long> hourDistribution;

    // 小说阅读排行
    private List<NovelReadingStats> novelRanking;

    // 图表数据（已处理，可直接用于MPAndroidChart）
    private List<ChartEntry> durationChartData;
    private List<ChartEntry> hourChartData;

    // 通用状态
    private boolean isLoading;
    private String error;

    public StatisticsUiState() {
        this.currentPeriod = StatisticsPeriod.WEEK;
        this.dailyStatistics = new ArrayList<>();
        this.hourDistribution = new HashMap<>();
        this.novelRanking = new ArrayList<>();
        this.durationChartData = new ArrayList<>();
        this.hourChartData = new ArrayList<>();
        this.isLoading = false;
    }

    /**
     * 复制构造函数
     */
    public StatisticsUiState(StatisticsUiState other) {
        this.currentPeriod = other.currentPeriod;
        this.totalDuration = other.totalDuration;
        this.totalCharCount = other.totalCharCount;
        this.dailyStatistics = new ArrayList<>(other.dailyStatistics);
        this.hourDistribution = new HashMap<>(other.hourDistribution);
        this.novelRanking = new ArrayList<>(other.novelRanking);
        this.durationChartData = new ArrayList<>(other.durationChartData);
        this.hourChartData = new ArrayList<>(other.hourChartData);
        this.isLoading = other.isLoading;
        this.error = other.error;
    }

    // ==================== Getters/Setters ====================

    public StatisticsPeriod getCurrentPeriod() { return currentPeriod; }
    public void setCurrentPeriod(StatisticsPeriod currentPeriod) { 
        this.currentPeriod = currentPeriod; 
    }

    public long getTotalDuration() { return totalDuration; }
    public void setTotalDuration(long totalDuration) { this.totalDuration = totalDuration; }

    public long getTotalCharCount() { return totalCharCount; }
    public void setTotalCharCount(long totalCharCount) { this.totalCharCount = totalCharCount; }

    public List<ReadingStatistics> getDailyStatistics() { return dailyStatistics; }
    public void setDailyStatistics(List<ReadingStatistics> dailyStatistics) { 
        this.dailyStatistics = dailyStatistics; 
    }

    public Map<Integer, Long> getHourDistribution() { return hourDistribution; }
    public void setHourDistribution(Map<Integer, Long> hourDistribution) { 
        this.hourDistribution = hourDistribution; 
    }

    public List<NovelReadingStats> getNovelRanking() { return novelRanking; }
    public void setNovelRanking(List<NovelReadingStats> novelRanking) { 
        this.novelRanking = novelRanking; 
    }

    public List<ChartEntry> getDurationChartData() { return durationChartData; }
    public void setDurationChartData(List<ChartEntry> durationChartData) { 
        this.durationChartData = durationChartData; 
    }

    public List<ChartEntry> getHourChartData() { return hourChartData; }
    public void setHourChartData(List<ChartEntry> hourChartData) { 
        this.hourChartData = hourChartData; 
    }

    public boolean isLoading() { return isLoading; }
    public void setLoading(boolean loading) { isLoading = loading; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    // ==================== 辅助方法 ====================

    /**
     * 获取格式化的总阅读时长
     */
    public String getFormattedTotalDuration() {
        long hours = totalDuration / (1000 * 60 * 60);
        long minutes = (totalDuration % (1000 * 60 * 60)) / (1000 * 60);
        
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        } else {
            return minutes + "分钟";
        }
    }

    /**
     * 获取格式化的总阅读字数
     */
    public String getFormattedTotalCharCount() {
        if (totalCharCount >= 10000) {
            return String.format("%.1f万字", totalCharCount / 10000.0);
        } else {
            return totalCharCount + "字";
        }
    }

    /**
     * 图表数据条目
     */
    public static class ChartEntry {
        private final float x;      // X轴值
        private final float y;      // Y轴值
        private final String label; // 标签

        public ChartEntry(float x, float y, String label) {
            this.x = x;
            this.y = y;
            this.label = label;
        }

        public float getX() { return x; }
        public float getY() { return y; }
        public String getLabel() { return label; }
    }
}
