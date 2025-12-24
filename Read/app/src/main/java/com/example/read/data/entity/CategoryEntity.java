package com.example.read.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 分类实体类 - 存储用户自定义的分类
 */
@Entity(tableName = "categories")
public class CategoryEntity {
    
    @PrimaryKey
    @NonNull
    private String name;
    
    // 分类排序顺序
    private int sortOrder;
    
    // 创建时间
    private long createdTime;
    
    public CategoryEntity(@NonNull String name) {
        this.name = name;
        this.createdTime = System.currentTimeMillis();
    }
    
    @NonNull
    public String getName() {
        return name;
    }
    
    public void setName(@NonNull String name) {
        this.name = name;
    }
    
    public int getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }
}
