package com.example.read.presentation.reader;

import com.example.read.domain.model.Chapter;
import com.example.read.domain.model.Novel;
import com.example.read.domain.model.PageAnimation;
import com.example.read.domain.model.PageMode;
import com.example.read.domain.model.ReaderFont;
import com.example.read.domain.model.ReaderTheme;
import com.example.read.domain.model.ReadingPosition;
import com.example.read.domain.model.SearchResult;
import com.example.read.domain.model.TTSState;
import com.example.read.domain.model.VoiceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 阅读器界面UI状态
 * 
 * 验证需求：5.1, 5.4, 5.5, 5.6, 5.7, 6.2, 9.2, 10.1, 10.2, 11.4
 */
public class ReaderUiState {
    
    // 小说和章节信息
    private Novel novel;
    private Chapter currentChapter;
    private List<Chapter> chapters;
    private String displayContent; // 应用屏蔽词后的显示内容
    
    // 加载状态
    private boolean isLoading;
    private String error;
    
    // 工具栏显示状态
    private boolean showToolbar;
    
    // 阅读设置
    private float fontSize;
    private float lineSpacing;
    private ReaderTheme theme;
    private PageMode pageMode;
    private PageAnimation pageAnimation;
    private ReaderFont font;
    
    // 搜索相关
    private List<SearchResult> searchResults;
    private int currentSearchIndex;
    private String searchKeyword;
    private ReadingPosition savedPosition; // 搜索前保存的位置
    
    // TTS相关
    private TTSState ttsState;
    private List<VoiceInfo> availableVoices;
    
    // 阅读统计
    private long readingStartTime; // 当前阅读会话开始时间
    private int readCharCount; // 当前会话阅读字数
    
    // 阅读位置（用于恢复）
    private int savedScrollPosition; // 上下滚动模式的滚动位置
    private int savedPageIndex; // 左右翻页模式的页码
    
    // 预加载章节内容（用于翻页动画）
    private Chapter previousChapter; // 上一章
    private Chapter nextChapter; // 下一章
    private String previousChapterContent; // 上一章内容（已过滤）
    private String nextChapterContent; // 下一章内容（已过滤）
    
    // 书签相关状态
    private boolean bookmarkAdded; // 书签添加成功标志
    private boolean bookmarkDeleted; // 书签删除成功标志
    private int jumpToPosition; // 跳转到指定位置（书签跳转用）

    public ReaderUiState() {
        this.chapters = new ArrayList<>();
        this.displayContent = "";
        this.isLoading = false;
        this.error = null;
        this.showToolbar = false;
        this.fontSize = 18f;
        this.lineSpacing = 1.5f;
        this.theme = ReaderTheme.DAY;
        this.pageMode = PageMode.SCROLL;
        this.pageAnimation = PageAnimation.SLIDE;
        this.font = ReaderFont.DEFAULT;
        this.searchResults = new ArrayList<>();
        this.currentSearchIndex = -1;
        this.searchKeyword = "";
        this.ttsState = new TTSState();
        this.availableVoices = new ArrayList<>();
        this.readingStartTime = 0;
        this.readCharCount = 0;
        this.savedScrollPosition = 0;
        this.savedPageIndex = 0;
        this.previousChapter = null;
        this.nextChapter = null;
        this.previousChapterContent = "";
        this.nextChapterContent = "";
        this.bookmarkAdded = false;
        this.bookmarkDeleted = false;
        this.jumpToPosition = -1;
    }

