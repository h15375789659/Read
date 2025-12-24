package com.example.read.domain.model;

/**
 * 阅读器字体枚举
 * 
 * 提供几种常用字体选择
 */
public enum ReaderFont {
    /**
     * 系统默认字体
     */
    DEFAULT("default", "默认", null),
    
    /**
     * 宋体
     */
    SONG("song", "宋体", "fonts/simsun.ttf"),
    
    /**
     * 楷体
     */
    KAI("kai", "楷体", "fonts/simkai.ttf"),
    
    /**
     * 黑体
     */
    HEI("hei", "黑体", "fonts/simhei.ttf"),
    
    /**
     * 仿宋
     */
    FANGSONG("fangsong", "仿宋", "fonts/simfang.ttf");

    private final String id;
    private final String displayName;
    private final String fontPath;  // assets中的字体文件路径

    ReaderFont(String id, String displayName, String fontPath) {
        this.id = id;
        this.displayName = displayName;
        this.fontPath = fontPath;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFontPath() {
        return fontPath;
    }

    /**
     * 根据ID获取字体
     */
    public static ReaderFont fromId(String id) {
        if (id == null) return DEFAULT;
        for (ReaderFont font : values()) {
            if (font.id.equals(id)) {
                return font;
            }
        }
        return DEFAULT;
    }
}
