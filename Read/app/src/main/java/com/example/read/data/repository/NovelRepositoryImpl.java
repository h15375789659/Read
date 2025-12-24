package com.example.read.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.read.data.dao.CategoryDao;
import com.example.read.data.dao.ChapterDao;
import com.example.read.data.dao.NovelDao;
import com.example.read.data.entity.CategoryEntity;
import com.example.read.data.entity.ChapterEntity;
import com.example.read.data.entity.NovelEntity;
import com.example.read.domain.mapper.ChapterMapper;
import com.example.read.domain.mapper.NovelMapper;
import com.example.read.domain.model.Chapter;
import com.example.read.domain.model.Novel;
import com.example.read.domain.model.SearchResult;
import com.example.read.domain.repository.NovelRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 小说仓库实现类
 */
@Singleton
public class NovelRepositoryImpl implements NovelRepository {
    
    private static final int PREVIEW_CONTEXT_LENGTH = 30; // 搜索预览上下文长度
    
    private final NovelDao novelDao;
    private final ChapterDao chapterDao;
    private final CategoryDao categoryDao;
    
    @Inject
    public NovelRepositoryImpl(NovelDao novelDao, ChapterDao chapterDao, CategoryDao categoryDao) {
        this.novelDao = novelDao;
        this.chapterDao = chapterDao;
        this.categoryDao = categoryDao;
    }
    
    @Override
    public LiveData<List<Novel>> getAllNovels() {
        // 暂时使用简单查询，不获取章节标题
        return Transformations.map(novelDao.getAllNovels(), NovelMapper::toDomainList);
    }
    
    @Override
    public Novel getNovelById(long novelId) {
        NovelEntity entity = novelDao.getNovelById(novelId);
        return NovelMapper.toDomain(entity);
    }
    
    @Override
    public long insertNovel(Novel novel) {
        NovelEntity entity = NovelMapper.toEntity(novel);
        return novelDao.insertNovel(entity);
    }

    @Override
    public void updateNovel(Novel novel) {
        NovelEntity entity = NovelMapper.toEntity(novel);
        novelDao.updateNovel(entity);
    }
    
    @Override
    public void deleteNovel(long novelId) {
        // 由于设置了外键级联删除，删除小说时会自动删除关联的章节
        novelDao.deleteNovelById(novelId);
    }
    
