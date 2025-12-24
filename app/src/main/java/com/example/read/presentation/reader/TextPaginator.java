package com.example.read.presentation.reader;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分页器 - 将长文本分割成多个页面
 * 
 * 根据可用空间、字体大小、行间距计算每页可显示的文本
 */
public class TextPaginator {

    /**
     * 分页结果
     */
    public static class PageInfo {
        private final String content;      // 页面内容
        private final int startIndex;      // 在原文中的起始位置
        private final int endIndex;        // 在原文中的结束位置
        private final int pageNumber;      // 页码（从1开始）
        private final boolean isFirstPage; // 是否是章节第一页

        public PageInfo(String content, int startIndex, int endIndex, int pageNumber, boolean isFirstPage) {
            this.content = content;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.pageNumber = pageNumber;
            this.isFirstPage = isFirstPage;
        }

        public String getContent() { return content; }
        public int getStartIndex() { return startIndex; }
        public int getEndIndex() { return endIndex; }
        public int getPageNumber() { return pageNumber; }
        public boolean isFirstPage() { return isFirstPage; }
    }

    /**
     * 将文本分页
     * 
     * @param text 要分页的文本
     * @param textPaint 文本画笔
     * @param width 可用宽度
     * @param height 可用高度
     * @param lineSpacing 行间距倍数
     * @param titleHeight 标题占用高度（第一页需要减去）
     * @return 分页结果列表
     */
    public static List<PageInfo> paginate(String text, TextPaint textPaint, 
            int width, int height, float lineSpacing, int titleHeight) {
        
        List<PageInfo> pages = new ArrayList<>();
        
        if (text == null || text.isEmpty() || width <= 0 || height <= 0) {
            return pages;
        }
        
        // 先为文本添加首行缩进
        String indentedText = addFirstLineIndent(text);

        int textLength = indentedText.length();
        int startIndex = 0;
        int pageNumber = 1;
        boolean isFirstPage = true;

        while (startIndex < textLength) {
            // 第一页需要减去标题高度
            int availableHeight = isFirstPage ? (height - titleHeight) : height;
            
            // 计算当前页可以显示多少文本
            int endIndex = calculatePageEnd(indentedText, startIndex, textPaint, width, availableHeight, lineSpacing);
            
            // 提取页面内容
            String pageContent = indentedText.substring(startIndex, endIndex);
            
            // 创建页面信息
            pages.add(new PageInfo(pageContent, startIndex, endIndex, pageNumber, isFirstPage));
            
            // 移动到下一页
            startIndex = endIndex;
            pageNumber++;
            isFirstPage = false;
        }

        return pages;
    }
    
    /**
     * 为文本添加首行缩进
     * 每个段落的首行缩进两个中文字符宽度
     */
    private static String addFirstLineIndent(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 使用全角空格（\u3000）来实现精确的两字符缩进
        String indent = "\u3000\u3000"; // 两个全角空格
        
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\n", -1);
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // 如果行不为空且不是以空格开头，添加缩进
            if (!line.isEmpty() && !line.startsWith(" ") && !line.startsWith("\u3000")) {
                result.append(indent);
            }
            result.append(line);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }
        
        return result.toString();
    }

    /**
     * 计算当前页的结束位置
     * 使用二分查找找到能够填满页面的最大文本量
     */
    private static int calculatePageEnd(String text, int startIndex, TextPaint textPaint,
            int width, int height, float lineSpacing) {
        
        int textLength = text.length();
        
        // 如果剩余文本很少，直接返回
        if (startIndex >= textLength) {
            return textLength;
        }
        
        // 先检查剩余所有文本是否能放下
        String remainingText = text.substring(startIndex);
        StaticLayout fullLayout = createLayout(remainingText, textPaint, width, lineSpacing);
        if (fullLayout.getHeight() <= height) {
            return textLength;
        }
        
        // 计算单行高度（用于更精确的估算）
        StaticLayout singleLineLayout = createLayout("测", textPaint, width, lineSpacing);
        int lineHeight = singleLineLayout.getHeight();
        
        // 估算每页大约能放多少字符
        int estimatedCharsPerPage = (int) ((height / (float) lineHeight) * (width / textPaint.getTextSize()) * 1.5f);
        
        // 二分查找最佳结束位置
        int low = startIndex + 1;  // 至少包含一个字符
        int high = Math.min(textLength, startIndex + estimatedCharsPerPage * 2);  // 限制搜索范围
        int result = startIndex + 1;  // 至少显示一个字符

        while (low <= high) {
            int mid = (low + high) / 2;
            
            String testText = text.substring(startIndex, mid);
            StaticLayout layout = createLayout(testText, textPaint, width, lineSpacing);
            
            // 获取实际内容高度（不包括最后一行的额外行间距）
            int layoutHeight = layout.getHeight();
            
            if (layoutHeight <= height) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return result;
    }

    /**
     * 查找更好的断点（段落或句子边界）- 保留但不再使用
     * 如果需要在特定位置断开，可以调用此方法
     */
    @SuppressWarnings("unused")
    private static int findBetterBreakPoint(String text, int startIndex, int endIndex) {
        // 向前查找段落边界
        int searchStart = Math.max(startIndex, endIndex - 200);
        
        // 优先在段落边界断开
        int lastNewline = text.lastIndexOf('\n', endIndex - 1);
        if (lastNewline > searchStart) {
            return lastNewline + 1;
        }

        // 其次在句号处断开
        for (int i = endIndex - 1; i >= searchStart; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?') {
                return i + 1;
            }
        }

        // 最后在逗号或空格处断开
        for (int i = endIndex - 1; i >= searchStart; i--) {
            char c = text.charAt(i);
            if (c == '，' || c == ',' || c == ' ' || c == '、') {
                return i + 1;
            }
        }

        return endIndex;
    }

    /**
     * 创建StaticLayout
     */
    private static StaticLayout createLayout(String text, TextPaint paint, int width, float lineSpacing) {
        return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setLineSpacing(0, lineSpacing)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build();
    }

    /**
     * 计算标题高度
     * 注意：标题下方间距需要与 PageContentView.onDraw() 中的值一致
     * 
     * @param title 标题文本
     * @param titlePaint 标题画笔
     * @param width 可用宽度
     * @param lineSpacing 行间距
     * @param scaledDensity 用于sp转px的缩放密度
     * @return 标题占用的总高度
     */
    public static int calculateTitleHeight(String title, TextPaint titlePaint, int width, 
            float lineSpacing, float scaledDensity) {
        if (title == null || title.isEmpty()) {
            return 0;
        }
        
        StaticLayout layout = createLayout(title, titlePaint, width, lineSpacing);
        // 标题高度 + 下方间距（24sp，与PageContentView一致）
        int titleSpacing = (int) (24 * scaledDensity);
        return layout.getHeight() + titleSpacing;
    }
    
    /**
     * 计算标题高度（兼容旧版本）
     * @deprecated 使用 {@link #calculateTitleHeight(String, TextPaint, int, float, float)} 代替
     */
    @Deprecated
    public static int calculateTitleHeight(String title, TextPaint titlePaint, int width, float lineSpacing) {
        if (title == null || title.isEmpty()) {
            return 0;
        }
        
        StaticLayout layout = createLayout(title, titlePaint, width, lineSpacing);
        // 使用旧的计算方式作为后备
        return layout.getHeight() + (int)(titlePaint.getTextSize() * 1.5f);
    }
}
