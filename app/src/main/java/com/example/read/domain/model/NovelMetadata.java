package com.example.read.domain.model;

/**
 * 小说元数据模型
 * 用于网站解析时提取的小说基本信息
 */
public class NovelMetadata {
    private String title;
    private String author;
    private String description;

    public NovelMetadata() {}

    public NovelMetadata(String title, String author, String description) {
        this.title = title;
        this.author = author;
        this.description = description;
    }

    // Getters
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setDescription(String description) { this.description = description; }

    /**
     * 检查元数据是否完整（标题、作者、简介都非空）
     */
    public boolean isComplete() {
        return title != null && !title.isEmpty()
                && author != null && !author.isEmpty()
                && description != null && !description.isEmpty();
    }

    /**
     * 检查元数据是否有效（至少有标题）
     */
    public boolean isValid() {
        return title != null && !title.isEmpty();
    }
}
