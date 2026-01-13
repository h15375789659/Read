package com.example.read.presentation.statistics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.read.domain.model.NovelReadingStats;
import com.example.read.domain.model.ReadingStatistics;
import com.example.read.domain.model.StatisticsPeriod;
import com.example.read.domain.repository.StatisticsRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 统计ViewModel - 管理统计界面的业务逻辑和UI状态
 * 
 * 验证需求：12.2, 12.6
 */
@HiltViewModel
public class StatisticsViewModel extends ViewModel {

    private static final int RANKING_LIMIT = 10; // 排行榜显示数量

    private final StatisticsRepository statisticsRepository;
    private final ExecutorService executorService;

    // UI状态
    private final MutableLiveData<StatisticsUiState> _uiState = new MutableLiveData<>(new StatisticsUiState());
    public LiveData<StatisticsUiState> getUiState() { return _uiState; }

    // 当前周期的统计数据观察者
    private LiveData<List<ReadingStatistics>> currentStatisticsSource;
    private Observer<List<ReadingStatistics>> statisticsObserver;

    @Inject
    public StatisticsViewModel(StatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
        this.executorService = Executors.newSingleThreadExecutor();

        // 初始化加载（默认周视图）
        loadStatistics(StatisticsPeriod.WEEK);
    }

    /**
     * 加载统计数据
     * 验证需求：12.2 - 显示总阅读时长、总阅读字数
     * 
     * @param period 统计周期
     */
    public void loadStatistics(StatisticsPeriod period) {
        updateState(state -> {
            state.setLoading(true);
            state.setCurrentPeriod(period);
            state.setError(null);
        });

        // 移除旧的观察者
        if (currentStatisticsSource != null && statisticsObserver != null) {
            currentStatisticsSource.removeObserver(statisticsObserver);
        }

        // 设置新的数据源
        currentStatisticsSource = statisticsRepository.getStatisticsByPeriod(period);
        
        statisticsObserver = statistics -> {
            executorService.execute(() -> {
                try {
                    // 获取总计数据
                    long totalDuration = statisticsRepository.getTotalReadingDuration(period);
                    long totalCharCount = statisticsRepository.getTotalReadingCharCount(period);

                    // 获取小说排行
                    List<NovelReadingStats> ranking = statisticsRepository.getMostReadNovels(period, RANKING_LIMIT);

                    // 计算时段分布
                    Map<Integer, Long> hourDistribution = calculateHourDistribution(statistics);

                    // 准备图表数据
                    List<StatisticsUiState.ChartEntry> durationChartData = prepareDurationChartData(statistics, period);
                    List<StatisticsUiState.ChartEntry> hourChartData = prepareHourChartData(hourDistribution);

                    // 更新UI状态
                    updateState(state -> {
                        state.setTotalDuration(totalDuration);
                        state.setTotalCharCount(totalCharCount);
                        state.setDailyStatistics(statistics != null ? statistics : new ArrayList<>());
                        state.setHourDistribution(hourDistribution);
                        state.setNovelRanking(ranking != null ? ranking : new ArrayList<>());
                        state.setDurationChartData(durationChartData);
                        state.setHourChartData(hourChartData);
                        state.setLoading(false);
                    });
                } catch (Exception e) {
                    updateState(state -> {
                        state.setLoading(false);
                        state.setError("加载统计数据失败: " + e.getMessage());
                    });
                }
            });
        };

        currentStatisticsSource.observeForever(statisticsObserver);
    }

    /**
     * 切换统计周期
     * 验证需求：12.6 - 切换统计周期（日、周、月）更新显示对应周期的数据
     * 
     * @param period 新的统计周期
     */
    public void switchPeriod(StatisticsPeriod period) {
        StatisticsUiState currentState = _uiState.getValue();
        if (currentState != null && currentState.getCurrentPeriod() == period) {
            return; // 周期未变化，无需重新加载
        }
        loadStatistics(period);
    }

    /**
     * 切换到日视图
     */
    public void switchToDay() {
        switchPeriod(StatisticsPeriod.DAY);
    }

    /**
     * 切换到周视图
     */
    public void switchToWeek() {
        switchPeriod(StatisticsPeriod.WEEK);
    }

    /**
     * 切换到月视图
     */
    public void switchToMonth() {
        switchPeriod(StatisticsPeriod.MONTH);
    }

    /**
     * 刷新数据
     */
    public void refresh() {
        StatisticsUiState currentState = _uiState.getValue();
        StatisticsPeriod period = currentState != null ? currentState.getCurrentPeriod() : StatisticsPeriod.WEEK;
        loadStatistics(period);
    }

    /**
     * 计算时段分布
     * 验证需求：12.4 - 展示最常阅读的时间段分布
     */
    private Map<Integer, Long> calculateHourDistribution(List<ReadingStatistics> statistics) {
        Map<Integer, Long> distribution = new HashMap<>();
        
        // 初始化所有时段为0
        for (int i = 0; i < 24; i++) {
            distribution.put(i, 0L);
        }
        
        if (statistics == null || statistics.isEmpty()) {
            return distribution;
        }
        
        // 累加每个时段的阅读时长
        for (ReadingStatistics stat : statistics) {
            Map<Integer, Long> hourDist = stat.getHourDistribution();
            if (hourDist != null) {
                for (Map.Entry<Integer, Long> entry : hourDist.entrySet()) {
                    int hour = entry.getKey();
                    long duration = entry.getValue();
                    distribution.merge(hour, duration, Long::sum);
                }
            }
        }
        
        return distribution;
    }

