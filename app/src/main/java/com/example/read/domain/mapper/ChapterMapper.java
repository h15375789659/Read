package com.example.read.domain.mapper;

import com.example.read.data.entity.ChapterEntity;
import com.example.read.domain.model.Chapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 章节实体与领域模型转换器
 */
public class ChapterMapper {

    /**
     * Entity 转 Domain
     */
    public static Chapter toDomain(ChapterEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Chapter chapter = new Chapter();
        chapter.setId(entity.getId());
        chapter.setNovelId(entity.getNovelId());
        chapter.setTitle(entity.getTitle());
        chapter.setContent(entity.getContent());
        chapter.setChapterIndex(entity.getChapterIndex());
        chapter.setWordCount(entity.getWordCount());
        chapter.setSourceUrl(entity.getSourceUrl());
        chapter.setSummary(entity.getSummary());
        chapter.setCreateTime(entity.getCreateTime());
        
        return chapter;
    }

    /**
     * Domain 转 Entity
     */
    public static ChapterEntity toEntity(Chapter chapter) {
        if (chapter == null) {
            return null;
        }
        
        ChapterEntity entity = new ChapterEntity(
            chapter.getNovelId(),
            chapter.getTitle(),
            chapter.getContent(),
            chapter.getChapterIndex()
        );
        entity.setId(chapter.getId());
        entity.setWordCount(chapter.getWordCount());
        entity.setSourceUrl(chapter.getSourceUrl());
        entity.setSummary(chapter.getSummary());
        entity.setCreateTime(chapter.getCreateTime());
        
        return entity;
    }

    /**
     * Entity 列表转 Domain 列表
     */
    public static List<Chapter> toDomainList(List<ChapterEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        
        List<Chapter> chapters = new ArrayList<>();
        for (ChapterEntity entity : entities) {
            chapters.add(toDomain(entity));
        }
        return chapters;
    }

    /**
     * Domain 列表转 Entity 列表
     */
    public static List<ChapterEntity> toEntityList(List<Chapter> chapters) {
        if (chapters == null) {
            return new ArrayList<>();
        }
        
        List<ChapterEntity> entities = new ArrayList<>();
        for (Chapter chapter : chapters) {
            entities.add(toEntity(chapter));
        }
        return entities;
    }
}
