package com.example.read.domain.model;

/**
 * 小说领域模型
 */
public class Novel {
    private long id;
    private String title;
    private String author;
    private String description;
    private String coverPath;
    private NovelSource source;
    private String sourceUrl;
    private int totalChapters;
    private Long currentChapterId;
    private int currentPosition;
    private long lastReadTime;
    private long createTime;
    private String category;
    private boolean isPinned;
    private float readingProgress; // 0.0 - 1.0
    private String currentChapterTitle; // 当前阅读章节标题
    private String latestChapterTitle;  // 最新章节标题

    public Novel() {}

    public Novel(String title, String author) {
        this.title = title;
        this.author = author;
        this.source = NovelSource.LOCAL;
        this.category = "未分类";
        this.lastReadTime = System.currentTimeMillis();
        this.createTime = System.currentTimeMillis();
    }

    // Getters
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getCoverPath() { return coverPath; }
    public NovelSource getSource() { return source; }
    public String getSourceUrl() { return sourceUrl; }
    public int getTotalChapters() { return totalChapters; }
    public Long getCurrentChapterId() { return currentChapterId; }
    public int getCurrentPosition() { return currentPosition; }
    public long getLastReadTime() { return lastReadTime; }
    public long getCreateTime() { return createTime; }
    public String getCategory() { return category; }
    public boolean isPinned() { return isPinned; }
    public float getReadingProgress() { return readingProgress; }
    public String getCurrentChapterTitle() { return currentChapterTitle; }
    public String getLatestChapterTitle() { return latestChapterTitle; }


    // Setters
    public void setId(long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setDescription(String description) { this.description = description; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }
    public void setSource(NovelSource source) { this.source = source; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public void setTotalChapters(int totalChapters) { this.totalChapters = totalChapters; }
    public void setCurrentChapterId(Long currentChapterId) { this.currentChapterId = currentChapterId; }
    public void setCurrentPosition(int currentPosition) { this.currentPosition = currentPosition; }
    public void setLastReadTime(long lastReadTime) { this.lastReadTime = lastReadTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    public void setCategory(String category) { this.category = category; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
    public void setReadingProgress(float readingProgress) { this.readingProgress = readingProgress; }
    public void setCurrentChapterTitle(String currentChapterTitle) { this.currentChapterTitle = currentChapterTitle; }
    public void setLatestChapterTitle(String latestChapterTitle) { this.latestChapterTitle = latestChapterTitle; }

    /**
     * 计算阅读进度
     */
    public void calculateReadingProgress(int currentChapterIndex) {
        if (totalChapters > 0) {
            this.readingProgress = (float) currentChapterIndex / totalChapters;
        } else {
            this.readingProgress = 0f;
        }
    }
}
