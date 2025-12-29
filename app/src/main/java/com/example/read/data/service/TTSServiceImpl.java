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
 * TTS服务实现类 - 使用Android系统TextToSpeech API
 * 
 * 只查找中文语音，提供稳定的离线语音朗读功能
 * 
 * 验证需求：10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7
 */
@Singleton
public class TTSServiceImpl implements TTSService {

    private static final String TAG = "TTSServiceImpl";
    // 诊断日志标签，用于调试TTS问题
    private static final String DIAG_TAG = "TTS_DIAG";
    private static final String UTTERANCE_ID_PREFIX = "tts_utterance_";

    private TextToSpeech textToSpeech;
    private TTSCallback callback;
    private TTSState currentState;
    private boolean isInitialized = false;

    // 当前朗读的文本和位置
    private String currentText;
    private int currentPosition;
    private int pausedPosition;
    private int startOffset;  // 朗读起始位置偏移量
    private List<VoiceInfo> availableVoices;

    @Inject
    public TTSServiceImpl() {
        this.currentState = new TTSState();
        this.availableVoices = new ArrayList<>();
        this.currentPosition = 0;
        this.pausedPosition = 0;
        this.startOffset = 0;
    }

    /**
     * 初始化TTS引擎
     */
    @Override
    public void initialize(Context context, TTSCallback callback) {
        this.callback = callback;

        Log.d(DIAG_TAG, "========== TTS初始化开始 ==========");

        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.d(DIAG_TAG, "TTS引擎创建成功");
                
                // 获取默认引擎信息
                String defaultEngine = textToSpeech.getDefaultEngine();
                Log.d(DIAG_TAG, "默认TTS引擎: " + defaultEngine);
                
                // 尝试设置中文语言
                int result = textToSpeech.setLanguage(Locale.CHINESE);
                Log.d(DIAG_TAG, "设置中文语言结果: " + getLanguageResultString(result));
                
                if (result == TextToSpeech.LANG_MISSING_DATA || 
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // 尝试简体中文
                    result = textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE);
                    Log.d(DIAG_TAG, "设置简体中文结果: " + getLanguageResultString(result));
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA || 
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // 尝试中国区域
                        result = textToSpeech.setLanguage(new Locale("zh", "CN"));
                        Log.d(DIAG_TAG, "设置zh_CN结果: " + getLanguageResultString(result));
                    }
                }

                // 设置朗读进度监听器
                setupProgressListener();

                // 加载可用语音列表（只加载中文）
                loadAvailableVoices();

                isInitialized = true;
                currentState.setStatus(TTSStatus.IDLE);
                notifyStateChanged();

                if (callback != null) {
                    callback.onInitialized();
                }

                Log.d(DIAG_TAG, "========== TTS初始化完成 ==========");
                Log.d(TAG, "TTS初始化成功");
            } else {
                isInitialized = false;
                currentState.setStatus(TTSStatus.ERROR);
                currentState.setErrorMessage("TTS初始化失败");
                notifyStateChanged();

                if (callback != null) {
                    callback.onError("TTS初始化失败");
                }

                Log.e(DIAG_TAG, "TTS引擎创建失败，status=" + status);
                Log.e(TAG, "TTS初始化失败");
            }
        });
    }

    /**
     * 获取语言设置结果的字符串描述
     */
    private String getLanguageResultString(int result) {
        switch (result) {
            case TextToSpeech.LANG_AVAILABLE:
                return "LANG_AVAILABLE (语言可用)";
            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                return "LANG_COUNTRY_AVAILABLE (语言和国家可用)";
            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                return "LANG_COUNTRY_VAR_AVAILABLE (语言、国家和变体可用)";
            case TextToSpeech.LANG_MISSING_DATA:
                return "LANG_MISSING_DATA (缺少语言数据)";
            case TextToSpeech.LANG_NOT_SUPPORTED:
                return "LANG_NOT_SUPPORTED (语言不支持)";
            default:
                return "未知结果: " + result;
        }
    }


    /**
     * 设置朗读进度监听器
     * 优化：每次回调都通知，由UI层决定是否需要更新高亮
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
                // 更新当前朗读位置（相对于截取后的文本）
                currentPosition = start;
                // 计算在原文中的实际位置
                int actualPosition = startOffset + start;
                currentState.setCurrentPosition(actualPosition);

                // 每次都通知进度变化，由UI层判断是否需要更新高亮
                if (callback != null) {
                    callback.onProgress(actualPosition);
                }
            }
        });
    }

    /**
     * 加载可用语音列表
     * 只收集中文语音，按地区分组显示
     */
    private void loadAvailableVoices() {
        availableVoices.clear();
        
        // 诊断信息，用于界面显示
        StringBuilder diagInfo = new StringBuilder();
        
        Log.d(DIAG_TAG, "---------- 开始扫描语音 ----------");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Set<Voice> voices = textToSpeech.getVoices();
            
            // 获取TTS引擎信息
            String defaultEngine = textToSpeech.getDefaultEngine();
            diagInfo.append("TTS引擎: ").append(defaultEngine != null ? defaultEngine : "未知").append("\n");
            
            if (voices == null) {
                Log.e(DIAG_TAG, "getVoices()返回null，系统可能没有TTS引擎");
                diagInfo.append("错误: 无法获取语音列表\n");
            } else {
                diagInfo.append("系统语音总数: ").append(voices.size()).append("\n");
                Log.d(DIAG_TAG, "系统共有 " + voices.size() + " 个语音");
                
                Voice defaultVoice = textToSpeech.getDefaultVoice();
                String defaultVoiceId = defaultVoice != null ? defaultVoice.getName() : null;
                Log.d(DIAG_TAG, "默认语音: " + defaultVoiceId);

                // 分类存储中文语音列表
                List<Voice> mandarinVoices = new ArrayList<>();   // 普通话
                List<Voice> taiwanVoices = new ArrayList<>();     // 台湾
                List<Voice> cantoneseVoices = new ArrayList<>();  // 粤语
                
                int chineseCount = 0;
                int otherCount = 0;

                for (Voice voice : voices) {
                    String language = voice.getLocale().getLanguage();
                    String country = voice.getLocale().getCountry();
                    String voiceName = voice.getName();
                    
                    // 只收集中文语音
                    boolean isChinese = "zh".equals(language) || "cmn".equals(language) || "yue".equals(language);
                    
                    if (isChinese) {
                        chineseCount++;
                        Log.d(DIAG_TAG, "发现中文语音: " + voiceName + " [" + language + "_" + country + "]");
                        
                        String locale = voice.getLocale().toString();
                        
                        // 判断地区并分类
                        if (locale.contains("TW") || locale.contains("Hant")) {
                            taiwanVoices.add(voice);
                        } else if (locale.contains("HK") || voiceName.toLowerCase().contains("cantonese") 
                                || voiceName.toLowerCase().contains("yue") || "yue".equals(language)) {
                            cantoneseVoices.add(voice);
                        } else {
                            mandarinVoices.add(voice);
                        }
                    } else {
                        otherCount++;
                    }
                }
                
                diagInfo.append("中文语音: ").append(chineseCount).append(" 个\n");
                Log.d(DIAG_TAG, "中文语音: " + chineseCount + " 个, 其他语音: " + otherCount + " 个");
                Log.d(DIAG_TAG, "普通话: " + mandarinVoices.size() + ", 台湾: " + taiwanVoices.size() + ", 粤语: " + cantoneseVoices.size());

                // 添加普通话语音（最多3个）
                int mandarinCount = 0;
                for (Voice voice : mandarinVoices) {
                    if (mandarinCount >= 3) break;
                    String friendlyName = "普通话 " + (mandarinCount + 1);
                    addVoiceToList(voice, friendlyName, defaultVoiceId);
                    mandarinCount++;
                }
                
                // 添加台湾语音（最多2个）
                int taiwanCount = 0;
                for (Voice voice : taiwanVoices) {
                    if (taiwanCount >= 2) break;
                    String friendlyName = "台湾 " + (taiwanCount + 1);
                    addVoiceToList(voice, friendlyName, defaultVoiceId);
                    taiwanCount++;
                }
                
                // 添加粤语语音（最多2个）
                int cantoneseCount = 0;
                for (Voice voice : cantoneseVoices) {
                    if (cantoneseCount >= 2) break;
                    String friendlyName = "粤语 " + (cantoneseCount + 1);
                    addVoiceToList(voice, friendlyName, defaultVoiceId);
                    cantoneseCount++;
                }
            }
        } else {
            Log.d(DIAG_TAG, "Android版本低于5.0，无法获取语音列表");
            diagInfo.append("错误: Android版本过低\n");
        }

        // 如果没有找到中文语音，添加一个默认项（使用系统默认）
        if (availableVoices.isEmpty()) {
            Log.w(DIAG_TAG, "未找到中文语音，添加默认语音选项");
            diagInfo.append("警告: 未找到中文语音！\n");
            diagInfo.append("请在系统设置中安装中文TTS引擎");
            availableVoices.add(new VoiceInfo("default", "默认语音", diagInfo.toString(), true));
        }

        Log.d(DIAG_TAG, "---------- 语音扫描完成，共 " + availableVoices.size() + " 个可用 ----------");
        
        for (VoiceInfo voice : availableVoices) {
            Log.d(TAG, "可用语音: " + voice.getName() + " (ID: " + voice.getVoiceId() + ")");
        }
    }

    /**
     * 添加语音到列表
     */
    private void addVoiceToList(Voice voice, String friendlyName, String defaultVoiceId) {
        String description = getVoiceDescription(voice);
        VoiceInfo voiceInfo = new VoiceInfo(
                voice.getName(),
                friendlyName,
                description,
                voice.getName().equals(defaultVoiceId)
        );
        availableVoices.add(voiceInfo);
    }

    /**
     * 获取语音描述
     */
    private String getVoiceDescription(Voice voice) {
        String locale = voice.getLocale().getDisplayName(Locale.CHINESE);
        int quality = voice.getQuality();
        
        String qualityStr;
        if (quality >= Voice.QUALITY_VERY_HIGH) {
            qualityStr = "超高品质";
        } else if (quality >= Voice.QUALITY_HIGH) {
            qualityStr = "高品质";
        } else if (quality >= Voice.QUALITY_NORMAL) {
            qualityStr = "标准品质";
        } else {
            qualityStr = "普通品质";
        }
        
        return locale + " - " + qualityStr;
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
        textToSpeech.stop();

        // 保存当前文本和起始偏移量
        this.currentText = text;
        this.startOffset = Math.min(startPosition, text.length());
        this.currentPosition = 0;
        this.pausedPosition = this.startOffset;

        // 从指定位置开始朗读
        String textToSpeak = text.substring(this.startOffset);

        if (textToSpeak.isEmpty()) {
            // 如果从指定位置开始没有内容，触发完成回调
            currentState.setStatus(TTSStatus.IDLE);
            notifyStateChanged();
            if (callback != null) {
                callback.onComplete();
            }
            return;
        }

        // 生成唯一的utterance ID
        String utteranceId = UTTERANCE_ID_PREFIX + UUID.randomUUID().toString();

        // 使用Bundle设置参数
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

        // 开始朗读
        int result = textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId);

        if (result == TextToSpeech.SUCCESS) {
            currentState.setStatus(TTSStatus.PLAYING);
            currentState.setCurrentPosition(this.startOffset);
            notifyStateChanged();
            Log.d(TAG, "开始朗读，起始位置: " + this.startOffset);
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

        // 保存当前位置（加上起始偏移量，得到在原文中的实际位置）
        // currentPosition是相对于当前朗读文本的位置，需要加上startOffset
        pausedPosition = startOffset + currentPosition;

        // 停止TTS（Android TTS没有真正的暂停功能）
        textToSpeech.stop();

        currentState.setStatus(TTSStatus.PAUSED);
        currentState.setCurrentPosition(pausedPosition);
        notifyStateChanged();

        Log.d(TAG, "暂停朗读，位置: " + pausedPosition + " (startOffset=" + startOffset + ", currentPosition=" + currentPosition + ")");
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
        startOffset = 0;
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
