package com.example.read.domain.model;

/**
 * 章节领域模型
 */
public class Chapter {
    private long id;
    private long novelId;
    private String title;
    private String content;
    private int chapterIndex;
    private int wordCount;
    private String sourceUrl;
    private String summary;
    private long createTime;

    public Chapter() {}

    public Chapter(long novelId, String title, String content, int chapterIndex) {
        this.novelId = novelId;
        this.title = title;
        this.content = content;
        this.chapterIndex = chapterIndex;
        this.wordCount = content != null ? content.length() : 0;
        this.createTime = System.currentTimeMillis();
    }

    // Getters
    public long getId() { return id; }
    public long getNovelId() { return novelId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getChapterIndex() { return chapterIndex; }
    public int getWordCount() { return wordCount; }
    public String getSourceUrl() { return sourceUrl; }
    public String getSummary() { return summary; }
    public long getCreateTime() { return createTime; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setNovelId(long novelId) { this.novelId = novelId; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setChapterIndex(int chapterIndex) { this.chapterIndex = chapterIndex; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
