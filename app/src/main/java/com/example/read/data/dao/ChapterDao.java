package com.example.read.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.read.data.entity.ChapterEntity;
import com.example.read.data.entity.ChapterInfo;

import java.util.List;

/**
 * 章节数据访问对象 - 提供章节表的CRUD操作
 */
@Dao
public interface ChapterDao {

    // ========== 章节列表查询（不含content，避免CursorWindow溢出）==========
    
    @Query("SELECT id, novelId, title, chapterIndex, wordCount, sourceUrl, summary, createTime FROM chapters WHERE novelId = :novelId ORDER BY chapterIndex")
    LiveData<List<ChapterInfo>> getChapterInfosByNovelId(long novelId);

    @Query("SELECT id, novelId, title, chapterIndex, wordCount, sourceUrl, summary, createTime FROM chapters WHERE novelId = :novelId ORDER BY chapterIndex")
    List<ChapterInfo> getChapterInfosByNovelIdSync(long novelId);
    
    // ========== 单章节查询（含content）==========

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    ChapterEntity getChapterById(long chapterId);
    
    @Query("SELECT content FROM chapters WHERE id = :chapterId")
    String getChapterContentById(long chapterId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChapters(List<ChapterEntity> chapters);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertChapter(ChapterEntity chapter);

    @Update
    void updateChapter(ChapterEntity chapter);

    @Query("DELETE FROM chapters WHERE novelId = :novelId")
    void deleteChaptersByNovelId(long novelId);

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND content LIKE '%' || :keyword || '%'")
    List<ChapterEntity> searchInChapters(long novelId, String keyword);

    @Query("SELECT COUNT(*) FROM chapters WHERE novelId = :novelId")
    int getChapterCount(long novelId);

    /**
     * 获取小说总字数
     */
    @Query("SELECT COALESCE(SUM(wordCount), 0) FROM chapters WHERE novelId = :novelId")
    long getTotalWordCount(long novelId);

    @Query("UPDATE chapters SET summary = :summary WHERE id = :chapterId")
    void updateChapterSummary(long chapterId, String summary);

    /**
     * 获取有摘要的章节列表（不含content）
     */
    @Query("SELECT id, novelId, title, chapterIndex, wordCount, sourceUrl, summary, createTime FROM chapters WHERE novelId = :novelId AND summary IS NOT NULL AND summary != '' ORDER BY chapterIndex")
    List<ChapterInfo> getChaptersWithSummarySync(long novelId);

    /**
     * 删除章节摘要
     */
    @Query("UPDATE chapters SET summary = NULL WHERE id = :chapterId")
    void deleteChapterSummary(long chapterId);

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND chapterIndex = :index")
    ChapterEntity getChapterByIndex(long novelId, int index);
}