    /**
     * 复制构造函数，用于创建不可变状态的副本
     */
    public ReaderUiState(ReaderUiState other) {
        this.novel = other.novel;
        this.currentChapter = other.currentChapter;
        this.chapters = new ArrayList<>(other.chapters);
        this.displayContent = other.displayContent;
        this.isLoading = other.isLoading;
        this.error = other.error;
        this.showToolbar = other.showToolbar;
        this.fontSize = other.fontSize;
        this.lineSpacing = other.lineSpacing;
        this.theme = other.theme;
        this.pageMode = other.pageMode;
        this.pageAnimation = other.pageAnimation;
        this.font = other.font;
        this.searchResults = new ArrayList<>(other.searchResults);
        this.currentSearchIndex = other.currentSearchIndex;
        this.searchKeyword = other.searchKeyword;
        this.savedPosition = other.savedPosition;
        this.ttsState = other.ttsState != null ? other.ttsState.copy() : new TTSState();
        this.availableVoices = new ArrayList<>(other.availableVoices);
        this.readingStartTime = other.readingStartTime;
        this.readCharCount = other.readCharCount;
        this.savedScrollPosition = other.savedScrollPosition;
        this.savedPageIndex = other.savedPageIndex;
        this.previousChapter = other.previousChapter;
        this.nextChapter = other.nextChapter;
        this.previousChapterContent = other.previousChapterContent;
        this.nextChapterContent = other.nextChapterContent;
        this.bookmarkAdded = other.bookmarkAdded;
        this.bookmarkDeleted = other.bookmarkDeleted;
        this.jumpToPosition = other.jumpToPosition;
    }


    // ==================== Getters ====================
    
    public Novel getNovel() { return novel; }
    public Chapter getCurrentChapter() { return currentChapter; }
    public List<Chapter> getChapters() { return chapters; }
    public String getDisplayContent() { return displayContent; }
    public boolean isLoading() { return isLoading; }
    public String getError() { return error; }
    public boolean isShowToolbar() { return showToolbar; }
    public float getFontSize() { return fontSize; }
    public float getLineSpacing() { return lineSpacing; }
    public ReaderTheme getTheme() { return theme; }
    public PageMode getPageMode() { return pageMode; }
    public PageAnimation getPageAnimation() { return pageAnimation; }
    public ReaderFont getFont() { return font; }
    public List<SearchResult> getSearchResults() { return searchResults; }
    public int getCurrentSearchIndex() { return currentSearchIndex; }
    public String getSearchKeyword() { return searchKeyword; }
    public ReadingPosition getSavedPosition() { return savedPosition; }
    public TTSState getTtsState() { return ttsState; }
    public List<VoiceInfo> getAvailableVoices() { return availableVoices; }
    public long getReadingStartTime() { return readingStartTime; }
    public int getReadCharCount() { return readCharCount; }
    public int getSavedScrollPosition() { return savedScrollPosition; }
    public int getSavedPageIndex() { return savedPageIndex; }
    public Chapter getPreviousChapter() { return previousChapter; }
    public Chapter getNextChapter() { return nextChapter; }
    public String getPreviousChapterContent() { return previousChapterContent; }
    public String getNextChapterContent() { return nextChapterContent; }
    public boolean isBookmarkAdded() { return bookmarkAdded; }
    public boolean isBookmarkDeleted() { return bookmarkDeleted; }
    public int getJumpToPosition() { return jumpToPosition; }

    // ==================== Setters ====================
    
