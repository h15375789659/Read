package com.example.read.presentation.reader;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.read.R;
import com.example.read.domain.model.PageAnimation;
import com.example.read.domain.model.PageMode;
import com.example.read.domain.model.ReaderFont;
import com.example.read.domain.model.ReaderTheme;

/**
 * 阅读设置对话框
 * 
 * 验证需求：5.4, 5.5, 6.1, 6.2, 6.3, 6.4, 6.6
 */
public class ReaderSettingsDialog extends Dialog {

    // 字体大小范围：12-32sp
    private static final float MIN_FONT_SIZE = 12f;
    private static final float MAX_FONT_SIZE = 32f;
    
    // 行间距范围：1.0-3.0
    private static final float MIN_LINE_SPACING = 1.0f;
    private static final float MAX_LINE_SPACING = 3.0f;

    // UI组件
    private SeekBar fontSizeSeekBar;
    private TextView fontSizeValue;
    private SeekBar lineSpacingSeekBar;
    private TextView lineSpacingValue;
    private Spinner pageModeSpinner;
    private Spinner pageAnimationSpinner;
    private Spinner fontFamilySpinner;
    private View pageAnimationContainer;  // 翻页动画容器
    
    // 主题选择视图
    private View themeDayContainer;
    private View themeNightContainer;
    private View themeEyeCareContainer;
    private View themeCustomContainer;
    private ImageView themeDayCheck;
    private ImageView themeNightCheck;
    private ImageView themeEyeCareCheck;
    private ImageView themeCustomCheck;

    // 当前设置值
    private float currentFontSize = 18f;
    private float currentLineSpacing = 1.5f;
    private String currentThemeId = "day";
    private PageMode currentPageMode = PageMode.SCROLL;
    private PageAnimation currentPageAnimation = PageAnimation.SLIDE;
    private ReaderFont currentFont = ReaderFont.DEFAULT;

    // 回调接口
    private OnSettingsChangeListener listener;
    
    // 标志：是否正在初始化（防止初始化时触发回调）
    private boolean isInitializing = true;

