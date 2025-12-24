package com.example.read.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.read.domain.model.Chapter;
import com.example.read.domain.model.Novel;
import com.example.read.domain.model.SearchResult;

import java.util.List;

/**
 * 小说仓库接口 - 定义小说相关的数据操作
 */
public interface NovelRepository {
    
    /**
     * 获取所有小说（按置顶和最后阅读时间排序）
     */
    LiveData<List<Novel>> getAllNovels();
    
    /**
     * 根据ID获取小说
     */
    Novel getNovelById(long novelId);
    
    /**
     * 插入小说
     * @return 新插入小说的ID
     */
    long insertNovel(Novel novel);
    
    /**
     * 更新小说信息
     */
    void updateNovel(Novel novel);
    
    /**
     * 删除小说（级联删除所有章节）
     */
    void deleteNovel(long novelId);
    
    /**
     * 搜索小说（按标题或作者）
     */
    LiveData<List<Novel>> searchNovels(String keyword);
    
    /**
     * 按分类筛选小说
     */
    LiveData<List<Novel>> getNovelsByCategory(String category);
    
    /**
     * 获取所有分类
     */
    LiveData<List<String>> getAllCategories();
    
    /**
     * 获取小说的所有章节
     */
    LiveData<List<Chapter>> getChaptersByNovelId(long novelId);
    
    /**
     * 同步获取小说的所有章节
     */
    List<Chapter> getChaptersByNovelIdSync(long novelId);
    
    /**
     * 根据ID获取章节
     */
    Chapter getChapterById(long chapterId);
    
    /**
     * 根据索引获取章节
     */
    Chapter getChapterByIndex(long novelId, int index);
    
    /**
     * 插入章节列表
     */
    void insertChapters(long novelId, List<Chapter> chapters);
    
    /**
     * 更新阅读进度
     */
    void updateReadingProgress(long novelId, long chapterId, int position);
    
    /**
     * 在小说中搜索关键词
     */
    List<SearchResult> searchInNovel(long novelId, String keyword);
    
    /**
     * 获取过滤后的章节内容（应用屏蔽词）
     */
    String getFilteredChapterContent(long chapterId, List<String> blockedWords);
    
    /**
     * 更新小说置顶状态
     */
    void updatePinned(long novelId, boolean isPinned);
    
    /**
     * 获取章节数量
     */
    int getChapterCount(long novelId);
    
    /**
     * 添加分类
     */
    void addCategory(String categoryName);
    
    /**
     * 删除分类
     */
    void deleteCategory(String categoryName);
    
    /**
     * 更新小说分类
     */
    void updateCategory(long novelId, String category);
}