    public void setNovel(Novel novel) { this.novel = novel; }
    public void setCurrentChapter(Chapter currentChapter) { this.currentChapter = currentChapter; }
    public void setChapters(List<Chapter> chapters) { 
        this.chapters = chapters != null ? chapters : new ArrayList<>(); 
    }
    public void setDisplayContent(String displayContent) { 
        this.displayContent = displayContent != null ? displayContent : ""; 
    }
    public void setLoading(boolean loading) { isLoading = loading; }
    public void setError(String error) { this.error = error; }
    public void setShowToolbar(boolean showToolbar) { this.showToolbar = showToolbar; }
    public void setFontSize(float fontSize) { this.fontSize = fontSize; }
    public void setLineSpacing(float lineSpacing) { this.lineSpacing = lineSpacing; }
    public void setTheme(ReaderTheme theme) { this.theme = theme; }
    public void setPageMode(PageMode pageMode) { this.pageMode = pageMode; }
    public void setPageAnimation(PageAnimation pageAnimation) { this.pageAnimation = pageAnimation; }
    public void setFont(ReaderFont font) { this.font = font; }
    public void setSearchResults(List<SearchResult> searchResults) { 
        this.searchResults = searchResults != null ? searchResults : new ArrayList<>(); 
    }
    public void setCurrentSearchIndex(int currentSearchIndex) { this.currentSearchIndex = currentSearchIndex; }
    public void setSearchKeyword(String searchKeyword) { 
        this.searchKeyword = searchKeyword != null ? searchKeyword : ""; 
    }
    public void setSavedPosition(ReadingPosition savedPosition) { this.savedPosition = savedPosition; }
    public void setTtsState(TTSState ttsState) { 
        this.ttsState = ttsState != null ? ttsState : new TTSState(); 
    }
    public void setAvailableVoices(List<VoiceInfo> availableVoices) { 
        this.availableVoices = availableVoices != null ? availableVoices : new ArrayList<>(); 
    }
    public void setReadingStartTime(long readingStartTime) { this.readingStartTime = readingStartTime; }
    public void setReadCharCount(int readCharCount) { this.readCharCount = readCharCount; }
    public void setSavedScrollPosition(int savedScrollPosition) { this.savedScrollPosition = savedScrollPosition; }
    public void setSavedPageIndex(int savedPageIndex) { this.savedPageIndex = savedPageIndex; }
    public void setPreviousChapter(Chapter previousChapter) { this.previousChapter = previousChapter; }
    public void setNextChapter(Chapter nextChapter) { this.nextChapter = nextChapter; }
    public void setPreviousChapterContent(String previousChapterContent) { 
        this.previousChapterContent = previousChapterContent != null ? previousChapterContent : ""; 
    }
    public void setNextChapterContent(String nextChapterContent) { 
        this.nextChapterContent = nextChapterContent != null ? nextChapterContent : ""; 
    }
    public void setBookmarkAdded(boolean bookmarkAdded) { this.bookmarkAdded = bookmarkAdded; }
    public void setBookmarkDeleted(boolean bookmarkDeleted) { this.bookmarkDeleted = bookmarkDeleted; }
    public void setJumpToPosition(int jumpToPosition) { this.jumpToPosition = jumpToPosition; }

    // ==================== 便捷方法 ====================

    /**
     * 是否有搜索结果
     */
    public boolean hasSearchResults() {
        return searchResults != null && !searchResults.isEmpty();
    }

    /**
     * 获取当前搜索结果
     */
    public SearchResult getCurrentSearchResult() {
        if (hasSearchResults() && currentSearchIndex >= 0 && currentSearchIndex < searchResults.size()) {
            return searchResults.get(currentSearchIndex);
        }
        return null;
    }

    /**
     * 是否可以导航到上一个搜索结果
     */
    public boolean canNavigateToPreviousResult() {
        return hasSearchResults() && currentSearchIndex > 0;
    }

    /**
     * 是否可以导航到下一个搜索结果
     */
    public boolean canNavigateToNextResult() {
        return hasSearchResults() && currentSearchIndex < searchResults.size() - 1;
    }

    /**
     * 是否正在朗读
     */
    public boolean isTTSPlaying() {
        return ttsState != null && ttsState.isPlaying();
    }

    /**
     * 是否TTS暂停
     */
    public boolean isTTSPaused() {
        return ttsState != null && ttsState.isPaused();
    }

    /**
     * 获取当前章节索引（在章节列表中的位置）
     */
    public int getCurrentChapterIndex() {
        if (currentChapter == null || chapters == null || chapters.isEmpty()) {
            return 0;
        }
        // 在章节列表中查找当前章节的位置
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).getId() == currentChapter.getId()) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 是否可以切换到上一章
     */
    public boolean canGoPreviousChapter() {
        return getCurrentChapterIndex() > 0;
    }

    /**
     * 是否可以切换到下一章
     */
    public boolean canGoNextChapter() {
        if (chapters == null || chapters.isEmpty()) {
            return false;
        }
        return getCurrentChapterIndex() < chapters.size() - 1;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建加载状态
     */
    public static ReaderUiState loading() {
        ReaderUiState state = new ReaderUiState();
        state.setLoading(true);
        return state;
    }

    /**
     * 创建错误状态
     */
    public static ReaderUiState error(String errorMessage) {
        ReaderUiState state = new ReaderUiState();
        state.setError(errorMessage);
        return state;
    }
}
