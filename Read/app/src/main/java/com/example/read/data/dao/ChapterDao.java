package com.example.read.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.read.data.entity.ChapterEntity;

import java.util.List;

/**
 * 章节数据访问对象 - 提供章节表的CRUD操作
 */
@Dao
public interface ChapterDao {

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterIndex")
    LiveData<List<ChapterEntity>> getChaptersByNovelId(long novelId);

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterIndex")
    List<ChapterEntity> getChaptersByNovelIdSync(long novelId);

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    ChapterEntity getChapterById(long chapterId);

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

    @Query("UPDATE chapters SET summary = :summary WHERE id = :chapterId")
    void updateChapterSummary(long chapterId, String summary);

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND chapterIndex = :index")
    ChapterEntity getChapterByIndex(long novelId, int index);
}
