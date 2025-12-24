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
        
        // 绘制页面内容
        if (pageContent != null && !pageContent.isEmpty()) {
            StaticLayout contentLayout = createStaticLayout(pageContent, textPaint, contentWidth);
            canvas.save();
            canvas.translate(paddingHorizontal, y);
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
}
