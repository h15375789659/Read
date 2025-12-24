package com.example.read.data.service;

import android.content.Context;

import com.example.read.domain.model.TTSState;
import com.example.read.domain.model.TTSStatus;
import com.example.read.domain.model.VoiceInfo;
import com.example.read.domain.service.TTSService;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于测试的TTS服务实现
 * 不依赖Android TextToSpeech API，模拟TTS行为
 */
public class TestTTSService implements TTSService {

    private TTSCallback callback;
    private TTSState currentState;
    private boolean isInitialized = false;

    private String currentText;
    private int currentPosition;
    private int pausedPosition;
    private List<VoiceInfo> availableVoices;

    public TestTTSService() {
        this.currentState = new TTSState();
        this.availableVoices = new ArrayList<>();
        this.currentPosition = 0;
        this.pausedPosition = 0;

        // 添加默认语音
        availableVoices.add(new VoiceInfo("zh-CN-default", "中文默认", "中文", true));
        availableVoices.add(new VoiceInfo("en-US-default", "英文默认", "English", false));
    }

    /**
     * 用于测试的初始化方法（不需要Context）
     */
    public void initializeForTest(TTSCallback callback) {
        this.callback = callback;
        this.isInitialized = true;
        this.currentState.setStatus(TTSStatus.IDLE);

        if (callback != null) {
            callback.onInitialized();
            callback.onStateChanged(currentState.copy());
        }
    }

    @Override
    public void initialize(Context context, TTSCallback callback) {
        initializeForTest(callback);
    }

    @Override
    public void speak(String text, int startPosition) {
        if (!isInitialized) {
            if (callback != null) {
                callback.onError("TTS未初始化");
            }
            return;
        }

        if (text == null || text.isEmpty()) {
            if (callback != null) {
                callback.onError("文本内容为空");
            }
            return;
        }

        // 停止当前朗读
        if (currentState.isPlaying()) {
            stop();
        }

        // 保存当前文本
        this.currentText = text;
        this.currentPosition = Math.min(startPosition, text.length());
        this.pausedPosition = this.currentPosition;

        // 更新状态
        currentState.setStatus(TTSStatus.PLAYING);
        currentState.setCurrentPosition(this.currentPosition);
        notifyStateChanged();

        if (callback != null) {
            callback.onStart();
        }
    }

    @Override
    public void pause() {
        if (!isInitialized || !currentState.isPlaying()) {
            return;
        }

        pausedPosition = currentPosition;
        currentState.setStatus(TTSStatus.PAUSED);
        currentState.setCurrentPosition(pausedPosition);
        notifyStateChanged();
    }

    @Override
    public void resume() {
        if (!isInitialized || !currentState.isPaused()) {
            return;
        }

        if (currentText != null && !currentText.isEmpty()) {
            currentPosition = pausedPosition;
            currentState.setStatus(TTSStatus.PLAYING);
            currentState.setCurrentPosition(currentPosition);
            notifyStateChanged();

            if (callback != null) {
                callback.onStart();
            }
        }
    }

    @Override
    public void stop() {
        if (!isInitialized) {
            return;
        }

        currentState.setStatus(TTSStatus.IDLE);
        currentPosition = 0;
        pausedPosition = 0;
        currentState.setCurrentPosition(0);
        notifyStateChanged();
    }

    @Override
    public void setSpeechRate(float rate) {
        if (!isInitialized) {
            return;
        }

        float clampedRate = Math.max(0.5f, Math.min(2.0f, rate));
        currentState.setSpeechRate(clampedRate);
        notifyStateChanged();
    }

    @Override
    public void setVoice(String voiceId) {
        if (!isInitialized || voiceId == null) {
            return;
        }

        for (VoiceInfo voice : availableVoices) {
            if (voice.getVoiceId().equals(voiceId)) {
                currentState.setCurrentVoiceId(voiceId);
                notifyStateChanged();
                return;
            }
        }
    }

    @Override
    public List<VoiceInfo> getAvailableVoices() {
        return new ArrayList<>(availableVoices);
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public TTSState getCurrentState() {
        return currentState.copy();
    }

    @Override
    public int getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public void setCurrentChapterId(long chapterId) {
        currentState.setCurrentChapterId(chapterId);
        notifyStateChanged();
    }

    @Override
    public void shutdown() {
        isInitialized = false;
        currentState.setStatus(TTSStatus.IDLE);
        notifyStateChanged();
    }

    /**
     * 模拟朗读进度更新
     */
    public void simulateProgress(int position) {
        if (!isInitialized || !currentState.isPlaying()) {
            return;
        }

        currentPosition = position;
        currentState.setCurrentPosition(position);

        if (callback != null) {
            callback.onProgress(position);
        }
    }

    /**
     * 模拟朗读完成
     */
    public void simulateComplete() {
        if (!isInitialized) {
            return;
        }

        currentState.setStatus(TTSStatus.IDLE);
        currentPosition = currentText != null ? currentText.length() : 0;
        currentState.setCurrentPosition(currentPosition);
        notifyStateChanged();

        if (callback != null) {
            callback.onComplete();
        }
    }

    /**
     * 模拟错误
     */
    public void simulateError(String error) {
        currentState.setStatus(TTSStatus.ERROR);
        currentState.setErrorMessage(error);
        notifyStateChanged();

        if (callback != null) {
            callback.onError(error);
        }
    }

    private void notifyStateChanged() {
        if (callback != null) {
            callback.onStateChanged(currentState.copy());
        }
    }

    // 用于测试的辅助方法
    public String getCurrentText() {
        return currentText;
    }

    public int getPausedPosition() {
        return pausedPosition;
    }
}
