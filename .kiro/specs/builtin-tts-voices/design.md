# Design Document: 内置TTS音色功能

## Overview

本设计文档描述如何在小说阅读器应用中集成Sherpa-ONNX离线TTS引擎，内置两个中文音色（一男一女），并与现有的TTS功能无缝集成。

## Architecture

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      TTSService (接口)                       │
├─────────────────────────────────────────────────────────────┤
│                    TTSServiceImpl (实现)                     │
│  ┌─────────────────────┐    ┌─────────────────────────────┐ │
│  │   SystemTTSEngine   │    │   BuiltInTTSEngine          │ │
│  │  (Android系统TTS)    │    │  (Sherpa-ONNX引擎)          │ │
│  │                     │    │  ┌─────────┐ ┌─────────┐   │ │
│  │  - 普通话语音        │    │  │ 女声模型 │ │ 男声模型 │   │ │
│  │  - 台湾语音          │    │  └─────────┘ └─────────┘   │ │
│  │  - 粤语语音          │    │                            │ │
│  └─────────────────────┘    └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 引擎切换策略

```
用户选择音色
    │
    ▼
┌─────────────────┐
│ 是内置音色？     │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
   是        否
    │         │
    ▼         ▼
使用Sherpa  使用系统
ONNX引擎    TTS引擎
```

## Components and Interfaces

### 1. VoiceInfo 模型扩展

```java
public class VoiceInfo {
    private String voiceId;
    private String name;
    private String language;
    private boolean isDefault;
    private boolean isBuiltIn;        // 新增：是否为内置音色
    private String modelPath;         // 新增：模型文件路径（仅内置音色）
    
    // 内置音色ID常量
    public static final String BUILTIN_FEMALE_ID = "builtin_female_zh";
    public static final String BUILTIN_MALE_ID = "builtin_male_zh";
}
```

### 2. BuiltInTTSEngine 接口

```java
/**
 * 内置TTS引擎接口
 * 使用Sherpa-ONNX实现离线语音合成
 */
public interface BuiltInTTSEngine {
    
    /**
     * 初始化引擎
     * @param context Android上下文
     * @param modelPath 模型文件路径
     * @return 是否初始化成功
     */
    boolean initialize(Context context, String modelPath);
    
    /**
     * 合成语音
     * @param text 要合成的文本
     * @param speed 语速 (0.5 - 2.0)
     * @return 音频数据 (PCM格式)
     */
    float[] synthesize(String text, float speed);
    
    /**
     * 获取采样率
     */
    int getSampleRate();
    
    /**
     * 释放资源
     */
    void release();
    
    /**
     * 是否已初始化
     */
    boolean isInitialized();
}
```

### 3. SherpaOnnxTTSEngine 实现

```java
/**
 * Sherpa-ONNX TTS引擎实现
 */
public class SherpaOnnxTTSEngine implements BuiltInTTSEngine {
    
    private OfflineTts tts;
    private int sampleRate;
    private boolean initialized = false;
    
    @Override
    public boolean initialize(Context context, String modelPath) {
        // 从assets加载模型配置
        // 初始化Sherpa-ONNX OfflineTts
    }
    
    @Override
    public float[] synthesize(String text, float speed) {
        // 调用Sherpa-ONNX生成音频
        // 返回PCM float数组
    }
}
```

### 4. TTSServiceImpl 扩展

```java
@Singleton
public class TTSServiceImpl implements TTSService {
    
    // 现有字段...
    
    // 新增字段
    private BuiltInTTSEngine builtInEngine;
    private AudioTrack audioTrack;
    private boolean useBuiltInEngine = false;
    private String currentBuiltInVoiceId;
    
    // 新增方法
    private void initializeBuiltInEngine(Context context);
    private void speakWithBuiltInEngine(String text, int startPosition);
    private void switchToBuiltInEngine(String voiceId);
    private void switchToSystemEngine();
}
```

## Data Models

### 模型文件结构

```
app/src/main/assets/
└── tts_models/
    ├── female/                    # 女声模型
    │   ├── model.onnx            # VITS模型文件
    │   ├── lexicon.txt           # 词典文件
    │   ├── tokens.txt            # 音素文件
    │   └── model.json            # 模型配置
    └── male/                      # 男声模型
        ├── model.onnx
        ├── lexicon.txt
        ├── tokens.txt
        └── model.json
```

### 模型选择

基于Sherpa-ONNX官方提供的中文模型，推荐使用：

| 音色 | 模型 | 大小 | 采样率 |
|------|------|------|--------|
| 女声 | vits-zh-hf-fanchen-C (speaker 0) | ~116MB | 16kHz |
| 男声 | vits-zh-hf-fanchen-wnj | ~116MB | 16kHz |

**注意**：由于模型较大，建议使用更小的模型或考虑首次启动时下载。

### 替代方案：使用更小的模型

