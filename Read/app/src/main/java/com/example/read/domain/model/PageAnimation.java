package com.example.read.domain.model;

/**
 * 翻页动画类型枚举
 */
public enum PageAnimation {
    /**
     * 覆盖动画 - 新页面从侧边滑入覆盖旧页面
     */
    COVER("cover", "覆盖"),
    
    /**
     * 滑动动画 - 两个页面同时滑动
     */
    SLIDE("slide", "滑动"),
    
    /**
     * 仿真动画 - 模拟真实翻书效果
     */
    SIMULATION("simulation", "仿真"),
    
    /**
     * 滚动动画 - 平滑滚动效果
     */
    SCROLL("scroll", "滚动");

    private final String id;
    private final String displayName;

    PageAnimation(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PageAnimation fromId(String id) {
        for (PageAnimation anim : values()) {
            if (anim.id.equals(id)) {
                return anim;
            }
        }
        return SLIDE; // 默认滑动动画
    }
    
    public static PageAnimation[] values_array() {
        return values();
    }
}
