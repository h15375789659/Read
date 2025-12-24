package com.example.read.presentation.reader;

import android.app.Activity;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.read.R;
import com.example.read.domain.model.Chapter;
import com.example.read.domain.model.PageAnimation;
import com.example.read.domain.model.PageMode;
import com.example.read.domain.model.ReaderFont;
import com.example.read.domain.model.ReaderTheme;
import com.example.read.utils.NavigationHelper;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 阅读器Activity - 显示小说章节内容
 * 
 * 支持两种阅读模式：
 * 1. 上下滚动模式（ScrollView）
 * 2. 左右翻页模式（ViewPager2）
 * 
 * 验证需求：5.1, 5.2, 5.3, 5.6
 */
@AndroidEntryPoint
public class ReaderActivity extends AppCompatActivity {

    // 使用NavigationHelper中的常量
    public static final String EXTRA_NOVEL_ID = NavigationHelper.EXTRA_NOVEL_ID;
    public static final String EXTRA_CHAPTER_ID = NavigationHelper.EXTRA_CHAPTER_ID;

    private ReaderViewModel viewModel;

    // UI组件
    private View coordinatorLayout;
    private ScrollView contentScrollView;
    private TextView chapterTitleText;
    private TextView chapterContentText;
    private ProgressBar loadingProgress;
    
    // 状态栏信息
    private TextView statusChapterName;
    private TextView statusTimeBattery;
    
    // 左右翻页模式组件
    private ViewPager2 pageViewPager;
    private PageAdapter pageAdapter;
    private List<TextPaginator.PageInfo> currentPages;
    
    // 当前翻页模式
    private PageMode currentPageMode = PageMode.SCROLL;
    private PageAnimation currentPageAnimation = PageAnimation.SLIDE;
    
    // 手势检测器
    private GestureDetectorCompat gestureDetector;
    
    // 顶部工具栏
    private AppBarLayout topAppBar;
    private MaterialToolbar toolbar;
    private ImageButton btnSearch;
    private ImageButton btnBookmark;
    private ImageButton btnTts;
    
    // 底部工具栏
    private LinearLayout bottomToolbar;
    private TextView chapterProgressText;
    private SeekBar chapterSeekBar;
    private TextView totalChaptersText;
    private View btnPreviousChapter;
    private View btnCatalog;
    private View btnSettings;
    private View btnNextChapter;

    // 工具栏动画时长
    private static final int TOOLBAR_ANIMATION_DURATION = 200;
    
    // 章节列表对话框
    private BottomSheetDialog chapterListDialog;
    private ChapterAdapter chapterAdapter;
    
    // 搜索对话框
    private SearchDialog searchDialog;
    
    // 分页相关
    private boolean isPaginationReady = false;
    private long lastPaginatedChapterId = -1;  // 上次分页的章节ID
    private String lastPaginatedContent = "";   // 上次分页的内容
    private boolean needRestorePosition = true; // 是否需要恢复阅读位置
    private int pendingPageIndex = -1;          // 待恢复的页码
    
    // 时间更新
    private Handler timeHandler;
    private Runnable timeUpdateRunnable;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
    // 电量监听
    private BroadcastReceiver batteryReceiver;
    private int currentBatteryLevel = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        initViews();
        initViewModel();
        setupListeners();
        setupStatusInfo();
        observeData();
        
        // 加载小说
        loadNovelFromIntent();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        coordinatorLayout = findViewById(R.id.coordinator_layout);
        contentScrollView = findViewById(R.id.content_scroll_view);
        chapterTitleText = findViewById(R.id.chapter_title_text);
        chapterContentText = findViewById(R.id.chapter_content_text);
        loadingProgress = findViewById(R.id.loading_progress);
        
        // 状态栏信息
        statusChapterName = findViewById(R.id.status_chapter_name);
        statusTimeBattery = findViewById(R.id.status_time_battery);
        
        // 左右翻页模式
        pageViewPager = findViewById(R.id.page_view_pager);
        pageAdapter = new PageAdapter();
        pageViewPager.setAdapter(pageAdapter);
        
        // 顶部工具栏
        topAppBar = findViewById(R.id.top_app_bar);
        toolbar = findViewById(R.id.toolbar);
        btnSearch = findViewById(R.id.btn_search);
        btnBookmark = findViewById(R.id.btn_bookmark);
        btnTts = findViewById(R.id.btn_tts);
        
        // 底部工具栏
        bottomToolbar = findViewById(R.id.bottom_toolbar);
        chapterProgressText = findViewById(R.id.chapter_progress_text);
        chapterSeekBar = findViewById(R.id.chapter_seek_bar);
        totalChaptersText = findViewById(R.id.total_chapters_text);
        btnPreviousChapter = findViewById(R.id.btn_previous_chapter);
        btnCatalog = findViewById(R.id.btn_catalog);
        btnSettings = findViewById(R.id.btn_settings);
        btnNextChapter = findViewById(R.id.btn_next_chapter);
        
        // 初始化章节列表适配器
        chapterAdapter = new ChapterAdapter();
        
