package com.example.read.domain.model;

/**
 * 书签领域模型
 */
public class Bookmark {
    private long id;
    private long novelId;
    private long chapterId;
    private String chapterTitle;
    private int position;
    private String note;
    private long createTime;

    public Bookmark() {}

    public Bookmark(long novelId, long chapterId, String chapterTitle, int position) {
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
    public String getChapterTitle() { return chapterTitle; }
    public int getPosition() { return position; }
    public String getNote() { return note; }
    public long getCreateTime() { return createTime; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setNovelId(long novelId) { this.novelId = novelId; }
    public void setChapterId(long chapterId) { this.chapterId = chapterId; }
    public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }
    public void setPosition(int position) { this.position = position; }
    public void setNote(String note) { this.note = note; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
