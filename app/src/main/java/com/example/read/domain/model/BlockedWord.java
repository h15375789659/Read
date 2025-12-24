package com.example.read.domain.model;

/**
 * 屏蔽词领域模型
 */
public class BlockedWord {
    private long id;
    private String word;
    private long createTime;

    public BlockedWord() {}

    public BlockedWord(String word) {
        this.word = word;
        this.createTime = System.currentTimeMillis();
    }

    // Getters
    public long getId() { return id; }
    public String getWord() { return word; }
    public long getCreateTime() { return createTime; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setWord(String word) { this.word = word; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
