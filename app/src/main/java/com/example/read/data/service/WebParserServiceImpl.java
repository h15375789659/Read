package com.example.read.data.service;

import android.util.Log;

import com.example.read.domain.model.ChapterInfo;
import com.example.read.domain.model.NovelMetadata;
import com.example.read.domain.model.ParserRule;
import com.example.read.domain.service.WebParserService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
    
    private static final String TAG = "WebParserService";
    
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
    
    // 安全的广告过滤模式
    private static final Pattern[] SAFE_AD_PATTERNS = {
        Pattern.compile("(?m)^\u7b2c[\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341\u767e\u5343\u4e07\u96f6\\d]+\u7ae0.*$"),
        Pattern.compile("(?m)^.*Ctrl\\s*\\+\\s*D.*\u6536\u85cf.*$"),
        Pattern.compile("(?m)^\u4e0a\u4e00\u7ae0$"),
        Pattern.compile("(?m)^\u4e0b\u4e00\u7ae0$"),
        Pattern.compile("(?m)^\u76ee\u5f55$"),
        Pattern.compile("(?m)^https?://[^\\s]+$"),
        Pattern.compile("(?m)^www\\.[^\\s]+$"),
        Pattern.compile("(?m)^\u5929\u8695\u571f\u8c46$"),
        Pattern.compile("(?m)^\u7b14\u8da3\u9601$"),
        Pattern.compile("(?m)^\u65b0\u7b14\u8da3\u9601$"),
    };

    @Inject
    public WebParserServiceImpl() {
    }

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
        
        String title = extractTitle(doc, rule);
        metadata.setTitle(title);
        
        String author = extractAuthor(doc, rule);
        metadata.setAuthor(author);
        
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
        
        String chapterListSelector = rule.getChapterListSelector();
        if (chapterListSelector == null || chapterListSelector.isEmpty()) {
            return chapters;
        }
        
        Elements chapterElements = doc.select(chapterListSelector);
        
        int index = 0;
        for (Element element : chapterElements) {
            ChapterInfo chapterInfo = new ChapterInfo();
            
            String titleSelector = rule.getChapterTitleSelector();
            String title;
            if (titleSelector != null && !titleSelector.isEmpty()) {
                Element titleElement = element.selectFirst(titleSelector);
                title = titleElement != null ? titleElement.text().trim() : element.text().trim();
            } else {
                title = element.text().trim();
            }
            chapterInfo.setTitle(title);
            
            String linkSelector = rule.getChapterLinkSelector();
            String url;
            if (linkSelector != null && !linkSelector.isEmpty()) {
                Element linkElement = element.selectFirst(linkSelector);
                if (linkElement != null) {
                    url = linkElement.absUrl("href");
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
            
            if (chapterInfo.isValid()) {
                chapters.add(chapterInfo);
            }
        }
        
        return chapters;
    }

    private static final String[] CONTENT_SELECTORS = {
        "#content", "#chaptercontent", "#chapter-content", "#bookcontent",
        "#book_text", "#booktext", "#htmlContent", "#text-content",
        "#nr", "#nr1", "#nr_title", "#BookText", "#TextContent",
        "#contentbox", "#chapter_content", "#novelcontent",
        ".content", ".chaptercontent", ".chapter-content", ".bookcontent",
        ".book_text", ".booktext", ".novelcontent", ".novel-content",
        ".readcontent", ".read-content", ".article-content", ".txt",
        ".nr_title", ".chapter_content", ".text_content", ".TextContent",
        ".contentbox", ".book-content", ".main-content", ".post-content",
        "article", ".article", "#article",
        "[itemprop='articleBody']",
        ".panel-body", ".card-body", ".entry-content", ".post-body"
    };

    @Override
    public String extractChapterContent(String html, ParserRule rule) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        
        Document doc = Jsoup.parse(html);
        
        if (rule != null && rule.getRemoveSelectors() != null) {
            for (String selector : rule.getRemoveSelectors()) {
                if (selector != null && !selector.trim().isEmpty()) {
                    try {
                        doc.select(selector.trim()).remove();
                    } catch (Exception e) {
                        // 忽略无效选择器
                    }
                }
            }
        }
        
        removeAdElements(doc);
        
        String content = "";
        
        if (rule != null && rule.getContentSelector() != null && !rule.getContentSelector().isEmpty()) {
            Element contentElement = doc.selectFirst(rule.getContentSelector());
            if (contentElement != null) {
                content = extractTextWithParagraphs(contentElement);
            }
        }
        
        if (content.isEmpty()) {
            for (String selector : CONTENT_SELECTORS) {
                try {
                    Element contentElement = doc.selectFirst(selector);
                    if (contentElement != null) {
                        String text = extractTextWithParagraphs(contentElement);
                        if (text.length() > 100) {
                            content = text;
                            break;
                        }
                    }
                } catch (Exception e) {
                    // 忽略无效选择器
                }
            }
        }
        
        if (content.isEmpty()) {
            content = findLargestTextBlock(doc);
        }
        
        return cleanContent(content);
    }

    
    private String extractTextWithParagraphs(Element element) {
        if (element == null) return "";
        
        String html = element.html();
        html = html.replaceAll("(?i)<br\\b[^>]*>", "{{BR}}");
        html = html.replaceAll("(?i)</p>", "{{P_END}}");
        html = html.replaceAll("(?i)<p[^>]*>", "");
        html = html.replaceAll("<[^>]+>", "");
        html = decodeHtmlEntities(html);
        html = html.replace("{{BR}}", "\n");
        html = html.replace("{{P_END}}", "\n");
        
        String[] lines = html.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                sb.append(trimmed).append("\n");
            }
        }
        
        return sb.toString().trim();
    }
    
    private String decodeHtmlEntities(String html) {
        if (html == null) return "";
        
        html = html.replace("&nbsp;", " ");
        html = html.replace("&ldquo;", "\u201c");
        html = html.replace("&rdquo;", "\u201d");
        html = html.replace("&lsquo;", "\u2018");
        html = html.replace("&rsquo;", "\u2019");
        html = html.replace("&hellip;", "\u2026");
        html = html.replace("&mdash;", "\u2014");
        html = html.replace("&ndash;", "\u2013");
        html = html.replace("&amp;", "&");
        html = html.replace("&lt;", "<");
        html = html.replace("&gt;", ">");
        html = html.replace("&quot;", "\"");
        html = html.replace("&apos;", "'");
        html = html.replace("&#39;", "'");
        html = html.replace("&#34;", "\"");
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("&#(\\d+);");
        java.util.regex.Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            try {
                int code = Integer.parseInt(matcher.group(1));
                matcher.appendReplacement(sb, String.valueOf((char) code));
            } catch (Exception e) {
                // 保持原样
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    private String findLargestTextBlock(Document doc) {
        Element largestElement = null;
        int maxLength = 0;
        
        Elements candidates = doc.select("div, article, section, main");
        
        for (Element element : candidates) {
            String className = element.className().toLowerCase();
            String id = element.id().toLowerCase();
            if (className.contains("nav") || className.contains("header") || 
                className.contains("footer") || className.contains("sidebar") ||
                className.contains("menu") || className.contains("comment") ||
                id.contains("nav") || id.contains("header") || 
                id.contains("footer") || id.contains("sidebar")) {
                continue;
            }
            
            String text = element.text();
            if (text.length() > 200 && text.length() > maxLength) {
                maxLength = text.length();
                largestElement = element;
            }
        }
        
        return largestElement != null ? extractTextWithParagraphs(largestElement) : "";
    }

    @Override
    public String cleanContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        String cleaned = content;
        
        for (Pattern pattern : SAFE_AD_PATTERNS) {
            cleaned = pattern.matcher(cleaned).replaceAll("");
        }
        
        cleaned = cleaned.replaceAll("(?m)^[ \\t]+|[ \\t]+$", "");
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        
        if (!cleaned.contains("\n")) {
            cleaned = cleaned.replaceAll("([\u201d\u2019])\\s*", "$1\n");
        }
        
        cleaned = cleaned.trim();
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        
        return cleaned;
    }
    
    private String extractTitle(Document doc, ParserRule rule) {
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
        
        String pageTitle = doc.title();
        if (pageTitle != null && !pageTitle.isEmpty()) {
            pageTitle = pageTitle.replaceAll("[-_|].*$", "").trim();
            return pageTitle;
        }
        
        return "";
    }
    
    private String extractAuthor(Document doc, ParserRule rule) {
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
                    author = author.replaceAll("^(作者|作　者|Author)[：:]\\s*", "");
                    return author.trim();
                }
            }
        }
        
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
    
    private String extractDescription(Document doc, ParserRule rule) {
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
