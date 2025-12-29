package com.example.read.data.service;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.read.domain.model.ParsedNovel;
import com.example.read.domain.service.FileParserService;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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
    
    // 章节标题匹配模式 - 增强版本，支持多种常见格式
    // 格式1: 第X章/节/回/卷/集/部/篇 (支持中文数字和阿拉伯数字，支持各种分隔符)
    // 格式2: Chapter X / CHAPTER X
    // 格式3: 卷一/卷1/Part 1 等
    // 格式4: 【第X章】、「第X章」等带括号格式
    // 格式5: 数字开头 1. / 1、/ 001 等（仅匹配短标题，避免误判正文中的编号列表）
    // 格式6: 特殊章节名：楔子、序章、序言、引子、尾声、后记、番外等
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
        "^\\s*(" +
        // 第X章/节/回/卷/集/部/篇 (支持中文数字和阿拉伯数字，支持全角半角冒号、顿号、空格)
        "第[零一二三四五六七八九十百千万两〇\\d]+[章节回卷集部篇]([：:、\\s].*)?" + "|" +
        // 【第X章】「第X章」等带括号格式
        "[【「\\[（(]第[零一二三四五六七八九十百千万两〇\\d]+[章节回卷集部篇][】」\\]）)]([：:、\\s].*)?" + "|" +
        // 卷X / 卷一 等（独立的卷标题）
        "卷[零一二三四五六七八九十百千万两〇\\d]+([：:、\\s].*)?" + "|" +
        // Chapter X / CHAPTER X
        "[Cc][Hh][Aa][Pp][Tt][Ee][Rr]\\s*\\d+.*" + "|" +
        // Part X / PART X
        "[Pp][Aa][Rr][Tt]\\s*\\d+.*" + "|" +
        // Section X
        "[Ss][Ee][Cc][Tt][Ii][Oo][Nn]\\s*\\d+.*" + "|" +
        // 数字开头: 1. / 1、/ 1： / 001 等（限制后面内容长度不超过20字符，避免误判正文）
        "\\d{1,5}[、.．:：]\\s?.{0,20}" + "|" +
        // 特殊章节名（楔子、序章、序言、引子等）
        "楔子([：:、\\s].*)?" + "|" +
        "序章([：:、\\s].*)?" + "|" +
        "序言([：:、\\s].*)?" + "|" +
        "序([：:、\\s].*)?" + "|" +
        "引子([：:、\\s].*)?" + "|" +
        "尾声([：:、\\s].*)?" + "|" +
        "后记([：:、\\s].*)?" + "|" +
        "番外([：:、\\s].*)?" + "|" +
        "终章([：:、\\s].*)?" + "|" +
        "大结局([：:、\\s].*)?" + "|" +
        "完结([：:、\\s].*)?" +
        ")$",
        Pattern.CASE_INSENSITIVE
    );
    
    // 用于检测是否是有效的章节标题（排除正文中的引用）
    private static final int MAX_CHAPTER_TITLE_LENGTH = 60;
    
    // 单个章节最大字符数（超过此值将自动分割）
    private static final int MAX_CHAPTER_SIZE = 500000; // 50万字符，约1MB

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
     * 解析TXT文件内容 - 优化版本，支持自动编码检测
     */
    private ParsedNovel parseTxtContent(InputStream inputStream, String fileName) throws IOException {
        // 先将输入流读取到字节数组，以便多次读取进行编码检测
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        byte[] fileBytes = baos.toByteArray();
        Log.d(TAG, "文件字节数: " + fileBytes.length);
        
        // 自动检测文件编码
        String detectedCharset = detectCharset(fileBytes);
        Log.d(TAG, "检测到的编码: " + detectedCharset);
        
        // 使用检测到的编码读取内容
        StringBuilder fullContent = new StringBuilder();
        BufferedReader tempReader = new BufferedReader(
            new InputStreamReader(new ByteArrayInputStream(fileBytes), detectedCharset), 8192);
        char[] charBuffer = new char[8192];
        int charsRead;
        while ((charsRead = tempReader.read(charBuffer)) != -1) {
            fullContent.append(charBuffer, 0, charsRead);
        }
        tempReader.close();
        
        String content = fullContent.toString();
        Log.d(TAG, "原始内容长度: " + content.length());
        
        // 检查内容是否包含乱码（如果检测到大量乱码字符，尝试其他编码）
        if (containsGarbledText(content)) {
            Log.d(TAG, "检测到可能的乱码，尝试其他编码");
            String alternativeCharset = detectedCharset.equals("UTF-8") ? "GBK" : "UTF-8";
            try {
                tempReader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(fileBytes), alternativeCharset), 8192);
                fullContent.setLength(0);
                while ((charsRead = tempReader.read(charBuffer)) != -1) {
                    fullContent.append(charBuffer, 0, charsRead);
                }
                tempReader.close();
                String alternativeContent = fullContent.toString();
                
                // 如果替代编码产生的乱码更少，使用替代编码
                if (!containsGarbledText(alternativeContent) || 
                    countGarbledChars(alternativeContent) < countGarbledChars(content)) {
                    content = alternativeContent;
                    Log.d(TAG, "使用替代编码: " + alternativeCharset);
                }
            } catch (Exception e) {
                Log.w(TAG, "尝试替代编码失败", e);
            }
        }
        
        // 预处理：将章节标题前添加换行符，确保章节标题在行首
        // 这是关键步骤，因为很多TXT文件没有在章节标题前换行
        content = preprocessChapterTitles(content);
        
        // 将处理后的内容转换为 BufferedReader
        BufferedReader reader = new BufferedReader(new java.io.StringReader(content));
        
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
        
        Log.d(TAG, "预处理后解析完成，章节数: " + chapters.size());
        
        return novel;
    }
    
    /**
     * 预处理章节标题：在章节标题前插入换行符
     * 这样可以确保章节标题在行首，便于后续解析
     */
    private String preprocessChapterTitles(String content) {
        // 章节标题模式列表（按优先级排序）
        // 注意：使用 (?<![\\n\\r]) 确保不在已有换行符后重复添加
        
        // 1. 第X章/节/回/卷/集/部/篇 格式（支持中文数字和阿拉伯数字）
        // 例如：第一章、第1章、第一百二十三章、第一节、第一卷
        content = content.replaceAll(
            "(?<![\\n\\r])(第[零一二三四五六七八九十百千万两〇0-9]+[章节回卷集部篇])",
            "\n$1"
        );
        
        // 2. Chapter X 格式
        content = content.replaceAll(
            "(?i)(?<![\\n\\r])([Cc]hapter\\s*\\d+)",
            "\n$1"
        );
        
        // 3. 卷X 格式（独立的卷标题）
        content = content.replaceAll(
            "(?<![\\n\\r])(卷[零一二三四五六七八九十百千万两〇0-9]+)",
            "\n$1"
        );
        
        // 4. 特殊章节名：序、序章、序言、楔子、引子、尾声、后记、番外、终章、大结局
        content = content.replaceAll(
            "(?<![\\n\\r])((?:序章|序言|楔子|引子|尾声|后记|番外|终章|大结局|完结)[：:、\\s])",
            "\n$1"
        );
        
        // 5. 数字开头格式：1. / 1、/ 1： 等
        // 注意：只匹配1-4位数字，避免误匹配正文中的数字
        content = content.replaceAll(
            "(?<![\\n\\r0-9])(\\d{1,4}[、.．:：]\\s*[^0-9])",
            "\n$1"
        );
        
        // 清理：移除开头可能产生的多余换行
        if (content.startsWith("\n")) {
            content = content.substring(1);
        }
        
        // 清理：将连续多个换行符合并为一个
        content = content.replaceAll("\\n{2,}", "\n");
        
        Log.d(TAG, "预处理完成，处理后内容长度: " + content.length());
        
        return content;
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
        
        // 作者匹配模式 - 支持多种格式
        // 格式1: 作者：xxx / Author: xxx / 著：xxx / by xxx（行首）
        // 格式2: 《书名》作者：xxx（书名后面）
        // 格式3: 书名 作者：xxx
        Pattern authorPattern1 = Pattern.compile(
            "^\\s*(?:作者|Author|著|by|作\\s*者)[：:：\\s]+(.+)$",
            Pattern.CASE_INSENSITIVE
        );
        Pattern authorPattern2 = Pattern.compile(
            "(?:》|erta)\\s*(?:作者|Author|著|by|作\\s*者)[：:：\\s]*(.+?)(?:\\s*$|\\s+文案|\\s+简介|\\s+内容)",
            Pattern.CASE_INSENSITIVE
        );
        Pattern authorPattern3 = Pattern.compile(
            "(?:作者|Author|著|by|作\\s*者)[：:：\\s]+([^\\s《》\\n]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        while ((line = reader.readLine()) != null) {
            lineCount++;
            
            // 在前50行中尝试提取作者信息
            if (!authorFound && lineCount <= 50) {
                String trimmedLine = line.trim();
                String author = null;
                
                // 尝试格式1：行首的作者信息
                Matcher authorMatcher = authorPattern1.matcher(trimmedLine);
                if (authorMatcher.find()) {
                    author = authorMatcher.group(1).trim();
                }
                
                // 尝试格式2：书名后面的作者信息（如《书名》作者：xxx）
                if (author == null) {
                    authorMatcher = authorPattern2.matcher(trimmedLine);
                    if (authorMatcher.find()) {
                        author = authorMatcher.group(1).trim();
                    }
                }
                
                // 尝试格式3：通用格式
                if (author == null) {
                    authorMatcher = authorPattern3.matcher(trimmedLine);
                    if (authorMatcher.find()) {
                        author = authorMatcher.group(1).trim();
                    }
                }
                
                if (author != null && !author.isEmpty() && author.length() < 50) {
                    novel.setAuthor(author);
                    authorFound = true;
                    Log.d(TAG, "从文件中提取到作者: " + author);
                }
            }
            
            // 检查是否是章节标题
            if (isValidChapterTitle(line, lastChapterTitle)) {
                // 保存之前的章节
                if (currentTitle != null && currentContent.length() > 0) {
                    // 检查是否需要分割超大章节
                    List<ParsedNovel.ParsedChapter> splitChapters = splitLargeChapter(
                        currentTitle, currentContent.toString().trim(), chapterIndex);
                    chapters.addAll(splitChapters);
                    chapterIndex += splitChapters.size();
                    currentContent.setLength(0);
                } else if (currentContent.length() > 100) {
                    // 第一章之前的内容作为序言
                    List<ParsedNovel.ParsedChapter> splitChapters = splitLargeChapter(
                        "序言", currentContent.toString().trim(), chapterIndex);
                    chapters.addAll(splitChapters);
                    chapterIndex += splitChapters.size();
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
            List<ParsedNovel.ParsedChapter> splitChapters = splitLargeChapter(
                currentTitle, currentContent.toString().trim(), chapterIndex);
            chapters.addAll(splitChapters);
        } else if (chapters.isEmpty() && currentContent.length() > 0) {
            // 没有找到章节标题，使用智能分章兜底
            Log.d(TAG, "未识别到章节标题，使用智能分章");
            chapters = splitContentBySize(currentContent.toString().trim());
        }
        
        return chapters;
    }
    
    /**
     * 按固定大小分割内容（智能分章兜底方案）
     * 当无法识别章节标题时使用
     */
    private List<ParsedNovel.ParsedChapter> splitContentBySize(String content) {
        List<ParsedNovel.ParsedChapter> chapters = new ArrayList<>();
        
        if (content.length() <= MAX_CHAPTER_SIZE) {
            // 内容不大，作为单章处理
            ParsedNovel.ParsedChapter chapter = new ParsedNovel.ParsedChapter();
            chapter.setTitle("正文");
            chapter.setContent(content);
            chapter.setIndex(0);
            chapters.add(chapter);
            return chapters;
        }
        
        // 按段落分割，每章约5万字
        String[] paragraphs = content.split("\n");
        StringBuilder currentContent = new StringBuilder();
        int chapterNumber = 1;
        
        for (String paragraph : paragraphs) {
            if (currentContent.length() + paragraph.length() > MAX_CHAPTER_SIZE && currentContent.length() > 0) {
                ParsedNovel.ParsedChapter chapter = new ParsedNovel.ParsedChapter();
                chapter.setTitle("第" + chapterNumber + "部分");
                chapter.setContent(currentContent.toString().trim());
                chapter.setIndex(chapterNumber - 1);
                chapters.add(chapter);
                
                currentContent.setLength(0);
                chapterNumber++;
            }
            
            currentContent.append(paragraph).append("\n");
        }
        
        // 保存最后一部分
        if (currentContent.length() > 0) {
            ParsedNovel.ParsedChapter chapter = new ParsedNovel.ParsedChapter();
            chapter.setTitle("第" + chapterNumber + "部分");
            chapter.setContent(currentContent.toString().trim());
            chapter.setIndex(chapterNumber - 1);
            chapters.add(chapter);
        }
        
        Log.d(TAG, "智能分章完成，共分割为 " + chapters.size() + " 个部分");
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
        
        // 对于数字开头的模式，进行额外检查以排除正文中的编号列表
        if (trimmedLine.matches("^\\d{1,5}[、.．:：].*")) {
            // 排除包含明显正文特征的内容
            // 1. 包含句号、问号、感叹号等句末标点（说明是完整句子，不是章节标题）
            if (trimmedLine.matches(".*[。？！…】」）\\)]$")) {
                Log.d(TAG, "跳过疑似正文编号列表: " + trimmedLine);
                return false;
            }
            // 2. 内容过长（超过15个字符的数字开头行很可能是正文）
            String contentAfterNumber = trimmedLine.replaceFirst("^\\d{1,5}[、.．:：]\\s?", "");
            if (contentAfterNumber.length() > 15) {
                Log.d(TAG, "跳过过长的数字开头行: " + trimmedLine);
                return false;
            }
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
        
        // 匹配中文数字章节号（章/节/回/卷/集/部/篇）
        Pattern chinesePattern = Pattern.compile("第([零一二三四五六七八九十百千万两〇0-9]+)[章节回卷集部篇]");
        Matcher matcher = chinesePattern.matcher(title);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 匹配卷X格式
        Pattern volumePattern = Pattern.compile("卷([零一二三四五六七八九十百千万两〇0-9]+)");
        matcher = volumePattern.matcher(title);
        if (matcher.find()) {
            return "卷" + matcher.group(1);
        }
        
        // 匹配英文章节号
        Pattern englishPattern = Pattern.compile("[Cc]hapter\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        matcher = englishPattern.matcher(title);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 匹配Part X格式
        Pattern partPattern = Pattern.compile("[Pp]art\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        matcher = partPattern.matcher(title);
        if (matcher.find()) {
            return "Part" + matcher.group(1);
        }
        
        // 匹配数字章节号
        Pattern numberPattern = Pattern.compile("^\\s*(\\d{1,5})[、.．:：]");
        matcher = numberPattern.matcher(title);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 特殊章节名返回特殊标识
        if (title.contains("楔子") || title.contains("序章") || title.contains("序言") || 
            title.contains("引子") || title.contains("尾声") || title.contains("后记") ||
            title.contains("番外") || title.contains("终章") || title.contains("大结局")) {
            return "special_" + title.hashCode();
        }
        
        return null;
    }
    
    /**
     * 将超大章节分割成多个子章节
     * @param title 原章节标题
     * @param content 章节内容
     * @param startIndex 起始章节索引
     * @return 分割后的章节列表
     */
    private List<ParsedNovel.ParsedChapter> splitLargeChapter(String title, String content, int startIndex) {
        List<ParsedNovel.ParsedChapter> chapters = new ArrayList<>();
        
        if (content.length() <= MAX_CHAPTER_SIZE) {
            // 不需要分割
            ParsedNovel.ParsedChapter chapter = new ParsedNovel.ParsedChapter();
            chapter.setTitle(title);
            chapter.setContent(content);
            chapter.setIndex(startIndex);
            chapters.add(chapter);
            return chapters;
        }
        
        Log.d(TAG, "章节内容过大，开始分割: " + title + ", 长度: " + content.length());
        
        // 按段落分割
        String[] paragraphs = content.split("\n");
        StringBuilder currentContent = new StringBuilder();
        int partNumber = 1;
        
        for (String paragraph : paragraphs) {
            // 如果添加这个段落会超过限制，先保存当前内容
            if (currentContent.length() + paragraph.length() > MAX_CHAPTER_SIZE && currentContent.length() > 0) {
                ParsedNovel.ParsedChapter chapter = new ParsedNovel.ParsedChapter();
                chapter.setTitle(title + " (第" + partNumber + "部分)");
                chapter.setContent(currentContent.toString().trim());
                chapter.setIndex(startIndex + partNumber - 1);
                chapters.add(chapter);
                
                currentContent.setLength(0);
                partNumber++;
            }
            
            currentContent.append(paragraph).append("\n");
        }
        
        // 保存最后一部分
        if (currentContent.length() > 0) {
            ParsedNovel.ParsedChapter chapter = new ParsedNovel.ParsedChapter();
            if (partNumber > 1) {
                chapter.setTitle(title + " (第" + partNumber + "部分)");
            } else {
                chapter.setTitle(title);
            }
            chapter.setContent(currentContent.toString().trim());
            chapter.setIndex(startIndex + partNumber - 1);
            chapters.add(chapter);
        }
        
        Log.d(TAG, "章节分割完成，共分割为 " + chapters.size() + " 个部分");
        return chapters;
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
     * 修复：先读取所有字节，再统一转换为字符串，避免UTF-8多字节字符被截断
     */
    private String readZipEntryContent(ZipInputStream zipInputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        
        while ((len = zipInputStream.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        
        byte[] bytes = baos.toByteArray();
        
        // 尝试从XML声明中检测编码
        String encoding = detectXmlEncoding(bytes);
        if (encoding == null) {
            encoding = "UTF-8"; // EPUB标准默认UTF-8
        }
        
        try {
            return new String(bytes, encoding);
        } catch (Exception e) {
            Log.w(TAG, "使用编码 " + encoding + " 解析失败，尝试UTF-8", e);
            return new String(bytes, "UTF-8");
        }
    }
    
    /**
     * 从XML/HTML声明中检测编码
     */
    private String detectXmlEncoding(byte[] bytes) {
        // 取前1000字节检查XML声明
        int checkLength = Math.min(bytes.length, 1000);
        String header;
        try {
            header = new String(bytes, 0, checkLength, "ASCII");
        } catch (Exception e) {
            return null;
        }
        
        // 匹配 <?xml ... encoding="xxx" ?>
        Pattern xmlPattern = Pattern.compile("encoding\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = xmlPattern.matcher(header);
        if (matcher.find()) {
            String encoding = matcher.group(1).toUpperCase();
            // 验证编码是否有效
            try {
                Charset.forName(encoding);
                return encoding;
            } catch (Exception e) {
                Log.w(TAG, "无效的编码声明: " + encoding);
            }
        }
        
        // 匹配 <meta charset="xxx">
        Pattern metaPattern = Pattern.compile("<meta[^>]+charset\\s*=\\s*[\"']?([^\"'\\s>]+)", Pattern.CASE_INSENSITIVE);
        matcher = metaPattern.matcher(header);
        if (matcher.find()) {
            String encoding = matcher.group(1).toUpperCase();
            try {
                Charset.forName(encoding);
                return encoding;
            } catch (Exception e) {
                Log.w(TAG, "无效的charset声明: " + encoding);
            }
        }
        
        return null;
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
        
        // 解码HTML实体 - 增强版本
        html = decodeHtmlEntities(html);
        
        // 清理多余空白
        html = html.replaceAll("\\s*\\n\\s*\\n\\s*", "\n\n");
        
        return html.trim();
    }
    
    /**
     * 解码HTML实体 - 支持命名实体和数字实体
     */
    private String decodeHtmlEntities(String html) {
        if (html == null) return "";
        
        // 1. 解码十进制数字实体 &#1234;
        Pattern decimalPattern = Pattern.compile("&#(\\d+);");
        Matcher matcher = decimalPattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            try {
                int codePoint = Integer.parseInt(matcher.group(1));
                if (Character.isValidCodePoint(codePoint)) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
                }
            } catch (Exception e) {
                // 保持原样
            }
        }
        matcher.appendTail(sb);
        html = sb.toString();
        
        // 2. 解码十六进制数字实体 &#x1234; 或 &#X1234;
        Pattern hexPattern = Pattern.compile("&#[xX]([0-9a-fA-F]+);");
        matcher = hexPattern.matcher(html);
        sb = new StringBuffer();
        while (matcher.find()) {
            try {
                int codePoint = Integer.parseInt(matcher.group(1), 16);
                if (Character.isValidCodePoint(codePoint)) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
                }
            } catch (Exception e) {
                // 保持原样
            }
        }
        matcher.appendTail(sb);
        html = sb.toString();
        
        // 3. 解码常见命名实体
        html = html.replace("&nbsp;", " ");
        html = html.replace("&ensp;", " ");
        html = html.replace("&emsp;", "  ");
        html = html.replace("&thinsp;", " ");
        html = html.replace("&lt;", "<");
        html = html.replace("&gt;", ">");
        html = html.replace("&amp;", "&");
        html = html.replace("&quot;", "\"");
        html = html.replace("&apos;", "'");
        html = html.replace("&#39;", "'");
        html = html.replace("&ldquo;", "\u201C");
        html = html.replace("&rdquo;", "\u201D");
        html = html.replace("&lsquo;", "\u2018");
        html = html.replace("&rsquo;", "\u2019");
        html = html.replace("&mdash;", "\u2014");
        html = html.replace("&ndash;", "\u2013");
        html = html.replace("&hellip;", "\u2026");
        html = html.replace("&copy;", "\u00A9");
        html = html.replace("&reg;", "\u00AE");
        html = html.replace("&trade;", "\u2122");
        html = html.replace("&bull;", "\u2022");
        html = html.replace("&middot;", "\u00B7");
        html = html.replace("&laquo;", "\u00AB");
        html = html.replace("&raquo;", "\u00BB");
        html = html.replace("&deg;", "\u00B0");
        html = html.replace("&plusmn;", "\u00B1");
        html = html.replace("&times;", "\u00D7");
        html = html.replace("&divide;", "\u00F7");
        html = html.replace("&frac12;", "\u00BD");
        html = html.replace("&frac14;", "\u00BC");
        html = html.replace("&frac34;", "\u00BE");
        html = html.replace("&cent;", "\u00A2");
        html = html.replace("&pound;", "\u00A3");
        html = html.replace("&yen;", "\u00A5");
        html = html.replace("&euro;", "\u20AC");
        
        return html;
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
    
    /**
     * 自动检测文件编码
     * 支持 UTF-8, GBK, GB2312, GB18030 等常见中文编码
     */
    private String detectCharset(byte[] bytes) {
        // 检查 UTF-8 BOM
        if (bytes.length >= 3 && 
            (bytes[0] & 0xFF) == 0xEF && 
            (bytes[1] & 0xFF) == 0xBB && 
            (bytes[2] & 0xFF) == 0xBF) {
            Log.d(TAG, "检测到 UTF-8 BOM");
            return "UTF-8";
        }
        
        // 检查 UTF-16 BOM
        if (bytes.length >= 2) {
            if ((bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
                Log.d(TAG, "检测到 UTF-16 BE BOM");
                return "UTF-16BE";
            }
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
                Log.d(TAG, "检测到 UTF-16 LE BOM");
                return "UTF-16LE";
            }
        }
        
        // 没有 BOM，通过内容分析判断编码
        // 取前 8KB 进行分析
        int sampleSize = Math.min(bytes.length, 8192);
        
        // 统计 UTF-8 和 GBK 的有效性得分
        int utf8Score = calculateUtf8Score(bytes, sampleSize);
        int gbkScore = calculateGbkScore(bytes, sampleSize);
        
        Log.d(TAG, "编码检测得分 - UTF-8: " + utf8Score + ", GBK: " + gbkScore);
        
        // 如果 UTF-8 得分明显更高，使用 UTF-8
        if (utf8Score > gbkScore + 10) {
            return "UTF-8";
        }
        
        // 如果 GBK 得分更高或相近，使用 GBK（中文小说更常用 GBK）
        if (gbkScore > 0) {
            return "GBK";
        }
        
        // 默认使用 UTF-8
        return "UTF-8";
    }
    
    /**
     * 计算 UTF-8 编码的有效性得分
     */
    private int calculateUtf8Score(byte[] bytes, int length) {
        int score = 0;
        int i = 0;
        
        while (i < length) {
            int b = bytes[i] & 0xFF;
            
            if (b < 0x80) {
                // ASCII 字符
                score += 1;
                i++;
            } else if (b >= 0xC0 && b < 0xE0) {
                // 2字节 UTF-8 序列
                if (i + 1 < length && (bytes[i + 1] & 0xC0) == 0x80) {
                    score += 2;
                    i += 2;
                } else {
                    score -= 5; // 无效序列
                    i++;
                }
            } else if (b >= 0xE0 && b < 0xF0) {
                // 3字节 UTF-8 序列（中文字符常见）
                if (i + 2 < length && 
                    (bytes[i + 1] & 0xC0) == 0x80 && 
                    (bytes[i + 2] & 0xC0) == 0x80) {
                    score += 5; // 中文字符加分
                    i += 3;
                } else {
                    score -= 5; // 无效序列
                    i++;
                }
            } else if (b >= 0xF0 && b < 0xF8) {
                // 4字节 UTF-8 序列
                if (i + 3 < length && 
                    (bytes[i + 1] & 0xC0) == 0x80 && 
                    (bytes[i + 2] & 0xC0) == 0x80 &&
                    (bytes[i + 3] & 0xC0) == 0x80) {
                    score += 3;
                    i += 4;
                } else {
                    score -= 5;
                    i++;
                }
            } else {
                // 无效的 UTF-8 起始字节
                score -= 5;
                i++;
            }
        }
        
        return score;
    }
    
    /**
     * 计算 GBK 编码的有效性得分
     */
    private int calculateGbkScore(byte[] bytes, int length) {
        int score = 0;
        int i = 0;
        
        while (i < length) {
            int b1 = bytes[i] & 0xFF;
            
            if (b1 < 0x80) {
                // ASCII 字符
                score += 1;
                i++;
            } else if (b1 >= 0x81 && b1 <= 0xFE && i + 1 < length) {
                // GBK 双字节字符
                int b2 = bytes[i + 1] & 0xFF;
                
                // GBK 第二字节范围：0x40-0x7E, 0x80-0xFE
                if ((b2 >= 0x40 && b2 <= 0x7E) || (b2 >= 0x80 && b2 <= 0xFE)) {
                    // 检查是否在常用汉字区
                    if (b1 >= 0xB0 && b1 <= 0xF7) {
                        score += 5; // 常用汉字加分
                    } else {
                        score += 3;
                    }
                    i += 2;
                } else {
                    score -= 3;
                    i++;
                }
            } else {
                score -= 3;
                i++;
            }
        }
        
        return score;
    }
    
    /**
     * 检查文本是否包含乱码
     */
    private boolean containsGarbledText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 取样检查前 2000 个字符
        int sampleSize = Math.min(text.length(), 2000);
        String sample = text.substring(0, sampleSize);
        
        int garbledCount = countGarbledChars(sample);
        
        // 如果乱码字符超过 5%，认为是乱码
        return garbledCount > sampleSize * 0.05;
    }
    
    /**
     * 统计乱码字符数量
     */
    private int countGarbledChars(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // 替换字符（常见乱码标志）
            if (c == '\uFFFD') {
                count++;
            }
            // 私用区字符
            else if (c >= 0xE000 && c <= 0xF8FF) {
                count++;
            }
            // 其他可能的乱码字符（非常见 Unicode 范围）
            else if (c >= 0xFFF0 && c <= 0xFFFF) {
                count++;
            }
        }
        return count;
    }
}
