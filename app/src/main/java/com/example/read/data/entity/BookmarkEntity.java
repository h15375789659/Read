package com.example.read.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 书签实体类 - 存储用户书签信息
 */
@Entity(
    tableName = "bookmarks",
    foreignKeys = @ForeignKey(
        entity = NovelEntity.class,
        parentColumns = "id",
        childColumns = "novelId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("novelId")
)
public class BookmarkEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long novelId;
    private long chapterId;

    @NonNull
    private String chapterTitle;

    private int position;
    private String note;
    private long createTime;

    public BookmarkEntity(long novelId, long chapterId, @NonNull String chapterTitle, int position) {
        this.novelId = novelId;
        this.chapterId = chapterId;
        this.chapterTitle = chapterTitle;
        this.position = position;
        this.createTime = System.currentTimeMillis();
    }

    // Getters
    public long getId() { return id; }
    public long getNovelId() { return novelId; }
    public long getChapterId() { return chapterId; }
    @NonNull public String getChapterTitle() { return chapterTitle; }
    public int getPosition() { return position; }
    public String getNote() { return note; }
    public long getCreateTime() { return createTime; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setNovelId(long novelId) { this.novelId = novelId; }
    public void setChapterId(long chapterId) { this.chapterId = chapterId; }
    public void setChapterTitle(@NonNull String chapterTitle) { this.chapterTitle = chapterTitle; }
    public void setPosition(int position) { this.position = position; }
    public void setNote(String note) { this.note = note; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