        // 设置翻页适配器点击监听 - 支持点击翻页
        pageAdapter.setOnPageClickListener(new PageAdapter.OnPageClickListener() {
            @Override
            public void onPageClick() {
                // 点击屏幕中央区域显示/隐藏工具栏
                viewModel.toggleToolbar();
            }
        });
        
        // 设置翻页监听器 - 点击左右区域翻页
        pageAdapter.setOnPageTurnListener(new PageAdapter.OnPageTurnListener() {
            @Override
            public void onPreviousPage() {
                int current = pageViewPager.getCurrentItem();
                int currentChapterStart = pageAdapter.getCurrentChapterStartIndex();
                
                if (current > 0) {
                    // 还有上一页（可能是当前章节的上一页，或者是预加载的上一章最后一页）
                    pageViewPager.setCurrentItem(current - 1, true);
                } else if (current == 0 && currentChapterStart == 0) {
                    // 已经在第一页且没有预加载的上一章页面，尝试切换到上一章
                    ReaderUiState state = viewModel.getUiState().getValue();
                    if (state != null && state.canGoPreviousChapter()) {
                        viewModel.goToPreviousChapter();
                        needRestorePosition = false;
                        pendingPageIndex = Integer.MAX_VALUE; // 上一章跳转到最后一页
                    } else {
                        Toast.makeText(ReaderActivity.this, R.string.reader_first_chapter, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onNextPage() {
                int current = pageViewPager.getCurrentItem();
                int totalPages = pageAdapter.getTotalPages();
                int currentChapterEnd = pageAdapter.getCurrentChapterEndIndex();
                
                if (current < totalPages - 1) {
                    // 还有下一页（可能是当前章节的下一页，或者是预加载的下一章第一页）
                    pageViewPager.setCurrentItem(current + 1, true);
                } else if (current == totalPages - 1 && current == currentChapterEnd) {
                    // 已经在最后一页且没有预加载的下一章页面，尝试切换到下一章
                    ReaderUiState state = viewModel.getUiState().getValue();
                    if (state != null && state.canGoNextChapter()) {
                        viewModel.goToNextChapter();
                        needRestorePosition = false;
                        pendingPageIndex = 0; // 下一章从第一页开始
                    } else {
                        Toast.makeText(ReaderActivity.this, R.string.reader_last_chapter, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    /**
     * 初始化ViewModel
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ReaderViewModel.class);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 初始化手势检测器，用于检测单击事件
        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                viewModel.toggleToolbar();
                return true;
            }
        });
        
        // 上下滚动模式的章节切换状态
        final boolean[] isAtBottom = {false};
        final boolean[] isAtTop = {false};
        final long[] touchDownTime = {0}; // 记录按下时间
        final float[] touchDownY = {0};   // 记录按下位置
        
        // 设置ScrollView的触摸监听器
        contentScrollView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            
            if (currentPageMode != PageMode.SCROLL) return false;
            
            View child = contentScrollView.getChildAt(0);
            if (child == null) return false;
            
            int scrollY = contentScrollView.getScrollY();
            int scrollViewHeight = contentScrollView.getHeight();
            int childHeight = child.getHeight();
            
            // 检测当前位置
            boolean atBottom = scrollY + scrollViewHeight >= childHeight - 10;
            boolean atTop = scrollY <= 10;
            
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // 记录开始触摸时的位置状态和时间
                isAtBottom[0] = atBottom;
                isAtTop[0] = atTop;
                touchDownTime[0] = System.currentTimeMillis();
                touchDownY[0] = event.getY();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                // 计算触摸时长和移动距离
                long touchDuration = System.currentTimeMillis() - touchDownTime[0];
                float moveDistance = Math.abs(event.getY() - touchDownY[0]);
                
                // 只有当触摸时间较短且有明显移动时，才认为是滑动操作
                // 单击操作通常时间短且移动距离小，不应触发章节切换
                boolean isSwipeGesture = moveDistance > 50 || touchDuration > 300;
                
                if (!isSwipeGesture) {
                    // 这是单击操作，不触发章节切换
                    return false;
                }
                
                // 如果开始时在底部，结束时仍在底部，说明用户尝试继续下滑 -> 下一章
                if (isAtBottom[0] && atBottom) {
                    ReaderUiState state = viewModel.getUiState().getValue();
                    if (state != null && state.canGoNextChapter()) {
                        viewModel.goToNextChapter();
                        needRestorePosition = false;
                        contentScrollView.scrollTo(0, 0);
                    }
                }
                
                // 如果开始时在顶部，结束时仍在顶部，说明用户尝试继续上滑 -> 上一章
                if (isAtTop[0] && atTop) {
                    ReaderUiState state = viewModel.getUiState().getValue();
                    if (state != null && state.canGoPreviousChapter()) {
                        viewModel.goToPreviousChapter();
                        needRestorePosition = false;
                        // 跳转到上一章末尾
                        contentScrollView.post(() -> {
                            View c = contentScrollView.getChildAt(0);
                            if (c != null) {
                                contentScrollView.scrollTo(0, c.getHeight() - contentScrollView.getHeight());
                            }
                        });
                    }
                }
            }
            
            return false;
        });
        
        // 设置ScrollView的滚动监听器 - 检测是否滚动到底部
        contentScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (currentPageMode != PageMode.SCROLL) return;
            
            // 滚动监听器不再用于章节切换，只用于其他目的
        });
        
        // 返回按钮
        toolbar.setNavigationOnClickListener(v -> saveAndFinish());
        
        // 搜索按钮
        btnSearch.setOnClickListener(v -> showSearchDialog());
        
        // 书签按钮
        btnBookmark.setOnClickListener(v -> {
            Toast.makeText(this, R.string.reader_bookmark_added, Toast.LENGTH_SHORT).show();
        });
        
        // 朗读按钮
        btnTts.setOnClickListener(v -> viewModel.toggleTTS());
        
        // 上一章按钮
        btnPreviousChapter.setOnClickListener(v -> {
            ReaderUiState state = viewModel.getUiState().getValue();
            if (state != null && state.canGoPreviousChapter()) {
                viewModel.goToPreviousChapter();
                animateChapterChange(true);
            } else {
                Toast.makeText(this, R.string.reader_first_chapter, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 下一章按钮
        btnNextChapter.setOnClickListener(v -> {
            ReaderUiState state = viewModel.getUiState().getValue();
            if (state != null && state.canGoNextChapter()) {
                viewModel.goToNextChapter();
                animateChapterChange(false);
            } else {
                Toast.makeText(this, R.string.reader_last_chapter, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 目录按钮
        btnCatalog.setOnClickListener(v -> showChapterListDialog());
        
        // 设置按钮
        btnSettings.setOnClickListener(v -> showReaderSettingsDialog());
        
        // 章节进度条
        chapterSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateChapterProgressText(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                jumpToChapter(seekBar.getProgress());
            }
        });
        
        // 章节点击监听
        chapterAdapter.setOnChapterClickListener(chapter -> {
            if (chapterListDialog != null) {
                chapterListDialog.dismiss();
            }
            viewModel.loadChapter(chapter.getId());
            animateChapterChange(false);
        });
        
        // ViewPager2页面切换监听
        pageViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private boolean isChapterChanging = false; // 章节切换中标志
            
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                
                // 如果正在切换章节，忽略此回调
                if (isChapterChanging) return;
                
                // 检测是否翻到了相邻章节的页面
                if (pageAdapter.isPreviousChapterPage(position)) {
                    // 翻到了上一章的最后一页，延迟触发章节切换（等待翻页动画完成）
                    isChapterChanging = true;
                    pageViewPager.postDelayed(() -> {
                        ReaderUiState uiState = viewModel.getUiState().getValue();
                        if (uiState != null && uiState.canGoPreviousChapter()) {
                            viewModel.goToPreviousChapter();
                            needRestorePosition = false;
                            pendingPageIndex = Integer.MAX_VALUE; // 上一章跳转到最后一页
                        }
                        // 延迟重置标志，等待新章节加载完成
                        pageViewPager.postDelayed(() -> isChapterChanging = false, 300);
                    }, 300); // 等待翻页动画完成
                } else if (pageAdapter.isNextChapterPage(position)) {
                    // 翻到了下一章的第一页，延迟触发章节切换（等待翻页动画完成）
                    isChapterChanging = true;
                    pageViewPager.postDelayed(() -> {
                        ReaderUiState uiState = viewModel.getUiState().getValue();
                        if (uiState != null && uiState.canGoNextChapter()) {
                            viewModel.goToNextChapter();
                            needRestorePosition = false;
                            pendingPageIndex = 0; // 下一章从第一页开始
                        }
                        // 延迟重置标志，等待新章节加载完成
                        pageViewPager.postDelayed(() -> isChapterChanging = false, 300);
                    }, 300); // 等待翻页动画完成
                }
            }
            
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
            
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
    }

    /**
     * 观察数据变化
     */
    private void observeData() {
        viewModel.getUiState().observe(this, this::updateUI);
    }

    /**
     * 更新UI
     */
    private void updateUI(ReaderUiState state) {
        // 更新加载状态
        loadingProgress.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        
        // 更新翻页模式
        if (state.getPageMode() != currentPageMode) {
            currentPageMode = state.getPageMode();
            switchPageMode(currentPageMode);
        }
        
        // 更新翻页动画
        if (state.getPageAnimation() != currentPageAnimation) {
            currentPageAnimation = state.getPageAnimation();
            updatePageAnimation(currentPageAnimation);
        }
        
        // 根据模式显示/隐藏对应视图
        if (currentPageMode == PageMode.SCROLL) {
            contentScrollView.setVisibility(state.isLoading() ? View.INVISIBLE : View.VISIBLE);
            pageViewPager.setVisibility(View.GONE);
        } else {
            contentScrollView.setVisibility(View.GONE);
            pageViewPager.setVisibility(state.isLoading() ? View.INVISIBLE : View.VISIBLE);
        }
        
        // 更新错误信息
        if (state.getError() != null && !state.getError().isEmpty()) {
            Toast.makeText(this, state.getError(), Toast.LENGTH_SHORT).show();
            viewModel.clearError();
        }
        
        // 更新章节内容
        Chapter currentChapter = state.getCurrentChapter();
        if (currentChapter != null) {
            String content = state.getDisplayContent();
            String title = currentChapter.getTitle();
            
            // 更新左上角章节名（滚动模式用TextView，翻页模式在PageContentView中绘制）
            statusChapterName.setText(title);
            pageAdapter.setStatusChapterName(title);
            
            // 上下滚动模式
            chapterTitleText.setText(title);
            chapterContentText.setText(content);
            chapterAdapter.setCurrentChapterId(currentChapter.getId());
            
            // 恢复上下滚动模式的位置
            if (currentPageMode == PageMode.SCROLL && needRestorePosition && state.getSavedScrollPosition() > 0) {
                contentScrollView.post(() -> {
                    contentScrollView.scrollTo(0, state.getSavedScrollPosition());
                });
                needRestorePosition = false;
            }
            
            // 左右翻页模式 - 只有在章节或内容改变时才重新分页
            if (currentPageMode == PageMode.PAGE && !state.isLoading()) {
                // 检查是否需要重新分页（章节改变或内容改变）
                boolean needRepaginate = lastPaginatedChapterId != currentChapter.getId() 
                        || !content.equals(lastPaginatedContent);
                if (needRepaginate) {
                    lastPaginatedChapterId = currentChapter.getId();
                    lastPaginatedContent = content;
                    paginateContent(content, title, state);
                }
            }
        }
        
        // 更新工具栏显示状态
        updateToolbarVisibility(state.isShowToolbar());
        
        // 更新主题
        applyTheme(state.getTheme());
        
        // 更新字体大小和行间距
        chapterContentText.setTextSize(state.getFontSize());
        chapterContentText.setLineSpacing(0, state.getLineSpacing());
        chapterTitleText.setTextSize(state.getFontSize() + 2);
        
        // 更新上下滚动模式的字体
        Typeface scrollTypeface = getTypefaceForFont(state.getFont());
        chapterContentText.setTypeface(scrollTypeface);
        chapterTitleText.setTypeface(scrollTypeface, Typeface.BOLD);
        
        // 更新翻页适配器的显示设置
        pageAdapter.setFontSize(state.getFontSize());
        pageAdapter.setLineSpacing(state.getLineSpacing());
        if (state.getTheme() != null) {
            pageAdapter.setTextColor(state.getTheme().getTextColor());
            pageAdapter.setBackgroundColor(state.getTheme().getBackgroundColor());
        }
        
        // 更新字体
        pageAdapter.setFont(state.getFont());
        
        // 更新章节进度
        List<Chapter> chapters = state.getChapters();
        if (chapters != null && !chapters.isEmpty()) {
            int totalChapters = chapters.size();
            int currentIndex = state.getCurrentChapterIndex();
            
            chapterSeekBar.setMax(totalChapters - 1);
            chapterSeekBar.setProgress(currentIndex);
            updateChapterProgressText(currentIndex);
            totalChaptersText.setText(getString(R.string.reader_chapter_count, totalChapters));
            
            chapterAdapter.submitList(chapters);
        }
        
        // 更新TTS按钮状态
        updateTTSButton(state);
    }

    /**
     * 分页内容（用于左右翻页模式）
     * 包含上一章最后一页和下一章第一页，实现跨章节翻页动画
     */
    private void paginateContent(String content, String title, ReaderUiState state) {
        if (content == null || content.isEmpty()) return;
        
        // 延迟执行以确保视图已测量
        pageViewPager.post(() -> {
            int width = pageViewPager.getWidth();
            int height = pageViewPager.getHeight();
            
            if (width <= 0 || height <= 0) return;
            
            // 获取当前字体
            Typeface currentTypeface = Typeface.DEFAULT;
            ReaderFont font = state.getFont();
            if (font != null && font.getFontPath() != null) {
                try {
                    currentTypeface = Typeface.createFromAsset(getAssets(), font.getFontPath());
                } catch (Exception e) {
                    currentTypeface = Typeface.DEFAULT;
                }
            }
            
            // 创建文本画笔（使用与PageContentView相同的字体）
            TextPaint textPaint = new TextPaint();
            textPaint.setTextSize(spToPx(state.getFontSize()));
            textPaint.setAntiAlias(true);
            textPaint.setTypeface(currentTypeface);
            
            // 创建标题画笔
            TextPaint titlePaint = new TextPaint();
            titlePaint.setTextSize(spToPx(state.getFontSize() + 4));
            titlePaint.setTypeface(Typeface.create(currentTypeface, Typeface.BOLD));
            titlePaint.setAntiAlias(true);
            
            // 计算可用区域（减去内边距）
            // 注意：这里的值必须与 PageContentView 中的内边距保持一致
            int contentWidth = width - dpToPx(64);  // 左右各32dp
            int contentHeight = height - dpToPx(64);  // 上28dp + 下36dp
            
            // 留出一点余量，避免因为浮点数精度问题导致最后一行被截断
            contentHeight -= dpToPx(4);
            
            // 获取scaledDensity用于sp转px
            float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
            
            // 计算标题高度
            int titleHeight = TextPaginator.calculateTitleHeight(title, titlePaint, contentWidth, 
                    state.getLineSpacing(), scaledDensity);
            
            // 分页当前章节
            currentPages = TextPaginator.paginate(content, textPaint, contentWidth, contentHeight, 
                    state.getLineSpacing(), titleHeight);
            
            // 分页上一章（只需要最后一页）
            TextPaginator.PageInfo prevLastPage = null;
            String prevTitle = null;
            if (state.getPreviousChapter() != null && state.getPreviousChapterContent() != null 
                    && !state.getPreviousChapterContent().isEmpty()) {
                prevTitle = state.getPreviousChapter().getTitle();
                int prevTitleHeight = TextPaginator.calculateTitleHeight(prevTitle, titlePaint, contentWidth, 
                        state.getLineSpacing(), scaledDensity);
                List<TextPaginator.PageInfo> prevPages = TextPaginator.paginate(
                        state.getPreviousChapterContent(), textPaint, contentWidth, contentHeight, 
                        state.getLineSpacing(), prevTitleHeight);
                if (!prevPages.isEmpty()) {
                    prevLastPage = prevPages.get(prevPages.size() - 1);
                }
            }
            
            // 分页下一章（只需要第一页）
            TextPaginator.PageInfo nextFirstPage = null;
            String nextTitle = null;
            if (state.getNextChapter() != null && state.getNextChapterContent() != null 
                    && !state.getNextChapterContent().isEmpty()) {
                nextTitle = state.getNextChapter().getTitle();
                int nextTitleHeight = TextPaginator.calculateTitleHeight(nextTitle, titlePaint, contentWidth, 
                        state.getLineSpacing(), scaledDensity);
                List<TextPaginator.PageInfo> nextPages = TextPaginator.paginate(
                        state.getNextChapterContent(), textPaint, contentWidth, contentHeight, 
                        state.getLineSpacing(), nextTitleHeight);
                if (!nextPages.isEmpty()) {
                    nextFirstPage = nextPages.get(0);
                }
            }
            
            // 更新适配器（包含相邻章节页面）
            long currentChapterId = state.getCurrentChapter() != null ? state.getCurrentChapter().getId() : 0;
            pageAdapter.setPages(currentPages, title, currentChapterId, 
                    prevLastPage, prevTitle, nextFirstPage, nextTitle);
            
            // 恢复阅读位置（需要考虑上一章页面的偏移）
            int currentChapterStartIndex = pageAdapter.getCurrentChapterStartIndex();
            int targetPage = currentChapterStartIndex; // 默认跳转到当前章节第一页
            
            if (pendingPageIndex == Integer.MAX_VALUE) {
                // 特殊值：跳转到当前章节最后一页（从下一章跳转过来）
                targetPage = pageAdapter.getCurrentChapterEndIndex();
                pendingPageIndex = -1;
            } else if (pendingPageIndex >= 0) {
                // 有待恢复的页码（章节切换时设置）
                targetPage = currentChapterStartIndex + Math.min(pendingPageIndex, currentPages.size() - 1);
                pendingPageIndex = -1;
            } else if (needRestorePosition) {
                // 首次加载，恢复保存的位置
                targetPage = currentChapterStartIndex + Math.min(state.getSavedPageIndex(), currentPages.size() - 1);
                needRestorePosition = false;
            } else {
                // 不需要恢复位置，保持当前页面
                isPaginationReady = true;
                return;
            }
            
            pageViewPager.setCurrentItem(targetPage, false);
            
            isPaginationReady = true;
        });
    }

    /**
     * 切换翻页模式
     */
    private void switchPageMode(PageMode mode) {
        if (mode == PageMode.SCROLL) {
            // 切换到上下滚动模式
            contentScrollView.setVisibility(View.VISIBLE);
            pageViewPager.setVisibility(View.GONE);
            // 显示滚动模式的状态信息
            statusChapterName.setVisibility(View.VISIBLE);
            statusTimeBattery.setVisibility(View.VISIBLE);
        } else {
            // 切换到左右翻页模式
            contentScrollView.setVisibility(View.GONE);
            pageViewPager.setVisibility(View.VISIBLE);
            // 隐藏滚动模式的状态信息（翻页模式在PageContentView中绘制）
            statusChapterName.setVisibility(View.GONE);
            statusTimeBattery.setVisibility(View.GONE);
            
            // 强制重新分页
            lastPaginatedChapterId = -1;
            lastPaginatedContent = "";
            
            // 如果是首次加载（needRestorePosition 为 true），保留恢复位置的标志
            // 否则从第一页开始
            if (!needRestorePosition) {
                pendingPageIndex = 0;
            }
            // 注意：不要在这里设置 needRestorePosition = false，让 paginateContent 来处理
            
            // 触发重新分页
            ReaderUiState state = viewModel.getUiState().getValue();
            if (state != null && state.getCurrentChapter() != null) {
                lastPaginatedChapterId = state.getCurrentChapter().getId();
                lastPaginatedContent = state.getDisplayContent();
                paginateContent(state.getDisplayContent(), state.getCurrentChapter().getTitle(), state);
            }
        }
    }

    /**
     * 更新翻页动画
     */
    private void updatePageAnimation(PageAnimation animation) {
        pageViewPager.setPageTransformer(PageTransformers.getTransformer(animation));
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

    /**
     * 更新章节进度文本
     */
    private void updateChapterProgressText(int currentIndex) {
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state != null && state.getChapters() != null) {
            int total = state.getChapters().size();
            chapterProgressText.setText(getString(R.string.reader_chapter_progress, currentIndex + 1, total));
        }
    }

    /**
     * 跳转到指定章节
     */
    private void jumpToChapter(int index) {
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state != null && state.getChapters() != null && index < state.getChapters().size()) {
            Chapter chapter = state.getChapters().get(index);
            viewModel.loadChapter(chapter.getId());
            animateChapterChange(false);
        }
    }

    /**
     * 更新工具栏显示状态
     */
    private void updateToolbarVisibility(boolean show) {
        if (show) {
            showToolbars();
        } else {
            hideToolbars();
        }
    }

    /**
     * 显示工具栏（带动画）
     */
    private void showToolbars() {
        topAppBar.animate().setListener(null).cancel();
        bottomToolbar.animate().setListener(null).cancel();
        
        topAppBar.setVisibility(View.VISIBLE);
        topAppBar.setAlpha(0f);
        topAppBar.animate()
                .alpha(1f)
                .setDuration(TOOLBAR_ANIMATION_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(null)
                .start();
        
        bottomToolbar.setVisibility(View.VISIBLE);
        bottomToolbar.setTranslationY(bottomToolbar.getHeight());
        bottomToolbar.animate()
                .translationY(0)
                .setDuration(TOOLBAR_ANIMATION_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(null)
                .start();
    }

    /**
     * 隐藏工具栏（带动画）
     */
    private void hideToolbars() {
        topAppBar.animate().cancel();
        bottomToolbar.animate().cancel();
        
        if (topAppBar.getVisibility() == View.VISIBLE) {
            topAppBar.animate()
                    .alpha(0f)
                    .setDuration(TOOLBAR_ANIMATION_DURATION)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            topAppBar.setVisibility(View.GONE);
                            topAppBar.animate().setListener(null);
                        }
                    })
                    .start();
        }
        
        if (bottomToolbar.getVisibility() == View.VISIBLE) {
            bottomToolbar.animate()
                    .translationY(bottomToolbar.getHeight())
                    .setDuration(TOOLBAR_ANIMATION_DURATION)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            bottomToolbar.setVisibility(View.GONE);
                            bottomToolbar.animate().setListener(null);
                        }
                    })
                    .start();
        }
    }

    /**
     * 应用主题
     */
    private void applyTheme(ReaderTheme theme) {
        if (theme == null) return;
        
        coordinatorLayout.setBackgroundColor(theme.getBackgroundColor());
        chapterTitleText.setTextColor(theme.getTextColor());
        chapterContentText.setTextColor(theme.getTextColor());
        
        // 更新翻页适配器主题
        pageAdapter.setTextColor(theme.getTextColor());
        pageAdapter.setBackgroundColor(theme.getBackgroundColor());
    }

    /**
     * 更新TTS按钮状态
     */
    private void updateTTSButton(ReaderUiState state) {
        if (state.isTTSPlaying()) {
            btnTts.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnTts.setImageResource(android.R.drawable.ic_btn_speak_now);
        }
    }

    /**
     * 章节切换动画
     */
    private void animateChapterChange(boolean isPrevious) {
        if (currentPageMode == PageMode.SCROLL) {
            // 上下滚动模式的动画
            contentScrollView.smoothScrollTo(0, 0);
            
            float startX = isPrevious ? -100f : 100f;
            
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(chapterContentText, "alpha", 1f, 0f);
            fadeOut.setDuration(150);
            
            ObjectAnimator translateOut = ObjectAnimator.ofFloat(chapterContentText, "translationX", 0f, -startX);
            translateOut.setDuration(150);
            
            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    chapterContentText.setTranslationX(startX);
                    
                    ObjectAnimator fadeIn = ObjectAnimator.ofFloat(chapterContentText, "alpha", 0f, 1f);
                    fadeIn.setDuration(150);
                    
                    ObjectAnimator translateIn = ObjectAnimator.ofFloat(chapterContentText, "translationX", startX, 0f);
                    translateIn.setDuration(150);
                    
                    fadeIn.start();
                    translateIn.start();
                }
            });
            
            fadeOut.start();
            translateOut.start();
        } else {
            // 左右翻页模式 - 通过预加载的相邻章节页面实现翻页动画
            // 设置待跳转的页码，让 paginateContent 处理
            if (isPrevious) {
                pendingPageIndex = Integer.MAX_VALUE; // 上一章跳转到最后一页
            } else {
                pendingPageIndex = 0; // 下一章从第一页开始
            }
            needRestorePosition = false;
        }
    }

    /**
     * 显示章节列表对话框
     */
    private void showChapterListDialog() {
        if (chapterListDialog == null) {
            chapterListDialog = new BottomSheetDialog(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_chapter_list, null);
            chapterListDialog.setContentView(view);
            
            RecyclerView recyclerView = view.findViewById(R.id.chapter_list_recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(chapterAdapter);
            
            TextView chapterCountText = view.findViewById(R.id.chapter_count_text);
            ReaderUiState state = viewModel.getUiState().getValue();
            if (state != null && state.getChapters() != null) {
                chapterCountText.setText(getString(R.string.reader_chapter_count, state.getChapters().size()));
            }
        }
        
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state != null && state.getCurrentChapter() != null) {
            chapterAdapter.setCurrentChapterId(state.getCurrentChapter().getId());
            
            chapterListDialog.setOnShowListener(dialog -> {
                RecyclerView recyclerView = chapterListDialog.findViewById(R.id.chapter_list_recycler_view);
                if (recyclerView != null) {
                    int position = state.getCurrentChapterIndex();
                    recyclerView.scrollToPosition(Math.max(0, position - 3));
                }
            });
        }
        
        chapterListDialog.show();
    }

    /**
     * 从Intent加载小说
     */
    private void loadNovelFromIntent() {
        long novelId = getIntent().getLongExtra(EXTRA_NOVEL_ID, -1);
        if (novelId > 0) {
            viewModel.loadNovel(novelId);
        } else {
            Toast.makeText(this, "无效的小说ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 保存进度并退出
     */
    private void saveAndFinish() {
        int position = 0;
        if (currentPageMode == PageMode.SCROLL) {
            position = contentScrollView.getScrollY();
        } else {
            // 计算当前章节内的页码（减去上一章页面的偏移）
            int currentItem = pageViewPager.getCurrentItem();
            int startIndex = pageAdapter.getCurrentChapterStartIndex();
            position = Math.max(0, currentItem - startIndex);
        }
        viewModel.saveAndExit(position);
        
        // 返回结果给调用者
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state != null && state.getNovel() != null) {
            Intent resultIntent = NavigationHelper.createResultIntent(state.getNovel().getId());
            NavigationHelper.finishWithResult(this, Activity.RESULT_OK, resultIntent);
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
    }

    /**
     * 显示阅读设置对话框
     */
    private void showReaderSettingsDialog() {
        ReaderSettingsDialog settingsDialog = new ReaderSettingsDialog(this);
        
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state != null) {
            settingsDialog.setFontSize(state.getFontSize());
            settingsDialog.setLineSpacing(state.getLineSpacing());
            if (state.getTheme() != null) {
                settingsDialog.setCurrentThemeId(state.getTheme().getId());
            }
            settingsDialog.setPageMode(state.getPageMode());
            settingsDialog.setPageAnimation(state.getPageAnimation());
            settingsDialog.setFont(state.getFont());
        }
        
        settingsDialog.setOnSettingsChangeListener(new ReaderSettingsDialog.OnSettingsChangeListener() {
            @Override
            public void onFontSizeChanged(float fontSize) {
                viewModel.setFontSize(fontSize);
                // 如果是翻页模式，需要重新分页
                if (currentPageMode == PageMode.PAGE) {
                    // 强制重新分页
                    lastPaginatedChapterId = -1;
                    lastPaginatedContent = "";
                    ReaderUiState s = viewModel.getUiState().getValue();
                    if (s != null && s.getCurrentChapter() != null) {
                        lastPaginatedChapterId = s.getCurrentChapter().getId();
                        lastPaginatedContent = s.getDisplayContent();
                        paginateContent(s.getDisplayContent(), s.getCurrentChapter().getTitle(), s);
                    }
                }
            }

            @Override
            public void onLineSpacingChanged(float lineSpacing) {
                viewModel.setLineSpacing(lineSpacing);
                // 如果是翻页模式，需要重新分页
                if (currentPageMode == PageMode.PAGE) {
                    // 强制重新分页
                    lastPaginatedChapterId = -1;
                    lastPaginatedContent = "";
                    ReaderUiState s = viewModel.getUiState().getValue();
                    if (s != null && s.getCurrentChapter() != null) {
                        lastPaginatedChapterId = s.getCurrentChapter().getId();
                        lastPaginatedContent = s.getDisplayContent();
                        paginateContent(s.getDisplayContent(), s.getCurrentChapter().getTitle(), s);
                    }
                }
            }

            @Override
            public void onThemeChanged(ReaderTheme theme) {
                viewModel.setTheme(theme);
            }

            @Override
            public void onCustomThemeCreated(ReaderTheme theme) {
                viewModel.saveCustomTheme(theme);
                viewModel.setTheme(theme);
            }
            
            @Override
            public void onPageModeChanged(PageMode pageMode) {
                viewModel.setPageMode(pageMode);
            }
            
            @Override
            public void onPageAnimationChanged(PageAnimation pageAnimation) {
                viewModel.setPageAnimation(pageAnimation);
            }
            
            @Override
            public void onFontChanged(ReaderFont font) {
                viewModel.setFont(font);
                // 如果是翻页模式，需要重新分页
                if (currentPageMode == PageMode.PAGE) {
                    lastPaginatedChapterId = -1;
                    lastPaginatedContent = "";
                    ReaderUiState s = viewModel.getUiState().getValue();
                    if (s != null && s.getCurrentChapter() != null) {
                        lastPaginatedChapterId = s.getCurrentChapter().getId();
                        lastPaginatedContent = s.getDisplayContent();
                        paginateContent(s.getDisplayContent(), s.getCurrentChapter().getTitle(), s);
                    }
                }
            }
        });
        
        settingsDialog.show();
    }

    /**
     * 显示搜索对话框
     */
    private void showSearchDialog() {
        searchDialog = new SearchDialog(this);
        
        searchDialog.setOnSearchListener(new SearchDialog.OnSearchListener() {
            @Override
            public void onSearch(String keyword) {
                viewModel.searchInNovel(keyword);
            }

            @Override
            public void onSearchResultClick(com.example.read.domain.model.SearchResult result, int position) {
                viewModel.navigateToSearchResult(position);
            }

            @Override
            public void onNavigateToPrevious() {
                viewModel.navigateToPreviousResult();
            }

            @Override
            public void onNavigateToNext() {
                viewModel.navigateToNextResult();
            }

            @Override
            public void onReturnToOriginalPosition() {
                viewModel.returnToSavedPosition();
            }
        });
        
        searchDialog.show();
        
        viewModel.getUiState().observe(this, s -> {
            if (searchDialog != null && searchDialog.isShowing()) {
                if (s.hasSearchResults()) {
                    searchDialog.showSearchResults(s.getSearchResults());
                    searchDialog.setCurrentIndex(s.getCurrentSearchIndex());
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 停止时间更新
        if (timeHandler != null && timeUpdateRunnable != null) {
            timeHandler.removeCallbacks(timeUpdateRunnable);
        }
        
        int position = 0;
        if (currentPageMode == PageMode.SCROLL) {
            position = contentScrollView.getScrollY();
        } else {
            // 计算当前章节内的页码（减去上一章页面的偏移）
            int currentItem = pageViewPager.getCurrentItem();
            int startIndex = pageAdapter.getCurrentChapterStartIndex();
            position = Math.max(0, currentItem - startIndex);
        }
        viewModel.updateReadingPosition(position);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 恢复时间更新
        startTimeUpdate();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销电量广播接收器
        if (batteryReceiver != null) {
            try {
                unregisterReceiver(batteryReceiver);
            } catch (Exception ignored) {}
        }
        // 停止时间更新
        if (timeHandler != null && timeUpdateRunnable != null) {
            timeHandler.removeCallbacks(timeUpdateRunnable);
        }
    }
    
    /**
     * 设置状态栏信息（时间和电量）
     */
    private void setupStatusInfo() {
        // 初始化时间更新
        timeHandler = new Handler(Looper.getMainLooper());
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimeBattery();
                timeHandler.postDelayed(this, 60000); // 每分钟更新一次
            }
        };
        startTimeUpdate();
        
        // 注册电量广播接收器
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    currentBatteryLevel = (int) ((level / (float) scale) * 100);
                    updateTimeBattery();
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }
    
    /**
     * 开始时间更新
     */
    private void startTimeUpdate() {
        if (timeHandler != null && timeUpdateRunnable != null) {
            timeHandler.removeCallbacks(timeUpdateRunnable);
            timeHandler.post(timeUpdateRunnable);
        }
    }
    
    /**
     * 更新时间和电量显示
     */
    private void updateTimeBattery() {
        String time = timeFormat.format(new Date());
        String batteryText = currentBatteryLevel >= 0 ? currentBatteryLevel + "%" : "";
        String statusText;
        if (batteryText.isEmpty()) {
            statusText = time;
        } else {
            statusText = time + " · " + batteryText;
        }
        // 更新滚动模式的TextView
        statusTimeBattery.setText(statusText);
        // 更新翻页模式的PageContentView
        pageAdapter.setStatusTimeBattery(statusText);
    }

    /**
     * 根据 ReaderFont 获取 Typeface
     */
    private Typeface getTypefaceForFont(ReaderFont font) {
        if (font == null || font.getFontPath() == null) {
            return Typeface.DEFAULT;
        }
        try {
            return Typeface.createFromAsset(getAssets(), font.getFontPath());
        } catch (Exception e) {
            // 字体加载失败，使用默认字体
            return Typeface.DEFAULT;
        }
    }

    /**
     * 切换夜间模式
     */
    private void toggleNightMode() {
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state == null || state.getTheme() == null) return;
        
        String currentThemeId = state.getTheme().getId();
        if ("night".equals(currentThemeId)) {
            // 当前是夜间模式，切换到日间模式
            viewModel.setThemeById("day");
        } else {
            // 当前不是夜间模式，切换到夜间模式
            viewModel.setThemeById("night");
        }
    }
}
