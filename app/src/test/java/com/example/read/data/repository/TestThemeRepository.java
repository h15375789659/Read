package com.example.read.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.read.domain.model.ReaderTheme;
import com.example.read.domain.repository.ThemeRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * 用于测试的主题仓库实现
 * 不依赖Android SharedPreferences
 */
public class TestThemeRepository implements ThemeRepository {

    private ReaderTheme currentTheme;
    private final MutableLiveData<ReaderTheme> currentThemeLiveData;
    private final List<ReaderTheme> customThemes;
    private boolean autoSwitchEnabled;
    private int nightModeStartHour;
    private int nightModeEndHour;
    private Integer mockCurrentHour; // 用于测试的模拟时间

    public TestThemeRepository() {
        this.currentTheme = ReaderTheme.DAY;
        this.currentThemeLiveData = new MutableLiveData<>(currentTheme);
        this.customThemes = new ArrayList<>();
        this.autoSwitchEnabled = false;
        this.nightModeStartHour = 22;
        this.nightModeEndHour = 6;
    }

    @Override
    public ReaderTheme getCurrentTheme() {
        return currentTheme != null ? currentTheme.copy() : ReaderTheme.DAY;
    }

    @Override
    public LiveData<ReaderTheme> getCurrentThemeLiveData() {
        return currentThemeLiveData;
    }

    @Override
    public void setCurrentTheme(ReaderTheme theme) {
        if (theme == null) return;
        this.currentTheme = theme.copy();
        // 在测试环境中不使用postValue，直接设置值
        // currentThemeLiveData.postValue(currentTheme);
    }


    @Override
    public void setCurrentThemeById(String themeId) {
        ReaderTheme theme = getThemeById(themeId);
        if (theme != null) {
            setCurrentTheme(theme);
        }
    }

    @Override
    public List<ReaderTheme> getPresetThemes() {
        return Arrays.asList(ReaderTheme.getPresetThemes());
    }

    @Override
    public List<ReaderTheme> getCustomThemes() {
        return new ArrayList<>(customThemes);
    }

    @Override
    public List<ReaderTheme> getAllThemes() {
        List<ReaderTheme> all = new ArrayList<>();
        all.addAll(getPresetThemes());
        all.addAll(customThemes);
        return all;
    }

    @Override
    public void saveCustomTheme(ReaderTheme theme) {
        if (theme == null || !theme.isCustom()) return;
        
        int existingIndex = -1;
        for (int i = 0; i < customThemes.size(); i++) {
            if (customThemes.get(i).getId().equals(theme.getId())) {
                existingIndex = i;
                break;
            }
        }
        
        if (existingIndex >= 0) {
            customThemes.set(existingIndex, theme.copy());
        } else {
            customThemes.add(theme.copy());
        }
    }

    @Override
    public void deleteCustomTheme(String themeId) {
        customThemes.removeIf(t -> t.getId().equals(themeId));
        if (currentTheme != null && currentTheme.getId().equals(themeId)) {
            setCurrentTheme(ReaderTheme.DAY);
        }
    }

    @Override
    public ReaderTheme getThemeById(String themeId) {
        if (themeId == null) return null;
        
        for (ReaderTheme preset : ReaderTheme.getPresetThemes()) {
            if (preset.getId().equals(themeId)) {
                return preset.copy();
            }
        }
        
        for (ReaderTheme custom : customThemes) {
            if (custom.getId().equals(themeId)) {
                return custom.copy();
            }
        }
        
        return null;
    }

    @Override
    public boolean isAutoThemeSwitchEnabled() {
        return autoSwitchEnabled;
    }

    @Override
    public void setAutoThemeSwitchEnabled(boolean enabled) {
        this.autoSwitchEnabled = enabled;
        if (enabled) {
            applyAutoThemeSwitch();
        }
    }

    @Override
    public int getNightModeStartHour() {
        return nightModeStartHour;
    }

    @Override
    public int getNightModeEndHour() {
        return nightModeEndHour;
    }

    @Override
    public void setNightModeTimeRange(int startHour, int endHour) {
        this.nightModeStartHour = Math.max(0, Math.min(23, startHour));
        this.nightModeEndHour = Math.max(0, Math.min(23, endHour));
    }

    @Override
    public boolean shouldUseNightMode() {
        int currentHour = mockCurrentHour != null ? mockCurrentHour : 
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        if (nightModeStartHour > nightModeEndHour) {
            return currentHour >= nightModeStartHour || currentHour < nightModeEndHour;
        } else {
            return currentHour >= nightModeStartHour && currentHour < nightModeEndHour;
        }
    }

    @Override
    public void applyAutoThemeSwitch() {
        if (!autoSwitchEnabled) return;
        
        if (shouldUseNightMode()) {
            if (currentTheme == null || !currentTheme.isDarkTheme()) {
                setCurrentTheme(ReaderTheme.NIGHT);
            }
        } else {
            if (currentTheme == null || currentTheme.isDarkTheme()) {
                setCurrentTheme(ReaderTheme.DAY);
            }
        }
    }

    // 测试辅助方法
    public void setMockCurrentHour(int hour) {
        this.mockCurrentHour = hour;
    }

    public void clearMockCurrentHour() {
        this.mockCurrentHour = null;
    }
}
