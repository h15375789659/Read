package com.example.read.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.read.data.dao.BookmarkDao;
import com.example.read.data.entity.BookmarkEntity;
import com.example.read.domain.mapper.BookmarkMapper;
import com.example.read.domain.model.Bookmark;
import com.example.read.domain.repository.BookmarkRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 书签仓库实现类
 * 实现书签的CRUD操作和跳转逻辑
 */
@Singleton
public class BookmarkRepositoryImpl implements BookmarkRepository {

    private final BookmarkDao bookmarkDao;

    @Inject
    public BookmarkRepositoryImpl(BookmarkDao bookmarkDao) {
        this.bookmarkDao = bookmarkDao;
    }

    @Override
    public LiveData<List<Bookmark>> getBookmarksByNovelId(long novelId) {
        return Transformations.map(
            bookmarkDao.getBookmarksByNovelId(novelId),
            BookmarkMapper::toDomainList
        );
    }

    @Override
    public List<Bookmark> getBookmarksByNovelIdSync(long novelId) {
        // 需要在BookmarkDao中添加同步方法
        List<BookmarkEntity> entities = bookmarkDao.getBookmarksByNovelIdSync(novelId);
        return BookmarkMapper.toDomainList(entities);
    }

    @Override
    public Bookmark getBookmarkById(long bookmarkId) {
        BookmarkEntity entity = bookmarkDao.getBookmarkById(bookmarkId);
        return BookmarkMapper.toDomain(entity);
    }

    /**
     * 插入书签
     * 验证需求：7.1 - 保存当前章节和段落位置
     * 验证需求：7.2 - 允许用户为书签添加备注文字
     * 
     * @param bookmark 书签对象，应包含novelId, chapterId, chapterTitle, position, 可选note
     * @return 新插入书签的ID，如果bookmark为null则返回-1
     */
    @Override
    public long insertBookmark(Bookmark bookmark) {
        if (bookmark == null) {
            return -1;
        }
        
        // 验证必需字段
        if (bookmark.getChapterTitle() == null || bookmark.getChapterTitle().isEmpty()) {
            return -1;
        }
        
        BookmarkEntity entity = BookmarkMapper.toEntity(bookmark);
        // 确保createTime被设置
        if (entity.getCreateTime() == 0) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        
        return bookmarkDao.insertBookmark(entity);
    }

    /**
     * 删除书签
     * 验证需求：7.5 - 从本地存储中移除该书签记录
     * 
     * @param bookmarkId 书签ID
     */
    @Override
    public void deleteBookmark(long bookmarkId) {
        bookmarkDao.deleteBookmarkById(bookmarkId);
    }

    @Override
    public void deleteBookmarksByNovelId(long novelId) {
        bookmarkDao.deleteBookmarksByNovelId(novelId);
    }

    @Override
    public int getBookmarkCount(long novelId) {
        return bookmarkDao.getBookmarkCount(novelId);
    }

    /**
     * 书签跳转 - 获取书签对应的章节ID和位置
     * 验证需求：7.4 - 跳转到该书签对应的位置
     * 
     * 返回的Bookmark对象包含chapterId和position，
     * 调用方可以使用这些信息来定位到正确的阅读位置
     * 
     * @param bookmarkId 书签ID
     * @return 书签对象，不存在则返回null
     */
    @Override
    public Bookmark getBookmarkForJump(long bookmarkId) {
        BookmarkEntity entity = bookmarkDao.getBookmarkById(bookmarkId);
        if (entity == null) {
            return null;
        }
        return BookmarkMapper.toDomain(entity);
    }
}