    @Override
    public LiveData<List<Novel>> searchNovels(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllNovels();
        }
        // 暂时使用简单查询
        return Transformations.map(novelDao.searchNovels(keyword.trim()), NovelMapper::toDomainList);
    }
    
    @Override
    public LiveData<List<Novel>> getNovelsByCategory(String category) {
        if (category == null || category.isEmpty() || "全部".equals(category)) {
            return getAllNovels();
        }
        // 暂时使用简单查询
        return Transformations.map(novelDao.getNovelsByCategory(category), NovelMapper::toDomainList);
    }
    
    @Override
    public LiveData<List<String>> getAllCategories() {
        // 使用独立的分类表获取分类列表
        return categoryDao.getAllCategoryNames();
    }
    
    @Override
    public LiveData<List<Chapter>> getChaptersByNovelId(long novelId) {
        return Transformations.map(chapterDao.getChaptersByNovelId(novelId), ChapterMapper::toDomainList);
    }
    
    @Override
    public List<Chapter> getChaptersByNovelIdSync(long novelId) {
        List<ChapterEntity> entities = chapterDao.getChaptersByNovelIdSync(novelId);
        return ChapterMapper.toDomainList(entities);
    }
    
    @Override
    public Chapter getChapterById(long chapterId) {
        ChapterEntity entity = chapterDao.getChapterById(chapterId);
        return ChapterMapper.toDomain(entity);
    }
    
    @Override
    public Chapter getChapterByIndex(long novelId, int index) {
        ChapterEntity entity = chapterDao.getChapterByIndex(novelId, index);
        return ChapterMapper.toDomain(entity);
    }
    
    @Override
    public void insertChapters(long novelId, List<Chapter> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return;
        }
        
        // 设置novelId并转换为Entity
        List<ChapterEntity> entities = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            chapter.setNovelId(novelId);
            if (chapter.getChapterIndex() == 0) {
                chapter.setChapterIndex(i);
            }
            entities.add(ChapterMapper.toEntity(chapter));
        }
        
        chapterDao.insertChapters(entities);
        
        // 获取最后一章标题
        String latestChapterTitle = chapters.get(chapters.size() - 1).getTitle();
        
        // 更新小说的总章节数和最新章节标题
        novelDao.updateChapterInfo(novelId, chapters.size(), latestChapterTitle);
    }

    @Override
    public void updateReadingProgress(long novelId, long chapterId, int position) {
        long currentTime = System.currentTimeMillis();
        // 获取章节标题
        ChapterEntity chapter = chapterDao.getChapterById(chapterId);
        String chapterTitle = chapter != null ? chapter.getTitle() : null;
        // 更新阅读进度（包含章节标题）
        novelDao.updateReadingProgressWithTitle(novelId, chapterId, position, chapterTitle, currentTime);
    }
    
    @Override
    public List<SearchResult> searchInNovel(long novelId, String keyword) {
        List<SearchResult> results = new ArrayList<>();
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }
        
        String searchKeyword = keyword.trim();
        List<ChapterEntity> chapters = chapterDao.searchInChapters(novelId, searchKeyword);
        
        for (ChapterEntity chapter : chapters) {
            String content = chapter.getContent();
            int position = 0;
            
            // 查找所有匹配位置
            while ((position = content.indexOf(searchKeyword, position)) != -1) {
                String preview = SearchResult.generatePreview(
                    content, position, searchKeyword, PREVIEW_CONTEXT_LENGTH
                );
                
                SearchResult result = new SearchResult(
                    chapter.getId(),
                    chapter.getTitle(),
                    chapter.getChapterIndex(),
                    position,
                    preview,
                    searchKeyword
                );
                results.add(result);
                
                position += searchKeyword.length();
            }
        }
        
        return results;
    }
    
    @Override
    public String getFilteredChapterContent(long chapterId, List<String> blockedWords) {
        ChapterEntity chapter = chapterDao.getChapterById(chapterId);
        if (chapter == null) {
            return "";
        }
        
        String content = chapter.getContent();
        
        if (blockedWords == null || blockedWords.isEmpty()) {
            return content;
        }
        
        // 替换所有屏蔽词为星号
        for (String word : blockedWords) {
            if (word != null && !word.isEmpty()) {
                String replacement = generateStars(word.length());
                content = content.replace(word, replacement);
            }
        }
        
        return content;
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
    
    @Override
    public void updatePinned(long novelId, boolean isPinned) {
        novelDao.updatePinned(novelId, isPinned);
    }
    
    @Override
    public int getChapterCount(long novelId) {
        return chapterDao.getChapterCount(novelId);
    }
    
    @Override
    public void addCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return;
        }
        // 检查分类是否已存在
        if (categoryDao.categoryExists(categoryName.trim()) > 0) {
            return;
        }
        // 获取最大排序顺序
        Integer maxOrder = categoryDao.getMaxSortOrder();
        int newOrder = (maxOrder != null ? maxOrder : 0) + 1;
        
        CategoryEntity category = new CategoryEntity(categoryName.trim());
        category.setSortOrder(newOrder);
        categoryDao.insertCategory(category);
    }
    
    @Override
    public void deleteCategory(String categoryName) {
        // 删除分类时，将该分类下的所有小说的分类清空（设为null或空字符串）
        novelDao.updateCategoryToDefault(categoryName, null);
        // 从分类表中删除
        categoryDao.deleteCategoryByName(categoryName);
    }
    
    @Override
    public void updateCategory(long novelId, String category) {
        novelDao.updateCategory(novelId, category);
    }
}
