package com.example.read.domain.service;

import com.example.read.domain.model.ChapterInfo;
import com.example.read.domain.model.NovelMetadata;
import com.example.read.domain.model.ParserRule;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

/**
 * 网站解析服务接口
 * 负责从网站提取小说内容
 */
public interface WebParserService {
    
    /**
     * 获取网页HTML内容
     * @param url 网页URL
     * @return HTML内容
     */
    Single<String> fetchHtml(String url);
    
    /**
     * 从HTML中提取小说元数据（标题、作者、简介）
     * @param html HTML内容
     * @param rule 解析规则
     * @return 小说元数据
     */
    NovelMetadata extractNovelInfo(String html, ParserRule rule);
    
    /**
     * 从HTML中提取章节列表
     * @param html HTML内容
     * @param rule 解析规则
     * @return 章节信息列表
     */
    List<ChapterInfo> extractChapterList(String html, ParserRule rule);
    
    /**
     * 从HTML中提取章节正文内容
     * @param html HTML内容
     * @param rule 解析规则
     * @return 清理后的章节正文
     */
    String extractChapterContent(String html, ParserRule rule);
    
    /**
     * 清理内容（移除广告、无关元素等）
     * @param content 原始内容
     * @return 清理后的内容
     */
    String cleanContent(String content);
}
