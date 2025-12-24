package com.example.read.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析后的小说数据模型
 * 用于文件解析服务返回解析结果
 */
public class ParsedNovel {
    private String title;
    private String author;
    private String description;
    private String coverPath;
    private List<ParsedChapter> chapters;

    public ParsedNovel() {
        this.chapters = new ArrayList<>();
    }

    public ParsedNovel(String title, String author) {
        this.title = title;
        this.author = author;
        this.chapters = new ArrayList<>();
    }

    // Getters
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getCoverPath() { return coverPath; }
    public List<ParsedChapter> getChapters() { return chapters; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setDescription(String description) { this.description = description; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }
    public void setChapters(List<ParsedChapter> chapters) { this.chapters = chapters; }

    public void addChapter(ParsedChapter chapter) {
        if (this.chapters == null) {
            this.chapters = new ArrayList<>();
        }
        this.chapters.add(chapter);
    }

    /**
     * 解析后的章节数据
     */
    public static class ParsedChapter {
        private String title;
        private String content;
        private int index;

        public ParsedChapter() {}

        public ParsedChapter(String title, String content, int index) {
            this.title = title;
            this.content = content;
            this.index = index;
        }

        public String getTitle() { return title; }
        public String getContent() { return content; }
        public int getIndex() { return index; }

        public void setTitle(String title) { this.title = title; }
        public void setContent(String content) { this.content = content; }
        public void setIndex(int index) { this.index = index; }
    }
}
