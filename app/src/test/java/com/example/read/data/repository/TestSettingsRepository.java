package com.example.read.data.repository;

import androidx.lifecycle.LiveData;
import com.example.read.domain.repository.SettingsRepository;
import java.util.HashMap;
import java.util.Map;

public class TestSettingsRepository implements SettingsRepository {
    public static final String KEY_FONT_SIZE = "font_size";
    public static final String KEY_LINE_SPACING = "line_spacing";
    public static final String KEY_CURRENT_THEME_ID = "current_theme_id";
    public static final String KEY_AUTO_THEME_SWITCH = "auto_theme_switch";
    public static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
    public static final String KEY_VOLUME_KEY_PAGE_TURN = "volume_key_page_turn";
    public static final String KEY_FULL_SCREEN_READING = "full_screen_reading";

    private static final float DEFAULT_FONT_SIZE = 18f;
    private static final float DEFAULT_LINE_SPACING = 1.5f;
    private static final String DEFAULT_THEME_ID = "day";
    private static final float MIN_FONT_SIZE = 12f;
    private static final float MAX_FONT_SIZE = 32f;
    private static final float MIN_LINE_SPACING = 1.0f;
    private static final float MAX_LINE_SPACING = 3.0f;

    private final Map<String, Object> storage;

    public TestSettingsRepository() {
        this.storage = new HashMap<>();
    }

    @Override
    public float getFontSize() {
        Object value = storage.get(KEY_FONT_SIZE);
        return value != null ? (Float) value : DEFAULT_FONT_SIZE;
    }

    @Override
    public LiveData<Float> getFontSizeLiveData() { return null; }

    @Override
    public void setFontSize(float size) {
        float clampedSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, size));
        storage.put(KEY_FONT_SIZE, clampedSize);
    }

    @Override
    public float getLineSpacing() {
        Object value = storage.get(KEY_LINE_SPACING);
        return value != null ? (Float) value : DEFAULT_LINE_SPACING;
    }

    @Override
    public LiveData<Float> getLineSpacingLiveData() { return null; }

    @Override
    public void setLineSpacing(float spacing) {
        float clampedSpacing = Math.max(MIN_LINE_SPACING, Math.min(MAX_LINE_SPACING, spacing));
        storage.put(KEY_LINE_SPACING, clampedSpacing);
    }

    @Override
    public String getCurrentThemeId() {
        Object value = storage.get(KEY_CURRENT_THEME_ID);
        return value != null ? (String) value : DEFAULT_THEME_ID;
    }

    @Override
    public void setCurrentThemeId(String themeId) {
        storage.put(KEY_CURRENT_THEME_ID, themeId);
    }

    @Override
    public boolean isAutoThemeSwitchEnabled() {
        Object value = storage.get(KEY_AUTO_THEME_SWITCH);
        return value != null ? (Boolean) value : false;
    }

    @Override
    public void setAutoThemeSwitchEnabled(boolean enabled) {
        storage.put(KEY_AUTO_THEME_SWITCH, enabled);
    }

    @Override
    public boolean isKeepScreenOn() {
        Object value = storage.get(KEY_KEEP_SCREEN_ON);
        return value != null ? (Boolean) value : false;
    }

    @Override
    public void setKeepScreenOn(boolean keepOn) {
        storage.put(KEY_KEEP_SCREEN_ON, keepOn);
    }

    @Override
    public boolean isVolumeKeyPageTurn() {
        Object value = storage.get(KEY_VOLUME_KEY_PAGE_TURN);
        return value != null ? (Boolean) value : false;
    }

    @Override
    public void setVolumeKeyPageTurn(boolean enabled) {
        storage.put(KEY_VOLUME_KEY_PAGE_TURN, enabled);
    }

    @Override
    public boolean isFullScreenReading() {
        Object value = storage.get(KEY_FULL_SCREEN_READING);
        return value != null ? (Boolean) value : false;
    }

    @Override
    public void setFullScreenReading(boolean fullScreen) {
        storage.put(KEY_FULL_SCREEN_READING, fullScreen);
    }

    @Override
    public void resetToDefaults() {
        storage.put(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
        storage.put(KEY_LINE_SPACING, DEFAULT_LINE_SPACING);
        storage.put(KEY_CURRENT_THEME_ID, DEFAULT_THEME_ID);
        storage.put(KEY_AUTO_THEME_SWITCH, false);
        storage.put(KEY_KEEP_SCREEN_ON, false);
        storage.put(KEY_VOLUME_KEY_PAGE_TURN, false);
        storage.put(KEY_FULL_SCREEN_READING, false);
    }

    @Override
    public boolean isSettingPersisted(String key) {
        return storage.containsKey(key);
    }

    public void clearAll() { storage.clear(); }
    public int getStoredSettingsCount() { return storage.size(); }
    public Object getRawValue(String key) { return storage.get(key); }
}