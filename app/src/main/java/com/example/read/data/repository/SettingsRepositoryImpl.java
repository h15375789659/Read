package com.example.read.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.read.domain.repository.SettingsRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * 设置仓库实现类
 * 
 * 验证需求：5.4, 5.5, 13.4
 */
@Singleton
public class SettingsRepositoryImpl implements SettingsRepository {

    private static final String PREFS_NAME = "reader_settings";
    
    // 设置键
    public static final String KEY_FONT_SIZE = "font_size";
    public static final String KEY_LINE_SPACING = "line_spacing";
    public static final String KEY_CURRENT_THEME_ID = "current_theme_id";
    public static final String KEY_AUTO_THEME_SWITCH = "auto_theme_switch";
    public static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
    public static final String KEY_VOLUME_KEY_PAGE_TURN = "volume_key_page_turn";
    public static final String KEY_FULL_SCREEN_READING = "full_screen_reading";
    public static final String KEY_PAGE_MODE = "page_mode";
    public static final String KEY_PAGE_ANIMATION = "page_animation";
    public static final String KEY_FONT_FAMILY = "font_family";

    // 默认值
    private static final float DEFAULT_FONT_SIZE = 18f;
    private static final float DEFAULT_LINE_SPACING = 1.5f;
    private static final String DEFAULT_THEME_ID = "day";
    private static final String DEFAULT_PAGE_MODE = "scroll";
    private static final String DEFAULT_PAGE_ANIMATION = "slide";
    private static final String DEFAULT_FONT_FAMILY = "default";
    private static final float MIN_FONT_SIZE = 12f;
    private static final float MAX_FONT_SIZE = 32f;
    private static final float MIN_LINE_SPACING = 1.0f;
    private static final float MAX_LINE_SPACING = 3.0f;

    private final SharedPreferences prefs;
    private final MutableLiveData<Float> fontSizeLiveData;
    private final MutableLiveData<Float> lineSpacingLiveData;

    @Inject
    public SettingsRepositoryImpl(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.fontSizeLiveData = new MutableLiveData<>(getFontSize());
        this.lineSpacingLiveData = new MutableLiveData<>(getLineSpacing());
    }


    // ==================== 字体设置 ====================

    /**
     * 获取字体大小
     * 验证需求：5.4
     */
    @Override
    public float getFontSize() {
        return prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }

    @Override
    public LiveData<Float> getFontSizeLiveData() {
        return fontSizeLiveData;
    }

    /**
     * 设置字体大小
     * 验证需求：5.4 - 立即应用新的字体大小设置
     */
    @Override
    public void setFontSize(float size) {
        float clampedSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, size));
        prefs.edit().putFloat(KEY_FONT_SIZE, clampedSize).commit();
        fontSizeLiveData.postValue(clampedSize);
    }

    // ==================== 行间距设置 ====================

    /**
     * 获取行间距
     * 验证需求：5.5
     */
    @Override
    public float getLineSpacing() {
        return prefs.getFloat(KEY_LINE_SPACING, DEFAULT_LINE_SPACING);
    }

    @Override
    public LiveData<Float> getLineSpacingLiveData() {
        return lineSpacingLiveData;
    }

    /**
     * 设置行间距
     * 验证需求：5.5 - 立即应用新的行间距设置
     */
    @Override
    public void setLineSpacing(float spacing) {
        float clampedSpacing = Math.max(MIN_LINE_SPACING, Math.min(MAX_LINE_SPACING, spacing));
        prefs.edit().putFloat(KEY_LINE_SPACING, clampedSpacing).commit();
        lineSpacingLiveData.postValue(clampedSpacing);
    }

    // ==================== 主题设置 ====================

    @Override
    public String getCurrentThemeId() {
        return prefs.getString(KEY_CURRENT_THEME_ID, DEFAULT_THEME_ID);
    }

    @Override
    public void setCurrentThemeId(String themeId) {
        prefs.edit().putString(KEY_CURRENT_THEME_ID, themeId).apply();
    }

    // ==================== 自动切换设置 ====================

    @Override
    public boolean isAutoThemeSwitchEnabled() {
        return prefs.getBoolean(KEY_AUTO_THEME_SWITCH, false);
    }

    @Override
    public void setAutoThemeSwitchEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_THEME_SWITCH, enabled).apply();
    }

    // ==================== 其他设置 ====================

    @Override
    public boolean isKeepScreenOn() {
        return prefs.getBoolean(KEY_KEEP_SCREEN_ON, false);
    }

    @Override
    public void setKeepScreenOn(boolean keepOn) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, keepOn).apply();
    }

    @Override
    public boolean isVolumeKeyPageTurn() {
        return prefs.getBoolean(KEY_VOLUME_KEY_PAGE_TURN, false);
    }

    @Override
    public void setVolumeKeyPageTurn(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOLUME_KEY_PAGE_TURN, enabled).apply();
    }

    @Override
    public boolean isFullScreenReading() {
        return prefs.getBoolean(KEY_FULL_SCREEN_READING, false);
    }

    @Override
    public void setFullScreenReading(boolean fullScreen) {
        prefs.edit().putBoolean(KEY_FULL_SCREEN_READING, fullScreen).apply();
    }

    // ==================== 翻页设置 ====================

    @Override
    public String getPageMode() {
        return prefs.getString(KEY_PAGE_MODE, DEFAULT_PAGE_MODE);
    }

    @Override
    public void setPageMode(String modeId) {
        prefs.edit().putString(KEY_PAGE_MODE, modeId).commit();
    }

    @Override
    public String getPageAnimation() {
        return prefs.getString(KEY_PAGE_ANIMATION, DEFAULT_PAGE_ANIMATION);
    }

    @Override
    public void setPageAnimation(String animationId) {
        prefs.edit().putString(KEY_PAGE_ANIMATION, animationId).commit();
    }

    // ==================== 字体设置 ====================

    @Override
    public String getFontFamily() {
        return prefs.getString(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY);
    }

    @Override
    public void setFontFamily(String fontId) {
        prefs.edit().putString(KEY_FONT_FAMILY, fontId).commit();
    }

    // ==================== 设置持久化 ====================

    @Override
    public void resetToDefaults() {
        prefs.edit()
                .putFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
                .putFloat(KEY_LINE_SPACING, DEFAULT_LINE_SPACING)
                .putString(KEY_CURRENT_THEME_ID, DEFAULT_THEME_ID)
                .putBoolean(KEY_AUTO_THEME_SWITCH, false)
                .putBoolean(KEY_KEEP_SCREEN_ON, false)
                .putBoolean(KEY_VOLUME_KEY_PAGE_TURN, false)
                .putBoolean(KEY_FULL_SCREEN_READING, false)
                .putString(KEY_PAGE_MODE, DEFAULT_PAGE_MODE)
                .putString(KEY_PAGE_ANIMATION, DEFAULT_PAGE_ANIMATION)
                .putString(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY)
                .apply();
        
        fontSizeLiveData.postValue(DEFAULT_FONT_SIZE);
        lineSpacingLiveData.postValue(DEFAULT_LINE_SPACING);
    }

    /**
     * 检查设置是否已持久化
     * 验证需求：13.4
     */
    @Override
    public boolean isSettingPersisted(String key) {
        return prefs.contains(key);
    }
}
