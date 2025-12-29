package com.example.read.domain.model;

/**
 * 段落信息模型
 * 用于TTS朗读时选择起始段落
 */
public class ParagraphInfo {
    private String preview;      // 段落预览文本（前50个字符）
    private String fullText;     // 段落完整文本
    private int startPosition;   // 在章节中的起始位置（字符索引）
    private int paragraphIndex;  // 段落索引

    public ParagraphInfo() {}

    public ParagraphInfo(String preview, String fullText, int startPosition, int paragraphIndex) {
        this.preview = preview;
        this.fullText = fullText;
        this.startPosition = startPosition;
        this.paragraphIndex = paragraphIndex;
    }

    // Getters
    public String getPreview() { return preview; }
    public String getFullText() { return fullText; }
    public int getStartPosition() { return startPosition; }
    public int getParagraphIndex() { return paragraphIndex; }

    // Setters
    public void setPreview(String preview) { this.preview = preview; }
    public void setFullText(String fullText) { this.fullText = fullText; }
    public void setStartPosition(int startPosition) { this.startPosition = startPosition; }
    public void setParagraphIndex(int paragraphIndex) { this.paragraphIndex = paragraphIndex; }

    /**
     * 从文本创建预览
     * @param text 完整文本
     * @param maxLength 最大预览长度
     * @return 预览文本
     */
    public static String createPreview(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }
}
