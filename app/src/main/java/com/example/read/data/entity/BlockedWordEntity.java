package com.example.read.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 屏蔽词实体类 - 存储用户设置的屏蔽词
 * 每本小说可以有独立的屏蔽词列表
 * 注意：不使用外键约束，由应用层管理关联关系，删除小说时需手动清理屏蔽词
 */
@Entity(
    tableName = "blocked_words",
    indices = {
        @Index(value = {"novelId"})
    }
)
public class BlockedWordEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    // 关联的小说ID，每本小说有独立的屏蔽词
    private long novelId;

    @NonNull
    private String word;

    private long createTime;

    public BlockedWordEntity(long novelId, @NonNull String word) {
        this.novelId = novelId;
        this.word = word;
        this.createTime = System.currentTimeMillis();
    }

    // Getters
    public long getId() { return id; }
    public long getNovelId() { return novelId; }
    @NonNull public String getWord() { return word; }
    public long getCreateTime() { return createTime; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setNovelId(long novelId) { this.novelId = novelId; }
    public void setWord(@NonNull String word) { this.word = word; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
