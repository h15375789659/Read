package com.example.read.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 屏蔽词实体类 - 存储用户设置的屏蔽词
 */
@Entity(tableName = "blocked_words")
public class BlockedWordEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String word;

    private long createTime;

    public BlockedWordEntity(@NonNull String word) {
        this.word = word;
        this.createTime = System.currentTimeMillis();
    }

    // Getters
    public long getId() { return id; }
    @NonNull public String getWord() { return word; }
    public long getCreateTime() { return createTime; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setWord(@NonNull String word) { this.word = word; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
