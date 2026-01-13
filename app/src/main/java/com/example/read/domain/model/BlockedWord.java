package com.example.read.domain.model;

/**
 * 屏蔽词领域模型
 * 每本小说可以有独立的屏蔽词列表
 */
public class BlockedWord {
    private long id;
    private long novelId;  // 关联的小说ID
    private String word;
    private long createTime;

    public BlockedWord() {}

    public BlockedWord(long novelId, String word) {
        this.novelId = novelId;
        this.word = word;
        this.createTime = System.currentTimeMillis();
    }

    // Getters
    public long getId() { return id; }
    public long getNovelId() { return novelId; }
    public String getWord() { return word; }
    public long getCreateTime() { return createTime; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setNovelId(long novelId) { this.novelId = novelId; }
    public void setWord(String word) { this.word = word; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
