package com.example.read.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.read.domain.model.Bookmark;

import java.util.List;

/**
 * 书签仓库接口 - 定义书签相关的数据操作
 */
public interface BookmarkRepository {
    
    /**
     * 获取指定小说的所有书签（LiveData）
     * @param novelId 小说ID
     * @return 书签列表的LiveData
     */
    LiveData<List<Bookmark>> getBookmarksByNovelId(long novelId);
    
    /**
     * 获取指定小说的所有书签（同步）
     * @param novelId 小说ID
     * @return 书签列表
     */
    List<Bookmark> getBookmarksByNovelIdSync(long novelId);
    
    /**
     * 根据ID获取书签
     * @param bookmarkId 书签ID
     * @return 书签对象，不存在则返回null
     */
    Bookmark getBookmarkById(long bookmarkId);
    
    /**
     * 插入书签
     * 验证需求：7.1, 7.2 - 保存当前章节和段落位置，允许添加备注
     * @param bookmark 书签对象
     * @return 新插入书签的ID
     */
    long insertBookmark(Bookmark bookmark);
    
    /**
     * 删除书签
     * 验证需求：7.5 - 从本地存储中移除书签记录
     * @param bookmarkId 书签ID
     */
    void deleteBookmark(long bookmarkId);
    
    /**
     * 删除指定小说的所有书签
     * @param novelId 小说ID
     */
    void deleteBookmarksByNovelId(long novelId);
    
    /**
     * 获取指定小说的书签数量
     * @param novelId 小说ID
     * @return 书签数量
     */
    int getBookmarkCount(long novelId);
    
    /**
     * 书签跳转 - 获取书签对应的章节ID和位置
     * 验证需求：7.4 - 跳转到书签对应的位置
     * @param bookmarkId 书签ID
     * @return 书签对象（包含chapterId和position），不存在则返回null
     */
    Bookmark getBookmarkForJump(long bookmarkId);
}
