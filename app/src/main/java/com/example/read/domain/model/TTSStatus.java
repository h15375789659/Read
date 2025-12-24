package com.example.read.domain.model;

/**
 * TTS状态枚举
 * 表示语音朗读的不同状态
 */
public enum TTSStatus {
    /**
     * 空闲状态 - TTS未在播放
     */
    IDLE,
    
    /**
     * 播放中 - TTS正在朗读
     */
    PLAYING,
    
    /**
     * 暂停状态 - TTS已暂停
     */
    PAUSED,
    
    /**
     * 错误状态 - TTS发生错误
     */
    ERROR
}
