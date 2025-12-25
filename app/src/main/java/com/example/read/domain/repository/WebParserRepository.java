package com.example.read.domain.repository;

import com.example.read.domain.model.ChapterInfo;
import com.example.read.domain.model.Novel;
import com.example.read.domain.model.NovelMetadata;
import com.example.read.domain.model.ParserRule;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

/**
 * 网站解析Repository接口
 * 负责从网站解析小说并保存到本地
 */
public interface WebParserRepository {
    
    /**
     * 解析小说元数据（标题、作者、简介）
     * @param url 小说页面URL
     * @param rule 解析规则
     * @return 小说元数据
     */
    Single<NovelMetadata> parseNovelMetadata(String url, ParserRule rule);
    
    /**
     * 解析章节列表
     * @param url 小说页面URL
     * @param rule 解析规则
     * @return 章节信息列表
     */
    Single<List<ChapterInfo>> parseChapterList(String url, ParserRule rule);
    
    /**
     * 解析单个章节内容
     * @param url 章节页面URL
     * @param rule 解析规则
     * @return 章节正文内容
     */
    Single<String> parseChapterContent(String url, ParserRule rule);
    
    /**
     * 下载整本小说
     * @param url 小说页面URL
     * @param rule 解析规则
     * @param callback 进度回调
     * @return 下载完成的小说
     */
    Single<Novel> downloadNovel(String url, ParserRule rule, ProgressCallback callback);
    
    /**
     * 断点续传下载小说
     * @param novelId 已存在的小说ID
     * @param url 小说页面URL
     * @param rule 解析规则
     * @param callback 进度回调
     * @return 下载完成的小说
     */
    Single<Novel> resumeDownload(long novelId, String url, ParserRule rule, ProgressCallback callback);
    
    /**
     * 取消下载任务
     */
    void cancelDownload();
    
    /**
     * 检查是否有正在进行的下载
     * @return true 如果有下载正在进行
     */
    boolean isDownloading();
    
    /**
     * 验证URL格式
     * @param url 待验证的URL
     * @return true 如果URL格式有效
     */
    boolean isValidUrl(String url);
    
    /**
     * 检查是否已存在相同URL的小说
     * @param sourceUrl 小说源URL
     * @return 已存在的小说信息（包含ID和已下载章节数），如果不存在返回null
     */
    Single<ExistingNovelInfo> checkExistingNovel(String sourceUrl);
    
    /**
     * 已存在小说的信息
     */
    class ExistingNovelInfo {
        private final long novelId;
        private final String title;
        private final int downloadedChapters;
        private final int totalChapters;
        
        public ExistingNovelInfo(long novelId, String title, int downloadedChapters, int totalChapters) {
            this.novelId = novelId;
            this.title = title;
            this.downloadedChapters = downloadedChapters;
            this.totalChapters = totalChapters;
        }
        
        public long getNovelId() { return novelId; }
        public String getTitle() { return title; }
        public int getDownloadedChapters() { return downloadedChapters; }
        public int getTotalChapters() { return totalChapters; }
        
        public boolean isComplete() {
            return downloadedChapters >= totalChapters;
        }
    }
    
    /**
     * 进度回调接口
     */
    interface ProgressCallback {
        /**
         * 下载进度更新
         * @param current 当前已下载章节数
         * @param total 总章节数
         * @param currentChapterTitle 当前正在下载的章节标题
         */
        void onProgress(int current, int total, String currentChapterTitle);
    }
}
