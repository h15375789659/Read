package com.example.read.domain.model;

/**
 * 小说阅读统计模型 - 用于阅读排行
 */
public class NovelReadingStats {
    private long novelId;
    private String novelTitle;
    private String novelAuthor;
    private long totalDuration;     // 总阅读时长（毫秒）
    private long totalWordCount;    // 小说总字数
    private float readingProgress;  // 阅读进度 (0.0 - 1.0)
    private int totalChapters;      // 总章节数
    private int currentChapterIndex; // 当前章节索引

    public NovelReadingStats() {}

    public NovelReadingStats(long novelId, long totalDuration) {
        this.novelId = novelId;
        this.totalDuration = totalDuration;
    }

    // Getters
    public long getNovelId() { return novelId; }
    public String getNovelTitle() { return novelTitle; }
    public String getNovelAuthor() { return novelAuthor; }
    public long getTotalDuration() { return totalDuration; }
    public long getTotalWordCount() { return totalWordCount; }
    public float getReadingProgress() { return readingProgress; }
    public int getTotalChapters() { return totalChapters; }
    public int getCurrentChapterIndex() { return currentChapterIndex; }

    // Setters
    public void setNovelId(long novelId) { this.novelId = novelId; }
    public void setNovelTitle(String novelTitle) { this.novelTitle = novelTitle; }
    public void setNovelAuthor(String novelAuthor) { this.novelAuthor = novelAuthor; }
    public void setTotalDuration(long totalDuration) { this.totalDuration = totalDuration; }
    public void setTotalWordCount(long totalWordCount) { this.totalWordCount = totalWordCount; }
    public void setReadingProgress(float readingProgress) { this.readingProgress = readingProgress; }
    public void setTotalChapters(int totalChapters) { this.totalChapters = totalChapters; }
    public void setCurrentChapterIndex(int currentChapterIndex) { this.currentChapterIndex = currentChapterIndex; }

    /**
     * 获取格式化的阅读时长字符串
     */
    public String getFormattedDuration() {
        long hours = totalDuration / (1000 * 60 * 60);
        long minutes = (totalDuration % (1000 * 60 * 60)) / (1000 * 60);
        
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        } else {
            return minutes + "分钟";
        }
    }

    /**
     * 获取格式化的阅读进度字符串
     */
    public String getFormattedProgress() {
        int percent = Math.round(readingProgress * 100);
        return percent + "%";
    }

    /**
     * 获取格式化的总字数字符串
     */
    public String getFormattedWordCount() {
        if (totalWordCount >= 10000) {
            return String.format("%.1f万字", totalWordCount / 10000.0);
        } else {
            return totalWordCount + "字";
        }
    }
}
