package com.example.read.domain.service;

import android.content.Context;

import com.example.read.domain.model.TTSState;
import com.example.read.domain.model.VoiceInfo;

import java.util.List;

/**
 * TTS服务接口 - 定义语音朗读相关操作
 * 
 * 验证需求：10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7
 */
public interface TTSService {

    /**
     * 初始化TTS引擎
     * 
     * @param context Android上下文
     * @param callback 初始化和朗读回调
     */
    void initialize(Context context, TTSCallback callback);

    /**
     * 开始朗读文本
     * 验证需求：10.1 - 使用TTS引擎朗读当前章节内容
     * 
     * @param text 要朗读的文本
     * @param startPosition 开始位置（字符索引）
     */
    void speak(String text, int startPosition);

    /**
     * 暂停朗读
     * 验证需求：10.6 - 暂停朗读时保存当前朗读位置
     */
    void pause();

    /**
     * 恢复朗读
     * 验证需求：10.7 - 从暂停位置继续朗读
     */
    void resume();

    /**
     * 停止朗读
     * 验证需求：10.2 - 显示播放控制界面（停止）
     */
    void stop();

    /**
     * 设置语速
     * 验证需求：10.3 - 立即应用新的语速设置
     * 
     * @param rate 语速（0.5 - 2.0，1.0为正常速度）
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
     * 获取可用的语音列表
     * 
     * @return 可用语音列表
     */
    List<VoiceInfo> getAvailableVoices();

    /**
     * 检查TTS是否已初始化
     * 
     * @return 是否已初始化
     */
    boolean isInitialized();

    /**
     * 获取当前TTS状态
     * 
     * @return 当前状态
     */
    TTSState getCurrentState();

    /**
     * 获取当前朗读位置
     * 
     * @return 当前位置（字符索引）
     */
    int getCurrentPosition();

    /**
     * 设置当前章节ID
     * 
     * @param chapterId 章节ID
     */
    void setCurrentChapterId(long chapterId);

    /**
     * 关闭TTS引擎，释放资源
     */
    void shutdown();

    /**
     * TTS回调接口
     */
    interface TTSCallback {
        /**
         * TTS初始化完成
         */
        void onInitialized();

        /**
         * 开始朗读
         */
        void onStart();

        /**
         * 朗读进度更新
         * 
         * @param position 当前位置（字符索引）
         */
        void onProgress(int position);

        /**
         * 当前文本朗读完成
         * 验证需求：10.5 - 自动开始朗读下一章节
         */
        void onComplete();

        /**
         * 发生错误
         * 
         * @param error 错误信息
         */
        void onError(String error);

        /**
         * 状态变化
         * 
         * @param state 新状态
         */
        void onStateChanged(TTSState state);
    }
}
