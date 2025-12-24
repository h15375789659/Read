package com.example.read.data.repository;

import com.example.read.data.dao.ChapterDao;
import com.example.read.data.entity.ChapterEntity;
import com.example.read.domain.model.Chapter;
import com.example.read.domain.repository.AIServiceRepository;
import com.example.read.domain.service.AIService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * AI服务仓库实现类
 * 
 * 实现摘要生成、缓存管理和批量处理功能
 * 
 * 验证需求：8.2, 8.4, 8.5
 */
@Singleton
public class AIServiceRepositoryImpl implements AIServiceRepository {

    private final AIService aiService;
    private final ChapterDao chapterDao;

    @Inject
    public AIServiceRepositoryImpl(AIService aiService, ChapterDao chapterDao) {
        this.aiService = aiService;
        this.chapterDao = chapterDao;
    }

    /**
     * 生成单个章节的摘要
     * 验证需求：8.1 - 调用AI服务API并传递章节文本
     */
    @Override
    public Single<String> generateSummary(String chapterContent) {
        return aiService.generateSummary(chapterContent);
    }

    /**
     * 批量生成多个章节的摘要
     * 验证需求：8.5 - 按顺序处理多个章节并显示进度
     * 
     * 按照输入章节列表的顺序依次处理，确保顺序性
     */
    @Override
    @SuppressWarnings("unchecked")
    public Single<Map<Long, String>> batchGenerateSummaries(List<Chapter> chapters, ProgressCallback callback) {
        if (chapters == null || chapters.isEmpty()) {
            return Single.just(new LinkedHashMap<>());
        }

        return Single.<Map<Long, String>>create(emitter -> {
            // 使用LinkedHashMap保持插入顺序
            Map<Long, String> results = new LinkedHashMap<>();
            int total = chapters.size();
            
            for (int i = 0; i < chapters.size(); i++) {
                if (emitter.isDisposed()) {
                    break;
                }
                
                Chapter chapter = chapters.get(i);
                int currentIndex = i + 1;
                
                // 通知进度
                if (callback != null) {
                    callback.onProgress(currentIndex, total);
                }
                
                try {
                    // 首先检查缓存
                    String cachedSummary = getCachedSummary(chapter.getId());
                    if (cachedSummary != null && !cachedSummary.isEmpty()) {
                        results.put(chapter.getId(), cachedSummary);
                        continue;
                    }
                    
                    // 生成新摘要
                    String summary = aiService.generateSummary(chapter.getContent())
                            .blockingGet();
                    
                    // 保存到缓存
                    saveSummaryCache(chapter.getId(), summary);
                    
                    results.put(chapter.getId(), summary);
                } catch (Exception e) {
                    // 单个章节失败不影响其他章节，记录空摘要
                    results.put(chapter.getId(), null);
                }
            }
            
            emitter.onSuccess(results);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 获取缓存的摘要
     * 验证需求：8.4 - 直接显示缓存内容而不重复调用API
     * 
     * 从数据库中读取章节的summary字段
     */
    @Override
    public String getCachedSummary(long chapterId) {
        ChapterEntity entity = chapterDao.getChapterById(chapterId);
        if (entity != null) {
            return entity.getSummary();
        }
        return null;
    }

    /**
     * 保存摘要到缓存
     * 验证需求：8.2 - 显示摘要文字并保存到本地存储
     * 
     * 将摘要保存到数据库中章节的summary字段
     */
    @Override
    public void saveSummaryCache(long chapterId, String summary) {
        if (summary != null && !summary.isEmpty()) {
            chapterDao.updateChapterSummary(chapterId, summary);
        }
    }

    /**
     * 获取或生成摘要（优先使用缓存）
     * 验证需求：8.4 - 已有缓存的摘要直接返回缓存内容
     */
    @Override
    public Single<String> getOrGenerateSummary(Chapter chapter) {
        return Single.fromCallable(() -> {
            // 首先检查缓存
            String cachedSummary = getCachedSummary(chapter.getId());
            if (cachedSummary != null && !cachedSummary.isEmpty()) {
                return cachedSummary;
            }
            return null;
        }).subscribeOn(Schedulers.io())
          .flatMap(cached -> {
              if (cached != null) {
                  // 返回缓存的摘要
                  return Single.just(cached);
              }
              // 生成新摘要并保存
              return generateSummary(chapter.getContent())
                      .doOnSuccess(summary -> saveSummaryCache(chapter.getId(), summary));
          });
    }
}
