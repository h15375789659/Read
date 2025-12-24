package com.example.read.domain.mapper;

import com.example.read.data.entity.BlockedWordEntity;
import com.example.read.domain.model.BlockedWord;

import java.util.ArrayList;
import java.util.List;

/**
 * 屏蔽词实体与领域模型转换器
 */
public class BlockedWordMapper {

    /**
     * Entity 转 Domain
     */
    public static BlockedWord toDomain(BlockedWordEntity entity) {
        if (entity == null) {
            return null;
        }
        
        BlockedWord word = new BlockedWord();
        word.setId(entity.getId());
        word.setWord(entity.getWord());
        word.setCreateTime(entity.getCreateTime());
        
        return word;
    }

    /**
     * Domain 转 Entity
     */
    public static BlockedWordEntity toEntity(BlockedWord word) {
        if (word == null) {
            return null;
        }
        
        BlockedWordEntity entity = new BlockedWordEntity(word.getWord());
        entity.setId(word.getId());
        entity.setCreateTime(word.getCreateTime());
        
        return entity;
    }

    /**
     * Entity 列表转 Domain 列表
     */
    public static List<BlockedWord> toDomainList(List<BlockedWordEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        
        List<BlockedWord> words = new ArrayList<>();
        for (BlockedWordEntity entity : entities) {
            words.add(toDomain(entity));
        }
        return words;
    }

    /**
     * Domain 列表转 Entity 列表
     */
    public static List<BlockedWordEntity> toEntityList(List<BlockedWord> words) {
        if (words == null) {
            return new ArrayList<>();
        }
        
        List<BlockedWordEntity> entities = new ArrayList<>();
        for (BlockedWord word : words) {
            entities.add(toEntity(word));
        }
        return entities;
    }

    /**
     * 从 Domain 列表提取屏蔽词字符串列表
     */
    public static List<String> toWordList(List<BlockedWord> words) {
        if (words == null) {
            return new ArrayList<>();
        }
        
        List<String> wordStrings = new ArrayList<>();
        for (BlockedWord word : words) {
            if (word.getWord() != null && !word.getWord().isEmpty()) {
                wordStrings.add(word.getWord());
            }
        }
        return wordStrings;
    }
}
