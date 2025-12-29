package com.example.read.presentation.reader;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.read.domain.model.ReaderFont;

/**
 * 单页内容视图 - 用于左右翻页模式显示单页内容
 * 
 * 支持：
 * - 文本分页显示
 * - 自定义字体大小和行间距
 * - 主题颜色设置
 */
public class PageContentView extends View {

    // 文本画笔
    private TextPaint textPaint;
    
    // 页面内容
    private String pageContent = "";
    
    // 页码信息
    private String pageInfo = "";
    
    // 章节标题
    private String chapterTitle = "";
    
    // 状态信息：左上角章节名
    private String statusChapterName = "";
    
    // 状态信息：右下角时间电量
    private String statusTimeBattery = "";
    
    // 是否显示标题（第一页显示）
    private boolean showTitle = false;
    
    // 文本颜色
    private int textColor = 0xFF333333;
    
    // 背景颜色
    private int backgroundColor = 0xFFFFFFFF;
    
    // 字体大小（sp）
    private float fontSize = 18f;
    
    // 行间距倍数
    private float lineSpacing = 1.5f;
    
    // 当前字体
    private Typeface currentTypeface = Typeface.DEFAULT;
    
    // 内边距（dp值，在init中转换为px）
    private static final int PADDING_HORIZONTAL_DP = 32;
    private static final int PADDING_TOP_DP = 28;
    private static final int PADDING_BOTTOM_DP = 36;
    
    // 实际使用的内边距（px）
    private int paddingHorizontal;
    private int paddingTop;
    private int paddingBottom;
    
    // 标题画笔
    private TextPaint titlePaint;
    
    // 页码画笔
    private Paint pageInfoPaint;
    
    // 状态信息画笔
    private Paint statusPaint;
    
    // TTS高亮相关
    private Paint highlightPaint;           // 高亮背景画笔
    private int highlightStartInPage = -1;  // 页面内高亮起始位置
    private int highlightEndInPage = -1;    // 页面内高亮结束位置
    private int pageStartIndex = 0;         // 当前页面在章节中的起始位置
    private static final int HIGHLIGHT_COLOR = 0x4000BCD4; // 高亮颜色（半透明青色）
    
    // 搜索关键词高亮相关
    private String searchKeyword = "";      // 搜索关键词
    private Paint searchHighlightPaint;     // 搜索高亮画笔
    private static final int SEARCH_HIGHLIGHT_COLOR = 0xFFFFD54F; // 搜索高亮颜色（黄色）

    public PageContentView(Context context) {
        super(context);
        init();
    }

