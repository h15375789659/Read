package com.example.read.data.service;

import com.example.read.config.AIConfig;
import com.example.read.data.api.DashScopeApi;
import com.example.read.data.api.model.DashScopeRequest;
import com.example.read.data.api.model.DashScopeResponse;
import com.example.read.domain.error.AppError;
import com.example.read.domain.service.AIService;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * AI服务实现类 - 使用通义千问API
 * 
 * 验证需求：8.1, 8.3, 8.6
 */
@Singleton
public class AIServiceImpl implements AIService {

    private final DashScopeApi dashScopeApi;

    @Inject
    public AIServiceImpl(DashScopeApi dashScopeApi) {
        this.dashScopeApi = dashScopeApi;
    }

    /**
     * 调用AI API
     * 验证需求：8.1 - 调用AI服务API并传递章节文本
     * 验证需求：8.6 - API调用超时在30秒后终止请求并提示用户
     */
    @Override
    public Single<String> callAIApi(String prompt, String content) {
        // 构建请求
        DashScopeRequest request = new DashScopeRequest(
                AIConfig.MODEL_NAME,
                prompt,
                content
        );

        // 设置参数
        request.getParameters().setMax_tokens(AIConfig.MAX_TOKENS);
        request.getParameters().setTemperature(AIConfig.TEMPERATURE);

        String authorization = "Bearer " + AIConfig.DASHSCOPE_API_KEY;

        return dashScopeApi.generateText(authorization, "application/json", request)
                .subscribeOn(Schedulers.io())
                .timeout(AIConfig.API_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .map(response -> {
                    if (response.isSuccess()) {
                        String generatedText = response.getGeneratedText();
                        if (generatedText != null && !generatedText.isEmpty()) {
                            return generatedText;
                        }
                        throw new AppError.AIServiceError("AI返回内容为空", false);
                    } else {
                        throw new AppError.AIServiceError(
                                "AI服务调用失败: " + response.getErrorMessage(), 
                                false
                        );
                    }
                })
                .onErrorResumeNext(error -> {
                    // 验证需求：8.3 - AI服务调用失败显示错误提示
                    if (error instanceof java.util.concurrent.TimeoutException) {
                        // 验证需求：8.6 - 超时处理
                        return Single.error(new AppError.AIServiceError(
                                "AI服务请求超时，请稍后重试", true));
                    } else if (error instanceof AppError.AIServiceError) {
                        return Single.error(error);
                    } else {
                        return Single.error(new AppError.AIServiceError(
                                "AI服务调用失败: " + error.getMessage(), false));
                    }
                });
    }

    /**
     * 构建摘要生成的Prompt
     */
    @Override
    public String buildSummaryPrompt(String chapterContent) {
        return "你是一个专业的小说内容分析助手。请为以下小说章节内容生成一个简洁的摘要，" +
                "摘要应该包含主要情节、关键人物和重要事件。" +
                "摘要长度控制在100-200字之间，使用中文回复。";
    }

    /**
     * 生成章节摘要
     * 验证需求：8.1 - 调用AI服务API并传递章节文本
     */
    @Override
    public Single<String> generateSummary(String chapterContent) {
        if (chapterContent == null || chapterContent.trim().isEmpty()) {
            return Single.error(new AppError.AIServiceError("章节内容为空", false));
        }

        // 如果内容过长，截取前面部分
        String content = chapterContent;
        if (content.length() > 3000) {
            content = content.substring(0, 3000) + "...（内容已截断）";
        }

        String prompt = buildSummaryPrompt(content);
        return callAIApi(prompt, content);
    }
}
