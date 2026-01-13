package com.example.read.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.read.data.dao.BlockedWordDao;
import com.example.read.data.entity.BlockedWordEntity;
import com.example.read.domain.mapper.BlockedWordMapper;
import com.example.read.domain.model.BlockedWord;
import com.example.read.domain.repository.BlockedWordRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 屏蔽词仓库实现类
 * 支持按小说ID管理屏蔽词
 */
@Singleton
public class BlockedWordRepositoryImpl implements BlockedWordRepository {

    private final BlockedWordDao blockedWordDao;

    @Inject
    public BlockedWordRepositoryImpl(BlockedWordDao blockedWordDao) {
        this.blockedWordDao = blockedWordDao;
    }

    // ==================== 按小说ID操作（主要使用） ====================

    @Override
    public LiveData<List<BlockedWord>> getBlockedWordsByNovelId(long novelId) {
        return Transformations.map(blockedWordDao.getBlockedWordsByNovelId(novelId), BlockedWordMapper::toDomainList);
    }

    @Override
    public List<BlockedWord> getBlockedWordsByNovelIdSync(long novelId) {
        return BlockedWordMapper.toDomainList(blockedWordDao.getBlockedWordsByNovelIdSync(novelId));
    }

    @Override
    public List<String> getBlockedWordStringsByNovelId(long novelId) {
        return blockedWordDao.getBlockedWordStringsByNovelId(novelId);
    }

    @Override
    public long insertBlockedWord(long novelId, String word) {
        if (word == null || word.trim().isEmpty()) {
            return -1;
        }
        BlockedWordEntity entity = new BlockedWordEntity(novelId, word.trim());
        return blockedWordDao.insertBlockedWord(entity);
    }

    @Override
    public int getBlockedWordCountByNovelId(long novelId) {
        return blockedWordDao.getBlockedWordCountByNovelId(novelId);
    }

    @Override
    public void deleteBlockedWordsByNovelId(long novelId) {
        blockedWordDao.deleteBlockedWordsByNovelId(novelId);
    }

    // ==================== 通用操作 ====================

    @Override
    public LiveData<List<BlockedWord>> getAllBlockedWords() {
        return Transformations.map(blockedWordDao.getAllBlockedWords(), BlockedWordMapper::toDomainList);
    }

    @Override
    public List<BlockedWord> getAllBlockedWordsSync() {
        return BlockedWordMapper.toDomainList(blockedWordDao.getAllBlockedWordsSync());
    }

    @Override
    public List<String> getAllBlockedWordStrings() {
        return blockedWordDao.getAllBlockedWordStrings();
    }

    @Override
    public BlockedWord getBlockedWordById(long wordId) {
        BlockedWordEntity entity = blockedWordDao.getBlockedWordById(wordId);
        return BlockedWordMapper.toDomain(entity);
    }

    @Override
    @Deprecated
    public long insertBlockedWord(String word) {
        // 已废弃，默认使用 novelId = 0（不推荐）
        if (word == null || word.trim().isEmpty()) {
            return -1;
        }
        BlockedWordEntity entity = new BlockedWordEntity(0, word.trim());
        return blockedWordDao.insertBlockedWord(entity);
    }

    @Override
    public void deleteBlockedWord(long wordId) {
        blockedWordDao.deleteBlockedWordById(wordId);
    }

    @Override
    public void deleteAllBlockedWords() {
        blockedWordDao.deleteAllBlockedWords();
    }

    @Override
    public int getBlockedWordCount() {
        return blockedWordDao.getBlockedWordCount();
    }

    @Override
    public String applyBlockedWords(String text, List<String> blockedWords) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        if (blockedWords == null || blockedWords.isEmpty()) {
            return text;
        }
        
        String result = text;
        for (String word : blockedWords) {
            if (word != null && !word.isEmpty()) {
                // 直接删除屏蔽词（用空字符串替换）
                result = result.replace(word, "");
            }
        }
        
        return result;
    }
}
