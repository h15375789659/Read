package com.example.read.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.read.domain.model.NovelReadingStats;
import com.example.read.domain.model.ReadingStatistics;
import com.example.read.domain.model.StatisticsPeriod;

import java.util.List;

/**
 * 阅读统计仓库接口 - 定义阅读统计相关操作
 * 
 * 验证需求：12.1, 12.5, 12.6
 */
public interface StatisticsRepository {
    
    /**
     * 记录阅读会话
     * 验证需求：12.1 - 记录阅读时长到本地存储
     * 
     * @param novelId 小说ID
     * @param duration 阅读时长（毫秒）
     * @param charCount 阅读字数
     */
    void recordReadingSession(long novelId, long duration, int charCount);
    
    /**
     * 按周期获取统计数据
     * 验证需求：12.6 - 切换统计周期（日、周、月）更新显示对应周期的数据
     * 
     * @param period 统计周期
     * @return 统计数据列表（按日期分组）
     */
    LiveData<List<ReadingStatistics>> getStatisticsByPeriod(StatisticsPeriod period);
    
    /**
     * 同步获取按周期的统计数据
     * 
     * @param period 统计周期
     * @return 统计数据列表
     */
    List<ReadingStatistics> getStatisticsByPeriodSync(StatisticsPeriod period);
    
    /**
     * 获取指定周期的总阅读时长
     * 
     * @param period 统计周期
     * @return 总阅读时长（毫秒）
     */
    long getTotalReadingDuration(StatisticsPeriod period);
    
    /**
     * 获取指定周期的总阅读字数
     * 
     * @param period 统计周期
     * @return 总阅读字数
     */
    long getTotalReadingCharCount(StatisticsPeriod period);
    
    /**
     * 获取阅读最多的小说排行
     * 验证需求：12.5 - 列出阅读最多的小说排行
     * 
     * @param limit 返回数量限制
     * @return 小说阅读统计列表（按阅读时长降序）
     */
    List<NovelReadingStats> getMostReadNovels(int limit);
    
    /**
     * 获取指定周期内阅读最多的小说排行
     * 
     * @param period 统计周期
     * @param limit 返回数量限制
     * @return 小说阅读统计列表（按阅读时长降序）
     */
    List<NovelReadingStats> getMostReadNovels(StatisticsPeriod period, int limit);
}