    public PageContentView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PageContentView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化画笔
     */
    private void init() {
        // 将dp转换为px
        paddingHorizontal = dpToPx(PADDING_HORIZONTAL_DP);
        paddingTop = dpToPx(PADDING_TOP_DP);
        paddingBottom = dpToPx(PADDING_BOTTOM_DP);
        
        // 内容文本画笔
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(spToPx(fontSize));
        textPaint.setColor(textColor);
        
        // 标题画笔
        titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(spToPx(fontSize + 4));
        titlePaint.setColor(textColor);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        
        // 页码画笔
        pageInfoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pageInfoPaint.setTextSize(spToPx(12));
        pageInfoPaint.setColor(0xFF999999);
        pageInfoPaint.setTextAlign(Paint.Align.CENTER);
        
        // 状态信息画笔
        statusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        statusPaint.setTextSize(spToPx(13));
        statusPaint.setColor(0xFF999999);
        
        // TTS高亮画笔
        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(HIGHLIGHT_COLOR);
        highlightPaint.setStyle(Paint.Style.FILL);
        
        // 搜索高亮画笔
        searchHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        searchHighlightPaint.setColor(SEARCH_HIGHLIGHT_COLOR);
        searchHighlightPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 绘制背景
        canvas.drawColor(backgroundColor);
        
        int width = getWidth();
        int height = getHeight();
        int contentWidth = width - paddingHorizontal * 2;
        
        float y = paddingTop;
        
        // 绘制章节标题（如果是第一页）
        if (showTitle && chapterTitle != null && !chapterTitle.isEmpty()) {
            StaticLayout titleLayout = createStaticLayout(chapterTitle, titlePaint, contentWidth);
            canvas.save();
            canvas.translate(paddingHorizontal, y);
            titleLayout.draw(canvas);
            canvas.restore();
            y += titleLayout.getHeight() + spToPx(24);
        }
        
        // 绘制页面内容（包含TTS高亮和搜索高亮）
        if (pageContent != null && !pageContent.isEmpty()) {
            StaticLayout contentLayout = createStaticLayout(pageContent, textPaint, contentWidth);
            
            canvas.save();
            canvas.translate(paddingHorizontal, y);
            
            // 绘制搜索关键词高亮（先绘制，在文字下方）
            if (searchKeyword != null && !searchKeyword.isEmpty()) {
                drawSearchHighlight(canvas, contentLayout, pageContent, searchKeyword);
            }
            
            // 绘制TTS高亮背景
            if (highlightStartInPage >= 0 && highlightEndInPage > highlightStartInPage 
                    && highlightStartInPage < pageContent.length()) {
                drawHighlight(canvas, contentLayout, highlightStartInPage, 
                        Math.min(highlightEndInPage, pageContent.length()));
            }
            
            // 绘制文本
            contentLayout.draw(canvas);
            canvas.restore();
        }
        
        // 绘制页码信息
        if (pageInfo != null && !pageInfo.isEmpty()) {
            canvas.drawText(pageInfo, width / 2f, height - spToPx(16), pageInfoPaint);
        }
        
        // 绘制左上角章节名
        if (statusChapterName != null && !statusChapterName.isEmpty()) {
            canvas.drawText(statusChapterName, paddingHorizontal, spToPx(18), statusPaint);
        }
        
        // 绘制右下角时间电量
        if (statusTimeBattery != null && !statusTimeBattery.isEmpty()) {
            float textWidth = statusPaint.measureText(statusTimeBattery);
            canvas.drawText(statusTimeBattery, width - paddingHorizontal - textWidth, height - spToPx(16), statusPaint);
        }
    }
    
    /**
     * 绘制TTS高亮背景
     * 使用整行高亮方式
     */
    private void drawHighlight(Canvas canvas, StaticLayout layout, int start, int end) {
        if (layout == null || start >= end) return;
        
        // 获取内容宽度
        int contentWidth = getWidth() - paddingHorizontal * 2;
        
        // 获取高亮区域的行范围
        int startLine = layout.getLineForOffset(Math.min(start, layout.getText().length() - 1));
        int endLine = layout.getLineForOffset(Math.min(end - 1, layout.getText().length() - 1));
        
        // 绘制每一行的高亮（整行高亮）
        for (int line = startLine; line <= endLine; line++) {
            float top = layout.getLineTop(line);
            float bottom = layout.getLineBottom(line);
            canvas.drawRect(0, top, contentWidth, bottom, highlightPaint);
        }
    }
    
    /**
     * 绘制搜索关键词高亮
     * 只高亮关键词文字区域
     */
    private void drawSearchHighlight(Canvas canvas, StaticLayout layout, String text, String keyword) {
        if (layout == null || text == null || keyword == null || keyword.isEmpty()) return;
        
        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        
        int start = 0;
        while ((start = lowerText.indexOf(lowerKeyword, start)) != -1) {
            int end = start + keyword.length();
            
            // 获取关键词所在的行
            int startLine = layout.getLineForOffset(start);
            int endLine = layout.getLineForOffset(end - 1);
            
            // 绘制每一行中关键词的高亮
            for (int line = startLine; line <= endLine; line++) {
                int lineStart = layout.getLineStart(line);
                int lineEnd = layout.getLineEnd(line);
                
                // 计算当前行中需要高亮的范围
                int highlightStart = Math.max(start, lineStart);
                int highlightEnd = Math.min(end, lineEnd);
                
                if (highlightStart < highlightEnd) {
                    float left = layout.getPrimaryHorizontal(highlightStart);
                    float right;
                    
                    // 如果高亮结束位置是行尾，需要特殊处理
                    // getPrimaryHorizontal(lineEnd) 可能返回下一行开始位置，导致计算错误
                    if (highlightEnd >= lineEnd) {
                        // 使用行宽度作为右边界
                        right = layout.getLineRight(line);
                    } else {
                        right = layout.getPrimaryHorizontal(highlightEnd);
                    }
                    
                    float top = layout.getLineTop(line);
                    float bottom = layout.getLineBottom(line);
                    
                    // 确保 right > left，避免绘制无效矩形
                    if (right > left) {
                        canvas.drawRect(left, top, right, bottom, searchHighlightPaint);
                    }
                }
            }
            
            start = end;
        }
    }

