package com.example.read.domain.mapper;

import com.example.read.data.entity.BookmarkEntity;
import com.example.read.domain.model.Bookmark;

import java.util.ArrayList;
import java.util.List;

/**
 * 书签实体与领域模型转换器
 */
public class BookmarkMapper {

    /**
     * Entity 转 Domain
     */
    public static Bookmark toDomain(BookmarkEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Bookmark bookmark = new Bookmark();
        bookmark.setId(entity.getId());
        bookmark.setNovelId(entity.getNovelId());
        bookmark.setChapterId(entity.getChapterId());
        bookmark.setChapterTitle(entity.getChapterTitle());
        bookmark.setPosition(entity.getPosition());
        bookmark.setNote(entity.getNote());
        bookmark.setCreateTime(entity.getCreateTime());
        
        return bookmark;
    }

    /**
     * Domain 转 Entity
     */
    public static BookmarkEntity toEntity(Bookmark bookmark) {
        if (bookmark == null) {
            return null;
        }
        
        BookmarkEntity entity = new BookmarkEntity(
            bookmark.getNovelId(),
            bookmark.getChapterId(),
            bookmark.getChapterTitle(),
            bookmark.getPosition()
        );
        entity.setId(bookmark.getId());
        entity.setNote(bookmark.getNote());
        entity.setCreateTime(bookmark.getCreateTime());
        
        return entity;
    }

    /**
     * Entity 列表转 Domain 列表
     */
    public static List<Bookmark> toDomainList(List<BookmarkEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        
        List<Bookmark> bookmarks = new ArrayList<>();
        for (BookmarkEntity entity : entities) {
            bookmarks.add(toDomain(entity));
        }
        return bookmarks;
    }

    /**
     * Domain 列表转 Entity 列表
     */
    public static List<BookmarkEntity> toEntityList(List<Bookmark> bookmarks) {
        if (bookmarks == null) {
            return new ArrayList<>();
        }
        
        List<BookmarkEntity> entities = new ArrayList<>();
        for (Bookmark bookmark : bookmarks) {
            entities.add(toEntity(bookmark));
        }
        return entities;
    }
}
