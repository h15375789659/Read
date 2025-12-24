package com.example.read.data.service;

import com.example.read.domain.model.ChapterInfo;
import com.example.read.domain.model.NovelMetadata;
import com.example.read.domain.model.ParserRule;
import com.example.read.domain.service.WebParserService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 网站解析服务实现类
 * 使用Jsoup实现HTML解析
 */
@Singleton
public class WebParserServiceImpl implements WebParserService {
    
    // 网络请求超时时间（秒）
    private static final int TIMEOUT_SECONDS = 15;
    
    // 常见广告相关的CSS选择器
    private static final String[] DEFAULT_AD_SELECTORS = {
        ".ad", ".ads", ".advertisement", ".advert",
        "#ad", "#ads", "#advertisement",
        "[class*='ad-']", "[class*='ads-']",
        "[id*='ad-']", "[id*='ads-']",
        ".banner", "#banner",
        ".popup", "#popup",
        ".sponsor", "#sponsor",
        "script", "style", "iframe",
        ".comment", "#comment", ".comments", "#comments"
    };
    
    // 常见需要移除的文本模式
    private static final Pattern[] AD_TEXT_PATTERNS = {
        Pattern.compile("(?i)广告"),
        Pattern.compile("(?i)推荐阅读"),
        Pattern.compile("(?i)本章未完"),
        Pattern.compile("(?i)点击.*继续阅读"),
        Pattern.compile("(?i)加入书签"),
        Pattern.compile("(?i)手机阅读"),
        Pattern.compile("(?i)\\[.*?\\]"), // 移除方括号内容如[广告]
    };

    @Inject
    public WebParserServiceImpl() {}

    @Override
    public Single<String> fetchHtml(String url) {
        return Single.fromCallable(() -> {
            Document doc = Jsoup.connect(url)
                    .timeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();
            return doc.html();
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public NovelMetadata extractNovelInfo(String html, ParserRule rule) {
        if (html == null || html.isEmpty()) {
            return new NovelMetadata();
        }
        
        Document doc = Jsoup.parse(html);
        NovelMetadata metadata = new NovelMetadata();
        
        // 提取标题 - 尝试多种方式
        String title = extractTitle(doc, rule);
        metadata.setTitle(title);
        
        // 提取作者 - 尝试多种方式
        String author = extractAuthor(doc, rule);
        metadata.setAuthor(author);
        
        // 提取简介 - 尝试多种方式
        String description = extractDescription(doc, rule);
        metadata.setDescription(description);
        
        return metadata;
    }

    @Override
    public List<ChapterInfo> extractChapterList(String html, ParserRule rule) {
        List<ChapterInfo> chapters = new ArrayList<>();
        
        if (html == null || html.isEmpty() || rule == null) {
            return chapters;
        }
        
        Document doc = Jsoup.parse(html);
        
        // 使用规则中的章节列表选择器
        String chapterListSelector = rule.getChapterListSelector();
        if (chapterListSelector == null || chapterListSelector.isEmpty()) {
            return chapters;
        }
        
        Elements chapterElements = doc.select(chapterListSelector);
        
        int index = 0;
        for (Element element : chapterElements) {
            ChapterInfo chapterInfo = new ChapterInfo();
            
            // 提取章节标题
            String titleSelector = rule.getChapterTitleSelector();
            String title;
            if (titleSelector != null && !titleSelector.isEmpty()) {
                Element titleElement = element.selectFirst(titleSelector);
                title = titleElement != null ? titleElement.text().trim() : element.text().trim();
            } else {
                title = element.text().trim();
            }
            chapterInfo.setTitle(title);
            
            // 提取章节链接
            String linkSelector = rule.getChapterLinkSelector();
            String url;
            if (linkSelector != null && !linkSelector.isEmpty()) {
                Element linkElement = element.selectFirst(linkSelector);
                if (linkElement != null) {
                    url = linkElement.absUrl("href");
                    // 如果absUrl返回空，尝试使用attr
                    if (url.isEmpty()) {
                        url = linkElement.attr("href");
                    }
                } else {
                    url = element.absUrl("href");
                    if (url.isEmpty()) {
                        url = element.attr("href");
                    }
                }
            } else {
                // 如果元素本身是链接
                url = element.absUrl("href");
                if (url.isEmpty()) {
                    url = element.attr("href");
                }
                if (url.isEmpty()) {
                    Element linkElement = element.selectFirst("a");
                    if (linkElement != null) {
                        url = linkElement.absUrl("href");
                        if (url.isEmpty()) {
                            url = linkElement.attr("href");
                        }
                    }
                }
            }
            chapterInfo.setUrl(url);
            
            chapterInfo.setIndex(index++);
            
            // 只添加有效的章节
            if (chapterInfo.isValid()) {
                chapters.add(chapterInfo);
            }
        }
        
        return chapters;
    }

    @Override
    public String extractChapterContent(String html, ParserRule rule) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        
        Document doc = Jsoup.parse(html);
        
        // 首先移除规则中指定的元素
        if (rule != null && rule.getRemoveSelectors() != null) {
            for (String selector : rule.getRemoveSelectors()) {
                if (selector != null && !selector.trim().isEmpty()) {
                    doc.select(selector.trim()).remove();
                }
            }
        }
        
        // 移除默认的广告元素
        removeAdElements(doc);
        
        // 提取正文内容
        String content = "";
        if (rule != null && rule.getContentSelector() != null && !rule.getContentSelector().isEmpty()) {
            Element contentElement = doc.selectFirst(rule.getContentSelector());
            if (contentElement != null) {
                content = contentElement.text();
            }
        }
        
        // 清理内容
        return cleanContent(content);
    }

    @Override
    public String cleanContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        String cleaned = content;
        
        // 移除广告文本模式
        for (Pattern pattern : AD_TEXT_PATTERNS) {
            cleaned = pattern.matcher(cleaned).replaceAll("");
        }
        
        // 规范化空白字符
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        // 恢复段落分隔（将连续空格转换为换行）
        cleaned = cleaned.replaceAll("  +", "\n\n");
        
        return cleaned;
    }
    
    /**
     * 从文档中提取标题
     */
    private String extractTitle(Document doc, ParserRule rule) {
        // 尝试常见的标题选择器
        String[] titleSelectors = {
            "h1", ".title", "#title", ".book-title", "#book-title",
            ".novel-title", "#novel-title", "meta[property='og:title']"
        };
        
        for (String selector : titleSelectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String title = selector.startsWith("meta") 
                    ? element.attr("content") 
                    : element.text();
                if (title != null && !title.trim().isEmpty()) {
                    return title.trim();
                }
            }
        }
        
        // 回退到页面标题
        String pageTitle = doc.title();
        if (pageTitle != null && !pageTitle.isEmpty()) {
            // 移除常见的网站后缀
            pageTitle = pageTitle.replaceAll("[-_|].*$", "").trim();
            return pageTitle;
        }
        
        return "";
    }
    