    /**
     * 创建StaticLayout
     */
    private StaticLayout createStaticLayout(String text, TextPaint paint, int width) {
        return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setLineSpacing(0, lineSpacing)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build();
    }

    /**
     * sp转px
     */
    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    /**
     * dp转px
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ==================== 设置方法 ====================

    /**
     * 设置页面内容
     */
    public void setPageContent(String content) {
        this.pageContent = content != null ? content : "";
        invalidate();
    }

    /**
     * 设置章节标题
     */
    public void setChapterTitle(String title) {
        this.chapterTitle = title != null ? title : "";
        invalidate();
    }

    /**
     * 设置是否显示标题
     */
    public void setShowTitle(boolean show) {
        this.showTitle = show;
        invalidate();
    }

    /**
     * 设置页码信息
     */
    public void setPageInfo(String info) {
        this.pageInfo = info != null ? info : "";
        invalidate();
    }
    
    /**
     * 设置左上角章节名
     */
    public void setStatusChapterName(String name) {
        this.statusChapterName = name != null ? name : "";
        invalidate();
    }
    
    /**
     * 设置右下角时间电量
     */
    public void setStatusTimeBattery(String info) {
        this.statusTimeBattery = info != null ? info : "";
        invalidate();
    }

    /**
     * 设置字体大小
     */
    public void setFontSize(float size) {
        this.fontSize = size;
        textPaint.setTextSize(spToPx(size));
        titlePaint.setTextSize(spToPx(size + 4));
        invalidate();
    }

    /**
     * 设置行间距
     */
    public void setLineSpacing(float spacing) {
        this.lineSpacing = spacing;
        invalidate();
    }
    
    /**
     * 设置搜索关键词（用于高亮显示）
     */
    public void setSearchKeyword(String keyword) {
        this.searchKeyword = keyword != null ? keyword : "";
        invalidate();
    }
    
    /**
     * 清除搜索关键词高亮
     */
    public void clearSearchKeyword() {
        if (this.searchKeyword != null && !this.searchKeyword.isEmpty()) {
            this.searchKeyword = "";
            invalidate();
        }
    }

    /**
     * 设置字体
     */
    public void setFont(ReaderFont font) {
        if (font == null || font.getFontPath() == null) {
            currentTypeface = Typeface.DEFAULT;
        } else {
            try {
                AssetManager assets = getContext().getAssets();
                currentTypeface = Typeface.createFromAsset(assets, font.getFontPath());
            } catch (Exception e) {
                // 字体加载失败，使用默认字体
                currentTypeface = Typeface.DEFAULT;
            }
        }
        textPaint.setTypeface(currentTypeface);
        titlePaint.setTypeface(Typeface.create(currentTypeface, Typeface.BOLD));
        invalidate();
    }

    /**
     * 设置文本颜色
     */
    public void setTextColor(int color) {
        this.textColor = color;
        textPaint.setColor(color);
        titlePaint.setColor(color);
        invalidate();
    }

