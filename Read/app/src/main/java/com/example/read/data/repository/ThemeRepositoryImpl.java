package com.example.read.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.read.domain.model.ReaderTheme;
import com.example.read.domain.repository.ThemeRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * 主题仓库实现类
 * 
 * 验证需求：6.1, 6.2, 6.5, 6.6
 */
@Singleton
public class ThemeRepositoryImpl implements ThemeRepository {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_CURRENT_THEME_ID = "current_theme_id";
    private static final String KEY_CUSTOM_THEMES = "custom_themes";
    private static final String KEY_AUTO_SWITCH_ENABLED = "auto_switch_enabled";
    private static final String KEY_NIGHT_MODE_START = "night_mode_start";
    private static final String KEY_NIGHT_MODE_END = "night_mode_end";

    private static final int DEFAULT_NIGHT_START = 22; // 晚上10点
    private static final int DEFAULT_NIGHT_END = 6;    // 早上6点

    private final SharedPreferences prefs;
    private final Gson gson;
    private final MutableLiveData<ReaderTheme> currentThemeLiveData;

    private ReaderTheme currentTheme;
    private List<ReaderTheme> customThemes;

    @Inject
    public ThemeRepositoryImpl(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.currentThemeLiveData = new MutableLiveData<>();

        // 先加载自定义主题列表（必须在 loadCurrentTheme 之前，因为 getThemeById 需要访问 customThemes）
        loadCustomThemes();
        // 再加载当前主题
        loadCurrentTheme();

        // 应用自动主题切换
        applyAutoThemeSwitch();
    }

    /**
     * 加载当前主题
     */
    private void loadCurrentTheme() {
        String themeId = prefs.getString(KEY_CURRENT_THEME_ID, "day");
        currentTheme = getThemeById(themeId);
        if (currentTheme == null) {
            currentTheme = ReaderTheme.DAY;
        }
        currentThemeLiveData.postValue(currentTheme);
    }

    /**
     * 加载自定义主题列表
     */
    private void loadCustomThemes() {
        String json = prefs.getString(KEY_CUSTOM_THEMES, "[]");
        Type type = new TypeToken<List<ReaderTheme>>(){}.getType();
        customThemes = gson.fromJson(json, type);
        if (customThemes == null) {
            customThemes = new ArrayList<>();
        }
    }

    /**
     * 保存自定义主题列表
     */
    private void saveCustomThemesToPrefs() {
        String json = gson.toJson(customThemes);
        prefs.edit().putString(KEY_CUSTOM_THEMES, json).apply();
    }


    /**
     * 获取当前主题
     * 验证需求：6.2
     */
    @Override
    public ReaderTheme getCurrentTheme() {
        return currentTheme != null ? currentTheme.copy() : ReaderTheme.DAY;
    }

    /**
     * 获取当前主题的LiveData
     */
    @Override
    public LiveData<ReaderTheme> getCurrentThemeLiveData() {
        return currentThemeLiveData;
    }

    /**
     * 设置当前主题
     * 验证需求：6.2 - 立即应用该主题的背景色和文字色
     */
    @Override
    public void setCurrentTheme(ReaderTheme theme) {
        if (theme == null) return;
        
        this.currentTheme = theme.copy();
        prefs.edit().putString(KEY_CURRENT_THEME_ID, theme.getId()).commit();
        currentThemeLiveData.postValue(currentTheme);
    }

    /**
     * 根据ID设置主题
     */
    @Override
    public void setCurrentThemeById(String themeId) {
        ReaderTheme theme = getThemeById(themeId);
        if (theme != null) {
            setCurrentTheme(theme);
        }
    }

    /**
     * 获取所有预设主题
     * 验证需求：6.1 - 提供至少三种预设主题
     */
    @Override
    public List<ReaderTheme> getPresetThemes() {
        return Arrays.asList(ReaderTheme.getPresetThemes());
    }

    /**
     * 获取所有自定义主题
     */
    @Override
    public List<ReaderTheme> getCustomThemes() {
        return new ArrayList<>(customThemes);
    }

