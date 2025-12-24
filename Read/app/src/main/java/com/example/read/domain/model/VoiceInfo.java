package com.example.read.domain.model;

/**
 * 语音信息模型
 * 用于表示TTS引擎可用的语音选项
 */
public class VoiceInfo {
    private String voiceId;
    private String name;
    private String language;
    private boolean isDefault;

    public VoiceInfo() {}

    public VoiceInfo(String voiceId, String name, String language, boolean isDefault) {
        this.voiceId = voiceId;
        this.name = name;
        this.language = language;
        this.isDefault = isDefault;
    }

    // Getters
    public String getVoiceId() { return voiceId; }
    public String getName() { return name; }
    public String getLanguage() { return language; }
    public boolean isDefault() { return isDefault; }

    // Setters
    public void setVoiceId(String voiceId) { this.voiceId = voiceId; }
    public void setName(String name) { this.name = name; }
    public void setLanguage(String language) { this.language = language; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    @Override
    public String toString() {
        return "VoiceInfo{" +
                "voiceId='" + voiceId + '\'' +
                ", name='" + name + '\'' +
                ", language='" + language + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoiceInfo voiceInfo = (VoiceInfo) o;
        return voiceId != null ? voiceId.equals(voiceInfo.voiceId) : voiceInfo.voiceId == null;
    }

    @Override
    public int hashCode() {
        return voiceId != null ? voiceId.hashCode() : 0;
    }
}
