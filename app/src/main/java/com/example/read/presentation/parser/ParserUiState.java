package com.example.read.presentation.parser;

import com.example.read.domain.model.ChapterInfo;
import com.example.read.domain.model.NovelMetadata;
import com.example.read.domain.model.ParserRule;

import java.util.ArrayList;
import java.util.List;

/**
 * 网站解析界面UI状态
 * 
 * 验证需求：2.1, 2.2, 2.3, 2.4
 */
public class ParserUiState {
    private String url;
    private boolean isUrlValid;
    private ParserRule selectedRule;
    private List<ParserRule> availableRules;
    private NovelMetadata novelMetadata;
    private List<ChapterInfo> chapters;
    private DownloadProgress downloadProgress;
    private boolean isLoading;
    private boolean isParsing;
    private boolean isDownloading;
    private String error;
    private ParseState parseState;
    
    // 断点续传相关
    private Long existingNovelId;           // 已存在的小说ID
    private String existingNovelTitle;      // 已存在的小说标题
    private int existingDownloadedCount;    // 已下载章节数
    private boolean showResumeDialog;       // 是否显示续传对话框
    private boolean existingNovelComplete;  // 已存在的小说是否已完成下载

    public ParserUiState() {
        this.url = "";
        this.isUrlValid = false;
        this.availableRules = new ArrayList<>();
        this.chapters = new ArrayList<>();
        this.isLoading = false;
        this.isParsing = false;
        this.isDownloading = false;
        this.error = null;
        this.parseState = ParseState.IDLE;
        this.existingNovelId = null;
        this.existingNovelTitle = null;
        this.existingDownloadedCount = 0;
        this.showResumeDialog = false;
        this.existingNovelComplete = false;
    }

    // 复制构造函数
    public ParserUiState(ParserUiState other) {
        this.url = other.url;
        this.isUrlValid = other.isUrlValid;
        this.selectedRule = other.selectedRule;
        this.availableRules = new ArrayList<>(other.availableRules);
        this.novelMetadata = other.novelMetadata;
        this.chapters = new ArrayList<>(other.chapters);
        this.downloadProgress = other.downloadProgress;
        this.isLoading = other.isLoading;
        this.isParsing = other.isParsing;
        this.isDownloading = other.isDownloading;
        this.error = other.error;
        this.parseState = other.parseState;
        this.existingNovelId = other.existingNovelId;
        this.existingNovelTitle = other.existingNovelTitle;
        this.existingDownloadedCount = other.existingDownloadedCount;
        this.showResumeDialog = other.showResumeDialog;
        this.existingNovelComplete = other.existingNovelComplete;
    }

    // Getters
    public String getUrl() { return url; }
    public boolean isUrlValid() { return isUrlValid; }
    public ParserRule getSelectedRule() { return selectedRule; }
    public List<ParserRule> getAvailableRules() { return availableRules; }
    public NovelMetadata getNovelMetadata() { return novelMetadata; }
    public List<ChapterInfo> getChapters() { return chapters; }
    public DownloadProgress getDownloadProgress() { return downloadProgress; }
    public boolean isLoading() { return isLoading; }
    public boolean isParsing() { return isParsing; }
    public boolean isDownloading() { return isDownloading; }
    public String getError() { return error; }
    public ParseState getParseState() { return parseState; }
    public Long getExistingNovelId() { return existingNovelId; }
    public String getExistingNovelTitle() { return existingNovelTitle; }
    public int getExistingDownloadedCount() { return existingDownloadedCount; }
    public boolean isShowResumeDialog() { return showResumeDialog; }
    public boolean isExistingNovelComplete() { return existingNovelComplete; }

    // Setters
    public void setUrl(String url) { this.url = url != null ? url : ""; }
    public void setUrlValid(boolean urlValid) { isUrlValid = urlValid; }
    public void setSelectedRule(ParserRule selectedRule) { this.selectedRule = selectedRule; }
    public void setAvailableRules(List<ParserRule> availableRules) { 
        this.availableRules = availableRules != null ? availableRules : new ArrayList<>(); 
    }
    public void setNovelMetadata(NovelMetadata novelMetadata) { this.novelMetadata = novelMetadata; }
    public void setChapters(List<ChapterInfo> chapters) { 
        this.chapters = chapters != null ? chapters : new ArrayList<>(); 
    }
    public void setDownloadProgress(DownloadProgress downloadProgress) { this.downloadProgress = downloadProgress; }
    public void setLoading(boolean loading) { isLoading = loading; }
    public void setParsing(boolean parsing) { isParsing = parsing; }
    public void setDownloading(boolean downloading) { isDownloading = downloading; }
    public void setError(String error) { this.error = error; }
    public void setParseState(ParseState parseState) { this.parseState = parseState; }
    public void setExistingNovelId(Long existingNovelId) { this.existingNovelId = existingNovelId; }
    public void setExistingNovelTitle(String existingNovelTitle) { this.existingNovelTitle = existingNovelTitle; }
    public void setExistingDownloadedCount(int existingDownloadedCount) { this.existingDownloadedCount = existingDownloadedCount; }
    public void setShowResumeDialog(boolean showResumeDialog) { this.showResumeDialog = showResumeDialog; }
    public void setExistingNovelComplete(boolean existingNovelComplete) { this.existingNovelComplete = existingNovelComplete; }

    /**
     * 解析状态枚举
     */
    public enum ParseState {
        IDLE,           // 空闲状态
        PARSING_META,   // 正在解析元数据
        PARSING_LIST,   // 正在解析章节列表
        PARSED,         // 解析完成
        DOWNLOADING,    // 正在下载
        COMPLETED,      // 下载完成
        ERROR           // 错误状态
    }

    /**
     * 下载进度类
     */
    public static class DownloadProgress {
        private final int current;
        private final int total;
        private final String currentChapterTitle;

        public DownloadProgress(int current, int total, String currentChapterTitle) {
            this.current = current;
            this.total = total;
            this.currentChapterTitle = currentChapterTitle;
        }

        public int getCurrent() { return current; }
        public int getTotal() { return total; }
        public String getCurrentChapterTitle() { return currentChapterTitle; }

        /**
         * 获取进度百分比
         */
        public int getProgressPercent() {
            if (total <= 0) return 0;
            return (int) ((current * 100.0) / total);
        }
    }

    // 静态工厂方法
    public static ParserUiState idle() {
        return new ParserUiState();
    }

    public static ParserUiState loading() {
        ParserUiState state = new ParserUiState();
        state.setLoading(true);
        return state;
    }

    public static ParserUiState error(String errorMessage) {
        ParserUiState state = new ParserUiState();
        state.setError(errorMessage);
        state.setParseState(ParseState.ERROR);
        return state;
    }
}
