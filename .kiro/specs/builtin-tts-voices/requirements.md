# Requirements Document

## Introduction

本功能为小说阅读器应用内置两个小巧的中文TTS音色（一男一女），使用Sherpa-ONNX离线语音合成引擎。内置音色将标记为"软件内置"，与系统TTS音色区分显示，确保用户在没有安装系统中文TTS引擎的情况下也能正常使用语音朗读功能。

## Glossary

- **TTS_Service**: 语音朗读服务，负责文本转语音的核心功能
- **Sherpa_ONNX**: 开源的离线语音合成引擎，支持多种语言和音色模型
- **Voice_Model**: 语音模型文件，包含神经网络权重和配置，用于生成特定音色的语音
- **Built_In_Voice**: 软件内置音色，随APK一起分发，无需用户额外下载
- **System_Voice**: 系统TTS音色，由Android系统或第三方TTS引擎提供
- **Voice_Selector**: 音色选择器UI组件，用于展示和切换可用音色

## Requirements

### Requirement 1: 集成Sherpa-ONNX引擎

**User Story:** 作为开发者，我想集成Sherpa-ONNX离线TTS引擎，以便应用能够使用内置音色进行语音合成。

#### Acceptance Criteria

1. THE TTS_Service SHALL 集成Sherpa-ONNX Android库依赖
2. WHEN 应用启动时 THE TTS_Service SHALL 初始化Sherpa-ONNX引擎
3. THE TTS_Service SHALL 支持从assets目录加载语音模型文件
4. IF Sherpa-ONNX初始化失败 THEN THE TTS_Service SHALL 回退到系统TTS引擎

### Requirement 2: 内置中文语音模型

**User Story:** 作为用户，我想使用软件内置的中文音色，以便在没有安装系统TTS的情况下也能听书。

#### Acceptance Criteria

1. THE Voice_Model SHALL 包含一个中文女声模型（约15-20MB）
2. THE Voice_Model SHALL 包含一个中文男声模型（约15-20MB）
3. THE Voice_Model SHALL 存放在assets/tts_models目录下
4. WHEN 用户选择内置音色时 THE TTS_Service SHALL 使用对应的Sherpa-ONNX模型进行语音合成

### Requirement 3: 音色列表展示

**User Story:** 作为用户，我想在音色选择界面清楚地区分内置音色和系统音色，以便做出选择。

#### Acceptance Criteria

1. THE Voice_Selector SHALL 在音色列表顶部显示内置音色
2. THE Voice_Selector SHALL 为内置音色添加"软件内置"标签
3. THE Voice_Selector SHALL 在内置音色和系统音色之间显示分隔线
4. WHEN 系统没有可用的中文TTS音色时 THE Voice_Selector SHALL 仅显示内置音色

### Requirement 4: 内置音色与现有朗读功能集成

**User Story:** 作为用户，我想使用内置音色时也能享受现有的所有朗读功能（语速调节、暂停恢复、自动下一章等）。

#### Acceptance Criteria

1. WHEN 用户选择内置音色时 THE TTS_Service SHALL 使用Sherpa-ONNX引擎替代系统TTS进行语音合成
2. THE TTS_Service SHALL 确保内置音色支持现有的语速调节功能（0.5x - 2.0x）
3. THE TTS_Service SHALL 确保内置音色支持现有的暂停和恢复功能
4. THE TTS_Service SHALL 确保内置音色提供与系统TTS相同的进度回调接口
5. THE TTS_Service SHALL 确保内置音色触发相同的完成回调以支持自动下一章功能

### Requirement 5: 音色切换

**User Story:** 作为用户，我想在内置音色和系统音色之间自由切换，以便选择最适合的声音。

#### Acceptance Criteria

1. WHEN 用户从系统音色切换到内置音色时 THE TTS_Service SHALL 停止当前朗读并切换引擎
2. WHEN 用户从内置音色切换到系统音色时 THE TTS_Service SHALL 停止当前朗读并切换引擎
3. THE TTS_Service SHALL 保存用户的音色选择偏好
4. WHEN 应用重启时 THE TTS_Service SHALL 恢复用户上次选择的音色

### Requirement 6: 错误处理

**User Story:** 作为用户，我想在内置音色出现问题时得到友好的提示，并能自动切换到备用方案。

#### Acceptance Criteria

1. IF 内置音色模型文件损坏或缺失 THEN THE TTS_Service SHALL 显示错误提示并自动切换到系统TTS
2. IF Sherpa-ONNX合成失败 THEN THE TTS_Service SHALL 通知用户并提供重试选项
3. THE TTS_Service SHALL 在日志中记录内置音色相关的错误信息以便调试
