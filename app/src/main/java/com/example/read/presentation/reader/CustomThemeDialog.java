package com.example.read.presentation.reader;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.read.R;
import com.example.read.domain.model.ReaderTheme;
import com.google.android.material.button.MaterialButton;

/**
 * 自定义主题对话框
 * 
 * 验证需求：6.3, 6.4, 6.5
 */
public class CustomThemeDialog extends Dialog {

    // 预定义的背景颜色
    private static final int COLOR_BG_WHITE = 0xFFFFFFFF;
    private static final int COLOR_BG_SEPIA = 0xFFF5E6C8;
    private static final int COLOR_BG_GREEN = 0xFFC7EDCC;
    private static final int COLOR_BG_GRAY = 0xFFE0E0E0;
    private static final int COLOR_BG_DARK = 0xFF1E1E1E;

    // 预定义的文字颜色
    private static final int COLOR_TEXT_BLACK = 0xFF000000;
    private static final int COLOR_TEXT_DARK_GRAY = 0xFF333333;
    private static final int COLOR_TEXT_BROWN = 0xFF5B4636;
    private static final int COLOR_TEXT_LIGHT_GRAY = 0xFFE0E0E0;
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;

    // UI组件
    private FrameLayout previewContainer;
    private TextView previewText;
    private MaterialButton btnCancel;
    private MaterialButton btnSave;

    // 背景颜色选择器
    private View bgColorWhite;
    private View bgColorSepia;
    private View bgColorGreen;
    private View bgColorGray;
    private View bgColorDark;

    // 文字颜色选择器
    private View textColorBlack;
    private View textColorDarkGray;
    private View textColorBrown;
    private View textColorLightGray;
    private View textColorWhite;

    // 当前选择的颜色
    private int selectedBackgroundColor = COLOR_BG_SEPIA;
    private int selectedTextColor = COLOR_TEXT_BROWN;

    // 回调接口
    private OnThemeCreatedListener listener;

