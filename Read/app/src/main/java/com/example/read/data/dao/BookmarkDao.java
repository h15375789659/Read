package com.example.read.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.read.data.entity.BookmarkEntity;

import java.util.List;

/**
 * 书签数据访问对象 - 提供书签表的CRUD操作
 */
@Dao
public interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE novelId = :novelId ORDER BY createTime DESC")
    LiveData<List<BookmarkEntity>> getBookmarksByNovelId(long novelId);

    @Query("SELECT * FROM bookmarks WHERE novelId = :novelId ORDER BY createTime DESC")
    List<BookmarkEntity> getBookmarksByNovelIdSync(long novelId);

    @Query("SELECT * FROM bookmarks WHERE id = :bookmarkId")
    BookmarkEntity getBookmarkById(long bookmarkId);

    @Insert
    long insertBookmark(BookmarkEntity bookmark);

    @Delete
    void deleteBookmark(BookmarkEntity bookmark);

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    void deleteBookmarkById(long bookmarkId);

    @Query("DELETE FROM bookmarks WHERE novelId = :novelId")
    void deleteBookmarksByNovelId(long novelId);

    @Query("SELECT COUNT(*) FROM bookmarks WHERE novelId = :novelId")
    int getBookmarkCount(long novelId);
}
