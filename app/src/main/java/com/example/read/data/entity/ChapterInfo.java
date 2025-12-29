package com.example.read.data.entity;

import androidx.annotation.NonNull;

/**
 * 章节信息类 - 不含content字段，用于章节列表查询
 * 避免CursorWindow溢出问题
 */
public class ChapterInfo {
    private long id;
    private long novelId;
    @NonNull
    private String title;
    private int chapterIndex;
    private int wordCount;
    private String sourceUrl;
    private String summary;
    private long createTime;

    public ChapterInfo() {
        this.title = "";
    }

    // Getters
    public long getId() { return id; }
    public long getNovelId() { return novelId; }
    @NonNull public String getTitle() { return title; }
    public int getChapterIndex() { return chapterIndex; }
    public int getWordCount() { return wordCount; }
    public String getSourceUrl() { return sourceUrl; }
    public String getSummary() { return summary; }
    public long getCreateTime() { return createTime; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setNovelId(long novelId) { this.novelId = novelId; }
    public void setTitle(@NonNull String title) { this.title = title; }
    public void setChapterIndex(int chapterIndex) { this.chapterIndex = chapterIndex; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    
    /**
     * 转换为 ChapterEntity（content 为空）
     */
    public ChapterEntity toEntity() {
        ChapterEntity entity = new ChapterEntity();
        entity.setId(this.id);
        entity.setNovelId(this.novelId);
        entity.setTitle(this.title);
        entity.setContent(""); // content 需要单独加载
        entity.setChapterIndex(this.chapterIndex);
        entity.setWordCount(this.wordCount);
        entity.setSourceUrl(this.sourceUrl);
        entity.setSummary(this.summary);
        entity.setCreateTime(this.createTime);
        return entity;
    }
}
