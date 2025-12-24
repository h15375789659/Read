package com.example.read.domain.model;

/**
 * 搜索结果领域模型
 */
public class SearchResult {
    private long chapterId;
    private String chapterTitle;
    private int chapterIndex;
    private int position;
    private String preview; // 包含关键词的上下文预览
    private String keyword;

    public SearchResult() {}

    public SearchResult(long chapterId, String chapterTitle, int chapterIndex,
                        int position, String preview, String keyword) {
        this.chapterId = chapterId;
        this.chapterTitle = chapterTitle;
        this.chapterIndex = chapterIndex;
        this.position = position;
        this.preview = preview;
        this.keyword = keyword;
    }

    // Getters
    public long getChapterId() { return chapterId; }
    public String getChapterTitle() { return chapterTitle; }
    public int getChapterIndex() { return chapterIndex; }
    public int getPosition() { return position; }
    public String getPreview() { return preview; }
    public String getKeyword() { return keyword; }

    // Setters
    public void setChapterId(long chapterId) { this.chapterId = chapterId; }
    public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }
    public void setChapterIndex(int chapterIndex) { this.chapterIndex = chapterIndex; }
    public void setPosition(int position) { this.position = position; }
    public void setPreview(String preview) { this.preview = preview; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    /**
     * 生成搜索结果预览文本
     * @param content 章节内容
     * @param position 关键词位置
     * @param keyword 关键词
     * @param contextLength 上下文长度
     * @return 预览文本
     */
    public static String generatePreview(String content, int position, String keyword, int contextLength) {
        if (content == null || keyword == null) {
            return "";
        }
        
        int start = Math.max(0, position - contextLength);
        int end = Math.min(content.length(), position + keyword.length() + contextLength);
        
        StringBuilder preview = new StringBuilder();
        if (start > 0) {
            preview.append("...");
        }
        preview.append(content.substring(start, end));
        if (end < content.length()) {
            preview.append("...");
        }
        
        return preview.toString();
    }
}
