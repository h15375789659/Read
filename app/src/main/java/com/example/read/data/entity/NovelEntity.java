package com.example.read.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 小说实体类 - 存储小说的基本信息
 */
@Entity(tableName = "novels")
public class NovelEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String title;

    @NonNull
    private String author;

    private String description;
    private String coverPath;

    @NonNull
    private String source; // "local" 或 "web"

    private String sourceUrl;
    private int totalChapters;
    private Long currentChapterId;
    private int currentPosition;
    private long lastReadTime;
    private long createTime;

    @NonNull
    private String category;

    private boolean isPinned;
    
    // 缓存的章节标题信息，避免复杂JOIN查询
    private String currentChapterTitle;  // 当前阅读章节标题
    private String latestChapterTitle;   // 最新章节标题（最后一章）

    public NovelEntity(@NonNull String title, @NonNull String author) {
        this.title = title;
        this.author = author;
        this.description = "";
        this.source = "local";
        this.lastReadTime = System.currentTimeMillis();
        this.createTime = System.currentTimeMillis();
        this.category = "未分类";
        this.isPinned = false;
    }

    // Getters
    public long getId() { return id; }
    @NonNull public String getTitle() { return title; }
    @NonNull public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getCoverPath() { return coverPath; }
    @NonNull public String getSource() { return source; }
    public String getSourceUrl() { return sourceUrl; }
    public int getTotalChapters() { return totalChapters; }
    public Long getCurrentChapterId() { return currentChapterId; }
    public int getCurrentPosition() { return currentPosition; }
    public long getLastReadTime() { return lastReadTime; }
    public long getCreateTime() { return createTime; }
    @NonNull public String getCategory() { return category; }
    public boolean isPinned() { return isPinned; }
    public String getCurrentChapterTitle() { return currentChapterTitle; }
    public String getLatestChapterTitle() { return latestChapterTitle; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setTitle(@NonNull String title) { this.title = title; }
    public void setAuthor(@NonNull String author) { this.author = author; }
    public void setDescription(String description) { this.description = description; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }
    public void setSource(@NonNull String source) { this.source = source; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public void setTotalChapters(int totalChapters) { this.totalChapters = totalChapters; }
    public void setCurrentChapterId(Long currentChapterId) { this.currentChapterId = currentChapterId; }
    public void setCurrentPosition(int currentPosition) { this.currentPosition = currentPosition; }
    public void setLastReadTime(long lastReadTime) { this.lastReadTime = lastReadTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    public void setCategory(@NonNull String category) { this.category = category; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
    public void setCurrentChapterTitle(String currentChapterTitle) { this.currentChapterTitle = currentChapterTitle; }
    public void setLatestChapterTitle(String latestChapterTitle) { this.latestChapterTitle = latestChapterTitle; }
}
