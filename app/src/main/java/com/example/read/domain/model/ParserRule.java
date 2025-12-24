package com.example.read.domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 解析规则领域模型
 */
public class ParserRule {
    private long id;
    private String name;
    private String domain;
    private String chapterListSelector;
    private String chapterTitleSelector;
    private String chapterLinkSelector;
    private String contentSelector;
    private List<String> removeSelectors;
    private long createTime;

    public ParserRule() {
        this.removeSelectors = new ArrayList<>();
    }

    public ParserRule(String name, String domain, String chapterListSelector,
                      String chapterTitleSelector, String chapterLinkSelector,
                      String contentSelector) {
        this.name = name;
        this.domain = domain;
        this.chapterListSelector = chapterListSelector;
        this.chapterTitleSelector = chapterTitleSelector;
        this.chapterLinkSelector = chapterLinkSelector;
        this.contentSelector = contentSelector;
        this.removeSelectors = new ArrayList<>();
        this.createTime = System.currentTimeMillis();
    }

    // Getters
    public long getId() { return id; }
    public String getName() { return name; }
    public String getDomain() { return domain; }
    public String getChapterListSelector() { return chapterListSelector; }
    public String getChapterTitleSelector() { return chapterTitleSelector; }
    public String getChapterLinkSelector() { return chapterLinkSelector; }
    public String getContentSelector() { return contentSelector; }
    public List<String> getRemoveSelectors() { return removeSelectors; }
    public long getCreateTime() { return createTime; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDomain(String domain) { this.domain = domain; }
    public void setChapterListSelector(String chapterListSelector) { this.chapterListSelector = chapterListSelector; }
    public void setChapterTitleSelector(String chapterTitleSelector) { this.chapterTitleSelector = chapterTitleSelector; }
    public void setChapterLinkSelector(String chapterLinkSelector) { this.chapterLinkSelector = chapterLinkSelector; }
    public void setContentSelector(String contentSelector) { this.contentSelector = contentSelector; }
    public void setRemoveSelectors(List<String> removeSelectors) { this.removeSelectors = removeSelectors; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    /**
     * 从逗号分隔的字符串设置移除选择器列表
     */
    public void setRemoveSelectorsFromString(String selectorsString) {
        if (selectorsString != null && !selectorsString.isEmpty()) {
            this.removeSelectors = Arrays.asList(selectorsString.split(","));
        } else {
            this.removeSelectors = new ArrayList<>();
        }
    }

    /**
     * 获取逗号分隔的移除选择器字符串
     */
    public String getRemoveSelectorsAsString() {
        if (removeSelectors == null || removeSelectors.isEmpty()) {
            return null;
        }
        return String.join(",", removeSelectors);
    }

    /**
     * 验证规则是否完整
     */
    public boolean isValid() {
        return domain != null && !domain.isEmpty()
                && chapterListSelector != null && !chapterListSelector.isEmpty()
                && contentSelector != null && !contentSelector.isEmpty();
    }
}
