package com.example.read.domain.model;

/**
 * 章节信息模型
 * 用于网站解析时提取的章节列表信息
 */
public class ChapterInfo {
    private String title;
    private String url;
    private int index;

    public ChapterInfo() {}

    public ChapterInfo(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public ChapterInfo(String title, String url, int index) {
        this.title = title;
        this.url = url;
        this.index = index;
    }

    // Getters
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public int getIndex() { return index; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setUrl(String url) { this.url = url; }
    public void setIndex(int index) { this.index = index; }

    /**
     * 检查章节信息是否有效
     */
    public boolean isValid() {
        return title != null && !title.isEmpty()
                && url != null && !url.isEmpty();
    }
}
