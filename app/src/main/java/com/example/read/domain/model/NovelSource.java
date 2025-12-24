package com.example.read.domain.model;

/**
 * 小说来源枚举
 */
public enum NovelSource {
    LOCAL("local"),
    WEB("web");

    private final String value;

    NovelSource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NovelSource fromString(String value) {
        for (NovelSource source : values()) {
            if (source.value.equalsIgnoreCase(value)) {
                return source;
            }
        }
        return LOCAL;
    }
}