    /**
     * 设置背景颜色
     */
    @Override
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        invalidate();
    }

    /**
     * 获取可用内容高度（用于分页计算）
     */
    public int getContentHeight() {
        return getHeight() - paddingTop - paddingBottom;
    }

    /**
     * 获取可用内容宽度
     */
    public int getContentWidth() {
        return getWidth() - paddingHorizontal * 2;
    }

    /**
     * 获取字体大小
     */
    public float getFontSize() {
        return fontSize;
    }

    /**
     * 获取行间距
     */
    public float getLineSpacing() {
        return lineSpacing;
    }

    /**
     * 获取TextPaint（用于外部分页计算）
     */
    public TextPaint getTextPaint() {
        return textPaint;
    }
    
    /**
     * 设置当前页面在章节中的起始位置（缩进后文本）
     * @param startIndex 起始位置
     */
    public void setPageStartIndex(int startIndex) {
        this.pageStartIndex = startIndex;
    }
    
    /**
     * 获取当前页面在章节中的起始位置
     */
    public int getPageStartIndex() {
        return pageStartIndex;
    }
    
    // 原始文本起始位置（用于TTS高亮匹配）
    private int originalStartIndex = 0;
    private int originalEndIndex = 0;
    
    /**
     * 设置原始文本位置范围（用于TTS高亮匹配）
     * @param originalStart 原始文本起始位置
     * @param originalEnd 原始文本结束位置
     */
    public void setOriginalTextRange(int originalStart, int originalEnd) {
        this.originalStartIndex = originalStart;
        this.originalEndIndex = originalEnd;
    }
    
    /**
     * 设置TTS高亮位置（基于原始文本位置）
     * 使用简化的段落查找逻辑：直接在页面内容中通过换行符定位段落
     * @param originalPosition TTS在原始文本中的当前位置
     */
    public void setTTSHighlight(int originalPosition) {
        if (pageContent == null || pageContent.isEmpty()) {
            clearTTSHighlight();
            return;
        }
        
        // 检查TTS位置是否在当前页面的原始文本范围内
        if (originalPosition < originalStartIndex || originalPosition >= originalEndIndex) {
            clearTTSHighlight();
            return;
        }
        
        // 将原始位置转换为页面内位置，然后查找段落
        int pagePosition = convertOriginalToPagePosition(originalPosition);
        
        if (pagePosition < 0 || pagePosition >= pageContent.length()) {
            // 位置无效，高亮整个页面
            highlightStartInPage = 0;
            highlightEndInPage = pageContent.length();
            invalidate();
            return;
        }
        
        // 在页面内容中查找包含该位置的段落
        int paragraphStart = findParagraphStart(pageContent, pagePosition);
        int paragraphEnd = findParagraphEnd(pageContent, pagePosition);
        
        // 更新高亮范围
        if (highlightStartInPage != paragraphStart || highlightEndInPage != paragraphEnd) {
            highlightStartInPage = paragraphStart;
            highlightEndInPage = paragraphEnd;
            invalidate();
        }
    }
    
    /**
     * 将原始文本位置转换为页面内位置
     * 考虑首行缩进带来的偏移
     */
    private int convertOriginalToPagePosition(int originalPosition) {
        if (pageContent == null || pageContent.isEmpty()) {
            return -1;
        }
        
        // 计算原始位置相对于页面原始起始位置的偏移
        int relativeOriginalPos = originalPosition - originalStartIndex;
        
        if (relativeOriginalPos < 0) {
            return -1;
        }
        
        // 遍历页面内容，计算缩进偏移
        int pagePos = 0;
        int originalOffset = 0;
        boolean atLineStart = true;
        
        while (pagePos < pageContent.length()) {
            char c = pageContent.charAt(pagePos);
            
            // 检查是否是缩进字符（全角空格在行首）
            if (atLineStart && c == '\u3000') {
                pagePos++;
                continue;
            }
            
            // 检查是否已经到达目标位置
            if (originalOffset >= relativeOriginalPos) {
                return pagePos;
            }
            
            atLineStart = (c == '\n');
            pagePos++;
            originalOffset++;
        }
        
        return Math.min(pagePos, pageContent.length() - 1);
    }
    
    /**
     * 清除TTS高亮
     */
    public void clearTTSHighlight() {
        if (highlightStartInPage >= 0 || highlightEndInPage >= 0) {
            highlightStartInPage = -1;
            highlightEndInPage = -1;
            invalidate();
        }
    }
    
    /**
     * 查找段落起始位置
     */
    private int findParagraphStart(String text, int position) {
        if (position <= 0) return 0;
        
        // 向前查找换行符
        for (int i = position - 1; i >= 0; i--) {
            if (text.charAt(i) == '\n') {
                return i + 1;
            }
        }
        return 0;
    }
    
    /**
     * 查找段落结束位置
     */
    private int findParagraphEnd(String text, int position) {
        if (position >= text.length()) return text.length();
        
        // 向后查找换行符
        for (int i = position; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                return i;
            }
        }
        return text.length();
    }
}
