package com.example.read.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.read.domain.model.BlockedWord;

import java.util.List;

/**
 * 屏蔽词仓库接口 - 定义屏蔽词相关的数据操作
 */
public interface BlockedWordRepository {

    /**
     * 获取所有屏蔽词（按创建时间降序）
     */
    LiveData<List<BlockedWord>> getAllBlockedWords();

    /**
     * 同步获取所有屏蔽词
     */
    List<BlockedWord> getAllBlockedWordsSync();

    /**
     * 获取所有屏蔽词字符串列表
     */
    List<String> getAllBlockedWordStrings();

    /**
     * 根据ID获取屏蔽词
     */
    BlockedWord getBlockedWordById(long wordId);

    /**
     * 添加屏蔽词
     * @param word 屏蔽词字符串
     * @return 新插入屏蔽词的ID
     */
    long insertBlockedWord(String word);

    /**
     * 删除屏蔽词
     */
    void deleteBlockedWord(long wordId);

    /**
     * 删除所有屏蔽词
     */
    void deleteAllBlockedWords();

    /**
     * 获取屏蔽词数量
     */
    int getBlockedWordCount();

    /**
     * 应用屏蔽词过滤文本
     * @param text 原始文本
     * @param blockedWords 屏蔽词列表
     * @return 过滤后的文本（屏蔽词被替换为星号）
     */
    String applyBlockedWords(String text, List<String> blockedWords);
}
