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
        private final String content;           // 页面内容（带缩进）
        private final int startIndex;           // 在缩进后文本中的起始位置
        private final int endIndex;             // 在缩进后文本中的结束位置
        private final int originalStartIndex;   // 在原始文本中的起始位置（用于TTS高亮）
        private final int originalEndIndex;     // 在原始文本中的结束位置（用于TTS高亮）
        private final int pageNumber;           // 页码（从1开始）
        private final boolean isFirstPage;      // 是否是章节第一页

        public PageInfo(String content, int startIndex, int endIndex, int pageNumber, boolean isFirstPage) {
            this(content, startIndex, endIndex, startIndex, endIndex, pageNumber, isFirstPage);
        }
        
        public PageInfo(String content, int startIndex, int endIndex, 
                       int originalStartIndex, int originalEndIndex, 
                       int pageNumber, boolean isFirstPage) {
            this.content = content;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.originalStartIndex = originalStartIndex;
            this.originalEndIndex = originalEndIndex;
            this.pageNumber = pageNumber;
            this.isFirstPage = isFirstPage;
        }

        public String getContent() { return content; }
        public int getStartIndex() { return startIndex; }
        public int getEndIndex() { return endIndex; }
        /** 获取在原始文本中的起始位置（用于TTS高亮匹配） */
        public int getOriginalStartIndex() { return originalStartIndex; }
        /** 获取在原始文本中的结束位置（用于TTS高亮匹配） */
        public int getOriginalEndIndex() { return originalEndIndex; }
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
        
        // 先为文本添加首行缩进，同时构建位置映射
        IndentResult indentResult = addFirstLineIndentWithMapping(text);
        String indentedText = indentResult.indentedText;
        int[] indentedToOriginal = indentResult.indentedToOriginal;

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
            
            // 计算原始文本中的位置
            int originalStartIndex = indentedToOriginal[startIndex];
            int originalEndIndex = endIndex < indentedToOriginal.length ? 
                    indentedToOriginal[endIndex] : text.length();
            
            // 创建页面信息（包含原始位置）
            pages.add(new PageInfo(pageContent, startIndex, endIndex, 
                    originalStartIndex, originalEndIndex, pageNumber, isFirstPage));
            
            // 移动到下一页
            startIndex = endIndex;
            pageNumber++;
            isFirstPage = false;
        }

        return pages;
    }
    
    /**
     * 缩进结果，包含缩进后的文本和位置映射
     */
    private static class IndentResult {
        String indentedText;
        int[] indentedToOriginal; // 缩进后位置 -> 原始位置的映射
        
        IndentResult(String indentedText, int[] indentedToOriginal) {
            this.indentedText = indentedText;
            this.indentedToOriginal = indentedToOriginal;
        }
    }
    
    /**
     * 为文本添加首行缩进，同时构建位置映射
     * 每个段落的首行缩进两个中文字符宽度
     * 
     * 优化版本：使用 int[] 数组预分配，避免 ArrayList 的装箱开销
     * 
     * @return IndentResult 包含缩进后文本和位置映射
     */
    private static IndentResult addFirstLineIndentWithMapping(String text) {
        if (text == null || text.isEmpty()) {
            return new IndentResult(text, new int[0]);
        }
        
        // 使用全角空格（\u3000）来实现精确的两字符缩进
        String indent = "\u3000\u3000"; // 两个全角空格
        int indentLength = indent.length();
        
        // 预估结果大小：原文长度 + 每行可能的缩进（假设平均每50字符一行）
        int estimatedLines = text.length() / 50 + 1;
        int estimatedSize = text.length() + estimatedLines * indentLength;
        
        StringBuilder result = new StringBuilder(estimatedSize);
        int[] mapping = new int[estimatedSize + 100]; // 预分配映射数组
        int mappingIndex = 0;
        
        int textLength = text.length();
        int originalIndex = 0;
        int lineStart = 0;
        
        // 遍历文本，找到每行的开始和结束
        for (int i = 0; i <= textLength; i++) {
            boolean isEnd = (i == textLength);
            boolean isNewline = !isEnd && text.charAt(i) == '\n';
            
            if (isNewline || isEnd) {
                // 处理当前行 [lineStart, i)
                String line = text.substring(lineStart, i);
                
                // 如果行不为空且不是以空格开头，添加缩进
                if (!line.isEmpty() && !line.startsWith(" ") && !line.startsWith("\u3000")) {
                    // 确保映射数组足够大
                    if (mappingIndex + indentLength + line.length() + 1 >= mapping.length) {
                        int[] newMapping = new int[mapping.length * 2];
                        System.arraycopy(mapping, 0, newMapping, 0, mappingIndex);
                        mapping = newMapping;
                    }
                    
                    result.append(indent);
                    // 缩进字符映射到当前原始位置（段落开始）
                    for (int j = 0; j < indentLength; j++) {
                        mapping[mappingIndex++] = originalIndex;
                    }
                }
                
                // 确保映射数组足够大
                if (mappingIndex + line.length() + 1 >= mapping.length) {
                    int[] newMapping = new int[mapping.length * 2];
                    System.arraycopy(mapping, 0, newMapping, 0, mappingIndex);
                    mapping = newMapping;
                }
                
                // 添加行内容
                result.append(line);
                for (int j = 0; j < line.length(); j++) {
                    mapping[mappingIndex++] = originalIndex + j;
                }
                originalIndex += line.length();
                
                // 添加换行符（如果不是最后）
                if (isNewline) {
                    result.append('\n');
                    mapping[mappingIndex++] = originalIndex;
                    originalIndex++; // 跳过原始文本中的换行符
                }
                
                lineStart = i + 1;
            }
        }
        
        // 裁剪映射数组到实际大小
        int[] finalMapping = new int[mappingIndex];
        System.arraycopy(mapping, 0, finalMapping, 0, mappingIndex);
        
        return new IndentResult(result.toString(), finalMapping);
    }
    
    /**
     * 为文本添加首行缩进（保留旧方法用于兼容）
     * 每个段落的首行缩进两个中文字符宽度
     */
    private static String addFirstLineIndent(String text) {
        return addFirstLineIndentWithMapping(text).indentedText;
    }

    /**
     * 计算当前页的结束位置
     * 优化版本：使用估算 + 线性调整，减少 StaticLayout 创建次数
     */
    private static int calculatePageEnd(String text, int startIndex, TextPaint textPaint,
            int width, int height, float lineSpacing) {
        
        int textLength = text.length();
        
        // 如果剩余文本很少，直接返回
        if (startIndex >= textLength) {
            return textLength;
        }
        
        // 计算单行高度和每行大约能放多少字符
        float charWidth = textPaint.measureText("测");
        int charsPerLine = Math.max(1, (int) (width / charWidth));
        
        // 使用 StaticLayout 计算单行实际高度
        StaticLayout singleLineLayout = createLayout("测", textPaint, width, lineSpacing);
        int lineHeight = singleLineLayout.getHeight();
        
        // 估算每页能放多少行和字符
        int linesPerPage = Math.max(1, height / lineHeight);
        int estimatedCharsPerPage = charsPerLine * linesPerPage;
        
        // 剩余文本长度
        int remainingLength = textLength - startIndex;
        
        // 如果剩余文本预估能放下，直接验证
        if (remainingLength <= estimatedCharsPerPage) {
            String remainingText = text.substring(startIndex);
            StaticLayout fullLayout = createLayout(remainingText, textPaint, width, lineSpacing);
            if (fullLayout.getHeight() <= height) {
                return textLength;
            }
        }
        
        // 使用估算值作为起点，然后线性调整
        int estimatedEnd = Math.min(textLength, startIndex + estimatedCharsPerPage);
        
        // 先测试估算位置
        String testText = text.substring(startIndex, estimatedEnd);
        StaticLayout layout = createLayout(testText, textPaint, width, lineSpacing);
        int layoutHeight = layout.getHeight();
        
        if (layoutHeight <= height) {
            // 估算偏小，向后扩展（每次增加 10% 的字符）
            int step = Math.max(10, estimatedCharsPerPage / 10);
            int lastGoodEnd = estimatedEnd;
            
            while (estimatedEnd < textLength) {
                estimatedEnd = Math.min(textLength, estimatedEnd + step);
                testText = text.substring(startIndex, estimatedEnd);
                layout = createLayout(testText, textPaint, width, lineSpacing);
                
                if (layout.getHeight() > height) {
                    // 超出了，在 lastGoodEnd 和 estimatedEnd 之间二分查找
                    return binarySearchEnd(text, startIndex, lastGoodEnd, estimatedEnd, textPaint, width, height, lineSpacing);
                }
                lastGoodEnd = estimatedEnd;
            }
            return textLength;
        } else {
            // 估算偏大，向前收缩（每次减少 10% 的字符）
            int step = Math.max(10, estimatedCharsPerPage / 10);
            int lastBadEnd = estimatedEnd;
            
            while (estimatedEnd > startIndex + 1) {
                estimatedEnd = Math.max(startIndex + 1, estimatedEnd - step);
                testText = text.substring(startIndex, estimatedEnd);
                layout = createLayout(testText, textPaint, width, lineSpacing);
                
                if (layout.getHeight() <= height) {
                    // 找到了合适的位置，在 estimatedEnd 和 lastBadEnd 之间二分查找
                    return binarySearchEnd(text, startIndex, estimatedEnd, lastBadEnd, textPaint, width, height, lineSpacing);
                }
                lastBadEnd = estimatedEnd;
            }
            return startIndex + 1; // 至少返回一个字符
        }
    }
    
    /**
     * 在已知范围内二分查找最佳结束位置
     * @param goodEnd 已知能放下的位置
     * @param badEnd 已知放不下的位置
     */
    private static int binarySearchEnd(String text, int startIndex, int goodEnd, int badEnd,
            TextPaint textPaint, int width, int height, float lineSpacing) {
        
        int low = goodEnd;
        int high = badEnd;
        int result = goodEnd;
        
        // 限制二分查找次数，避免过多计算
        int maxIterations = 8;
        int iterations = 0;
        
        while (low < high - 1 && iterations < maxIterations) {
            int mid = (low + high) / 2;
            String testText = text.substring(startIndex, mid);
            StaticLayout layout = createLayout(testText, textPaint, width, lineSpacing);
            
            if (layout.getHeight() <= height) {
                result = mid;
                low = mid;
            } else {
                high = mid;
            }
            iterations++;
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
