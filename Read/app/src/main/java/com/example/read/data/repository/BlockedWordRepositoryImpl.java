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
 */
@Singleton
public class BlockedWordRepositoryImpl implements BlockedWordRepository {

    private final BlockedWordDao blockedWordDao;

    @Inject
    public BlockedWordRepositoryImpl(BlockedWordDao blockedWordDao) {
        this.blockedWordDao = blockedWordDao;
    }

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
    public long insertBlockedWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return -1;
        }
        BlockedWordEntity entity = new BlockedWordEntity(word.trim());
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
                String replacement = generateStars(word.length());
                result = result.replace(word, replacement);
            }
        }
        
        return result;
    }

    /**
     * 生成指定长度的星号字符串
     */
    private String generateStars(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("*");
        }
        return sb.toString();
    }
}
