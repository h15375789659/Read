package com.example.read.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.read.domain.model.TTSState;
import com.example.read.domain.model.VoiceInfo;
import com.example.read.domain.repository.TTSRepository;
import com.example.read.domain.service.TTSService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * TTS仓库实现类
 * 
 * 验证需求：10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7
 */
@Singleton
public class TTSRepositoryImpl implements TTSRepository {

    private final TTSService ttsService;
    private final Context context;

    private final MutableLiveData<TTSState> ttsStateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentPositionLiveData = new MutableLiveData<>(0);

    private OnChapterCompleteListener chapterCompleteListener;

    @Inject
    public TTSRepositoryImpl(
            @ApplicationContext Context context,
            TTSService ttsService
    ) {
        this.context = context;
        this.ttsService = ttsService;

        // 初始化TTS服务
        initializeTTS();
    }

    /**
     * 初始化TTS服务
     */
    private void initializeTTS() {
        ttsService.initialize(context, new TTSService.TTSCallback() {
            @Override
            public void onInitialized() {
                ttsStateLiveData.postValue(ttsService.getCurrentState());
            }

            @Override
            public void onStart() {
                ttsStateLiveData.postValue(ttsService.getCurrentState());
            }

            @Override
            public void onProgress(int position) {
                currentPositionLiveData.postValue(position);
            }

            @Override
            public void onComplete() {
                ttsStateLiveData.postValue(ttsService.getCurrentState());
                
                // 验证需求：10.5 - 通知章节完成，以便自动切换下一章
                if (chapterCompleteListener != null) {
                    TTSState state = ttsService.getCurrentState();
                    chapterCompleteListener.onChapterComplete(state.getCurrentChapterId());
                }
            }

            @Override
            public void onError(String error) {
                TTSState state = ttsService.getCurrentState();
                ttsStateLiveData.postValue(state);
            }

            @Override
            public void onStateChanged(TTSState state) {
                ttsStateLiveData.postValue(state);
            }
        });
    }

    /**
     * 开始朗读
     * 验证需求：10.1 - 使用TTS引擎朗读当前章节内容
     */
    @Override
    public void startReading(String text, int startPosition) {
        ttsService.speak(text, startPosition);
    }

    /**
     * 暂停朗读
     * 验证需求：10.6 - 暂停朗读时保存当前朗读位置
     */
    @Override
    public void pauseReading() {
        ttsService.pause();
    }

    /**
     * 恢复朗读
     * 验证需求：10.7 - 从暂停位置继续朗读
     */
    @Override
    public void resumeReading() {
        ttsService.resume();
    }

    /**
     * 停止朗读
     * 验证需求：10.2 - 显示播放控制界面（停止）
     */
    @Override
    public void stopReading() {
        ttsService.stop();
    }

    /**
     * 设置语速
     * 验证需求：10.3 - 立即应用新的语速设置
     */
    @Override
    public void setSpeechRate(float rate) {
        ttsService.setSpeechRate(rate);
    }

    /**
     * 设置语音
     * 验证需求：10.4 - 切换到指定的语音引擎
     */
    @Override
    public void setVoice(String voiceId) {
        ttsService.setVoice(voiceId);
    }

    /**
     * 获取TTS状态的LiveData
     */
    @Override
    public LiveData<TTSState> getTTSState() {
        return ttsStateLiveData;
    }

    /**
     * 获取当前朗读位置的LiveData
     */
    @Override
    public LiveData<Integer> getCurrentPosition() {
        return currentPositionLiveData;
    }

    /**
     * 获取可用语音列表
     */
    @Override
    public List<VoiceInfo> getAvailableVoices() {
        return ttsService.getAvailableVoices();
    }

    /**
     * 设置当前章节ID
     */
    @Override
    public void setCurrentChapterId(long chapterId) {
        ttsService.setCurrentChapterId(chapterId);
    }

    /**
     * 设置章节完成回调
     * 验证需求：10.5 - 自动开始朗读下一章节
     */
    @Override
    public void setOnChapterCompleteListener(OnChapterCompleteListener listener) {
        this.chapterCompleteListener = listener;
    }

    /**
     * 检查TTS是否已初始化
     */
    @Override
    public boolean isInitialized() {
        return ttsService.isInitialized();
    }

    /**
     * 释放资源
     */
    @Override
    public void release() {
        ttsService.shutdown();
    }
}
