package com.example.read.data.service;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

import com.example.read.domain.model.TTSState;
import com.example.read.domain.model.TTSStatus;
import com.example.read.domain.model.VoiceInfo;
import com.example.read.domain.service.TTSService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * TTS服务实现类 - 使用Android TextToSpeech API
 * 
 * 验证需求：10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7
 */
@Singleton
public class TTSServiceImpl implements TTSService {

    private static final String TAG = "TTSServiceImpl";
    private static final String UTTERANCE_ID_PREFIX = "tts_utterance_";

    private TextToSpeech textToSpeech;
    private TTSCallback callback;
    private TTSState currentState;
    private boolean isInitialized = false;

    // 当前朗读的文本和位置
    private String currentText;
    private int currentPosition;
    private int pausedPosition;
    private List<VoiceInfo> availableVoices;

    @Inject
    public TTSServiceImpl() {
        this.currentState = new TTSState();
        this.availableVoices = new ArrayList<>();
        this.currentPosition = 0;
        this.pausedPosition = 0;
    }

    /**
     * 初始化TTS引擎
     */
    @Override
    public void initialize(Context context, TTSCallback callback) {
        this.callback = callback;

        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // 设置默认语言为中文
                int result = textToSpeech.setLanguage(Locale.CHINESE);
                if (result == TextToSpeech.LANG_MISSING_DATA || 
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // 如果中文不支持，尝试使用默认语言
                    textToSpeech.setLanguage(Locale.getDefault());
                    Log.w(TAG, "中文语言不支持，使用默认语言");
                }

                // 设置朗读进度监听器
                setupProgressListener();

                // 加载可用语音列表
                loadAvailableVoices();

                isInitialized = true;
                currentState.setStatus(TTSStatus.IDLE);
                notifyStateChanged();

                if (callback != null) {
                    callback.onInitialized();
                }

                Log.d(TAG, "TTS初始化成功");
            } else {
                isInitialized = false;
                currentState.setStatus(TTSStatus.ERROR);
                currentState.setErrorMessage("TTS初始化失败");
                notifyStateChanged();

                if (callback != null) {
                    callback.onError("TTS初始化失败");
                }

                Log.e(TAG, "TTS初始化失败");
            }
        });
    }


    /**
     * 设置朗读进度监听器
     */
    private void setupProgressListener() {
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                currentState.setStatus(TTSStatus.PLAYING);
                notifyStateChanged();

                if (callback != null) {
                    callback.onStart();
                }

                Log.d(TAG, "开始朗读: " + utteranceId);
            }

            @Override
            public void onDone(String utteranceId) {
                currentState.setStatus(TTSStatus.IDLE);
                currentPosition = currentText != null ? currentText.length() : 0;
                currentState.setCurrentPosition(currentPosition);
                notifyStateChanged();

                if (callback != null) {
                    // 验证需求：10.5 - 当前章节朗读完成，通知回调以便自动切换下一章
                    callback.onComplete();
                }

                Log.d(TAG, "朗读完成: " + utteranceId);
            }

            @Override
            public void onError(String utteranceId) {
                currentState.setStatus(TTSStatus.ERROR);
                currentState.setErrorMessage("朗读出错");
                notifyStateChanged();

                if (callback != null) {
                    callback.onError("朗读出错");
                }

                Log.e(TAG, "朗读出错: " + utteranceId);
            }

            @Override
            public void onRangeStart(String utteranceId, int start, int end, int frame) {
                // 更新当前朗读位置
                currentPosition = start;
                currentState.setCurrentPosition(currentPosition);

                if (callback != null) {
                    callback.onProgress(currentPosition);
                }
            }
        });
    }

    /**
     * 加载可用语音列表
     */
    private void loadAvailableVoices() {
        availableVoices.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Set<Voice> voices = textToSpeech.getVoices();
            if (voices != null) {
                Voice defaultVoice = textToSpeech.getDefaultVoice();
                String defaultVoiceId = defaultVoice != null ? defaultVoice.getName() : null;

                for (Voice voice : voices) {
                    // 只添加中文和英文语音
                    String language = voice.getLocale().getLanguage();
                    if ("zh".equals(language) || "en".equals(language)) {
                        VoiceInfo voiceInfo = new VoiceInfo(
                                voice.getName(),
                                voice.getName(),
                                voice.getLocale().getDisplayName(),
                                voice.getName().equals(defaultVoiceId)
                        );
                        availableVoices.add(voiceInfo);
                    }
                }
            }
        }

        // 如果没有找到语音，添加一个默认项
        if (availableVoices.isEmpty()) {
            availableVoices.add(new VoiceInfo("default", "默认语音", "中文", true));
        }

        Log.d(TAG, "加载了 " + availableVoices.size() + " 个可用语音");
    }

    /**
     * 开始朗读文本
     * 验证需求：10.1 - 使用TTS引擎朗读当前章节内容
     */
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
        stop();

        // 保存当前文本
        this.currentText = text;
        this.currentPosition = startPosition;
        this.pausedPosition = startPosition;

        // 从指定位置开始朗读
        String textToSpeak = text.substring(Math.min(startPosition, text.length()));

        // 生成唯一的utterance ID
        String utteranceId = UTTERANCE_ID_PREFIX + UUID.randomUUID().toString();

        // 使用Bundle设置参数
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

        // 开始朗读
        int result = textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId);

        if (result == TextToSpeech.SUCCESS) {
            currentState.setStatus(TTSStatus.PLAYING);
            currentState.setCurrentPosition(startPosition);
            notifyStateChanged();
            Log.d(TAG, "开始朗读，起始位置: " + startPosition);
        } else {
            currentState.setStatus(TTSStatus.ERROR);
            currentState.setErrorMessage("启动朗读失败");
            notifyStateChanged();

            if (callback != null) {
                callback.onError("启动朗读失败");
            }
            Log.e(TAG, "启动朗读失败");
        }
    }

    /**
     * 暂停朗读
     * 验证需求：10.6 - 暂停朗读时保存当前朗读位置
     */
    @Override
    public void pause() {
        if (!isInitialized || !currentState.isPlaying()) {
            return;
        }

        // 保存当前位置
        pausedPosition = currentPosition;

        // 停止TTS（Android TTS没有真正的暂停功能）
        textToSpeech.stop();

        currentState.setStatus(TTSStatus.PAUSED);
        currentState.setCurrentPosition(pausedPosition);
        notifyStateChanged();

        Log.d(TAG, "暂停朗读，位置: " + pausedPosition);
    }

    /**
     * 恢复朗读
     * 验证需求：10.7 - 从暂停位置继续朗读
     */
    @Override
    public void resume() {
        if (!isInitialized || !currentState.isPaused()) {
            return;
        }

        if (currentText != null && !currentText.isEmpty()) {
            // 从暂停位置继续朗读
            speak(currentText, pausedPosition);
            Log.d(TAG, "恢复朗读，位置: " + pausedPosition);
        }
    }

    /**
     * 停止朗读
     * 验证需求：10.2 - 显示播放控制界面（停止）
     */
    @Override
    public void stop() {
        if (!isInitialized) {
            return;
        }

        textToSpeech.stop();

        currentState.setStatus(TTSStatus.IDLE);
        currentPosition = 0;
        pausedPosition = 0;
        currentState.setCurrentPosition(0);
        notifyStateChanged();

        Log.d(TAG, "停止朗读");
    }


    /**
     * 设置语速
     * 验证需求：10.3 - 立即应用新的语速设置
     */
    @Override
    public void setSpeechRate(float rate) {
        if (!isInitialized) {
            return;
        }

        // 限制语速范围在0.5到2.0之间
        float clampedRate = Math.max(0.5f, Math.min(2.0f, rate));

        int result = textToSpeech.setSpeechRate(clampedRate);

        if (result == TextToSpeech.SUCCESS) {
            currentState.setSpeechRate(clampedRate);
            notifyStateChanged();
            Log.d(TAG, "设置语速: " + clampedRate);
        } else {
            Log.e(TAG, "设置语速失败");
        }
    }

    /**
     * 设置语音
     * 验证需求：10.4 - 切换到指定的语音引擎
     */
    @Override
    public void setVoice(String voiceId) {
        if (!isInitialized || voiceId == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Set<Voice> voices = textToSpeech.getVoices();
            if (voices != null) {
                for (Voice voice : voices) {
                    if (voice.getName().equals(voiceId)) {
                        int result = textToSpeech.setVoice(voice);
                        if (result == TextToSpeech.SUCCESS) {
                            currentState.setCurrentVoiceId(voiceId);
                            notifyStateChanged();
                            Log.d(TAG, "设置语音: " + voiceId);
                        } else {
                            Log.e(TAG, "设置语音失败: " + voiceId);
                        }
                        return;
                    }
                }
            }
        }

        Log.w(TAG, "未找到语音: " + voiceId);
    }

    /**
     * 获取可用的语音列表
     */
    @Override
    public List<VoiceInfo> getAvailableVoices() {
        return new ArrayList<>(availableVoices);
    }

    /**
     * 检查TTS是否已初始化
     */
    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 获取当前TTS状态
     */
    @Override
    public TTSState getCurrentState() {
        return currentState.copy();
    }

    /**
     * 获取当前朗读位置
     */
    @Override
    public int getCurrentPosition() {
        return currentPosition;
    }

    /**
     * 设置当前章节ID
     */
    @Override
    public void setCurrentChapterId(long chapterId) {
        currentState.setCurrentChapterId(chapterId);
        notifyStateChanged();
    }

    /**
     * 关闭TTS引擎，释放资源
     */
    @Override
    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        isInitialized = false;
        currentState.setStatus(TTSStatus.IDLE);
        notifyStateChanged();

        Log.d(TAG, "TTS引擎已关闭");
    }

    /**
     * 通知状态变化
     */
    private void notifyStateChanged() {
        if (callback != null) {
            callback.onStateChanged(currentState.copy());
        }
    }
}