    /**
     * 准备每日阅读时长图表数据
     * 验证需求：12.3 - 展示每日阅读时长的图表
     */
    private List<StatisticsUiState.ChartEntry> prepareDurationChartData(
            List<ReadingStatistics> statistics, StatisticsPeriod period) {
        List<StatisticsUiState.ChartEntry> chartData = new ArrayList<>();
        
        // 将统计数据转换为日期->时长的映射
        Map<Long, Long> dateToMinutes = new HashMap<>();
        if (statistics != null) {
            for (ReadingStatistics stat : statistics) {
                // 将毫秒转换为分钟
                long minutes = stat.getTotalDuration() / (1000 * 60);
                dateToMinutes.put(stat.getDate(), minutes);
            }
        }
        
        // 根据周期生成完整的日期列表
        Calendar calendar = Calendar.getInstance();
        List<Long> dateList = new ArrayList<>();
        List<String> labelList = new ArrayList<>();
        
        switch (period) {
            case DAY:
                // 日视图：显示今天的24小时（暂时保持原逻辑）
                if (statistics != null && !statistics.isEmpty()) {
                    int index = 0;
                    for (ReadingStatistics stat : statistics) {
                        String label = new SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(new Date(stat.getDate()));
                        float durationMinutes = stat.getTotalDuration() / (1000f * 60f);
                        chartData.add(new StatisticsUiState.ChartEntry(index++, durationMinutes, label));
                    }
                }
                return chartData;
                
            case WEEK:
                // 周视图：显示过去7天，使用中文星期
                String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
                for (int i = 6; i >= 0; i--) {
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    calendar.add(Calendar.DAY_OF_YEAR, -i);
                    // 获取当天开始时间戳
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    
                    dateList.add(calendar.getTimeInMillis());
                    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0=周日, 1=周一...
                    labelList.add(weekDays[dayOfWeek]);
                }
                break;
                
            case MONTH:
            default:
                // 月视图：显示过去30天
                SimpleDateFormat monthFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
                for (int i = 29; i >= 0; i--) {
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    calendar.add(Calendar.DAY_OF_YEAR, -i);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    
                    dateList.add(calendar.getTimeInMillis());
                    labelList.add(monthFormat.format(calendar.getTime()));
                }
                break;
        }
        
        // 生成图表数据，没有数据的日期显示0
        for (int i = 0; i < dateList.size(); i++) {
            long date = dateList.get(i);
            String label = labelList.get(i);
            long minutes = dateToMinutes.getOrDefault(date, 0L);
            chartData.add(new StatisticsUiState.ChartEntry(i, minutes, label));
        }
        
        return chartData;
    }

    /**
     * 准备时段分布图表数据
     * 验证需求：12.4 - 展示最常阅读的时间段分布
     */
    private List<StatisticsUiState.ChartEntry> prepareHourChartData(Map<Integer, Long> hourDistribution) {
        List<StatisticsUiState.ChartEntry> chartData = new ArrayList<>();
        
        if (hourDistribution == null || hourDistribution.isEmpty()) {
            return chartData;
        }
        
        for (int hour = 0; hour < 24; hour++) {
            long duration = hourDistribution.getOrDefault(hour, 0L);
            // 将毫秒转换为分钟
            float durationMinutes = duration / (1000f * 60f);
            String label = String.format(Locale.getDefault(), "%02d:00", hour);
            chartData.add(new StatisticsUiState.ChartEntry(hour, durationMinutes, label));
        }
        
        return chartData;
    }

    /**
     * 获取当前周期的显示名称
     */
    public String getPeriodDisplayName(StatisticsPeriod period) {
        if (period == null) return "周";
        switch (period) {
            case DAY:
                return "日";
            case WEEK:
                return "周";
            case MONTH:
                return "月";
            default:
                return "周";
        }
    }

    /**
     * 清除错误信息
     */
    public void clearError() {
        updateState(state -> state.setError(null));
    }

    /**
     * 生成测试数据（仅用于调试）
     */
    public void generateTestData() {
        executorService.execute(() -> {
            try {
                statisticsRepository.generateTestData();
                // 生成完成后刷新数据
                StatisticsUiState currentState = _uiState.getValue();
                StatisticsPeriod period = currentState != null ? currentState.getCurrentPeriod() : StatisticsPeriod.WEEK;
                loadStatistics(period);
            } catch (Exception e) {
                updateState(state -> state.setError("生成测试数据失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 更新UI状态的辅助方法
     */
    private void updateState(StateUpdater updater) {
        StatisticsUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new StatisticsUiState();
        }
        StatisticsUiState newState = new StatisticsUiState(currentState);
        updater.update(newState);
        _uiState.postValue(newState);
    }

    /**
     * 状态更新接口
     */
    private interface StateUpdater {
        void update(StatisticsUiState state);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        
        // 移除观察者
        if (currentStatisticsSource != null && statisticsObserver != null) {
            currentStatisticsSource.removeObserver(statisticsObserver);
        }
        
        // 关闭线程池
        executorService.shutdown();
    }
}
