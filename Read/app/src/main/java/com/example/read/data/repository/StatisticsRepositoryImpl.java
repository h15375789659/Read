package com.example.read.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.read.data.dao.NovelDao;
import com.example.read.data.dao.ReadingStatisticsDao;
import com.example.read.data.entity.NovelEntity;
import com.example.read.data.entity.ReadingStatisticsEntity;
import com.example.read.domain.mapper.ReadingStatisticsMapper;
import com.example.read.domain.model.NovelReadingStats;
import com.example.read.domain.model.ReadingStatistics;
import com.example.read.domain.model.StatisticsPeriod;
import com.example.read.domain.repository.StatisticsRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 阅读统计仓库实现类
 * 
 * 实现阅读会话记录、统计数据查询和阅读排行功能
 * 
 * 验证需求：12.1, 12.5, 12.6
 */
@Singleton
public class StatisticsRepositoryImpl implements StatisticsRepository {

    private final ReadingStatisticsDao statisticsDao;
    private final NovelDao novelDao;

    @Inject
    public StatisticsRepositoryImpl(ReadingStatisticsDao statisticsDao, NovelDao novelDao) {
        this.statisticsDao = statisticsDao;
        this.novelDao = novelDao;
    }

    /**
     * 记录阅读会话
     * 验证需求：12.1 - 记录阅读时长到本地存储
     * 
     * 记录包含：日期、小说ID、阅读时长、阅读字数、阅读时段
     */
    @Override
    public void recordReadingSession(long novelId, long duration, int charCount) {
        // 验证参数
        if (duration <= 0) {
            return;
        }
        
        // 获取当前日期（精确到天）
        long dateTimestamp = getDayStartTimestamp(System.currentTimeMillis());
        
        // 获取当前小时（0-23）
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        
        // 创建统计实体
        ReadingStatisticsEntity entity = new ReadingStatisticsEntity(
                dateTimestamp,
                novelId,
                duration,
                charCount,
                hourOfDay
        );
        
        // 保存到数据库
        statisticsDao.insertStatistics(entity);
    }

    /**
     * 按周期获取统计数据
     * 验证需求：12.6 - 切换统计周期（日、周、月）更新显示对应周期的数据
     */
    @Override
    public LiveData<List<ReadingStatistics>> getStatisticsByPeriod(StatisticsPeriod period) {
        long startDate = period.getStartTimestamp();
        long endDate = period.getEndTimestamp();
        
        return Transformations.map(
                statisticsDao.getStatisticsByDateRange(startDate, endDate),
                ReadingStatisticsMapper::toDomainListGroupedByDate
        );
    }

    /**
     * 同步获取按周期的统计数据
     */
    @Override
    public List<ReadingStatistics> getStatisticsByPeriodSync(StatisticsPeriod period) {
        long startDate = period.getStartTimestamp();
        long endDate = period.getEndTimestamp();
        
        List<ReadingStatisticsEntity> entities = statisticsDao.getStatisticsByDateRangeSync(startDate, endDate);
        return ReadingStatisticsMapper.toDomainListGroupedByDate(entities);
    }

    /**
     * 获取指定周期的总阅读时长
     */
    @Override
    public long getTotalReadingDuration(StatisticsPeriod period) {
        long startDate = period.getStartTimestamp();
        long endDate = period.getEndTimestamp();
        
        Long total = statisticsDao.getTotalReadingDuration(startDate, endDate);
        return total != null ? total : 0L;
    }

    /**
     * 获取指定周期的总阅读字数
     */
    @Override
    public long getTotalReadingCharCount(StatisticsPeriod period) {
        long startDate = period.getStartTimestamp();
        long endDate = period.getEndTimestamp();
        
        Long total = statisticsDao.getTotalReadingCharCount(startDate, endDate);
        return total != null ? total : 0L;
    }

    /**
     * 获取阅读最多的小说排行（默认使用月周期）
     * 验证需求：12.5 - 列出阅读最多的小说排行
     */
    @Override
    public List<NovelReadingStats> getMostReadNovels(int limit) {
        return getMostReadNovels(StatisticsPeriod.MONTH, limit);
    }

    /**
     * 获取指定周期内阅读最多的小说排行
     * 验证需求：12.5 - 列出阅读最多的小说排行
     * 
     * 返回的列表按总阅读时长降序排列
     */
    @Override
    public List<NovelReadingStats> getMostReadNovels(StatisticsPeriod period, int limit) {
        long startDate = period.getStartTimestamp();
        long endDate = period.getEndTimestamp();
        
        // 从DAO获取按小说分组的阅读时长
        List<ReadingStatisticsDao.NovelReadingDuration> durations = 
                statisticsDao.getMostReadNovels(startDate, endDate, limit);
        
        if (durations == null || durations.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 转换为领域模型并填充小说信息
        List<NovelReadingStats> result = new ArrayList<>();
        for (ReadingStatisticsDao.NovelReadingDuration duration : durations) {
            NovelReadingStats stats = new NovelReadingStats(duration.novelId, duration.totalDuration);
            
            // 获取小说信息
            NovelEntity novel = novelDao.getNovelById(duration.novelId);
            if (novel != null) {
                stats.setNovelTitle(novel.getTitle());
                stats.setNovelAuthor(novel.getAuthor());
            } else {
                stats.setNovelTitle("未知小说");
                stats.setNovelAuthor("未知作者");
            }
            
            result.add(stats);
        }
        
        return result;
    }

    /**
     * 获取一天的开始时间戳（00:00:00.000）
     */
    private long getDayStartTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
