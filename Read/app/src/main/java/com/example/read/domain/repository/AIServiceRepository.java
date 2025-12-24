package com.example.read.domain.repository;

import com.example.read.domain.model.Chapter;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;

/**
 * AI服务仓库接口 - 定义AI摘要相关操作
 * 
 * 验证需求：8.2, 8.4, 8.5
 */
public interface AIServiceRepository {
    
    /**
     * 生成单个章节的摘要
     * 验证需求：8.1 - 调用AI服务API并传递章节文本
     * 
     * @param chapterContent 章节内容
     * @return 返回生成的摘要
     */
    Single<String> generateSummary(String chapterContent);
    
    /**
     * 批量生成多个章节的摘要
     * 验证需求：8.5 - 按顺序处理多个章节并显示进度
     * 
     * @param chapters 章节列表
     * @param callback 进度回调
     * @return 返回章节ID到摘要的映射
     */
    Single<Map<Long, String>> batchGenerateSummaries(List<Chapter> chapters, ProgressCallback callback);
    
    /**
     * 获取缓存的摘要
     * 验证需求：8.4 - 直接显示缓存内容而不重复调用API
     * 
     * @param chapterId 章节ID
     * @return 缓存的摘要，如果不存在则返回null
     */
    String getCachedSummary(long chapterId);
    
    /**
     * 保存摘要到缓存
     * 验证需求：8.2 - 显示摘要文字并保存到本地存储
     * 
     * @param chapterId 章节ID
     * @param summary 摘要内容
     */
    void saveSummaryCache(long chapterId, String summary);
    
    /**
     * 获取或生成摘要（优先使用缓存）
     * 验证需求：8.4 - 已有缓存的摘要直接返回缓存内容
     * 
     * @param chapter 章节对象
     * @return 返回摘要（缓存或新生成）
     */
    Single<String> getOrGenerateSummary(Chapter chapter);
    
    /**
     * 进度回调接口
     */
    interface ProgressCallback {
        /**
         * 进度更新回调
         * @param current 当前处理的章节索引（从1开始）
         * @param total 总章节数
         */
        void onProgress(int current, int total);
    }
}
