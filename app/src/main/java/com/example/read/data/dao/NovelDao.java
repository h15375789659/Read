package com.example.read.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.read.data.entity.NovelEntity;

import java.util.List;

/**
 * 小说数据访问对象 - 提供小说表的CRUD操作
 */
@Dao
public interface NovelDao {
    
    @Query("SELECT * FROM novels ORDER BY isPinned DESC, lastReadTime DESC")
    LiveData<List<NovelEntity>> getAllNovels();

    @Query("SELECT * FROM novels WHERE id = :novelId")
    NovelEntity getNovelById(long novelId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNovel(NovelEntity novel);

    @Update
    void updateNovel(NovelEntity novel);

    @Delete
    void deleteNovel(NovelEntity novel);

    @Query("DELETE FROM novels WHERE id = :novelId")
    void deleteNovelById(long novelId);

    @Query("SELECT * FROM novels WHERE title LIKE '%' || :keyword || '%' OR author LIKE '%' || :keyword || '%'")
    LiveData<List<NovelEntity>> searchNovels(String keyword);

    @Query("SELECT * FROM novels WHERE category = :category AND category IS NOT NULL AND category != '' ORDER BY isPinned DESC, lastReadTime DESC")
    LiveData<List<NovelEntity>> getNovelsByCategory(String category);

    @Query("SELECT DISTINCT category FROM novels")
    LiveData<List<String>> getAllCategories();

    @Query("UPDATE novels SET currentChapterId = :chapterId, currentPosition = :position, lastReadTime = :lastReadTime WHERE id = :novelId")
    void updateReadingProgress(long novelId, long chapterId, int position, long lastReadTime);
    
    /**
     * 更新阅读进度（包含章节标题）
     */
    @Query("UPDATE novels SET currentChapterId = :chapterId, currentPosition = :position, " +
           "currentChapterTitle = :chapterTitle, lastReadTime = :lastReadTime WHERE id = :novelId")
    void updateReadingProgressWithTitle(long novelId, long chapterId, int position, String chapterTitle, long lastReadTime);
    
    /**
     * 更新最新章节标题
     */
    @Query("UPDATE novels SET latestChapterTitle = :title WHERE id = :novelId")
    void updateLatestChapterTitle(long novelId, String title);
    
    /**
     * 更新总章节数和最新章节标题
     */
    @Query("UPDATE novels SET totalChapters = :totalChapters, latestChapterTitle = :latestTitle WHERE id = :novelId")
    void updateChapterInfo(long novelId, int totalChapters, String latestTitle);

    @Query("UPDATE novels SET totalChapters = :totalChapters WHERE id = :novelId")
    void updateTotalChapters(long novelId, int totalChapters);

    @Query("UPDATE novels SET isPinned = :isPinned WHERE id = :novelId")
    void updatePinned(long novelId, boolean isPinned);
    
    @Query("UPDATE novels SET category = :newCategory WHERE category = :oldCategory")
    void updateCategoryToDefault(String oldCategory, String newCategory);
    
    @Query("UPDATE novels SET category = :category WHERE id = :novelId")
    void updateCategory(long novelId, String category);
    
    /**
     * 根据源URL查询小说（用于断点续传检测）
     */
    @Query("SELECT * FROM novels WHERE sourceUrl = :sourceUrl LIMIT 1")
    NovelEntity getNovelBySourceUrl(String sourceUrl);

}
