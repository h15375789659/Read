package com.example.read.domain.model;

/**
 * 小说阅读统计模型 - 用于阅读排行
 */
public class NovelReadingStats {
    private long novelId;
    private String novelTitle;
    private String novelAuthor;
    private long totalDuration; // 总阅读时长（毫秒）
    private int totalCharCount; // 总阅读字数

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
    public int getTotalCharCount() { return totalCharCount; }

    // Setters
    public void setNovelId(long novelId) { this.novelId = novelId; }
    public void setNovelTitle(String novelTitle) { this.novelTitle = novelTitle; }
    public void setNovelAuthor(String novelAuthor) { this.novelAuthor = novelAuthor; }
    public void setTotalDuration(long totalDuration) { this.totalDuration = totalDuration; }
    public void setTotalCharCount(int totalCharCount) { this.totalCharCount = totalCharCount; }

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
}
