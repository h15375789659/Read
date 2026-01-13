package com.example.read.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.read.data.entity.BlockedWordEntity;

import java.util.List;

/**
 * 屏蔽词数据访问对象 - 提供屏蔽词表的CRUD操作
 * 支持按小说ID查询屏蔽词
 */
@Dao
public interface BlockedWordDao {

    // ==================== 按小说ID查询（新增） ====================

    /**
     * 获取指定小说的所有屏蔽词（按创建时间降序）
     */
    @Query("SELECT * FROM blocked_words WHERE novelId = :novelId ORDER BY createTime DESC")
    LiveData<List<BlockedWordEntity>> getBlockedWordsByNovelId(long novelId);

    /**
     * 同步获取指定小说的所有屏蔽词
     */
    @Query("SELECT * FROM blocked_words WHERE novelId = :novelId")
    List<BlockedWordEntity> getBlockedWordsByNovelIdSync(long novelId);

    /**
     * 获取指定小说的所有屏蔽词字符串列表
     */
    @Query("SELECT word FROM blocked_words WHERE novelId = :novelId")
    List<String> getBlockedWordStringsByNovelId(long novelId);

    /**
     * 获取指定小说的屏蔽词数量
     */
    @Query("SELECT COUNT(*) FROM blocked_words WHERE novelId = :novelId")
    int getBlockedWordCountByNovelId(long novelId);

    /**
     * 删除指定小说的所有屏蔽词
     */
    @Query("DELETE FROM blocked_words WHERE novelId = :novelId")
    void deleteBlockedWordsByNovelId(long novelId);

    // ==================== 通用查询（保留兼容） ====================

    @Query("SELECT * FROM blocked_words ORDER BY createTime DESC")
    LiveData<List<BlockedWordEntity>> getAllBlockedWords();

    @Query("SELECT * FROM blocked_words")
    List<BlockedWordEntity> getAllBlockedWordsSync();

    @Query("SELECT * FROM blocked_words WHERE id = :wordId")
    BlockedWordEntity getBlockedWordById(long wordId);

    @Insert
    long insertBlockedWord(BlockedWordEntity word);

    @Delete
    void deleteBlockedWord(BlockedWordEntity word);

    @Query("DELETE FROM blocked_words WHERE id = :wordId")
    void deleteBlockedWordById(long wordId);

    @Query("DELETE FROM blocked_words")
    void deleteAllBlockedWords();

    @Query("SELECT COUNT(*) FROM blocked_words")
    int getBlockedWordCount();

    @Query("SELECT word FROM blocked_words")
    List<String> getAllBlockedWordStrings();
}
