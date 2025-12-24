package com.example.read.data.service;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.read.domain.model.ParsedNovel;
import com.example.read.domain.service.FileParserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;

/**
 * 文件解析服务实现
 * 支持TXT和EPUB格式的小说文件解析
 */
@Singleton
public class FileParserServiceImpl implements FileParserService {

    private static final String TAG = "FileParserService";
    
    private final Context context;
    
    // 章节标题匹配模式 - 更严格的版本
    // 匹配以"第X章"、"Chapter X"或"数字、"开头的行
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
        "^\\s*(第[\\u96f6\\u4e00\\u4e8c\\u4e09\\u56db\\u4e94\\u516d\\u4e03\\u516b\\u4e5d\\u5341\\u767e\\u5343\\u4e070-9]+[\\u7ae0\\u8282\\u56de\\u5377\\u96c6\\u90e8\\u7bc7].*|" +
        "[Cc]hapter\\s*\\d+.*|" +
        "\\d{1,5}[\\u3001.\\uff0e].*)$",
        Pattern.CASE_INSENSITIVE
    );
    
    // 用于检测是否是有效的章节标题（排除正文中的引用）
    private static final int MAX_CHAPTER_TITLE_LENGTH = 50;

    @Inject
    public FileParserServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public Single<ParsedNovel> parseTxtFile(Uri uri) {
        return Single.fromCallable(() -> {
            Log.d(TAG, "开始解析TXT文件: " + uri);
            ContentResolver resolver = context.getContentResolver();
            
            try (InputStream inputStream = resolver.openInputStream(uri)) {
                if (inputStream == null) {
                    Log.e(TAG, "无法打开文件输入流");
                    throw new IOException("无法打开文件");
                }
                Log.d(TAG, "成功打开文件输入流");
                ParsedNovel result = parseTxtContent(inputStream, getFileNameFromUri(uri));
                Log.d(TAG, "TXT解析完成，标题: " + result.getTitle() + ", 章节数: " + 
                        (result.getChapters() != null ? result.getChapters().size() : 0));
                return result;
            } catch (Exception e) {
                Log.e(TAG, "解析TXT文件异常", e);
                throw e;
            }
        });
    }


    @Override
    public Single<ParsedNovel> parseEpubFile(Uri uri) {
        return Single.fromCallable(() -> {
            Log.d(TAG, "开始解析EPUB文件: " + uri);
            ContentResolver resolver = context.getContentResolver();
            
            try (InputStream inputStream = resolver.openInputStream(uri)) {
                if (inputStream == null) {
                    Log.e(TAG, "无法打开文件输入流");
                    throw new IOException("无法打开文件");
                }
                Log.d(TAG, "成功打开文件输入流");
                ParsedNovel result = parseEpubContent(inputStream, getFileNameFromUri(uri));
                Log.d(TAG, "EPUB解析完成，标题: " + result.getTitle() + ", 章节数: " + 
                        (result.getChapters() != null ? result.getChapters().size() : 0));
                return result;
            } catch (Exception e) {
                Log.e(TAG, "解析EPUB文件异常", e);
                throw e;
            }
        });
    }

    /**
     * 解析TXT文件内容 - 优化版本，流式处理避免内存问题
     */
    private ParsedNovel parseTxtContent(InputStream inputStream, String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8192);
        
        ParsedNovel novel = new ParsedNovel();
        
        // 从文件名提取标题
        String title = fileName;
        if (title.toLowerCase().endsWith(".txt")) {
            title = title.substring(0, title.length() - 4);
        }
        novel.setTitle(title);
        novel.setAuthor("未知作者");
        
        // 流式解析章节，避免一次性加载整个文件
        // 同时尝试从前几行提取作者信息
        List<ParsedNovel.ParsedChapter> chapters = parseChaptersStreamingWithAuthor(reader, novel);
        novel.setChapters(chapters);
        
        return novel;
    }

    /**
     * 流式解析章节内容并尝试提取作者信息
     */
    private List<ParsedNovel.ParsedChapter> parseChaptersStreamingWithAuthor(BufferedReader reader, ParsedNovel novel) throws IOException {
        List<ParsedNovel.ParsedChapter> chapters = new ArrayList<>();
        StringBuilder currentContent = new StringBuilder();
        String currentTitle = null;
        String line;
        int chapterIndex = 0;
        String lastChapterTitle = null;
        int lineCount = 0;
        boolean authorFound = false;
        
        // 作者匹配模式
        Pattern authorPattern = Pattern.compile(
            "^\\s*(?:作者|Author|著|by|作\\s*者)[：:：\\s]+(.+)$",
            Pattern.CASE_INSENSITIVE
        );
        
        while ((line = reader.readLine()) != null) {
            lineCount++;
            
            // 在前50行中尝试提取作者信息
            if (!authorFound && lineCount <= 50) {
                Matcher authorMatcher = authorPattern.matcher(line.trim());
                if (authorMatcher.find()) {
                    String author = authorMatcher.group(1).trim();
                    if (!author.isEmpty() && author.length() < 50) {
                        novel.setAuthor(author);
                        authorFound = true;
                        Log.d(TAG, "从文件中提取到作者: " + author);
                    }
                }
            }
            
            // 检查是否是章节标题
            if (isValidChapterTitle(line, lastChapterTitle)) {
                // 保存之前的章节
                if (currentTitle != null && currentContent.length() > 0) {
                    ParsedNovel.ParsedChapter chapter = new ParsedNovel.ParsedChapter();
                    chapter.setTitle(currentTitle);
                    chapter.setContent(currentContent.toString().trim());
                    chapter.setIndex(chapterIndex++);
                    chapters.add(chapter);
                    currentContent.setLength(0);
                } else if (currentContent.length() > 100) {
                    // 第一章之前的内容作为序言
                    ParsedNovel.ParsedChapter chapter = new ParsedNovel.ParsedChapter();
                    chapter.setTitle("序言");
                    chapter.setContent(currentContent.toString().trim());
                    chapter.setIndex(chapterIndex++);
                    chapters.add(chapter);
                    currentContent.setLength(0);
                }
                
                currentTitle = line.trim();
                lastChapterTitle = extractChapterNumber(currentTitle);
            } else {
                currentContent.append(line).append("\n");
            }
        }
        
        // 保存最后一个章节
        if (currentTitle != null && currentContent.length() > 0) {
            ParsedNovel.ParsedChapter chapter = new ParsedNovel.ParsedChapter();
            chapter.setTitle(currentTitle);
            chapter.setContent(currentContent.toString().trim());
            chapter.setIndex(chapterIndex++);
            chapters.add(chapter);
        } else if (chapters.isEmpty() && currentContent.length() > 0) {
            // 没有找到章节标题，将整个内容作为一个章节
            ParsedNovel.ParsedChapter chapter = new ParsedNovel.ParsedChapter();
            chapter.setTitle("正文");
            chapter.setContent(currentContent.toString().trim());
            chapter.setIndex(0);
            chapters.add(chapter);
        }
        
        return chapters;
    }
    
    /**
     * 检查是否是有效的章节标题
     * @param line 当前行
     * @param lastChapterNumber 上一个章节的编号（用于检测重复）
     * @return 是否是有效的章节标题
     */
    private boolean isValidChapterTitle(String line, String lastChapterNumber) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        String trimmedLine = line.trim();
        
        // 检查长度限制
        if (trimmedLine.length() > MAX_CHAPTER_TITLE_LENGTH) {
            return false;
        }
        
        // 检查是否匹配章节标题模式
        Matcher matcher = CHAPTER_PATTERN.matcher(trimmedLine);
        if (!matcher.matches()) {
            return false;
        }
        
        // 提取当前章节编号
        String currentChapterNumber = extractChapterNumber(trimmedLine);
        
        // 如果章节编号与上一个相同，可能是正文中的引用，跳过
        if (currentChapterNumber != null && currentChapterNumber.equals(lastChapterNumber)) {
            Log.d(TAG, "跳过重复章节标题: " + trimmedLine);
            return false;
        }
        
        return true;
    }
    
    /**
     * 从章节标题中提取章节编号
     * 例如："第八百五十五章 炼制" -> "八百五十五"
     */
    private String extractChapterNumber(String title) {
        if (title == null) return null;
        
        // 匹配中文数字章节号
        Pattern chinesePattern = Pattern.compile("第([零一二三四五六七八九十百千万0-9]+)[章节回卷集部篇]");
        Matcher matcher = chinesePattern.matcher(title);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 匹配英文章节号
        Pattern englishPattern = Pattern.compile("[Cc]hapter\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        matcher = englishPattern.matcher(title);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 匹配数字章节号
        Pattern numberPattern = Pattern.compile("^(\\d{1,5})[、.．]");
        matcher = numberPattern.matcher(title);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }


    /**
     * 解析EPUB文件内容
     * EPUB本质上是一个ZIP文件，包含HTML/XHTML内容
     */
    private ParsedNovel parseEpubContent(InputStream inputStream, String fileName) throws IOException {
        ParsedNovel novel = new ParsedNovel();
        
        // 从文件名提取标题
        String title = fileName;
        if (title.toLowerCase().endsWith(".epub")) {
            title = title.substring(0, title.length() - 5);
        }
        novel.setTitle(title);
        novel.setAuthor("未知作者");
        
        List<ParsedNovel.ParsedChapter> chapters = new ArrayList<>();
        
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            int chapterIndex = 0;
            
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName().toLowerCase();
                
                // 解析HTML/XHTML内容文件
                if ((entryName.endsWith(".html") || entryName.endsWith(".xhtml") || entryName.endsWith(".htm"))
                        && !entryName.contains("toc") && !entryName.contains("nav")) {
                    
                    String htmlContent = readZipEntryContent(zipInputStream);
                    String textContent = extractTextFromHtml(htmlContent);
                    
                    if (textContent != null && !textContent.trim().isEmpty() && textContent.length() > 50) {
                        String chapterTitle = extractTitleFromHtml(htmlContent);
                        if (chapterTitle == null || chapterTitle.isEmpty()) {
                            chapterTitle = "第" + (chapterIndex + 1) + "章";
                        }
                        
                        ParsedNovel.ParsedChapter chapter = new ParsedNovel.ParsedChapter();
                        chapter.setTitle(chapterTitle);
                        chapter.setContent(textContent.trim());
                        chapter.setIndex(chapterIndex);
                        chapters.add(chapter);
                        chapterIndex++;
                    }
                }
                // 尝试从OPF文件提取元数据
                else if (entryName.endsWith(".opf")) {
                    String opfContent = readZipEntryContent(zipInputStream);
                    extractMetadataFromOpf(opfContent, novel);
                }
                
                zipInputStream.closeEntry();
            }
        }
        
        // 如果没有解析到章节，创建一个空章节
        if (chapters.isEmpty()) {
            ParsedNovel.ParsedChapter chapter = new ParsedNovel.ParsedChapter();
            chapter.setTitle("正文");
            chapter.setContent("无法解析EPUB内容");
            chapter.setIndex(0);
            chapters.add(chapter);
        }
        
        novel.setChapters(chapters);
        return novel;
    }

    /**
     * 读取ZIP条目内容
     */
    private String readZipEntryContent(ZipInputStream zipInputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        byte[] buffer = new byte[1024];
        int len;
        
        while ((len = zipInputStream.read(buffer)) > 0) {
            content.append(new String(buffer, 0, len, "UTF-8"));
        }
        
        return content.toString();
    }

    /**
     * 从HTML中提取纯文本内容
     */
    private String extractTextFromHtml(String html) {
        if (html == null) return "";
        
        // 移除script和style标签及其内容
        html = html.replaceAll("(?is)<script.*?</script>", "");
        html = html.replaceAll("(?is)<style.*?</style>", "");
        
        // 将段落和换行标签转换为换行符
        html = html.replaceAll("(?i)</p>", "\n\n");
        html = html.replaceAll("(?i)<br\\s*/?>", "\n");
        html = html.replaceAll("(?i)</div>", "\n");
        
        // 移除所有HTML标签
        html = html.replaceAll("<[^>]+>", "");
        
        // 解码HTML实体
        html = html.replace("&nbsp;", " ");
        html = html.replace("&lt;", "<");
        html = html.replace("&gt;", ">");
        html = html.replace("&amp;", "&");
        html = html.replace("&quot;", "\"");
        html = html.replace("&#39;", "'");
        
        // 清理多余空白
        html = html.replaceAll("\\s*\\n\\s*\\n\\s*", "\n\n");
        
        return html.trim();
    }

    /**
     * 从HTML中提取标题
     */
    private String extractTitleFromHtml(String html) {
        if (html == null) return null;
        
        // 尝试从title标签提取
        Pattern titlePattern = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = titlePattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // 尝试从h1标签提取
        Pattern h1Pattern = Pattern.compile("<h1[^>]*>([^<]+)</h1>", Pattern.CASE_INSENSITIVE);
        matcher = h1Pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return null;
    }

    /**
     * 从OPF文件提取元数据
     */
    private void extractMetadataFromOpf(String opfContent, ParsedNovel novel) {
        if (opfContent == null) return;
        
        // 提取标题
        Pattern titlePattern = Pattern.compile("<dc:title[^>]*>([^<]+)</dc:title>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = titlePattern.matcher(opfContent);
        if (matcher.find()) {
            novel.setTitle(matcher.group(1).trim());
        }
        
        // 提取作者
        Pattern authorPattern = Pattern.compile("<dc:creator[^>]*>([^<]+)</dc:creator>", Pattern.CASE_INSENSITIVE);
        matcher = authorPattern.matcher(opfContent);
        if (matcher.find()) {
            novel.setAuthor(matcher.group(1).trim());
        }
        
        // 提取简介
        Pattern descPattern = Pattern.compile("<dc:description[^>]*>([^<]+)</dc:description>", Pattern.CASE_INSENSITIVE);
        matcher = descPattern.matcher(opfContent);
        if (matcher.find()) {
            novel.setDescription(matcher.group(1).trim());
        }
    }

    /**
     * 从URI获取文件名
     */
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        
        // 对于content:// URI，使用ContentResolver查询真实文件名
        if ("content".equals(uri.getScheme())) {
            try {
                android.database.Cursor cursor = context.getContentResolver().query(
                        uri, null, null, null, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            int nameIndex = cursor.getColumnIndex(
                                    android.provider.OpenableColumns.DISPLAY_NAME);
                            if (nameIndex >= 0) {
                                fileName = cursor.getString(nameIndex);
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "无法从ContentResolver获取文件名", e);
            }
        }
        
        // 如果上面的方法失败，尝试从路径中提取
        if (fileName == null || fileName.isEmpty()) {
            String path = uri.getPath();
            if (path != null) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                    fileName = path.substring(lastSlash + 1);
                }
            }
        }
        
        return fileName != null && !fileName.isEmpty() ? fileName : "未知文件";
    }
}
