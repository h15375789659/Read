package com.example.read.domain.mapper;

import com.example.read.data.entity.NovelEntity;
import com.example.read.domain.model.Novel;
import com.example.read.domain.model.NovelSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 小说实体与领域模型转换器
 */
public class NovelMapper {

    /**
     * Entity 转 Domain
     */
    public static Novel toDomain(NovelEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Novel novel = new Novel();
        novel.setId(entity.getId());
        novel.setTitle(entity.getTitle());
        novel.setAuthor(entity.getAuthor());
        novel.setDescription(entity.getDescription());
        novel.setCoverPath(entity.getCoverPath());
        novel.setSource(NovelSource.fromString(entity.getSource()));
        novel.setSourceUrl(entity.getSourceUrl());
        novel.setTotalChapters(entity.getTotalChapters());
        novel.setCurrentChapterId(entity.getCurrentChapterId());
        novel.setCurrentPosition(entity.getCurrentPosition());
        novel.setLastReadTime(entity.getLastReadTime());
        novel.setCreateTime(entity.getCreateTime());
        novel.setCategory(entity.getCategory());
        novel.setPinned(entity.isPinned());
        novel.setCurrentChapterTitle(entity.getCurrentChapterTitle());
        novel.setLatestChapterTitle(entity.getLatestChapterTitle());
        
        return novel;
    }

    /**
     * Domain 转 Entity
     */
    public static NovelEntity toEntity(Novel novel) {
        if (novel == null) {
            return null;
        }
        
        NovelEntity entity = new NovelEntity(novel.getTitle(), novel.getAuthor());
        entity.setId(novel.getId());
        entity.setDescription(novel.getDescription());
        entity.setCoverPath(novel.getCoverPath());
        entity.setSource(novel.getSource() != null ? novel.getSource().getValue() : "local");
        entity.setSourceUrl(novel.getSourceUrl());
        entity.setTotalChapters(novel.getTotalChapters());
        entity.setCurrentChapterId(novel.getCurrentChapterId());
        entity.setCurrentPosition(novel.getCurrentPosition());
        entity.setLastReadTime(novel.getLastReadTime());
        entity.setCreateTime(novel.getCreateTime());
        entity.setCategory(novel.getCategory() != null ? novel.getCategory() : "未分类");
        entity.setPinned(novel.isPinned());
        entity.setCurrentChapterTitle(novel.getCurrentChapterTitle());
        entity.setLatestChapterTitle(novel.getLatestChapterTitle());
        
        return entity;
    }

    /**
     * Entity 列表转 Domain 列表
     */
    public static List<Novel> toDomainList(List<NovelEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        
        List<Novel> novels = new ArrayList<>();
        for (NovelEntity entity : entities) {
            novels.add(toDomain(entity));
        }
        return novels;
    }

    /**
     * Domain 列表转 Entity 列表
     */
    public static List<NovelEntity> toEntityList(List<Novel> novels) {
        if (novels == null) {
            return new ArrayList<>();
        }
        
        List<NovelEntity> entities = new ArrayList<>();
        for (Novel novel : novels) {
            entities.add(toEntity(novel));
        }
        return entities;
    }
}
