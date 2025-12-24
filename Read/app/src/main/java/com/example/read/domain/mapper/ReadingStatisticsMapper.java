package com.example.read.domain.mapper;

import com.example.read.data.entity.ReadingStatisticsEntity;
import com.example.read.domain.model.ReadingStatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阅读统计实体与领域模型转换器
 */
public class ReadingStatisticsMapper {

    /**
     * Entity 转 Domain（单条记录）
     */
    public static ReadingStatistics toDomain(ReadingStatisticsEntity entity) {
        if (entity == null) {
            return null;
        }
        
        ReadingStatistics stats = new ReadingStatistics();
        stats.setDate(entity.getDate());
        stats.setTotalDuration(entity.getReadingDuration());
        stats.setTotalCharCount(entity.getReadingCharCount());
        stats.addHourDuration(entity.getHourOfDay(), entity.getReadingDuration());
        
        return stats;
    }

    /**
     * Domain 转 Entity
     */
    public static ReadingStatisticsEntity toEntity(ReadingStatistics stats, long novelId, int hourOfDay) {
        if (stats == null) {
            return null;
        }
        
        return new ReadingStatisticsEntity(
            stats.getDate(),
            novelId,
            stats.getTotalDuration(),
            stats.getTotalCharCount(),
            hourOfDay
        );
    }

    /**
     * Entity 列表聚合为按日期分组的 Domain 列表
     */
    public static List<ReadingStatistics> toDomainListGroupedByDate(List<ReadingStatisticsEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 按日期聚合
        Map<Long, ReadingStatistics> dateMap = new HashMap<>();
        
        for (ReadingStatisticsEntity entity : entities) {
            long date = entity.getDate();
            ReadingStatistics stats = dateMap.get(date);
            
            if (stats == null) {
                stats = new ReadingStatistics();
                stats.setDate(date);
                stats.setTotalDuration(0);
                stats.setTotalCharCount(0);
                dateMap.put(date, stats);
            }
            
            // 累加时长和字数
            stats.setTotalDuration(stats.getTotalDuration() + entity.getReadingDuration());
            stats.setTotalCharCount(stats.getTotalCharCount() + entity.getReadingCharCount());
            stats.addHourDuration(entity.getHourOfDay(), entity.getReadingDuration());
        }
        
        return new ArrayList<>(dateMap.values());
    }

    /**
     * 计算总阅读时长
     */
    public static long calculateTotalDuration(List<ReadingStatisticsEntity> entities) {
        if (entities == null) {
            return 0;
        }
        
        long total = 0;
        for (ReadingStatisticsEntity entity : entities) {
            total += entity.getReadingDuration();
        }
        return total;
    }

    /**
     * 计算总阅读字数
     */
    public static int calculateTotalCharCount(List<ReadingStatisticsEntity> entities) {
        if (entities == null) {
            return 0;
        }
        
        int total = 0;
        for (ReadingStatisticsEntity entity : entities) {
            total += entity.getReadingCharCount();
        }
        return total;
    }
}
