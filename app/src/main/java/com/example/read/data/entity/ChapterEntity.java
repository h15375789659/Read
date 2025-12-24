package com.example.read.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 章节实体类 - 存储小说章节信息
 */
@Entity(
    tableName = "chapters",
    foreignKeys = @ForeignKey(
        entity = NovelEntity.class,
        parentColumns = "id",
        childColumns = "novelId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("novelId"), @Index("chapterIndex")}
)
public class ChapterEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long novelId;

    @NonNull
    private String title;

    @NonNull
    private String content;

    private int chapterIndex;
    private int wordCount;
    private String sourceUrl;
    private String summary;
    private long createTime;

    public ChapterEntity(long novelId, @NonNull String title, @NonNull String content, int chapterIndex) {
        this.novelId = novelId;
        this.title = title;
        this.content = content;
        this.chapterIndex = chapterIndex;
        this.wordCount = content.length();
        this.createTime = System.currentTimeMillis();
    }

    // Getters
    public long getId() { return id; }
    public long getNovelId() { return novelId; }
    @NonNull public String getTitle() { return title; }
    @NonNull public String getContent() { return content; }
    public int getChapterIndex() { return chapterIndex; }
    public int getWordCount() { return wordCount; }
    public String getSourceUrl() { return sourceUrl; }
    public String getSummary() { return summary; }
    public long getCreateTime() { return createTime; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setNovelId(long novelId) { this.novelId = novelId; }
    public void setTitle(@NonNull String title) { this.title = title; }
    public void setContent(@NonNull String content) { this.content = content; }
    public void setChapterIndex(int chapterIndex) { this.chapterIndex = chapterIndex; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
