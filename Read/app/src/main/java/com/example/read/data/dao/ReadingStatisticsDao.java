package com.example.read.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.read.data.entity.ReadingStatisticsEntity;

import java.util.List;

/**
 * 阅读统计数据访问对象 - 提供阅读统计表的CRUD操作
 */
@Dao
public interface ReadingStatisticsDao {

    @Query("SELECT * FROM reading_statistics WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<ReadingStatisticsEntity>> getStatisticsByDateRange(long startDate, long endDate);

    @Query("SELECT * FROM reading_statistics WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<ReadingStatisticsEntity> getStatisticsByDateRangeSync(long startDate, long endDate);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertStatistics(ReadingStatisticsEntity statistics);

    @Query("SELECT SUM(readingDuration) FROM reading_statistics WHERE date BETWEEN :startDate AND :endDate")
    Long getTotalReadingDuration(long startDate, long endDate);

    @Query("SELECT SUM(readingCharCount) FROM reading_statistics WHERE date BETWEEN :startDate AND :endDate")
    Long getTotalReadingCharCount(long startDate, long endDate);

    @Query("SELECT novelId, SUM(readingDuration) as totalDuration FROM reading_statistics " +
           "WHERE date BETWEEN :startDate AND :endDate " +
           "GROUP BY novelId ORDER BY totalDuration DESC LIMIT :limit")
    List<NovelReadingDuration> getMostReadNovels(long startDate, long endDate, int limit);

    @Query("SELECT hourOfDay, SUM(readingDuration) as totalDuration FROM reading_statistics " +
           "WHERE date BETWEEN :startDate AND :endDate " +
           "GROUP BY hourOfDay ORDER BY hourOfDay")
    List<HourlyReadingDuration> getHourlyDistribution(long startDate, long endDate);

    @Query("DELETE FROM reading_statistics WHERE date < :beforeDate")
    void deleteOldStatistics(long beforeDate);

    /**
     * 小说阅读时长统计结果
     */
    class NovelReadingDuration {
        public long novelId;
        public long totalDuration;
    }

    /**
     * 每小时阅读时长统计结果
     */
    class HourlyReadingDuration {
        public int hourOfDay;
        public long totalDuration;
    }
}
