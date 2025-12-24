package com.example.read.domain.model;

/**
 * 翻页模式枚举
 */
public enum PageMode {
    /**
     * 上下滚动模式
     */
    SCROLL("scroll", "上下滚动"),
    
    /**
     * 左右翻页模式
     */
    PAGE("page", "左右翻页");

    private final String id;
    private final String displayName;

    PageMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PageMode fromId(String id) {
        for (PageMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return SCROLL; // 默认上下滚动
    }
}
