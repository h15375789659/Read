package com.example.read.domain.service;

import io.reactivex.rxjava3.core.Single;

/**
 * AI服务接口 - 定义AI相关操作
 * 用于生成章节摘要等AI功能
 */
public interface AIService {
    
    /**
     * 调用AI API
     * 验证需求：8.1 - 调用AI服务API并传递章节文本
     * 
     * @param prompt 系统提示词
     * @param content 用户内容（章节文本）
     * @return 返回AI生成的响应
     */
    Single<String> callAIApi(String prompt, String content);
    
    /**
     * 构建摘要生成的Prompt
     * 
     * @param chapterContent 章节内容
     * @return 构建好的提示词
     */
    String buildSummaryPrompt(String chapterContent);
    
    /**
     * 生成章节摘要
     * 验证需求：8.1 - 调用AI服务API并传递章节文本
     * 
     * @param chapterContent 章节内容
     * @return 返回生成的摘要
     */
    Single<String> generateSummary(String chapterContent);
}
