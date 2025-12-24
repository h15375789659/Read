package com.example.read.domain.model;

/**
 * 阅读器主题模型
 * 用于定义阅读界面的颜色配置
 * 
 * 验证需求：6.1, 6.2, 6.5, 6.6
 */
public class ReaderTheme {
    
    // 颜色常量
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_BLACK = 0xFF000000;
    
    private String id;
    private String name;
    private int backgroundColor;
    private int textColor;
    private boolean isCustom;
    private long createTime;

    // 预设主题
    public static final ReaderTheme DAY = new ReaderTheme(
            "day", "日间", COLOR_WHITE, COLOR_BLACK, false);
    
    public static final ReaderTheme NIGHT = new ReaderTheme(
            "night", "夜间", 0xFF1E1E1E, 0xFFE0E0E0, false);
    
    public static final ReaderTheme EYE_CARE = new ReaderTheme(
            "eye_care", "护眼", 0xFFC7EDCC, 0xFF333333, false);
    
    public static final ReaderTheme SEPIA = new ReaderTheme(
            "sepia", "羊皮纸", 0xFFF5E6C8, 0xFF5B4636, false);

    public ReaderTheme() {
        this.createTime = System.currentTimeMillis();
    }

    public ReaderTheme(String id, String name, int backgroundColor, int textColor, boolean isCustom) {
        this.id = id;
        this.name = name;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.isCustom = isCustom;
        this.createTime = System.currentTimeMillis();
    }

    /**
     * 创建自定义主题
     */
    public static ReaderTheme createCustom(String name, int backgroundColor, int textColor) {
        String id = "custom_" + System.currentTimeMillis();
        return new ReaderTheme(id, name, backgroundColor, textColor, true);
    }

    /**
     * 根据ID获取预设主题
     */
    public static ReaderTheme getPresetById(String id) {
        if (id == null) return DAY;
        switch (id) {
            case "day":
                return DAY;
            case "night":
                return NIGHT;
            case "eye_care":
                return EYE_CARE;
            case "sepia":
                return SEPIA;
            default:
                return DAY;
        }
    }

    /**
     * 获取所有预设主题
     */
    public static ReaderTheme[] getPresetThemes() {
        return new ReaderTheme[]{DAY, NIGHT, EYE_CARE, SEPIA};
    }

    /**
     * 判断是否为夜间模式主题
     */
    public boolean isDarkTheme() {
        // 通过背景色亮度判断（不使用Android Color类）
        int red = (backgroundColor >> 16) & 0xFF;
        int green = (backgroundColor >> 8) & 0xFF;
        int blue = backgroundColor & 0xFF;
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;
        return luminance < 0.5;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getBackgroundColor() { return backgroundColor; }
    public int getTextColor() { return textColor; }
    public boolean isCustom() { return isCustom; }
    public long getCreateTime() { return createTime; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setBackgroundColor(int backgroundColor) { this.backgroundColor = backgroundColor; }
    public void setTextColor(int textColor) { this.textColor = textColor; }
    public void setCustom(boolean custom) { isCustom = custom; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    /**
     * 创建副本
     */
    public ReaderTheme copy() {
        ReaderTheme copy = new ReaderTheme();
        copy.id = this.id;
        copy.name = this.name;
        copy.backgroundColor = this.backgroundColor;
        copy.textColor = this.textColor;
        copy.isCustom = this.isCustom;
        copy.createTime = this.createTime;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReaderTheme that = (ReaderTheme) o;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ReaderTheme{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", backgroundColor=" + Integer.toHexString(backgroundColor) +
                ", textColor=" + Integer.toHexString(textColor) +
                ", isCustom=" + isCustom +
                '}';
    }
}
