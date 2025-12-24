package com.example.read.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.read.domain.model.TTSState;
import com.example.read.domain.model.VoiceInfo;

import java.util.List;

/**
 * TTS仓库接口 - 管理语音朗读功能
 * 
 * 验证需求：10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7
 */
public interface TTSRepository {

    /**
     * 开始朗读
     * 验证需求：10.1 - 使用TTS引擎朗读当前章节内容
     * 
     * @param text 要朗读的文本
     * @param startPosition 开始位置
     */
    void startReading(String text, int startPosition);

    /**
     * 暂停朗读
     * 验证需求：10.6 - 暂停朗读时保存当前朗读位置
     */
    void pauseReading();

    /**
     * 恢复朗读
     * 验证需求：10.7 - 从暂停位置继续朗读
     */
    void resumeReading();

    /**
     * 停止朗读
     * 验证需求：10.2 - 显示播放控制界面（停止）
     */
    void stopReading();

    /**
     * 设置语速
     * 验证需求：10.3 - 立即应用新的语速设置
     * 
     * @param rate 语速（0.5 - 2.0）
     */
    void setSpeechRate(float rate);

    /**
     * 设置语音
     * 验证需求：10.4 - 切换到指定的语音引擎
     * 
     * @param voiceId 语音ID
     */
    void setVoice(String voiceId);

    /**
     * 获取TTS状态的LiveData
     * 
     * @return TTS状态LiveData
     */
    LiveData<TTSState> getTTSState();

    /**
     * 获取当前朗读位置的LiveData
     * 
     * @return 当前位置LiveData
     */
    LiveData<Integer> getCurrentPosition();

    /**
     * 获取可用语音列表
     * 
     * @return 可用语音列表
     */
    List<VoiceInfo> getAvailableVoices();

    /**
     * 设置当前章节ID
     * 
     * @param chapterId 章节ID
     */
    void setCurrentChapterId(long chapterId);

    /**
     * 设置章节完成回调
     * 验证需求：10.5 - 自动开始朗读下一章节
     * 
     * @param listener 章节完成监听器
     */
    void setOnChapterCompleteListener(OnChapterCompleteListener listener);

    /**
     * 检查TTS是否已初始化
     * 
     * @return 是否已初始化
     */
    boolean isInitialized();

    /**
     * 释放资源
     */
    void release();

    /**
     * 章节完成监听器
     */
    interface OnChapterCompleteListener {
        /**
         * 当前章节朗读完成
         * 验证需求：10.5 - 自动开始朗读下一章节
         * 
         * @param chapterId 完成的章节ID
         */
        void onChapterComplete(long chapterId);
    }
}
