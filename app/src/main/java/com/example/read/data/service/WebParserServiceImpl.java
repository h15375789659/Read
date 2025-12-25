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
    
    // 安全的广告过滤模式（只匹配独立的整行，避免误删正文）
    // 使用多行模式(?m)，只删除完整的一行
    private static final Pattern[] SAFE_AD_PATTERNS = {
        // 章节标题重复行（独立一行的章节标题）
        // 第=\u7b2c 章=\u7ae0
        Pattern.compile("(?m)^\u7b2c[\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341\u767e\u5343\u4e07\u96f6\\d]+\u7ae0.*$"),
        // 收藏提示行（包含Ctrl+D和收藏的整行）
        // 收藏=\u6536\u85cf
        Pattern.compile("(?m)^.*Ctrl\\s*\\+\\s*D.*\u6536\u85cf.*$"),
        // 导航链接行（上一章、目录、下一章）
        // 上=\u4e0a 下=\u4e0b 一=\u4e00 章=\u7ae0 目录=\u76ee\u5f55
        Pattern.compile("(?m)^\u4e0a\u4e00\u7ae0$"),      // 上一章
        Pattern.compile("(?m)^\u4e0b\u4e00\u7ae0$"),      // 下一章
        Pattern.compile("(?m)^\u76ee\u5f55$"),            // 目录
        // 独立的网址行
        Pattern.compile("(?m)^https?://[^\\s]+$"),
        Pattern.compile("(?m)^www\\.[^\\s]+$"),
        // 独立的常见网站名行（只匹配独立一行）
        // 天蚕土豆=\u5929\u8695\u571f\u8c46
        Pattern.compile("(?m)^\u5929\u8695\u571f\u8c46$"),
        // 笔趣阁=\u7b14\u8da3\u9601
        Pattern.compile("(?m)^\u7b14\u8da3\u9601$"),
        Pattern.compile("(?m)^\u65b0\u7b14\u8da3\u9601$"),
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

    // 常见的章节内容选择器（按优先级排序）
    private static final String[] CONTENT_SELECTORS = {
        // 常见ID选择器
        "#content", "#chaptercontent", "#chapter-content", "#bookcontent",
        "#book_text", "#booktext", "#htmlContent", "#text-content",
        "#nr", "#nr1", "#nr_title", "#BookText", "#TextContent",
        "#contentbox", "#chapter_content", "#novelcontent",
        // 常见class选择器
        ".content", ".chaptercontent", ".chapter-content", ".bookcontent",
        ".book_text", ".booktext", ".novelcontent", ".novel-content",
        ".readcontent", ".read-content", ".article-content", ".txt",
        ".nr_title", ".chapter_content", ".text_content", ".TextContent",
        ".contentbox", ".book-content", ".main-content", ".post-content",
        // 语义化标签
        "article", ".article", "#article",
        "[itemprop='articleBody']",
        // 更多回退选择器
        ".panel-body", ".card-body", ".entry-content", ".post-body"
    };

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
                    try {
                        doc.select(selector.trim()).remove();
                    } catch (Exception e) {
                        // 忽略无效选择器
                    }
                }
            }
        }
        
        // 移除默认的广告元素
        removeAdElements(doc);
        
        // 提取正文内容
        String content = "";
        
        // 1. 首先尝试使用规则中的选择器
        if (rule != null && rule.getContentSelector() != null && !rule.getContentSelector().isEmpty()) {
            Element contentElement = doc.selectFirst(rule.getContentSelector());
            if (contentElement != null) {
                content = extractTextWithParagraphs(contentElement);
            }
        }
        
        // 2. 如果规则选择器没有找到内容，尝试常见选择器
        if (content.isEmpty()) {
            for (String selector : CONTENT_SELECTORS) {
                try {
                    Element contentElement = doc.selectFirst(selector);
                    if (contentElement != null) {
                        String text = extractTextWithParagraphs(contentElement);
                        // 内容长度大于100才认为是有效内容
                        if (text.length() > 100) {
                            content = text;
                            Log.d(TAG, "使用回退选择器找到内容: " + selector + ", 长度: " + text.length());
                            break;
                        }
                    }
                } catch (Exception e) {
                    // 忽略无效选择器
                }
            }
        }
        
        // 3. 如果还是没有找到，尝试查找最大的文本块
        if (content.isEmpty()) {
            content = findLargestTextBlock(doc);
            if (!content.isEmpty()) {
                Log.d(TAG, "使用最大文本块方法找到内容, 长度: " + content.length());
            }
        }
        
        // 如果仍然没有内容，记录警告
        if (content.isEmpty()) {
            Log.w(TAG, "无法提取章节内容，HTML长度: " + html.length());
        }
        
        // 清理内容
        return cleanContent(content);
    }
    
    /**
     * 提取元素文本，保留段落结构
     * 智能处理HTML中的段落和换行标签
     * 
     * 修复：正确处理 <p> 标签内部的 <br> 标签
     */
    private String extractTextWithParagraphs(Element element) {
        if (element == null) return "";
        
        // 获取元素的HTML内容
        String html = element.html();
        
        // 将 <br> 和 <br/> 替换为换行占位符（不区分大小写，支持带属性的br标签）
        html = html.replaceAll("(?i)<br\\b[^>]*>", "{{BR}}");
        
        // 将 </p> 替换为段落结束占位符
        html = html.replaceAll("(?i)</p>", "{{P_END}}");
        
        // 将 <p...> 开始标签移除（保留内容）
        html = html.replaceAll("(?i)<p[^>]*>", "");
        
        // 移除其他HTML标签，但保留内容
        html = html.replaceAll("<[^>]+>", "");
        
        // 解码常见HTML实体
        html = decodeHtmlEntities(html);
        
        // 将占位符替换为实际换行（紧凑模式：所有换行都只用单换行）
        html = html.replace("{{BR}}", "\n");
        html = html.replace("{{P_END}}", "\n");
        
        // 清理：移除每行首尾空白，跳过空行（紧凑模式）
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
    
    /**
     * 解码常见HTML实体
     */
    private String decodeHtmlEntities(String html) {
        if (html == null) return "";
        
        // 常见HTML实体解码（使用Unicode转义避免编码问题）
        html = html.replace("&nbsp;", " ");
        html = html.replace("&ldquo;", "\u201c");  // 左双引号 "
        html = html.replace("&rdquo;", "\u201d");  // 右双引号 "
        html = html.replace("&lsquo;", "\u2018");  // 左单引号 '
        html = html.replace("&rsquo;", "\u2019");  // 右单引号 '
        html = html.replace("&hellip;", "\u2026"); // 省略号 …
        html = html.replace("&mdash;", "\u2014");  // 长破折号 —
        html = html.replace("&ndash;", "\u2013");  // 短破折号 –
        html = html.replace("&amp;", "&");
        html = html.replace("&lt;", "<");
        html = html.replace("&gt;", ">");
        html = html.replace("&quot;", "\"");
        html = html.replace("&apos;", "'");
        html = html.replace("&#39;", "'");
        html = html.replace("&#34;", "\"");
        
        // 处理数字实体 &#xxx;
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
    
    /**
     * 查找页面中最大的文本块（作为最后的回退方案）
     * 修复：使用 extractTextWithParagraphs 保留换行结构
     */
    private String findLargestTextBlock(Document doc) {
        Element largestElement = null;
        int maxLength = 0;
        
        // 查找所有可能包含正文的元素
        Elements candidates = doc.select("div, article, section, main");
        
        for (Element element : candidates) {
            // 跳过导航、页眉、页脚等
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
            // 只考虑长度大于200且比当前最大的文本块
            if (text.length() > 200 && text.length() > maxLength) {
                maxLength = text.length();
                largestElement = element;
            }
        }
        
        // 使用 extractTextWithParagraphs 提取内容，保留换行结构
        return largestElement != null ? extractTextWithParagraphs(largestElement) : "";
    }

    @Override
    public String cleanContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        String cleaned = content;
        
        // 使用安全的广告过滤（只删除独立的整行）
        for (Pattern pattern : SAFE_AD_PATTERNS) {
            cleaned = pattern.matcher(cleaned).replaceAll("");
        }
        
        // 移除行首行尾空白（只移除空格和制表符，保留换行符）
        cleaned = cleaned.replaceAll("(?m)^[ \\t]+|[ \\t]+$", "");
        
        // 移除多余的空白行（超过2个连续换行变成2个）
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        
        // 如果内容没有换行，尝试按句子分段（只在完全没有换行时）
        if (!cleaned.contains("\n")) {
            // 在对话结束后添加换行（引号后跟非标点）
            // "=\u201d '=\u2019
            cleaned = cleaned.replaceAll("([\u201d\u2019])\\s*", "$1\n");
        }
        
        // 最终清理：移除首尾空白和多余空行
        cleaned = cleaned.trim();
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        
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
