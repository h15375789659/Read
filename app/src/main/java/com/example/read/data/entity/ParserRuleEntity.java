package com.example.read.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 解析规则实体类 - 存储网站解析规则配置
 */
@Entity(tableName = "parser_rules")
public class ParserRuleEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;

    @NonNull
    private String domain;

    @NonNull
    private String chapterListSelector;

    @NonNull
    private String chapterTitleSelector;

    @NonNull
    private String chapterLinkSelector;

    @NonNull
    private String contentSelector;

    private String removeSelectors; // 逗号分隔的选择器列表
    private long createTime;

    public ParserRuleEntity(@NonNull String name, @NonNull String domain,
                           @NonNull String chapterListSelector, @NonNull String chapterTitleSelector,
                           @NonNull String chapterLinkSelector, @NonNull String contentSelector) {
        this.name = name;
        this.domain = domain;
        this.chapterListSelector = chapterListSelector;
        this.chapterTitleSelector = chapterTitleSelector;
        this.chapterLinkSelector = chapterLinkSelector;
        this.contentSelector = contentSelector;
        this.createTime = System.currentTimeMillis();
    }

    // Getters
    public long getId() { return id; }
    @NonNull public String getName() { return name; }
    @NonNull public String getDomain() { return domain; }
    @NonNull public String getChapterListSelector() { return chapterListSelector; }
    @NonNull public String getChapterTitleSelector() { return chapterTitleSelector; }
    @NonNull public String getChapterLinkSelector() { return chapterLinkSelector; }
    @NonNull public String getContentSelector() { return contentSelector; }
    public String getRemoveSelectors() { return removeSelectors; }
    public long getCreateTime() { return createTime; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setName(@NonNull String name) { this.name = name; }
    public void setDomain(@NonNull String domain) { this.domain = domain; }
    public void setChapterListSelector(@NonNull String chapterListSelector) { this.chapterListSelector = chapterListSelector; }
    public void setChapterTitleSelector(@NonNull String chapterTitleSelector) { this.chapterTitleSelector = chapterTitleSelector; }
    public void setChapterLinkSelector(@NonNull String chapterLinkSelector) { this.chapterLinkSelector = chapterLinkSelector; }
    public void setContentSelector(@NonNull String contentSelector) { this.contentSelector = contentSelector; }
    public void setRemoveSelectors(String removeSelectors) { this.removeSelectors = removeSelectors; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