    /**
     * 获取所有主题（预设+自定义）
     */
    @Override
    public List<ReaderTheme> getAllThemes() {
        List<ReaderTheme> allThemes = new ArrayList<>();
        allThemes.addAll(getPresetThemes());
        allThemes.addAll(customThemes);
        return allThemes;
    }

    /**
     * 保存自定义主题
     * 验证需求：6.5 - 将该主题添加到主题列表
     */
    @Override
    public void saveCustomTheme(ReaderTheme theme) {
        if (theme == null || !theme.isCustom()) return;

        // 检查是否已存在
        int existingIndex = -1;
        for (int i = 0; i < customThemes.size(); i++) {
            if (customThemes.get(i).getId().equals(theme.getId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            // 更新现有主题
            customThemes.set(existingIndex, theme.copy());
        } else {
            // 添加新主题
            customThemes.add(theme.copy());
        }

        saveCustomThemesToPrefs();
    }

    /**
     * 删除自定义主题
     */
    @Override
    public void deleteCustomTheme(String themeId) {
        if (themeId == null) return;

        customThemes.removeIf(theme -> theme.getId().equals(themeId));
        saveCustomThemesToPrefs();

        // 如果删除的是当前主题，切换到默认主题
        if (currentTheme != null && currentTheme.getId().equals(themeId)) {
            setCurrentTheme(ReaderTheme.DAY);
        }
    }

    /**
     * 根据ID获取主题
     */
    @Override
    public ReaderTheme getThemeById(String themeId) {
        if (themeId == null) return null;

        // 先查找预设主题
        for (ReaderTheme preset : ReaderTheme.getPresetThemes()) {
            if (preset.getId().equals(themeId)) {
                return preset.copy();
            }
        }

        // 再查找自定义主题
        for (ReaderTheme custom : customThemes) {
            if (custom.getId().equals(themeId)) {
                return custom.copy();
            }
        }

        return null;
    }

    /**
     * 是否启用自动主题切换
     */
    @Override
    public boolean isAutoThemeSwitchEnabled() {
        return prefs.getBoolean(KEY_AUTO_SWITCH_ENABLED, false);
    }

    /**
     * 设置自动主题切换
     * 验证需求：6.6
     */
    @Override
    public void setAutoThemeSwitchEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_SWITCH_ENABLED, enabled).apply();
        if (enabled) {
            applyAutoThemeSwitch();
        }
    }

    /**
     * 获取夜间模式开始时间
     */
    @Override
    public int getNightModeStartHour() {
        return prefs.getInt(KEY_NIGHT_MODE_START, DEFAULT_NIGHT_START);
    }

    /**
     * 获取夜间模式结束时间
     */
    @Override
    public int getNightModeEndHour() {
        return prefs.getInt(KEY_NIGHT_MODE_END, DEFAULT_NIGHT_END);
    }

    /**
     * 设置夜间模式时间范围
     */
    @Override
    public void setNightModeTimeRange(int startHour, int endHour) {
        prefs.edit()
                .putInt(KEY_NIGHT_MODE_START, Math.max(0, Math.min(23, startHour)))
                .putInt(KEY_NIGHT_MODE_END, Math.max(0, Math.min(23, endHour)))
                .apply();
    }

    /**
     * 根据当前时间判断是否应该使用夜间模式
     * 验证需求：6.6 - 当系统时间进入夜间时段时自动切换
     */
    @Override
    public boolean shouldUseNightMode() {
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int startHour = getNightModeStartHour();
        int endHour = getNightModeEndHour();

        // 处理跨午夜的情况（如22:00 - 06:00）
        if (startHour > endHour) {
            return currentHour >= startHour || currentHour < endHour;
        } else {
            return currentHour >= startHour && currentHour < endHour;
        }
    }

    /**
     * 应用自动主题切换
     */
    @Override
    public void applyAutoThemeSwitch() {
        if (!isAutoThemeSwitchEnabled()) return;

        if (shouldUseNightMode()) {
            // 切换到夜间模式
            if (currentTheme == null || !currentTheme.isDarkTheme()) {
                setCurrentTheme(ReaderTheme.NIGHT);
            }
        } else {
            // 切换到日间模式
            if (currentTheme == null || currentTheme.isDarkTheme()) {
                setCurrentTheme(ReaderTheme.DAY);
            }
        }
    }
}