    /**
     * 从文档中提取作者
     */
    private String extractAuthor(Document doc, ParserRule rule) {
        // 尝试常见的作者选择器
        String[] authorSelectors = {
            ".author", "#author", ".book-author", "#book-author",
            ".writer", "#writer", "meta[property='og:author']",
            "[itemprop='author']"
        };
        
        for (String selector : authorSelectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String author = selector.startsWith("meta") 
                    ? element.attr("content") 
                    : element.text();
                if (author != null && !author.trim().isEmpty()) {
                    // 清理作者名称（移除"作者："等前缀）
                    author = author.replaceAll("^(作者|作　者|Author)[：:]\\s*", "");
                    return author.trim();
                }
            }
        }
        
        // 尝试在文本中查找作者信息
        Elements elements = doc.select("*:containsOwn(作者)");
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("作者")) {
                String author = text.replaceAll(".*作者[：:]\\s*", "")
                                   .replaceAll("\\s.*", "")
                                   .trim();
                if (!author.isEmpty()) {
                    return author;
                }
            }
        }
        
        return "";
    }
    
    /**
     * 从文档中提取简介
     */
    private String extractDescription(Document doc, ParserRule rule) {
        // 尝试常见的简介选择器
        String[] descSelectors = {
            ".description", "#description", ".intro", "#intro",
            ".summary", "#summary", ".book-intro", "#book-intro",
            "meta[property='og:description']", "meta[name='description']"
        };
        
        for (String selector : descSelectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String desc = selector.startsWith("meta") 
                    ? element.attr("content") 
                    : element.text();
                if (desc != null && !desc.trim().isEmpty()) {
                    return desc.trim();
                }
            }
        }
        
        return "";
    }
    
    /**
     * 移除文档中的广告元素
     */
    private void removeAdElements(Document doc) {
        for (String selector : DEFAULT_AD_SELECTORS) {
            try {
                doc.select(selector).remove();
            } catch (Exception e) {
                // 忽略无效选择器
            }
        }
    }
}
