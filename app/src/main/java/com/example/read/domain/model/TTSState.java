package com.example.read.domain.model;

/**
 * TTS状态模型
 * 用于表示语音朗读的当前状态
 */
public class TTSState {
    private TTSStatus status;
    private float speechRate;
    private String currentVoiceId;
    private int currentPosition;
    private long currentChapterId;
    private String errorMessage;

    public TTSState() {
        this.status = TTSStatus.IDLE;
        this.speechRate = 1.0f;
        this.currentPosition = 0;
        this.currentChapterId = -1;
    }

    public TTSState(TTSStatus status, float speechRate, String currentVoiceId, 
                    int currentPosition, long currentChapterId) {
        this.status = status;
        this.speechRate = speechRate;
        this.currentVoiceId = currentVoiceId;
        this.currentPosition = currentPosition;
        this.currentChapterId = currentChapterId;
    }

    // 便捷方法
    public boolean isPlaying() {
        return status == TTSStatus.PLAYING;
    }

    public boolean isPaused() {
        return status == TTSStatus.PAUSED;
    }

    public boolean isIdle() {
        return status == TTSStatus.IDLE;
    }

    public boolean hasError() {
        return status == TTSStatus.ERROR;
    }

    // Getters
    public TTSStatus getStatus() { return status; }
    public float getSpeechRate() { return speechRate; }
    public String getCurrentVoiceId() { return currentVoiceId; }
    public int getCurrentPosition() { return currentPosition; }
    public long getCurrentChapterId() { return currentChapterId; }
    public String getErrorMessage() { return errorMessage; }

    // Setters
    public void setStatus(TTSStatus status) { this.status = status; }
    public void setSpeechRate(float speechRate) { this.speechRate = speechRate; }
    public void setCurrentVoiceId(String currentVoiceId) { this.currentVoiceId = currentVoiceId; }
    public void setCurrentPosition(int currentPosition) { this.currentPosition = currentPosition; }
    public void setCurrentChapterId(long currentChapterId) { this.currentChapterId = currentChapterId; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    /**
     * 创建一个副本
     */
    public TTSState copy() {
        TTSState copy = new TTSState();
        copy.status = this.status;
        copy.speechRate = this.speechRate;
        copy.currentVoiceId = this.currentVoiceId;
        copy.currentPosition = this.currentPosition;
        copy.currentChapterId = this.currentChapterId;
        copy.errorMessage = this.errorMessage;
        return copy;
    }

    @Override
    public String toString() {
        return "TTSState{" +
                "status=" + status +
                ", speechRate=" + speechRate +
                ", currentVoiceId='" + currentVoiceId + '\'' +
                ", currentPosition=" + currentPosition +
                ", currentChapterId=" + currentChapterId +
                '}';
    }
}