    public CustomThemeDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_custom_theme);

        initViews();
        setupListeners();
        updatePreview();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        previewContainer = findViewById(R.id.preview_container);
        previewText = findViewById(R.id.preview_text);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);

        // 背景颜色选择器
        bgColorWhite = findViewById(R.id.bg_color_white);
        bgColorSepia = findViewById(R.id.bg_color_sepia);
        bgColorGreen = findViewById(R.id.bg_color_green);
        bgColorGray = findViewById(R.id.bg_color_gray);
        bgColorDark = findViewById(R.id.bg_color_dark);

        // 文字颜色选择器
        textColorBlack = findViewById(R.id.text_color_black);
        textColorDarkGray = findViewById(R.id.text_color_dark_gray);
        textColorBrown = findViewById(R.id.text_color_brown);
        textColorLightGray = findViewById(R.id.text_color_light_gray);
        textColorWhite = findViewById(R.id.text_color_white);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 背景颜色选择
        // 验证需求：6.3 - 允许用户从调色板选择任意颜色（背景色）
        bgColorWhite.setOnClickListener(v -> selectBackgroundColor(COLOR_BG_WHITE));
        bgColorSepia.setOnClickListener(v -> selectBackgroundColor(COLOR_BG_SEPIA));
        bgColorGreen.setOnClickListener(v -> selectBackgroundColor(COLOR_BG_GREEN));
        bgColorGray.setOnClickListener(v -> selectBackgroundColor(COLOR_BG_GRAY));
        bgColorDark.setOnClickListener(v -> selectBackgroundColor(COLOR_BG_DARK));

        // 文字颜色选择
        // 验证需求：6.4 - 允许用户从调色板选择任意颜色（文字色）
        textColorBlack.setOnClickListener(v -> selectTextColor(COLOR_TEXT_BLACK));
        textColorDarkGray.setOnClickListener(v -> selectTextColor(COLOR_TEXT_DARK_GRAY));
        textColorBrown.setOnClickListener(v -> selectTextColor(COLOR_TEXT_BROWN));
        textColorLightGray.setOnClickListener(v -> selectTextColor(COLOR_TEXT_LIGHT_GRAY));
        textColorWhite.setOnClickListener(v -> selectTextColor(COLOR_TEXT_WHITE));

        // 取消按钮
        btnCancel.setOnClickListener(v -> dismiss());

        // 保存按钮
        // 验证需求：6.5 - 将该主题添加到主题列表
        btnSave.setOnClickListener(v -> saveTheme());
    }

    /**
     * 选择背景颜色
     */
    private void selectBackgroundColor(int color) {
        selectedBackgroundColor = color;
        updatePreview();
        updateBackgroundColorSelection(color);
    }

    /**
     * 选择文字颜色
     */
    private void selectTextColor(int color) {
        selectedTextColor = color;
        updatePreview();
        updateTextColorSelection(color);
    }

    /**
     * 更新预览
     */
    private void updatePreview() {
        previewContainer.setBackgroundColor(selectedBackgroundColor);
        previewText.setTextColor(selectedTextColor);
    }

    /**
     * 更新背景颜色选择状态
     */
    private void updateBackgroundColorSelection(int selectedColor) {
        // 重置所有选择器的缩放
        resetViewScale(bgColorWhite);
        resetViewScale(bgColorSepia);
        resetViewScale(bgColorGreen);
        resetViewScale(bgColorGray);
        resetViewScale(bgColorDark);

        // 放大选中的颜色
        View selectedView = getBackgroundColorView(selectedColor);
        if (selectedView != null) {
            selectedView.setScaleX(1.2f);
            selectedView.setScaleY(1.2f);
        }
    }

    /**
     * 更新文字颜色选择状态
     */
    private void updateTextColorSelection(int selectedColor) {
        // 重置所有选择器的缩放
        resetViewScale(textColorBlack);
        resetViewScale(textColorDarkGray);
        resetViewScale(textColorBrown);
        resetViewScale(textColorLightGray);
        resetViewScale(textColorWhite);

        // 放大选中的颜色
        View selectedView = getTextColorView(selectedColor);
        if (selectedView != null) {
            selectedView.setScaleX(1.2f);
            selectedView.setScaleY(1.2f);
        }
    }

    /**
     * 重置视图缩放
     */
    private void resetViewScale(View view) {
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
    }

    /**
     * 根据颜色获取背景颜色视图
     */
    private View getBackgroundColorView(int color) {
        if (color == COLOR_BG_WHITE) return bgColorWhite;
        if (color == COLOR_BG_SEPIA) return bgColorSepia;
        if (color == COLOR_BG_GREEN) return bgColorGreen;
        if (color == COLOR_BG_GRAY) return bgColorGray;
        if (color == COLOR_BG_DARK) return bgColorDark;
        return null;
    }

    /**
     * 根据颜色获取文字颜色视图
     */
    private View getTextColorView(int color) {
        if (color == COLOR_TEXT_BLACK) return textColorBlack;
        if (color == COLOR_TEXT_DARK_GRAY) return textColorDarkGray;
        if (color == COLOR_TEXT_BROWN) return textColorBrown;
        if (color == COLOR_TEXT_LIGHT_GRAY) return textColorLightGray;
        if (color == COLOR_TEXT_WHITE) return textColorWhite;
        return null;
    }

    /**
     * 保存主题
     * 验证需求：6.5 - 将该主题添加到主题列表
     */
    private void saveTheme() {
        // 创建自定义主题
        ReaderTheme customTheme = ReaderTheme.createCustom(
                getContext().getString(R.string.theme_custom),
                selectedBackgroundColor,
                selectedTextColor
        );

        // 回调通知
        if (listener != null) {
            listener.onThemeCreated(customTheme);
        }

        dismiss();
    }

    /**
     * 设置主题创建监听器
     */
    public void setOnThemeCreatedListener(OnThemeCreatedListener listener) {
        this.listener = listener;
    }

    /**
     * 主题创建监听器接口
     */
    public interface OnThemeCreatedListener {
        void onThemeCreated(ReaderTheme theme);
    }
}
