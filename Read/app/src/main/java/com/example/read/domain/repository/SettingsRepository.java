package com.example.read.domain.repository;

import androidx.lifecycle.LiveData;

/**
 * 设置仓库接口 - 管理应用设置
 * 
 * 验证需求：5.4, 5.5, 13.4
 */
public interface SettingsRepository {

    // ==================== 字体设置 ====================

    /**
     * 获取字体大小
     * 验证需求：5.4 - 立即应用新的字体大小设置
     * 
     * @return 字体大小（sp）
     */
    float getFontSize();

    /**
     * 获取字体大小的LiveData
     * 
     * @return 字体大小LiveData
     */
    LiveData<Float> getFontSizeLiveData();

    /**
     * 设置字体大小
     * 验证需求：5.4 - 立即应用新的字体大小设置
     * 
     * @param size 字体大小（sp），范围12-32
     */
    void setFontSize(float size);

    // ==================== 行间距设置 ====================

    /**
     * 获取行间距
     * 验证需求：5.5 - 立即应用新的行间距设置
     * 
     * @return 行间距倍数
     */
    float getLineSpacing();

    /**
     * 获取行间距的LiveData
     * 
     * @return 行间距LiveData
     */
    LiveData<Float> getLineSpacingLiveData();

    /**
     * 设置行间距
     * 验证需求：5.5 - 立即应用新的行间距设置
     * 
     * @param spacing 行间距倍数，范围1.0-3.0
     */
    void setLineSpacing(float spacing);

    // ==================== 主题设置 ====================

    /**
     * 获取当前主题ID
     * 
     * @return 主题ID
     */
    String getCurrentThemeId();

    /**
     * 设置当前主题ID
     * 
     * @param themeId 主题ID
     */
    void setCurrentThemeId(String themeId);

    // ==================== 自动切换设置 ====================

    /**
     * 是否启用自动主题切换
     * 
     * @return 是否启用
     */
    boolean isAutoThemeSwitchEnabled();

    /**
     * 设置自动主题切换
     * 
     * @param enabled 是否启用
     */
    void setAutoThemeSwitchEnabled(boolean enabled);

    // ==================== 其他设置 ====================

    /**
     * 获取屏幕常亮设置
     * 
     * @return 是否屏幕常亮
     */
    boolean isKeepScreenOn();

    /**
     * 设置屏幕常亮
     * 
     * @param keepOn 是否屏幕常亮
     */
    void setKeepScreenOn(boolean keepOn);

    /**
     * 获取音量键翻页设置
     * 
     * @return 是否启用音量键翻页
     */
    boolean isVolumeKeyPageTurn();

    /**
     * 设置音量键翻页
     * 
     * @param enabled 是否启用
     */
    void setVolumeKeyPageTurn(boolean enabled);

    /**
     * 获取全屏阅读设置
     * 
     * @return 是否全屏阅读
     */
    boolean isFullScreenReading();

    /**
     * 设置全屏阅读
     * 
     * @param fullScreen 是否全屏
     */
    void setFullScreenReading(boolean fullScreen);

    // ==================== 翻页设置 ====================

    /**
     * 获取翻页模式
     * 
     * @return 翻页模式ID
     */
    String getPageMode();

    /**
     * 设置翻页模式
     * 
     * @param modeId 翻页模式ID
     */
    void setPageMode(String modeId);

    /**
     * 获取翻页动画类型
     * 
     * @return 翻页动画ID
     */
    String getPageAnimation();

    /**
     * 设置翻页动画类型
     * 
     * @param animationId 翻页动画ID
     */
    void setPageAnimation(String animationId);

    // ==================== 字体设置 ====================

    /**
     * 获取当前字体ID
     * 
     * @return 字体ID
     */
    String getFontFamily();

    /**
     * 设置字体
     * 
     * @param fontId 字体ID
     */
    void setFontFamily(String fontId);

    // ==================== 设置持久化 ====================

    /**
     * 重置所有设置为默认值
     */
    void resetToDefaults();

    /**
     * 检查设置是否已持久化
     * 验证需求：13.4 - 将设置保存到SharedPreferences
     * 
     * @param key 设置键
     * @return 是否已持久化
     */
    boolean isSettingPersisted(String key);
}