    public ReaderSettingsDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_reader_settings);
        
        initViews();
        setupListeners();
        updateUI();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        // 字体大小
        fontSizeSeekBar = findViewById(R.id.font_size_seek_bar);
        fontSizeValue = findViewById(R.id.font_size_value);
        
        // 行间距
        lineSpacingSeekBar = findViewById(R.id.line_spacing_seek_bar);
        lineSpacingValue = findViewById(R.id.line_spacing_value);
        
        // 主题选择
        themeDayContainer = findViewById(R.id.theme_day);
        themeNightContainer = findViewById(R.id.theme_night);
        themeEyeCareContainer = findViewById(R.id.theme_eye_care);
        themeCustomContainer = findViewById(R.id.theme_custom);
        
        themeDayCheck = findViewById(R.id.theme_day_check);
        themeNightCheck = findViewById(R.id.theme_night_check);
        themeEyeCareCheck = findViewById(R.id.theme_eye_care_check);
        themeCustomCheck = findViewById(R.id.theme_custom_check);
        
        // 翻页设置
        pageModeSpinner = findViewById(R.id.page_mode_spinner);
        pageAnimationSpinner = findViewById(R.id.page_animation_spinner);
        fontFamilySpinner = findViewById(R.id.font_family_spinner);
        pageAnimationContainer = findViewById(R.id.page_animation_container);
        
        // 初始化翻页模式Spinner
        String[] pageModeNames = new String[]{
                getContext().getString(R.string.page_mode_scroll),
                getContext().getString(R.string.page_mode_page)
        };
        ArrayAdapter<String> pageModeAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, pageModeNames);
        pageModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pageModeSpinner.setAdapter(pageModeAdapter);
        
        // 初始化翻页动画Spinner（移除无动画选项）
        String[] pageAnimationNames = new String[]{
                getContext().getString(R.string.page_animation_cover),
                getContext().getString(R.string.page_animation_slide),
                getContext().getString(R.string.page_animation_simulation),
                getContext().getString(R.string.page_animation_scroll)
        };
        ArrayAdapter<String> pageAnimationAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, pageAnimationNames);
        pageAnimationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pageAnimationSpinner.setAdapter(pageAnimationAdapter);
        
        // 初始化字体Spinner
        ReaderFont[] fonts = ReaderFont.values();
        String[] fontNames = new String[fonts.length];
        for (int i = 0; i < fonts.length; i++) {
            fontNames[i] = fonts[i].getDisplayName();
        }
        ArrayAdapter<String> fontAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, fontNames);
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontFamilySpinner.setAdapter(fontAdapter);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 字体大小SeekBar监听
        // 验证需求：5.4 - 实现字体大小调节SeekBar
        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isInitializing) {
                    float fontSize = progressToFontSize(progress);
                    currentFontSize = fontSize;
                    updateFontSizeDisplay(fontSize);
                    
                    if (listener != null) {
                        listener.onFontSizeChanged(fontSize);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 行间距SeekBar监听
        // 验证需求：5.5 - 实现行间距调节SeekBar
        lineSpacingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isInitializing) {
                    float lineSpacing = progressToLineSpacing(progress);
                    currentLineSpacing = lineSpacing;
                    updateLineSpacingDisplay(lineSpacing);
                    
                    if (listener != null) {
                        listener.onLineSpacingChanged(lineSpacing);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 主题选择监听
        // 验证需求：6.1, 6.2 - 实现主题选择（日间、夜间、护眼、自定义）
        themeDayContainer.setOnClickListener(v -> selectTheme("day"));
        themeNightContainer.setOnClickListener(v -> selectTheme("night"));
        themeEyeCareContainer.setOnClickListener(v -> selectTheme("eye_care"));
        themeCustomContainer.setOnClickListener(v -> showCustomThemeDialog());
        
        // 翻页模式监听
        pageModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing) return;  // 初始化时不触发回调
                
                PageMode newMode = position == 0 ? PageMode.SCROLL : PageMode.PAGE;
                if (newMode != currentPageMode) {
                    currentPageMode = newMode;
                    // 根据翻页模式显示/隐藏翻页动画选项
                    updatePageAnimationVisibility();
                    if (listener != null) {
                        listener.onPageModeChanged(newMode);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // 翻页动画监听
        pageAnimationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing) return;  // 初始化时不触发回调
                
                PageAnimation newAnimation = PageAnimation.values()[position];
                if (newAnimation != currentPageAnimation) {
                    currentPageAnimation = newAnimation;
                    if (listener != null) {
                        listener.onPageAnimationChanged(newAnimation);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // 字体选择监听
        fontFamilySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing) return;  // 初始化时不触发回调
                
                ReaderFont newFont = ReaderFont.values()[position];
                if (newFont != currentFont) {
                    currentFont = newFont;
                    if (listener != null) {
                        listener.onFontChanged(newFont);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * 更新UI显示
     */
    private void updateUI() {
        // 更新字体大小
        int fontProgress = fontSizeToProgress(currentFontSize);
        fontSizeSeekBar.setProgress(fontProgress);
        updateFontSizeDisplay(currentFontSize);
        
        // 更新行间距
        int lineProgress = lineSpacingToProgress(currentLineSpacing);
        lineSpacingSeekBar.setProgress(lineProgress);
        updateLineSpacingDisplay(currentLineSpacing);
        
        // 更新主题选择
        updateThemeSelection(currentThemeId);
        
        // 更新翻页模式
        pageModeSpinner.setSelection(currentPageMode == PageMode.SCROLL ? 0 : 1);
        
        // 更新翻页动画
        pageAnimationSpinner.setSelection(currentPageAnimation.ordinal());
        
        // 更新字体
        fontFamilySpinner.setSelection(currentFont.ordinal());
        
        // 根据翻页模式显示/隐藏翻页动画选项
        updatePageAnimationVisibility();
        
        // 初始化完成，允许触发回调
        isInitializing = false;
    }

    /**
     * 根据翻页模式更新翻页动画选项的可见性
     * 上下滚动模式不需要翻页动画
     */
    private void updatePageAnimationVisibility() {
        if (pageAnimationContainer != null) {
            pageAnimationContainer.setVisibility(
                    currentPageMode == PageMode.PAGE ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 更新字体大小显示
     */
    private void updateFontSizeDisplay(float fontSize) {
        fontSizeValue.setText(getContext().getString(R.string.settings_font_size_value, fontSize));
    }

    /**
     * 更新行间距显示
     */
    private void updateLineSpacingDisplay(float lineSpacing) {
        lineSpacingValue.setText(getContext().getString(R.string.settings_line_spacing_value, lineSpacing));
    }

    /**
     * 更新主题选择状态
     * 验证需求：6.2 - 选择主题后立即应用
     */
    private void updateThemeSelection(String themeId) {
        // 隐藏所有选中标记
        themeDayCheck.setVisibility(View.GONE);
        themeNightCheck.setVisibility(View.GONE);
        themeEyeCareCheck.setVisibility(View.GONE);
        themeCustomCheck.setVisibility(View.GONE);
        
        // 显示当前选中的主题标记
        switch (themeId) {
            case "day":
                themeDayCheck.setVisibility(View.VISIBLE);
                break;
            case "night":
                themeNightCheck.setVisibility(View.VISIBLE);
                break;
            case "eye_care":
                themeEyeCareCheck.setVisibility(View.VISIBLE);
                break;
            default:
                // 自定义主题
                if (themeId.startsWith("custom_") || themeId.equals("sepia")) {
                    themeCustomCheck.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    /**
     * 选择主题
     * 验证需求：6.2 - 立即应用该主题的背景色和文字色
     */
    private void selectTheme(String themeId) {
        currentThemeId = themeId;
        updateThemeSelection(themeId);
        
        if (listener != null) {
            ReaderTheme theme = ReaderTheme.getPresetById(themeId);
            listener.onThemeChanged(theme);
        }
    }

    /**
     * 显示自定义主题对话框
     * 验证需求：6.3, 6.4 - 允许用户自定义背景色和文字色
     */
    private void showCustomThemeDialog() {
        CustomThemeDialog customDialog = new CustomThemeDialog(getContext());
        customDialog.setOnThemeCreatedListener(theme -> {
            currentThemeId = theme.getId();
            updateThemeSelection(currentThemeId);
            
            if (listener != null) {
                listener.onCustomThemeCreated(theme);
            }
        });
        customDialog.show();
    }

    // ==================== 进度条与值的转换 ====================

    /**
     * 将SeekBar进度转换为字体大小
     * 进度范围：0-20 对应 字体大小：12-32sp
     */
    private float progressToFontSize(int progress) {
        return MIN_FONT_SIZE + (progress / 20f) * (MAX_FONT_SIZE - MIN_FONT_SIZE);
    }

    /**
     * 将字体大小转换为SeekBar进度
     */
    private int fontSizeToProgress(float fontSize) {
        return Math.round((fontSize - MIN_FONT_SIZE) / (MAX_FONT_SIZE - MIN_FONT_SIZE) * 20);
    }

    /**
     * 将SeekBar进度转换为行间距
     * 进度范围：0-20 对应 行间距：1.0-3.0
     */
    private float progressToLineSpacing(int progress) {
        return MIN_LINE_SPACING + (progress / 20f) * (MAX_LINE_SPACING - MIN_LINE_SPACING);
    }

    /**
     * 将行间距转换为SeekBar进度
     */
    private int lineSpacingToProgress(float lineSpacing) {
        return Math.round((lineSpacing - MIN_LINE_SPACING) / (MAX_LINE_SPACING - MIN_LINE_SPACING) * 20);
    }

    // ==================== 设置方法 ====================

    /**
     * 设置当前字体大小
     */
    public void setFontSize(float fontSize) {
        this.currentFontSize = fontSize;
        if (fontSizeSeekBar != null) {
            fontSizeSeekBar.setProgress(fontSizeToProgress(fontSize));
            updateFontSizeDisplay(fontSize);
        }
    }

    /**
     * 设置当前行间距
     */
    public void setLineSpacing(float lineSpacing) {
        this.currentLineSpacing = lineSpacing;
        if (lineSpacingSeekBar != null) {
            lineSpacingSeekBar.setProgress(lineSpacingToProgress(lineSpacing));
            updateLineSpacingDisplay(lineSpacing);
        }
    }

    /**
     * 设置当前主题ID
     */
    public void setCurrentThemeId(String themeId) {
        this.currentThemeId = themeId;
        if (themeDayCheck != null) {
            updateThemeSelection(themeId);
        }
    }

    /**
     * 设置当前翻页模式
     */
    public void setPageMode(PageMode pageMode) {
        this.currentPageMode = pageMode;
        if (pageModeSpinner != null) {
            pageModeSpinner.setSelection(pageMode == PageMode.SCROLL ? 0 : 1);
        }
    }

    /**
     * 设置当前翻页动画
     */
    public void setPageAnimation(PageAnimation pageAnimation) {
        this.currentPageAnimation = pageAnimation;
        if (pageAnimationSpinner != null) {
            pageAnimationSpinner.setSelection(pageAnimation.ordinal());
        }
    }

    /**
     * 设置当前字体
     */
    public void setFont(ReaderFont font) {
        this.currentFont = font;
        if (fontFamilySpinner != null) {
            fontFamilySpinner.setSelection(font.ordinal());
        }
    }

    /**
     * 设置设置变更监听器
     */
    public void setOnSettingsChangeListener(OnSettingsChangeListener listener) {
        this.listener = listener;
    }

    /**
     * 设置变更监听器接口
     */
    public interface OnSettingsChangeListener {
        /**
         * 字体大小变更
         * 验证需求：5.4
         */
        void onFontSizeChanged(float fontSize);
        
        /**
         * 行间距变更
         * 验证需求：5.5
         */
        void onLineSpacingChanged(float lineSpacing);
        
        /**
         * 主题变更
         * 验证需求：6.2
         */
        void onThemeChanged(ReaderTheme theme);
        
        /**
         * 自定义主题创建
         * 验证需求：6.5
         */
        void onCustomThemeCreated(ReaderTheme theme);
        
        /**
         * 翻页模式变更
         */
        void onPageModeChanged(PageMode pageMode);
        
        /**
         * 翻页动画变更
         */
        void onPageAnimationChanged(PageAnimation pageAnimation);
        
        /**
         * 字体变更
         */
        void onFontChanged(ReaderFont font);
    }
}
