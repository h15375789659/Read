package com.example.read.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.read.domain.model.ReaderTheme;

import java.util.List;

/**
 * 主题仓库接口 - 管理阅读器主题
 * 
 * 验证需求：6.1, 6.2, 6.5, 6.6
 */
public interface ThemeRepository {

    /**
     * 获取当前主题
     * 验证需求：6.2 - 立即应用该主题的背景色和文字色
     * 
     * @return 当前主题
     */
    ReaderTheme getCurrentTheme();

    /**
     * 获取当前主题的LiveData
     * 
     * @return 当前主题LiveData
     */
    LiveData<ReaderTheme> getCurrentThemeLiveData();

    /**
     * 设置当前主题
     * 验证需求：6.2 - 立即应用该主题的背景色和文字色
     * 
     * @param theme 要应用的主题
     */
    void setCurrentTheme(ReaderTheme theme);

    /**
     * 根据ID设置主题
     * 
     * @param themeId 主题ID
     */
    void setCurrentThemeById(String themeId);

    /**
     * 获取所有预设主题
     * 验证需求：6.1 - 提供至少三种预设主题
     * 
     * @return 预设主题列表
     */
    List<ReaderTheme> getPresetThemes();

    /**
     * 获取所有自定义主题
     * 
     * @return 自定义主题列表
     */
    List<ReaderTheme> getCustomThemes();

    /**
     * 获取所有主题（预设+自定义）
     * 
     * @return 所有主题列表
     */
    List<ReaderTheme> getAllThemes();

    /**
     * 保存自定义主题
     * 验证需求：6.5 - 将该主题添加到主题列表
     * 
     * @param theme 自定义主题
     */
    void saveCustomTheme(ReaderTheme theme);

    /**
     * 删除自定义主题
     * 
     * @param themeId 主题ID
     */
    void deleteCustomTheme(String themeId);

    /**
     * 获取根据ID获取主题
     * 
     * @param themeId 主题ID
     * @return 主题，如果不存在返回null
     */
    ReaderTheme getThemeById(String themeId);

    /**
     * 是否启用自动主题切换
     * 
     * @return 是否启用
     */
    boolean isAutoThemeSwitchEnabled();

    /**
     * 设置自动主题切换
     * 验证需求：6.6 - 自动切换到夜间模式
     * 
     * @param enabled 是否启用
     */
    void setAutoThemeSwitchEnabled(boolean enabled);

    /**
     * 获取夜间模式开始时间（小时，0-23）
     * 
     * @return 开始时间
     */
    int getNightModeStartHour();

    /**
     * 获取夜间模式结束时间（小时，0-23）
     * 
     * @return 结束时间
     */
    int getNightModeEndHour();

    /**
     * 设置夜间模式时间范围
     * 
     * @param startHour 开始时间（0-23）
     * @param endHour 结束时间（0-23）
     */
    void setNightModeTimeRange(int startHour, int endHour);

    /**
     * 根据当前时间判断是否应该使用夜间模式
     * 验证需求：6.6 - 当系统时间进入夜间时段时自动切换
     * 
     * @return 是否应该使用夜间模式
     */
    boolean shouldUseNightMode();

    /**
     * 应用自动主题切换
     * 如果启用了自动切换，根据当前时间自动切换主题
     */
    void applyAutoThemeSwitch();
}
