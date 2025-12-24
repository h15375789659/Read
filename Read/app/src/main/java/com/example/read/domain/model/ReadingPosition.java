package com.example.read.domain.model;

/**
 * 阅读位置领域模型
 * 用于保存和恢复阅读位置，特别是在搜索功能中使用
 */
public class ReadingPosition {
    private long chapterId;
    private int position;

    public ReadingPosition() {}

    public ReadingPosition(long chapterId, int position) {
        this.chapterId = chapterId;
        this.position = position;
    }

    // Getters
    public long getChapterId() {
        return chapterId;
    }

    public int getPosition() {
        return position;
    }

    // Setters
    public void setChapterId(long chapterId) {
        this.chapterId = chapterId;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ReadingPosition that = (ReadingPosition) obj;
        return chapterId == that.chapterId && position == that.position;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(chapterId);
        result = 31 * result + position;
        return result;
    }

    @Override
    public String toString() {
        return "ReadingPosition{" +
                "chapterId=" + chapterId +
                ", position=" + position +
                '}';
    }
}