考虑到APK体积，可以使用 `sherpa-onnx-vits-zh-ll` 模型：
- 大小：约115MB（5个说话人）
- 可以选择其中2个说话人（一男一女）
- 采样率：16kHz

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: 内置音色列表完整性与排序

*For any* 初始化完成的TTS服务，调用 `getAvailableVoices()` 返回的列表应满足：
1. 包含至少2个 `isBuiltIn=true` 的音色
2. 所有内置音色排在系统音色之前
3. 内置音色的 `voiceId` 以 "builtin_" 开头

**Validates: Requirements 2.1, 2.2, 3.1, 3.2**

### Property 2: 音色切换一致性

*For any* 有效的 `voiceId`（无论是内置音色还是系统音色），调用 `setVoice(voiceId)` 后，`getCurrentState().getCurrentVoiceId()` 应等于该 `voiceId`，且后续的 `speak()` 调用应使用对应的引擎。

**Validates: Requirements 2.4, 4.1, 5.1, 5.2**

### Property 3: 语速设置有效性

*For any* 语速值 `rate`（包括边界值和超出范围的值），调用 `setSpeechRate(rate)` 后：
- 如果 `rate < 0.5`，实际语速应为 0.5
- 如果 `rate > 2.0`，实际语速应为 2.0
- 否则实际语速应等于 `rate`

**Validates: Requirements 4.2**

### Property 4: 暂停恢复状态一致性

*For any* 正在朗读的TTS服务（无论使用内置音色还是系统音色）：
1. 调用 `pause()` 后，`getCurrentState().isPaused()` 应返回 `true`
2. 调用 `resume()` 后，`getCurrentState().isPlaying()` 应返回 `true`
3. 暂停位置应被正确保存，恢复后从该位置继续

**Validates: Requirements 4.3**

### Property 5: 引擎回退机制

*For any* 内置引擎初始化失败或模型文件缺失的情况：
1. 系统应自动回退到系统TTS引擎
2. `isInitialized()` 仍返回 `true`（只要系统TTS可用）
3. 音色列表中不应包含失败的内置音色

**Validates: Requirements 1.4, 6.1**

## Error Handling

### 错误场景及处理

| 错误场景 | 处理方式 |
|----------|----------|
| 模型文件缺失 | 记录日志，不添加对应内置音色到列表 |
| Sherpa-ONNX初始化失败 | 回退到系统TTS，显示Toast提示 |
| 内置音色合成失败 | 通知回调，提供重试选项 |
| 内存不足 | 释放内置引擎资源，切换到系统TTS |

### 日志标签

```java
private static final String TAG = "TTSServiceImpl";
private static final String BUILTIN_TAG = "BuiltInTTS";  // 内置TTS相关日志
```

## Testing Strategy

### 单元测试

1. **VoiceInfo扩展测试**
   - 测试 `isBuiltIn` 字段的getter/setter
   - 测试内置音色ID常量

2. **音色列表测试**
   - 验证内置音色在列表顶部
   - 验证内置音色有正确的标签

### 属性测试

使用jqwik进行属性测试：

1. **Property 1**: 内置音色列表完整性
2. **Property 2**: 音色切换一致性
3. **Property 3**: 内置音色标识正确性
4. **Property 4**: 语速设置有效性

### 集成测试

1. **引擎切换测试**
   - 从系统音色切换到内置音色
   - 从内置音色切换到系统音色
   - 在朗读过程中切换音色

2. **朗读功能测试**
   - 使用内置音色朗读文本
   - 暂停/恢复功能
   - 语速调节功能

## Dependencies

### Gradle依赖

```kotlin
// build.gradle.kts
dependencies {
    // Sherpa-ONNX AAR (从GitHub Releases下载)
    implementation(files("libs/sherpa-onnx-1.12.20.aar"))
}
```

### 模型文件

需要从Hugging Face下载中文TTS模型：
- https://huggingface.co/csukuangfj/sherpa-onnx-vits-zh-ll

## Implementation Notes

### 音频播放

内置TTS引擎生成的是PCM音频数据，需要使用 `AudioTrack` 播放：

```java
private void playAudio(float[] samples, int sampleRate) {
    // 将float[]转换为short[]
    short[] shortSamples = new short[samples.length];
    for (int i = 0; i < samples.length; i++) {
        shortSamples[i] = (short) (samples[i] * 32767);
    }
    
    // 使用AudioTrack播放
    AudioTrack audioTrack = new AudioTrack.Builder()
        .setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build())
        .setAudioFormat(new AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build())
        .setBufferSizeInBytes(shortSamples.length * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build();
    
    audioTrack.write(shortSamples, 0, shortSamples.length);
    audioTrack.play();
}
```

### 进度回调

由于Sherpa-ONNX是一次性生成整段音频，进度回调需要基于音频播放位置估算：

```java
// 根据播放位置估算文本位置
int estimatedTextPosition = (int) ((playbackPosition / totalDuration) * textLength);
callback.onProgress(estimatedTextPosition);
```
