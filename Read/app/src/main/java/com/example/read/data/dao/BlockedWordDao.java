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
 */
@Dao
public interface BlockedWordDao {

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
