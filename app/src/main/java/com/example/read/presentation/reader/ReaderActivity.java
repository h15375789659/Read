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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import com.example.read.domain.model.ParagraphInfo;
import com.example.read.domain.model.ReaderFont;
import com.example.read.domain.model.ReaderTheme;
import com.example.read.domain.model.TTSState;
import com.example.read.domain.model.TTSStatus;
import com.example.read.utils.NavigationHelper;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

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

    // 调试日志标签
    private static final String TAG = "ReaderChapterSwitch";

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
    private ImageButton btnSummaryManager;
    
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
    private androidx.lifecycle.Observer<ReaderUiState> searchDialogObserver; // 搜索对话框的观察者
    
    // 搜索悬浮导航栏
    private LinearLayout searchNavigationBar;
    private TextView searchNavCancel;
    private ImageButton searchNavPrevious;
    private TextView searchNavPosition;
    private ImageButton searchNavNext;
    private TextView searchNavReturn;
    
    // AI摘要对话框
    private androidx.appcompat.app.AlertDialog summaryDialog;
    
    // 摘要管理对话框
    private BottomSheetDialog summaryManagerDialog;
    private SummaryAdapter summaryAdapter;
    
    // TTS控制面板
    private TTSControlDialog ttsControlDialog;
    
    // TTS悬浮控制条
    private LinearLayout ttsMiniController;
    private ImageButton ttsMiniPlayPause;
    private ImageButton ttsMiniStop;
    private ImageButton ttsMiniLocate;
    private ImageButton ttsMiniExpand;
    
    // 分页相关
    private boolean isPaginationReady = false;
    private long lastPaginatedChapterId = -1;  // 上次分页的章节ID
    private String lastPaginatedContent = "";   // 上次分页的内容
    private String lastScrollModeContent = "";  // 上次滚动模式的内容（用于避免重复设置覆盖高亮）
    private boolean needRestorePosition = true; // 是否需要恢复阅读位置
    private int pendingPageIndex = -1;          // 待恢复的页码
    private int pendingJumpCharPosition = -1;   // 待跳转的字符位置（搜索跳转用）
    
    // 上次分页使用的显示设置（用于检测变化）
    private float lastPaginatedFontSize = -1;
    private float lastPaginatedLineSpacing = -1;
    private ReaderFont lastPaginatedFont = null;
    
    // 时间更新
    private Handler timeHandler;
    private Runnable timeUpdateRunnable;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
    // 电量监听
    private BroadcastReceiver batteryReceiver;
    private int currentBatteryLevel = -1;
    
    // 分页后台线程
    private ExecutorService paginationExecutor;
    private final AtomicInteger paginationTaskId = new AtomicInteger(0);  // 分页任务版本号，用于取消旧任务
    
    // 章节切换状态（提升为类成员变量，以便在分页完成回调中访问）
    private boolean isChapterChanging = false;           // 章节切换中标志
    private long lastChapterChangeTime = 0;              // 上次章节切换时间
    private Runnable pendingChapterChange = null;        // 待执行的章节切换任务
    private static final long CHAPTER_CHANGE_DEBOUNCE = 1200; // 章节切换防抖时间（毫秒）

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_reader);
        
        // 全屏模式必须在setContentView之后调用，否则getInsetsController()返回null
        enableFullScreen();
        
        // 初始化分页后台线程
        paginationExecutor = Executors.newSingleThreadExecutor();

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
        btnSummaryManager = findViewById(R.id.btn_summary_manager);
        
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
        
        // TTS悬浮控制条
        ttsMiniController = findViewById(R.id.tts_mini_controller);
        ttsMiniPlayPause = findViewById(R.id.tts_mini_play_pause);
        ttsMiniStop = findViewById(R.id.tts_mini_stop);
        ttsMiniLocate = findViewById(R.id.tts_mini_locate);
        ttsMiniExpand = findViewById(R.id.tts_mini_expand);
        
        // 搜索悬浮导航栏
        searchNavigationBar = findViewById(R.id.search_navigation_bar);
        searchNavCancel = findViewById(R.id.search_nav_cancel);
        searchNavPrevious = findViewById(R.id.search_nav_previous);
        searchNavPosition = findViewById(R.id.search_nav_position);
        searchNavNext = findViewById(R.id.search_nav_next);
        searchNavReturn = findViewById(R.id.search_nav_return);
        
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
                int totalPages = pageAdapter.getTotalPages();
                
                Log.d(TAG, "[点击翻页] onPreviousPage - current=" + current 
                        + ", currentChapterStart=" + currentChapterStart
                        + ", totalPages=" + totalPages
                        + ", isPaginationReady=" + isPaginationReady);
                
                if (current > 0) {
                    // 还有上一页（可能是当前章节的上一页，或者是预加载的上一章最后一页）
                    Log.d(TAG, "[点击翻页] 翻到上一页: " + (current - 1));
                    pageViewPager.setCurrentItem(current - 1, true);
                } else if (current == 0 && currentChapterStart == 0) {
                    // 已经在第一页且没有预加载的上一章页面，尝试切换到上一章
                    ReaderUiState state = viewModel.getUiState().getValue();
                    boolean canGoPrev = state != null && state.canGoPreviousChapter();
                    Log.d(TAG, "[点击翻页] 尝试切换上一章 - canGoPreviousChapter=" + canGoPrev
                            + ", currentChapterIndex=" + (state != null ? state.getCurrentChapterIndex() : -1));
                    if (canGoPrev) {
                        viewModel.goToPreviousChapter();
                        needRestorePosition = false;
                        pendingPageIndex = Integer.MAX_VALUE; // 上一章跳转到最后一页
                    } else {
                        Toast.makeText(ReaderActivity.this, R.string.reader_first_chapter, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "[点击翻页] 无法翻到上一页：current=" + current + ", currentChapterStart=" + currentChapterStart);
                }
            }

            @Override
            public void onNextPage() {
                int current = pageViewPager.getCurrentItem();
                int totalPages = pageAdapter.getTotalPages();
                int currentChapterEnd = pageAdapter.getCurrentChapterEndIndex();
                
                Log.d(TAG, "[点击翻页] onNextPage - current=" + current 
                        + ", totalPages=" + totalPages
                        + ", currentChapterEnd=" + currentChapterEnd
                        + ", isPaginationReady=" + isPaginationReady);
                
                if (current < totalPages - 1) {
                    // 还有下一页（可能是当前章节的下一页，或者是预加载的下一章第一页）
                    Log.d(TAG, "[点击翻页] 翻到下一页: " + (current + 1));
                    pageViewPager.setCurrentItem(current + 1, true);
                } else if (current == totalPages - 1 && current == currentChapterEnd) {
                    // 已经在最后一页且没有预加载的下一章页面，尝试切换到下一章
                    ReaderUiState state = viewModel.getUiState().getValue();
                    boolean canGoNext = state != null && state.canGoNextChapter();
                    Log.d(TAG, "[点击翻页] 尝试切换下一章 - canGoNextChapter=" + canGoNext
                            + ", currentChapterIndex=" + (state != null ? state.getCurrentChapterIndex() : -1)
                            + ", totalChapters=" + (state != null && state.getChapters() != null ? state.getChapters().size() : 0));
                    if (canGoNext) {
                        viewModel.goToNextChapter();
                        needRestorePosition = false;
                        pendingPageIndex = 0; // 下一章从第一页开始
                    } else {
                        Toast.makeText(ReaderActivity.this, R.string.reader_last_chapter, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "[点击翻页] 无法翻到下一页：current=" + current + ", totalPages=" + totalPages + ", currentChapterEnd=" + currentChapterEnd);
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
            if (child == null) {
                Log.w(TAG, "[滚动模式] ScrollView子视图为空，无法检测位置");
                return false;
            }
            
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
                Log.d(TAG, "[滚动模式] ACTION_DOWN - scrollY=" + scrollY + ", scrollViewHeight=" + scrollViewHeight 
                        + ", childHeight=" + childHeight + ", atTop=" + atTop + ", atBottom=" + atBottom);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                // 计算触摸时长和移动距离
                long touchDuration = System.currentTimeMillis() - touchDownTime[0];
                float moveDistance = Math.abs(event.getY() - touchDownY[0]);
                
                // 只有当触摸时间较短且有明显移动时，才认为是滑动操作
                // 单击操作通常时间短且移动距离小，不应触发章节切换
                boolean isSwipeGesture = moveDistance > 50 || touchDuration > 300;
                
                Log.d(TAG, "[滚动模式] ACTION_UP - moveDistance=" + moveDistance + ", touchDuration=" + touchDuration 
                        + ", isSwipeGesture=" + isSwipeGesture + ", startAtTop=" + isAtTop[0] + ", startAtBottom=" + isAtBottom[0]
                        + ", nowAtTop=" + atTop + ", nowAtBottom=" + atBottom);
                
                if (!isSwipeGesture) {
                    // 这是单击操作，不触发章节切换
                    Log.d(TAG, "[滚动模式] 判定为单击操作，不触发章节切换");
                    return false;
                }
                
                // 如果开始时在底部，结束时仍在底部，说明用户尝试继续下滑 -> 下一章
                if (isAtBottom[0] && atBottom) {
                    ReaderUiState state = viewModel.getUiState().getValue();
                    boolean canGoNext = state != null && state.canGoNextChapter();
                    Log.d(TAG, "[滚动模式] 尝试切换下一章 - canGoNextChapter=" + canGoNext 
                            + ", currentChapterIndex=" + (state != null ? state.getCurrentChapterIndex() : -1)
                            + ", totalChapters=" + (state != null && state.getChapters() != null ? state.getChapters().size() : 0));
                    if (canGoNext) {
                        viewModel.goToNextChapter();
                        needRestorePosition = false;
                        contentScrollView.scrollTo(0, 0);
                    }
                }
                
                // 如果开始时在顶部，结束时仍在顶部，说明用户尝试继续上滑 -> 上一章
                if (isAtTop[0] && atTop) {
                    ReaderUiState state = viewModel.getUiState().getValue();
                    boolean canGoPrev = state != null && state.canGoPreviousChapter();
                    Log.d(TAG, "[滚动模式] 尝试切换上一章 - canGoPreviousChapter=" + canGoPrev
                            + ", currentChapterIndex=" + (state != null ? state.getCurrentChapterIndex() : -1));
                    if (canGoPrev) {
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
        
        // 书签按钮 - 显示书签列表对话框
        btnBookmark.setOnClickListener(v -> showBookmarkListDialog());
        
        // 朗读按钮 - 显示TTS控制面板
        btnTts.setOnClickListener(v -> showTTSControlDialog());
        
        // 摘要管理按钮
        btnSummaryManager.setOnClickListener(v -> showSummaryManagerDialog());
        
        // TTS悬浮控制条按钮
        ttsMiniPlayPause.setOnClickListener(v -> {
            ReaderUiState state = viewModel.getUiState().getValue();
            if (state != null && state.getTtsState() != null) {
                if (state.getTtsState().isPlaying()) {
                    viewModel.pauseTTS();
                } else {
                    viewModel.resumeTTS();
                }
            }
        });
        
        ttsMiniStop.setOnClickListener(v -> viewModel.stopTTS());
        
        // 跳转到当前朗读位置按钮
        ttsMiniLocate.setOnClickListener(v -> jumpToTTSPosition());
        
        ttsMiniExpand.setOnClickListener(v -> showTTSControlDialog());
        
        // 搜索悬浮导航栏按钮（使用防抖方法）
        searchNavCancel.setOnClickListener(v -> {
            // 取消搜索：清除搜索状态和高亮，隐藏导航栏
            viewModel.clearSearchResults();
            hideSearchNavigationBar();
        });
        
        searchNavPrevious.setOnClickListener(v -> {
            navigateSearchWithDebounce(false);
        });
        
        searchNavNext.setOnClickListener(v -> {
            navigateSearchWithDebounce(true);
        });
        
        searchNavReturn.setOnClickListener(v -> {
            viewModel.returnToSavedPosition();
            hideSearchNavigationBar();
        });
        
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
            // 注意：isChapterChanging, lastChapterChangeTime, pendingChapterChange 已提升为类成员变量
            private int lastTriggeredPosition = -1; // 上次触发章节切换的位置
            
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                
                int totalPages = pageAdapter.getTotalPages();
                int currentChapterStart = pageAdapter.getCurrentChapterStartIndex();
                int currentChapterEnd = pageAdapter.getCurrentChapterEndIndex();
                int currentChapterPageCount = pageAdapter.getCurrentChapterPageCount();
                boolean isPrevChapterPage = pageAdapter.isPreviousChapterPage(position);
                boolean isNextChapterPage = pageAdapter.isNextChapterPage(position);
                
                Log.d(TAG, "[翻页模式] onPageSelected - position=" + position 
                        + ", totalPages=" + totalPages
                        + ", currentChapterStart=" + currentChapterStart
                        + ", currentChapterEnd=" + currentChapterEnd
                        + ", currentChapterPageCount=" + currentChapterPageCount
                        + ", isPrevChapterPage=" + isPrevChapterPage
                        + ", isNextChapterPage=" + isNextChapterPage
                        + ", isChapterChanging=" + isChapterChanging
                        + ", isPaginationReady=" + isPaginationReady);
                
                // 如果正在切换章节，忽略此回调
                if (isChapterChanging) {
                    Log.d(TAG, "[翻页模式] 忽略回调：正在切换章节中");
                    return;
                }
                
                // 防抖检查：如果距离上次章节切换时间太短，忽略
                long currentTime = System.currentTimeMillis();
                long timeSinceLastChange = currentTime - lastChapterChangeTime;
                if (timeSinceLastChange < CHAPTER_CHANGE_DEBOUNCE) {
                    Log.d(TAG, "[翻页模式] 忽略章节切换：防抖检查未通过，距上次切换=" + timeSinceLastChange + "ms");
                    return;
                }
                
                // 取消之前待执行的章节切换任务
                if (pendingChapterChange != null) {
                    pageViewPager.removeCallbacks(pendingChapterChange);
                    pendingChapterChange = null;
                }
                
                // 检测是否翻到上一章页面
                if (isPrevChapterPage) {
                    Log.d(TAG, "[翻页模式] 检测到翻到上一章页面，准备切换上一章");
                    // 翻到了上一章的最后一页，延迟触发章节切换
                    isChapterChanging = true;
                    lastTriggeredPosition = position;
                    pendingChapterChange = () -> {
                        // 再次检查防抖时间
                        if (System.currentTimeMillis() - lastChapterChangeTime < CHAPTER_CHANGE_DEBOUNCE) {
                            Log.d(TAG, "[翻页模式] 上一章切换取消：防抖检查未通过");
                            isChapterChanging = false;
                            pendingChapterChange = null;
                            return;
                        }
                        
                        // 检查当前位置是否仍然是上一章页面（用户可能已经翻回去了）
                        int currentPosition = pageViewPager.getCurrentItem();
                        if (!pageAdapter.isPreviousChapterPage(currentPosition)) {
                            Log.d(TAG, "[翻页模式] 上一章切换取消：用户已翻回当前章节");
                            isChapterChanging = false;
                            pendingChapterChange = null;
                            return;
                        }
                        
                        ReaderUiState uiState = viewModel.getUiState().getValue();
                        boolean canGoPrev = uiState != null && uiState.canGoPreviousChapter();
                        Log.d(TAG, "[翻页模式] 执行上一章切换 - canGoPreviousChapter=" + canGoPrev
                                + ", currentChapterIndex=" + (uiState != null ? uiState.getCurrentChapterIndex() : -1));
                        if (canGoPrev) {
                            lastChapterChangeTime = System.currentTimeMillis();
                            viewModel.goToPreviousChapter();
                            needRestorePosition = false;
                            pendingPageIndex = Integer.MAX_VALUE; // 上一章跳转到最后一页
                        }
                        // 延迟重置标志，等待新章节加载完成
                        pageViewPager.postDelayed(() -> {
                            Log.d(TAG, "[翻页模式] 重置isChapterChanging标志");
                            isChapterChanging = false;
                            pendingChapterChange = null;
                        }, 800);
                    };
                    pageViewPager.postDelayed(pendingChapterChange, 400); // 等待翻页动画完成
                }
                // 检测是否翻到下一章页面
                else if (isNextChapterPage) {
                    Log.d(TAG, "[翻页模式] 检测到翻到下一章页面，准备切换下一章");
                    // 翻到了下一章的第一页，延迟触发章节切换
                    isChapterChanging = true;
                    lastTriggeredPosition = position;
                    pendingChapterChange = () -> {
                        // 再次检查防抖时间
                        if (System.currentTimeMillis() - lastChapterChangeTime < CHAPTER_CHANGE_DEBOUNCE) {
                            Log.d(TAG, "[翻页模式] 下一章切换取消：防抖检查未通过");
                            isChapterChanging = false;
                            pendingChapterChange = null;
                            return;
                        }
                        
                        // 检查当前位置是否仍然是下一章页面（用户可能已经翻回去了）
                        int currentPosition = pageViewPager.getCurrentItem();
                        if (!pageAdapter.isNextChapterPage(currentPosition)) {
                            Log.d(TAG, "[翻页模式] 下一章切换取消：用户已翻回当前章节");
                            isChapterChanging = false;
                            pendingChapterChange = null;
                            return;
                        }
                        
                        ReaderUiState uiState = viewModel.getUiState().getValue();
                        boolean canGoNext = uiState != null && uiState.canGoNextChapter();
                        Log.d(TAG, "[翻页模式] 执行下一章切换 - canGoNextChapter=" + canGoNext
                                + ", currentChapterIndex=" + (uiState != null ? uiState.getCurrentChapterIndex() : -1)
                                + ", totalChapters=" + (uiState != null && uiState.getChapters() != null ? uiState.getChapters().size() : 0));
                        if (canGoNext) {
                            lastChapterChangeTime = System.currentTimeMillis();
                            viewModel.goToNextChapter();
                            needRestorePosition = false;
                            pendingPageIndex = 0; // 下一章从第一页开始
                        }
                        // 延迟重置标志，等待新章节加载完成
                        pageViewPager.postDelayed(() -> {
                            Log.d(TAG, "[翻页模式] 重置isChapterChanging标志");
                            isChapterChanging = false;
                            pendingChapterChange = null;
                        }, 800);
                    };
                    pageViewPager.postDelayed(pendingChapterChange, 400); // 等待翻页动画完成
                }
            }
            
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
            
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                // 当用户开始拖动时，检查是否应该取消待执行的章节切换
                if (state == ViewPager2.SCROLL_STATE_DRAGGING && pendingChapterChange != null) {
                    // 检查当前是否仍在相邻章节页面
                    int currentPosition = pageViewPager.getCurrentItem();
                    boolean stillInAdjacentChapter = pageAdapter.isPreviousChapterPage(currentPosition) 
                            || pageAdapter.isNextChapterPage(currentPosition);
                    
                    if (!stillInAdjacentChapter) {
                        // 用户已翻回当前章节，取消章节切换
                        Log.d(TAG, "[翻页模式] 用户拖动，取消章节切换任务");
                        pageViewPager.removeCallbacks(pendingChapterChange);
                        pendingChapterChange = null;
                        isChapterChanging = false;
                    } else {
                        Log.d(TAG, "[翻页模式] 用户在相邻章节页面拖动，保持章节切换任务");
                    }
                }
                
                // 关键修复：当用户停止滑动（IDLE状态）时，检查页面位置是否正确
                // 如果用户停在相邻章节的预览页面上，但分页已完成，需要确保状态一致
                if (state == ViewPager2.SCROLL_STATE_IDLE && isPaginationReady && !isChapterChanging) {
                    int currentPosition = pageViewPager.getCurrentItem();
                    boolean isOnPrevChapterPage = pageAdapter.isPreviousChapterPage(currentPosition);
                    boolean isOnNextChapterPage = pageAdapter.isNextChapterPage(currentPosition);
                    
                    // 如果停在相邻章节页面上，但没有待执行的章节切换任务
                    // 说明之前的章节切换被取消了，需要检查是否应该重新触发
                    if ((isOnPrevChapterPage || isOnNextChapterPage) && pendingChapterChange == null) {
                        Log.d(TAG, "[翻页模式] IDLE状态检测到停在相邻章节页面，position=" + currentPosition 
                                + ", isPrev=" + isOnPrevChapterPage + ", isNext=" + isOnNextChapterPage
                                + ", 距上次切换=" + (System.currentTimeMillis() - lastChapterChangeTime) + "ms");
                        
                        // 检查防抖时间是否已过
                        if (System.currentTimeMillis() - lastChapterChangeTime >= CHAPTER_CHANGE_DEBOUNCE) {
                            // 防抖时间已过，可以触发章节切换
                            if (isOnPrevChapterPage) {
                                ReaderUiState uiState = viewModel.getUiState().getValue();
                                if (uiState != null && uiState.canGoPreviousChapter()) {
                                    Log.d(TAG, "[翻页模式] IDLE触发上一章切换");
                                    isChapterChanging = true;
                                    lastChapterChangeTime = System.currentTimeMillis();
                                    viewModel.goToPreviousChapter();
                                    needRestorePosition = false;
                                    pendingPageIndex = Integer.MAX_VALUE;
                                }
                            } else if (isOnNextChapterPage) {
                                ReaderUiState uiState = viewModel.getUiState().getValue();
                                if (uiState != null && uiState.canGoNextChapter()) {
                                    Log.d(TAG, "[翻页模式] IDLE触发下一章切换");
                                    isChapterChanging = true;
                                    lastChapterChangeTime = System.currentTimeMillis();
                                    viewModel.goToNextChapter();
                                    needRestorePosition = false;
                                    pendingPageIndex = 0;
                                }
                            }
                        }
                    }
                }
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
        
        // 处理书签添加成功
        if (state.isBookmarkAdded()) {
            Toast.makeText(this, R.string.bookmark_added, Toast.LENGTH_SHORT).show();
            viewModel.clearBookmarkAddedState();
        }
        
        // 处理书签删除成功
        if (state.isBookmarkDeleted()) {
            Toast.makeText(this, R.string.bookmark_deleted, Toast.LENGTH_SHORT).show();
            viewModel.clearBookmarkDeletedState();
        }
        
        // 处理书签跳转位置
        if (state.getJumpToPosition() >= 0) {
            final int jumpPosition = state.getJumpToPosition();
            // 先清除跳转位置状态，避免重复处理
            viewModel.clearJumpToPosition();
            
            if (currentPageMode == PageMode.SCROLL) {
                // 滚动模式：jumpPosition 是字符位置，需要计算对应的滚动位置
                String content = state.getDisplayContent();
                if (content != null && !content.isEmpty() && jumpPosition >= 0) {
                    // 根据字符位置计算滚动比例
                    float ratio = (float) jumpPosition / content.length();
                    ratio = Math.max(0f, Math.min(1f, ratio)); // 限制在0-1范围内
                    final float finalRatio = ratio;
                    contentScrollView.post(() -> {
                        View child = contentScrollView.getChildAt(0);
                        if (child != null) {
                            int targetScroll = (int) (child.getHeight() * finalRatio);
                            contentScrollView.scrollTo(0, targetScroll);
                        }
                    });
                }
            } else {
                // 翻页模式：需要根据字符位置找到对应的页面
                if (isPaginationReady && currentPages != null && !currentPages.isEmpty()) {
                    // 分页已完成，根据字符位置找到对应页面
                    int targetPageInChapter = findPageForPosition(jumpPosition);
                    int startIndex = pageAdapter.getCurrentChapterStartIndex();
                    int targetPage = startIndex + targetPageInChapter;
                    pageViewPager.setCurrentItem(targetPage, false);
                } else {
                    // 分页未完成，保存字符位置，在分页完成后处理
                    // 使用负数标记这是字符位置而不是页码
                    pendingJumpCharPosition = jumpPosition;
                }
            }
        }
        
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
            // 只有当内容真正改变时才重新设置文本，避免覆盖TTS高亮
            String currentText = chapterContentText.getText().toString();
            // 比较时需要去除可能的Spannable格式
            if (!content.equals(currentText) && !content.equals(lastScrollModeContent)) {
                // 检查是否有搜索关键词需要高亮
                String searchKeyword = state.getSearchKeyword();
                if (searchKeyword != null && !searchKeyword.isEmpty()) {
                    // 应用搜索关键词高亮
                    chapterContentText.setText(highlightSearchKeyword(content, searchKeyword));
                } else {
                    chapterContentText.setText(content);
                }
                lastScrollModeContent = content;
                // 内容改变后，重新应用TTS高亮（如果正在播放）
                TTSState ttsState = state.getTtsState();
                if (ttsState != null && ttsState.isPlaying()) {
                    // 延迟一帧后重新应用高亮，确保文本已设置完成
                    chapterContentText.post(() -> updateScrollModeHighlight(ttsState.getCurrentPosition()));
                }
            }
            
            // 更新翻页模式的搜索关键词高亮
            String searchKeyword = state.getSearchKeyword();
            pageAdapter.setSearchKeyword(searchKeyword != null ? searchKeyword : "");
            chapterAdapter.setCurrentChapterId(currentChapter.getId());
            
            // 处理待跳转的TTS位置（章节切换后的跳转）
            if (pendingTTSJumpPosition >= 0 && pendingTTSJumpChapterId == currentChapter.getId()) {
                final int jumpPos = pendingTTSJumpPosition;
                pendingTTSJumpPosition = -1;
                pendingTTSJumpChapterId = -1;
                // 延迟执行跳转，确保分页完成
                if (currentPageMode == PageMode.SCROLL) {
                    contentScrollView.post(() -> performTTSJump(jumpPos));
                } else {
                    // 翻页模式需要等待分页完成
                    pageViewPager.postDelayed(() -> performTTSJump(jumpPos), 300);
                }
            }
            
            // 恢复上下滚动模式的位置
            if (currentPageMode == PageMode.SCROLL && needRestorePosition && state.getSavedScrollPosition() > 0) {
                contentScrollView.post(() -> {
                    contentScrollView.scrollTo(0, state.getSavedScrollPosition());
                });
                needRestorePosition = false;
            }
            
            // 左右翻页模式 - 检查是否需要重新分页
            if (currentPageMode == PageMode.PAGE && !state.isLoading()) {
                // 检查是否需要重新分页（章节改变、内容改变、字体/字号/行间距改变）
                boolean needRepaginate = lastPaginatedChapterId != currentChapter.getId() 
                        || !content.equals(lastPaginatedContent)
                        || lastPaginatedFontSize != state.getFontSize()
                        || lastPaginatedLineSpacing != state.getLineSpacing()
                        || lastPaginatedFont != state.getFont();
                        
                Log.d(TAG, "[updateUI] 翻页模式检查 - chapterId=" + currentChapter.getId()
                        + ", lastPaginatedChapterId=" + lastPaginatedChapterId
                        + ", needRepaginate=" + needRepaginate
                        + ", isPaginationReady=" + isPaginationReady
                        + ", hasPrevChapter=" + (state.getPreviousChapter() != null)
                        + ", hasNextChapter=" + (state.getNextChapter() != null)
                        + ", prevContentLength=" + (state.getPreviousChapterContent() != null ? state.getPreviousChapterContent().length() : 0)
                        + ", nextContentLength=" + (state.getNextChapterContent() != null ? state.getNextChapterContent().length() : 0));
                        
                if (needRepaginate) {
                    isPaginationReady = false; // 重置分页状态
                    lastPaginatedChapterId = currentChapter.getId();
                    lastPaginatedContent = content;
                    lastPaginatedFontSize = state.getFontSize();
                    lastPaginatedLineSpacing = state.getLineSpacing();
                    lastPaginatedFont = state.getFont();
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
     * 包含上一章最后一页、当前章节所有页面、下一章第一页
     * 
     * 优化：将耗时的分页计算移到后台线程，避免 ANR
     */
    private void paginateContent(String content, String title, ReaderUiState state) {
        if (content == null || content.isEmpty()) return;
        
        // 增加任务版本号，取消之前的分页任务
        final int currentTaskId = paginationTaskId.incrementAndGet();
        
        // 显示加载状态
        loadingProgress.setVisibility(View.VISIBLE);
        
        // 延迟执行以确保视图已测量
        pageViewPager.post(() -> {
            final int width = pageViewPager.getWidth();
            final int height = pageViewPager.getHeight();
            
            if (width <= 0 || height <= 0) {
                loadingProgress.setVisibility(View.GONE);
                return;
            }
            
            // 检查 Activity 是否已销毁
            if (isFinishing() || isDestroyed() || paginationExecutor == null) {
                loadingProgress.setVisibility(View.GONE);
                return;
            }
            
            // 获取当前字体
            final Typeface currentTypeface;
            ReaderFont font = state.getFont();
            if (font != null && font.getFontPath() != null) {
                Typeface tempTypeface;
                try {
                    tempTypeface = Typeface.createFromAsset(getAssets(), font.getFontPath());
                } catch (Exception e) {
                    tempTypeface = Typeface.DEFAULT;
                }
                currentTypeface = tempTypeface;
            } else {
                currentTypeface = Typeface.DEFAULT;
            }
            
            // 获取显示设置
            final float fontSize = state.getFontSize();
            final float lineSpacing = state.getLineSpacing();
            final float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
            final float density = getResources().getDisplayMetrics().density;
            
            // 获取相邻章节信息
            final String prevChapterContent = state.getPreviousChapterContent();
            final String prevChapterTitle = state.getPreviousChapter() != null ? state.getPreviousChapter().getTitle() : null;
            final String nextChapterContent = state.getNextChapterContent();
            final String nextChapterTitle = state.getNextChapter() != null ? state.getNextChapter().getTitle() : null;
            final long currentChapterId = state.getCurrentChapter() != null ? state.getCurrentChapter().getId() : 0;
            final int savedPageIndex = state.getSavedPageIndex();
            
            // 在后台线程执行分页计算
            paginationExecutor.execute(() -> {
                // 检查任务是否已被取消
                if (currentTaskId != paginationTaskId.get()) {
                    Log.d(TAG, "[分页] 任务已取消: taskId=" + currentTaskId + ", currentTaskId=" + paginationTaskId.get());
                    return;
                }
                
                // 检查 Activity 是否已销毁
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                
                try {
                    // 创建文本画笔（使用与PageContentView相同的字体）
                    TextPaint textPaint = new TextPaint();
                    textPaint.setTextSize(fontSize * scaledDensity);
                    textPaint.setAntiAlias(true);
                    textPaint.setTypeface(currentTypeface);
                    
                    // 创建标题画笔
                    TextPaint titlePaint = new TextPaint();
                    titlePaint.setTextSize((fontSize + 4) * scaledDensity);
                    titlePaint.setTypeface(Typeface.create(currentTypeface, Typeface.BOLD));
                    titlePaint.setAntiAlias(true);
                    
                    // 计算可用区域（减去内边距）
                    int contentWidth = width - (int)(64 * density);  // 左右各32dp
                    int contentHeight = height - (int)(64 * density);  // 上28dp + 下36dp
                    contentHeight -= (int)(4 * density);  // 留出余量
                    
                    // 计算标题高度
                    int titleHeight = (int) TextPaginator.calculateTitleHeight(title, titlePaint, contentWidth, 
                            lineSpacing, scaledDensity);
                    
                    // 再次检查任务是否已被取消
                    if (currentTaskId != paginationTaskId.get() || isFinishing() || isDestroyed()) {
                        return;
                    }
                    
                    // 分页当前章节
                    final List<TextPaginator.PageInfo> pages = TextPaginator.paginate(content, textPaint, contentWidth, contentHeight, 
                            lineSpacing, titleHeight);
                    
                    // 再次检查任务是否已被取消
                    if (currentTaskId != paginationTaskId.get() || isFinishing() || isDestroyed()) {
                        return;
                    }
                    
                    // 获取上一章最后一页（如果有）
                    TextPaginator.PageInfo prevLastPage = null;
                    if (prevChapterContent != null && !prevChapterContent.isEmpty() && prevChapterTitle != null) {
                        int prevTitleHeight = (int) TextPaginator.calculateTitleHeight(prevChapterTitle, titlePaint, contentWidth, 
                                lineSpacing, scaledDensity);
                        List<TextPaginator.PageInfo> prevPages = TextPaginator.paginate(
                                prevChapterContent, textPaint, contentWidth, contentHeight, 
                                lineSpacing, prevTitleHeight);
                        if (!prevPages.isEmpty()) {
                            prevLastPage = prevPages.get(prevPages.size() - 1);
                        }
                    }
                    
                    // 再次检查任务是否已被取消
                    if (currentTaskId != paginationTaskId.get() || isFinishing() || isDestroyed()) {
                        return;
                    }
                    
                    // 获取下一章第一页（如果有）
                    TextPaginator.PageInfo nextFirstPage = null;
                    if (nextChapterContent != null && !nextChapterContent.isEmpty() && nextChapterTitle != null) {
                        int nextTitleHeight = (int) TextPaginator.calculateTitleHeight(nextChapterTitle, titlePaint, contentWidth, 
                                lineSpacing, scaledDensity);
                        List<TextPaginator.PageInfo> nextPages = TextPaginator.paginate(
                                nextChapterContent, textPaint, contentWidth, contentHeight, 
                                lineSpacing, nextTitleHeight);
                        if (!nextPages.isEmpty()) {
                            nextFirstPage = nextPages.get(0);
                        }
                    }
                    
                    // 最终检查任务是否已被取消
                    if (currentTaskId != paginationTaskId.get() || isFinishing() || isDestroyed()) {
                        return;
                    }
                    
                    // 保存结果用于主线程
                    final TextPaginator.PageInfo finalPrevLastPage = prevLastPage;
                    final TextPaginator.PageInfo finalNextFirstPage = nextFirstPage;
                    
                    // 在主线程更新 UI
                    runOnUiThread(() -> {
                        // 再次检查任务是否已被取消
                        if (currentTaskId != paginationTaskId.get() || isFinishing() || isDestroyed()) {
                            return;
                        }
                        
                        // 隐藏加载状态
                        loadingProgress.setVisibility(View.GONE);
                        
                        // 更新当前页面列表
                        currentPages = pages;
                        
                        // 设置页面数据（包含相邻章节的首尾页）
                        pageAdapter.setPages(pages, title, currentChapterId, finalPrevLastPage, prevChapterTitle, finalNextFirstPage, nextChapterTitle);
                        
                        // 设置左上角章节名
                        pageAdapter.setStatusChapterName(title);
                        
                        // 恢复阅读位置（需要考虑上一章页面的偏移）
                        int currentChapterStartIndex = pageAdapter.getCurrentChapterStartIndex();
                        int targetPage = currentChapterStartIndex; // 默认跳转到当前章节第一页
                        
                        Log.d(TAG, "[分页完成] 当前章节页数=" + pages.size() 
                                + ", 总页数=" + pageAdapter.getTotalPages()
                                + ", currentChapterStartIndex=" + currentChapterStartIndex
                                + ", currentChapterEndIndex=" + pageAdapter.getCurrentChapterEndIndex()
                                + ", hasPrevPage=" + (finalPrevLastPage != null)
                                + ", hasNextPage=" + (finalNextFirstPage != null)
                                + ", pendingPageIndex=" + pendingPageIndex
                                + ", needRestorePosition=" + needRestorePosition
                                + ", savedPageIndex=" + savedPageIndex);
                        
                        if (pendingPageIndex == Integer.MAX_VALUE) {
                            // 特殊值：跳转到当前章节最后一页（从下一章跳转过来）
                            targetPage = pageAdapter.getCurrentChapterEndIndex();
                            Log.d(TAG, "[分页完成] 跳转到最后一页: " + targetPage);
                            pendingPageIndex = -1;
                        } else if (pendingPageIndex >= 0) {
                            // 有待恢复的页码（章节切换时设置）
                            targetPage = currentChapterStartIndex + Math.min(pendingPageIndex, pages.size() - 1);
                            Log.d(TAG, "[分页完成] 恢复待定页码: " + targetPage);
                            pendingPageIndex = -1;
                        } else if (needRestorePosition) {
                            // 首次加载，恢复保存的位置
                            targetPage = currentChapterStartIndex + Math.min(savedPageIndex, pages.size() - 1);
                            Log.d(TAG, "[分页完成] 首次加载恢复位置: " + targetPage);
                            needRestorePosition = false;
                        } else if (pendingJumpCharPosition >= 0) {
                            // 有待跳转的字符位置（搜索跳转）
                            int targetPageInChapter = findPageForPosition(pendingJumpCharPosition);
                            targetPage = currentChapterStartIndex + targetPageInChapter;
                            Log.d(TAG, "[分页完成] 搜索跳转到字符位置: " + pendingJumpCharPosition + ", 目标页: " + targetPage);
                            pendingJumpCharPosition = -1;
                        } else {
                            // 不需要恢复位置，保持当前页面
                            Log.d(TAG, "[分页完成] 保持当前页面，不跳转");
                            isPaginationReady = true;
                            // 分页完成，重置章节切换标志
                            isChapterChanging = false;
                            return;
                        }
                        
                        pageViewPager.setCurrentItem(targetPage, false);
                        
                        isPaginationReady = true;
                        // 分页完成，重置章节切换标志（关键修复：不再等待800ms延迟）
                        isChapterChanging = false;
                        Log.d(TAG, "[分页完成] isPaginationReady=true, isChapterChanging=false, 当前页=" + targetPage);
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "[分页] 分页计算异常", e);
                    // 在主线程隐藏加载状态
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            loadingProgress.setVisibility(View.GONE);
                        }
                    });
                }
            });
        });
    }
    
    /**
     * 保存当前页码用于重新分页后恢复
     * 在字体、字号、行间距变化时调用
     */
    private void saveCurrentPageIndexForRepagination() {
        if (pageAdapter.getCurrentChapterPageCount() > 0) {
            int currentItem = pageViewPager.getCurrentItem();
            int startIndex = pageAdapter.getCurrentChapterStartIndex();
            // 计算当前在章节中的页码（相对于章节起始）
            int currentPageInChapter = currentItem - startIndex;
            if (currentPageInChapter >= 0) {
                pendingPageIndex = currentPageInChapter;
            } else {
                pendingPageIndex = 0;
            }
        }
    }
    
    /**
     * 根据字符位置找到对应的页面索引（在当前章节内）
     * 用于搜索跳转功能
     * 
     * @param charPosition 字符位置（在原始文本中的位置）
     * @return 页面索引（相对于当前章节，从0开始）
     */
    private int findPageForPosition(int charPosition) {
        if (currentPages == null || currentPages.isEmpty()) {
            return 0;
        }
        
        // 遍历所有页面，找到包含该字符位置的页面
        for (int i = 0; i < currentPages.size(); i++) {
            TextPaginator.PageInfo page = currentPages.get(i);
            // 使用原始文本位置进行匹配
            int pageStart = page.getOriginalStartIndex();
            int pageEnd = page.getOriginalEndIndex();
            
            if (charPosition >= pageStart && charPosition < pageEnd) {
                Log.d(TAG, "[findPageForPosition] 找到页面: " + i 
                        + ", charPosition=" + charPosition 
                        + ", pageStart=" + pageStart 
                        + ", pageEnd=" + pageEnd);
                return i;
            }
        }
        
        // 如果没找到，返回最后一页（可能字符位置超出范围）
        Log.d(TAG, "[findPageForPosition] 未找到匹配页面，返回最后一页: " + (currentPages.size() - 1)
                + ", charPosition=" + charPosition);
        return currentPages.size() - 1;
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

    // 当前工具栏显示状态，用于避免重复更新
    private boolean currentToolbarVisible = false;
    
    /**
     * 更新工具栏显示状态
     */
    private void updateToolbarVisibility(boolean show) {
        // 避免重复更新导致闪动
        if (show == currentToolbarVisible) {
            return;
        }
        currentToolbarVisible = show;
        
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
     * 更新TTS按钮状态和悬浮控制条
     */
    private void updateTTSButton(ReaderUiState state) {
        TTSState ttsState = state.getTtsState();
        boolean isActive = ttsState != null && (ttsState.isPlaying() || ttsState.isPaused());
        
        // 更新顶部工具栏TTS按钮图标
        if (state.isTTSPlaying()) {
            btnTts.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnTts.setImageResource(android.R.drawable.ic_btn_speak_now);
        }
        
        // 更新悬浮控制条显示状态
        if (isActive) {
            ttsMiniController.setVisibility(View.VISIBLE);
            // 更新播放/暂停按钮图标
            if (ttsState.isPlaying()) {
                ttsMiniPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                ttsMiniPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
        } else {
            ttsMiniController.setVisibility(View.GONE);
        }
        
        // 更新TTS控制面板状态（如果已打开）
        if (ttsControlDialog != null && ttsControlDialog.isAdded()) {
            ttsControlDialog.setTTSState(ttsState);
            
            // 更新朗读位置高亮
            if (ttsState != null && ttsState.isPlaying()) {
                ttsControlDialog.updateReadingPosition(ttsState.getCurrentPosition());
            } else if (ttsState == null || ttsState.isIdle()) {
                ttsControlDialog.clearReadingHighlight();
            }
        }
        
        // 更新主阅读界面的TTS高亮
        updateTTSHighlight(ttsState);
    }
    
    /**
     * 更新主阅读界面的TTS高亮
     */
    private void updateTTSHighlight(TTSState ttsState) {
        if (ttsState == null || !ttsState.isPlaying()) {
            // TTS未播放，清除高亮
            if (currentPageMode == PageMode.SCROLL) {
                clearScrollModeHighlight();
            } else {
                pageAdapter.clearTTSHighlight();
            }
            // 重置自动翻页记录
            lastTTSAutoPageIndex = -1;
            return;
        }
        
        // 检查当前章节是否与TTS朗读的章节一致
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state != null && state.getCurrentChapter() != null) {
            long currentChapterId = state.getCurrentChapter().getId();
            long ttsChapterId = ttsState.getCurrentChapterId();
            if (ttsChapterId > 0 && currentChapterId != ttsChapterId) {
                // 当前章节与TTS章节不一致，不显示高亮
                if (currentPageMode == PageMode.SCROLL) {
                    clearScrollModeHighlight();
                } else {
                    pageAdapter.clearTTSHighlight();
                }
                // 重置自动翻页记录
                lastTTSAutoPageIndex = -1;
                return;
            }
        }
        
        int position = ttsState.getCurrentPosition();
        
        if (currentPageMode == PageMode.SCROLL) {
            // 滚动模式：使用Spannable高亮
            updateScrollModeHighlight(position);
        } else {
            // 翻页模式：更新PageAdapter高亮并自动翻页
            pageAdapter.setTTSHighlightPosition(position);
            // 自动翻页到TTS当前位置所在的页面
            autoScrollToTTSPage(position);
        }
    }
    
    // 上次TTS自动翻页的目标页码，用于避免重复翻页
    private int lastTTSAutoPageIndex = -1;
    
    /**
     * 翻页模式下自动翻到TTS当前位置所在的页面
     * @param ttsPosition TTS在章节中的当前位置
     */
    private void autoScrollToTTSPage(int ttsPosition) {
        if (currentPages == null || currentPages.isEmpty()) {
            return;
        }
        
        // 根据TTS位置找到对应的页码
        int targetPageIndex = -1;
        for (int i = 0; i < currentPages.size(); i++) {
            TextPaginator.PageInfo page = currentPages.get(i);
            // 使用原始文本位置进行匹配
            int originalStart = page.getOriginalStartIndex();
            int originalEnd = page.getOriginalEndIndex();
            if (ttsPosition >= originalStart && ttsPosition < originalEnd) {
                targetPageIndex = i;
                break;
            }
        }
        
        if (targetPageIndex < 0) {
            return;
        }
        
        // 如果目标页码与上次相同，不需要翻页
        if (targetPageIndex == lastTTSAutoPageIndex) {
            return;
        }
        
        lastTTSAutoPageIndex = targetPageIndex;
        
        // 计算在ViewPager2中的实际位置（考虑上一章预加载页面）
        int startIndex = pageAdapter.getCurrentChapterStartIndex();
        int targetPage = startIndex + targetPageIndex;
        
        // 获取当前页码
        int currentPage = pageViewPager.getCurrentItem();
        
        // 如果当前页码与目标页码不同，自动翻页
        if (currentPage != targetPage) {
            // 使用平滑滚动翻页
            pageViewPager.setCurrentItem(targetPage, true);
        }
    }
    
    // 上次高亮的段落范围，用于避免重复更新
    private int lastHighlightStart = -1;
    private int lastHighlightEnd = -1;
    
    /**
     * 更新滚动模式的TTS高亮
     */
    private void updateScrollModeHighlight(int position) {
        String content = chapterContentText.getText().toString();
        if (content.isEmpty() || position < 0 || position >= content.length()) {
            clearScrollModeHighlight();
            return;
        }
        
        // 找到当前段落的起始和结束位置
        int paragraphStart = findParagraphStart(content, position);
        int paragraphEnd = findParagraphEnd(content, position);
        
        // 如果段落范围没有变化，不需要更新
        if (paragraphStart == lastHighlightStart && paragraphEnd == lastHighlightEnd) {
            return;
        }
        
        lastHighlightStart = paragraphStart;
        lastHighlightEnd = paragraphEnd;
        
        // 使用Spannable设置高亮背景
        android.text.SpannableString spannableContent = new android.text.SpannableString(content);
        
        // 添加高亮背景
        spannableContent.setSpan(
                new android.text.style.BackgroundColorSpan(0x4000BCD4), // 半透明青色
                paragraphStart,
                paragraphEnd,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        
        chapterContentText.setText(spannableContent);
        
        // 自动滚动到高亮位置
        scrollToHighlight(paragraphStart);
    }
    
    /**
     * 清除滚动模式的高亮
     */
    private void clearScrollModeHighlight() {
        if (lastHighlightStart >= 0 || lastHighlightEnd >= 0) {
            lastHighlightStart = -1;
            lastHighlightEnd = -1;
            
            // 恢复原始文本（移除Spannable）
            ReaderUiState state = viewModel.getUiState().getValue();
            if (state != null && state.getDisplayContent() != null) {
                // 检查是否有搜索关键词需要保留高亮
                String searchKeyword = state.getSearchKeyword();
                if (searchKeyword != null && !searchKeyword.isEmpty()) {
                    chapterContentText.setText(highlightSearchKeyword(state.getDisplayContent(), searchKeyword));
                } else {
                    chapterContentText.setText(state.getDisplayContent());
                }
            }
        }
    }
    
    /**
     * 高亮搜索关键词（用于滚动模式）
     * @param content 文本内容
     * @param keyword 搜索关键词
     * @return 带高亮的 SpannableString
     */
    private android.text.SpannableString highlightSearchKeyword(String content, String keyword) {
        android.text.SpannableString spannable = new android.text.SpannableString(content);
        
        if (keyword == null || keyword.isEmpty()) {
            return spannable;
        }
        
        String lowerContent = content.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        
        int start = 0;
        while ((start = lowerContent.indexOf(lowerKeyword, start)) != -1) {
            int end = start + keyword.length();
            
            // 设置黄色背景高亮
            spannable.setSpan(
                    new android.text.style.BackgroundColorSpan(0xFFFFD54F), // 黄色
                    start,
                    end,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            
            start = end;
        }
        
        return spannable;
    }
    
    /**
     * 自动滚动到高亮位置
     */
    private void scrollToHighlight(int charPosition) {
        // 估算字符位置对应的滚动位置
        android.text.Layout layout = chapterContentText.getLayout();
        if (layout != null) {
            int line = layout.getLineForOffset(charPosition);
            int lineTop = layout.getLineTop(line);
            
            // 获取当前滚动位置
            int currentScrollY = contentScrollView.getScrollY();
            int viewHeight = contentScrollView.getHeight();
            
            // 如果高亮位置不在可见区域内，滚动到该位置
            if (lineTop < currentScrollY || lineTop > currentScrollY + viewHeight - 100) {
                // 滚动到高亮位置，留出一些上边距
                int targetScrollY = Math.max(0, lineTop - 100);
                contentScrollView.smoothScrollTo(0, targetScrollY);
            }
        }
    }
    
    /**
     * 查找段落起始位置
     */
    private int findParagraphStart(String text, int position) {
        if (position <= 0) return 0;
        
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
        
        for (int i = position; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                return i;
            }
        }
        return text.length();
    }
    
    /**
     * 跳转到当前TTS朗读位置
     */
    private void jumpToTTSPosition() {
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state == null || state.getTtsState() == null) return;
        
        TTSState ttsState = state.getTtsState();
        int ttsPosition = ttsState.getCurrentPosition();
        long ttsChapterId = ttsState.getCurrentChapterId();
        
        if (ttsPosition < 0 || ttsChapterId <= 0) return;
        
        // 检查当前章节是否与TTS朗读的章节一致
        Chapter currentChapter = state.getCurrentChapter();
        if (currentChapter == null || currentChapter.getId() != ttsChapterId) {
            // 需要先切换到TTS正在朗读的章节
            // 设置待跳转的TTS位置，在章节加载完成后处理
            pendingTTSJumpPosition = ttsPosition;
            pendingTTSJumpChapterId = ttsChapterId;
            viewModel.loadChapter(ttsChapterId);
            Toast.makeText(this, "正在跳转到朗读章节...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 当前章节就是TTS章节，直接跳转
        performTTSJump(ttsPosition);
    }
    
    // 待跳转的TTS位置（用于章节切换后的跳转）
    private int pendingTTSJumpPosition = -1;
    private long pendingTTSJumpChapterId = -1;
    
    /**
     * 执行TTS位置跳转
     */
    private void performTTSJump(int ttsPosition) {
        if (currentPageMode == PageMode.SCROLL) {
            // 滚动模式：滚动到对应位置
            scrollToHighlight(ttsPosition);
        } else {
            // 翻页模式：计算TTS位置对应的页码并跳转（使用原始文本位置）
            if (currentPages != null && !currentPages.isEmpty()) {
                int targetPageIndex = -1;
                for (int i = 0; i < currentPages.size(); i++) {
                    TextPaginator.PageInfo page = currentPages.get(i);
                    // 使用原始文本位置进行匹配
                    int originalStart = page.getOriginalStartIndex();
                    int originalEnd = page.getOriginalEndIndex();
                    if (ttsPosition >= originalStart && ttsPosition < originalEnd) {
                        targetPageIndex = i;
                        break;
                    }
                }
                
                if (targetPageIndex >= 0) {
                    int startIndex = pageAdapter.getCurrentChapterStartIndex();
                    int targetPage = startIndex + targetPageIndex;
                    pageViewPager.setCurrentItem(targetPage, true);
                }
            }
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
                pendingPageIndex = 0; // 上一章跳转到首页
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
            
            // 绑定快速滚动条
            com.example.read.presentation.widget.FastScrollerView fastScroller = 
                    view.findViewById(R.id.fast_scroller);
            if (fastScroller != null) {
                fastScroller.attachToRecyclerView(recyclerView);
            }
            
            // 标题栏和搜索栏
            LinearLayout titleBar = view.findViewById(R.id.title_bar);
            LinearLayout searchBar = view.findViewById(R.id.search_bar);
            ImageButton btnSearchChapter = view.findViewById(R.id.btn_search_chapter);
            ImageButton btnSearchBack = view.findViewById(R.id.btn_search_back);
            ImageButton btnClearSearch = view.findViewById(R.id.btn_clear_search);
            EditText searchEditText = view.findViewById(R.id.search_edit_text);
            TextView searchResultHint = view.findViewById(R.id.search_result_hint);
            TextView chapterCountText = view.findViewById(R.id.chapter_count_text);
            
            // 设置摘要按钮点击监听
            chapterAdapter.setOnSummaryClickListener((chapter, hasSummary) -> {
                showSummaryDialog(chapter, hasSummary);
            });
            
            // 设置章节数量
            ReaderUiState state = viewModel.getUiState().getValue();
            if (state != null && state.getChapters() != null) {
                chapterCountText.setText(getString(R.string.reader_chapter_count, state.getChapters().size()));
                // 设置原始章节列表用于过滤
                chapterAdapter.setOriginalList(state.getChapters());
                // 异步加载已有摘要的章节
                viewModel.loadChaptersWithSummary(chaptersWithSummary -> {
                    chapterAdapter.setChaptersWithSummary(chaptersWithSummary);
                });
            }
            
            // 搜索按钮点击 - 显示搜索栏
            btnSearchChapter.setOnClickListener(v -> {
                titleBar.setVisibility(View.GONE);
                searchBar.setVisibility(View.VISIBLE);
                searchEditText.requestFocus();
                showKeyboard(searchEditText);
            });
            
            // 搜索返回按钮 - 隐藏搜索栏
            btnSearchBack.setOnClickListener(v -> {
                searchBar.setVisibility(View.GONE);
                titleBar.setVisibility(View.VISIBLE);
                searchEditText.setText("");
                searchResultHint.setVisibility(View.GONE);
                btnClearSearch.setVisibility(View.GONE);
                chapterAdapter.clearFilter();
                hideKeyboard(searchEditText);
            });
            
            // 清除搜索按钮
            btnClearSearch.setOnClickListener(v -> {
                searchEditText.setText("");
                searchResultHint.setVisibility(View.GONE);
                btnClearSearch.setVisibility(View.GONE);
                chapterAdapter.clearFilter();
            });
            
            // 搜索框文本变化监听
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String keyword = s.toString().trim();
                    if (keyword.isEmpty()) {
                        btnClearSearch.setVisibility(View.GONE);
                        searchResultHint.setVisibility(View.GONE);
                        chapterAdapter.clearFilter();
                    } else {
                        btnClearSearch.setVisibility(View.VISIBLE);
                        int resultCount = chapterAdapter.filter(keyword);
                        searchResultHint.setVisibility(View.VISIBLE);
                        if (resultCount > 0) {
                            searchResultHint.setText(getString(R.string.chapter_search_result, resultCount));
                        } else {
                            searchResultHint.setText(R.string.chapter_search_no_result);
                        }
                    }
                }
            });
            
            // 搜索框回车键
            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard(searchEditText);
                    return true;
                }
                return false;
            });
            
            // 对话框关闭时重置搜索状态
            chapterListDialog.setOnDismissListener(dialog -> {
                searchBar.setVisibility(View.GONE);
                titleBar.setVisibility(View.VISIBLE);
                searchEditText.setText("");
                searchResultHint.setVisibility(View.GONE);
                btnClearSearch.setVisibility(View.GONE);
                chapterAdapter.clearFilter();
                hideKeyboard(searchEditText);
            });
        }
        
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state != null && state.getCurrentChapter() != null) {
            chapterAdapter.setCurrentChapterId(state.getCurrentChapter().getId());
            
            // 更新章节列表数据
            if (state.getChapters() != null) {
                chapterAdapter.setOriginalList(state.getChapters());
                // 异步更新已有摘要的章节
                viewModel.loadChaptersWithSummary(chaptersWithSummary -> {
                    chapterAdapter.setChaptersWithSummary(chaptersWithSummary);
                });
                TextView chapterCountText = chapterListDialog.findViewById(R.id.chapter_count_text);
                if (chapterCountText != null) {
                    chapterCountText.setText(getString(R.string.reader_chapter_count, state.getChapters().size()));
                }
            }
            
            chapterListDialog.setOnShowListener(dialog -> {
                RecyclerView recyclerView = chapterListDialog.findViewById(R.id.chapter_list_recycler_view);
                if (recyclerView != null && !chapterAdapter.hasFilter()) {
                    int position = state.getCurrentChapterIndex();
                    recyclerView.scrollToPosition(Math.max(0, position - 3));
                }
            });
        }
        
        chapterListDialog.show();
    }
    
    /**
     * 显示键盘
     */
    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    /**
     * 隐藏键盘
     */
    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * 启用全屏模式，隐藏系统状态栏
     * 兼容所有 Android 版本
     */
    private void enableFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) 及以上使用 WindowInsetsController
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // Android 10 及以下使用旧 API
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * 从Intent加载小说
     */
    private void loadNovelFromIntent() {
        long novelId = getIntent().getLongExtra(EXTRA_NOVEL_ID, -1);
        Log.d(TAG, "[加载小说] novelId=" + novelId + ", currentPageMode=" + currentPageMode);
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
                // 保存当前页码，分页后恢复
                if (currentPageMode == PageMode.PAGE) {
                    saveCurrentPageIndexForRepagination();
                }
                viewModel.setFontSize(fontSize);
            }

            @Override
            public void onLineSpacingChanged(float lineSpacing) {
                // 保存当前页码，分页后恢复
                if (currentPageMode == PageMode.PAGE) {
                    saveCurrentPageIndexForRepagination();
                }
                viewModel.setLineSpacing(lineSpacing);
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
                // 保存当前页码，分页后恢复
                if (currentPageMode == PageMode.PAGE) {
                    saveCurrentPageIndexForRepagination();
                }
                viewModel.setFont(font);
            }
            
            @Override
            public void onBlockedWordManageClick(long novelId, String novelTitle) {
                // 跳转到屏蔽词管理界面
                NavigationHelper.navigateToBlockedWord(ReaderActivity.this, novelId, novelTitle);
            }
        });
        
        // 设置小说信息（用于屏蔽词管理）
        if (state != null && state.getNovel() != null) {
            settingsDialog.setNovelInfo(state.getNovel().getId(), state.getNovel().getTitle());
        }
        
        settingsDialog.show();
    }
    
    /**
     * 获取当前阅读位置
     * 用于搜索前保存位置，以便返回时恢复
     * 
     * @return 当前位置（翻页模式返回当前页面的起始字符位置，滚动模式返回滚动位置）
     */
    private int getCurrentReadingPosition() {
        if (currentPageMode == PageMode.PAGE) {
            // 翻页模式：返回当前页面的起始字符位置（原始文本中的位置）
            int currentItem = pageViewPager.getCurrentItem();
            int startIndex = pageAdapter.getCurrentChapterStartIndex();
            int pageInChapter = Math.max(0, currentItem - startIndex);
            
            // 获取当前页面的原始文本起始位置
            if (currentPages != null && pageInChapter < currentPages.size()) {
                return currentPages.get(pageInChapter).getOriginalStartIndex();
            }
            return 0;
        } else {
            // 滚动模式：返回滚动位置
            return contentScrollView.getScrollY();
        }
    }

    /**
     * 显示搜索对话框
     */
    // 搜索导航防抖相关
    private boolean isSearchNavigating = false;
    private long lastSearchNavigationTime = 0;
    private static final long SEARCH_NAVIGATION_DEBOUNCE = 500; // 防抖时间（毫秒）
    private int pendingNavigationIndex = -1; // 待显示的导航索引
    private int displayedSearchIndex = -1; // 当前显示的搜索索引（独立于 ViewModel）

    private void showSearchDialog() {
        // 移除旧的观察者（如果存在）
        if (searchDialogObserver != null) {
            viewModel.getUiState().removeObserver(searchDialogObserver);
            searchDialogObserver = null;
        }
        
        searchDialog = new SearchDialog(this);
        
        searchDialog.setOnSearchListener(new SearchDialog.OnSearchListener() {
            @Override
            public void onSearch(String keyword) {
                // 重置显示索引
                displayedSearchIndex = -1;
                // 获取当前阅读位置
                int currentPosition = getCurrentReadingPosition();
                viewModel.searchInNovel(keyword, currentPosition);
            }

            @Override
            public void onSearchResultClick(com.example.read.domain.model.SearchResult result, int position) {
                // 记录点击的位置索引
                android.util.Log.d("SearchNav", "点击搜索结果: position=" + position);
                pendingNavigationIndex = position;
                displayedSearchIndex = position; // 设置显示索引
                // 跳转到搜索结果位置
                viewModel.navigateToSearchResult(position);
            }
        });
        
        // 对话框关闭时移除观察者
        searchDialog.setOnDismissListener(dialog -> {
            if (searchDialogObserver != null) {
                viewModel.getUiState().removeObserver(searchDialogObserver);
                searchDialogObserver = null;
            }
        });
        
        searchDialog.show();
        
        // 创建新的观察者
        searchDialogObserver = s -> {
            android.util.Log.d("SearchNav", "LiveData更新: pendingIndex=" + pendingNavigationIndex 
                + ", displayedIndex=" + displayedSearchIndex
                + ", hasResults=" + s.hasSearchResults()
                + ", vmIndex=" + s.getCurrentSearchIndex()
                + ", keyword=" + s.getSearchKeyword());
            
            if (searchDialog != null && searchDialog.isShowing()) {
                // 当有搜索关键词时，无论结果是否为空都更新显示
                // showSearchResults 方法会正确处理空结果（显示"无搜索结果"）
                String keyword = s.getSearchKeyword();
                if (keyword != null && !keyword.isEmpty()) {
                    searchDialog.showSearchResults(s.getSearchResults());
                    if (s.hasSearchResults()) {
                        searchDialog.setCurrentIndex(s.getCurrentSearchIndex());
                    }
                }
            }
            // 处理待显示的导航栏（点击搜索结果时触发）
            if (pendingNavigationIndex >= 0 && s.hasSearchResults()) {
                int indexToShow = pendingNavigationIndex;
                pendingNavigationIndex = -1;
                android.util.Log.d("SearchNav", "显示导航栏: indexToShow=" + indexToShow);
                showSearchNavigationBar(indexToShow, s.getSearchResults().size());
                // 重要：处理完 pendingNavigationIndex 后直接返回，避免被后续逻辑覆盖
                return;
            }
            // 更新悬浮导航栏位置显示（导航按钮点击时触发）
            // 使用 displayedSearchIndex 而不是 ViewModel 的 currentSearchIndex
            if (s.hasSearchResults() && searchNavigationBar.getVisibility() == View.VISIBLE) {
                // 只有当 displayedSearchIndex 有效时才使用它，否则使用 ViewModel 的值
                int indexToDisplay = displayedSearchIndex >= 0 ? displayedSearchIndex : s.getCurrentSearchIndex();
                android.util.Log.d("SearchNav", "更新导航栏: indexToDisplay=" + indexToDisplay);
                updateSearchNavigationBar(indexToDisplay, s.getSearchResults().size());
            }
        };
        
        viewModel.getUiState().observe(this, searchDialogObserver);
    }

    /**
     * 显示搜索悬浮导航栏
     */
    private void showSearchNavigationBar(int currentIndex, int total) {
        searchNavigationBar.setVisibility(View.VISIBLE);
        updateSearchNavigationBar(currentIndex, total);
        // 隐藏TTS悬浮控制条，避免重叠
        if (ttsMiniController.getVisibility() == View.VISIBLE) {
            ttsMiniController.setVisibility(View.GONE);
        }
    }

    /**
     * 隐藏搜索悬浮导航栏
     */
    private void hideSearchNavigationBar() {
        searchNavigationBar.setVisibility(View.GONE);
        // 如果TTS正在播放，恢复显示TTS悬浮控制条
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state != null && state.getTtsState() != null && state.getTtsState().isPlaying()) {
            ttsMiniController.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 更新搜索悬浮导航栏显示
     */
    private void updateSearchNavigationBar(int currentIndex, int total) {
        // 更新位置文本（显示为1-based索引）
        searchNavPosition.setText(String.format("%d / %d", currentIndex + 1, total));
        // 更新按钮状态
        searchNavPrevious.setEnabled(currentIndex > 0);
        searchNavPrevious.setAlpha(currentIndex > 0 ? 1.0f : 0.5f);
        searchNavNext.setEnabled(currentIndex < total - 1);
        searchNavNext.setAlpha(currentIndex < total - 1 ? 1.0f : 0.5f);
    }

    /**
     * 带防抖的搜索导航（修复问题二：防止快速点击导致显示异常）
     */
    private void navigateSearchWithDebounce(boolean isNext) {
        long currentTime = System.currentTimeMillis();
        if (isSearchNavigating || currentTime - lastSearchNavigationTime < SEARCH_NAVIGATION_DEBOUNCE) {
            return;
        }
        isSearchNavigating = true;
        lastSearchNavigationTime = currentTime;
        
        // 更新显示索引
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state != null && state.hasSearchResults()) {
            int total = state.getSearchResults().size();
            int oldIndex = displayedSearchIndex;
            if (isNext) {
                if (displayedSearchIndex < total - 1) {
                    displayedSearchIndex++;
                }
            } else {
                if (displayedSearchIndex > 0) {
                    displayedSearchIndex--;
                }
            }
            android.util.Log.d("SearchNav", "导航按钮点击: isNext=" + isNext + ", oldIndex=" + oldIndex + ", newDisplayedIndex=" + displayedSearchIndex);
            // 立即更新导航栏显示
            updateSearchNavigationBar(displayedSearchIndex, total);
            
            // 使用新的带索引参数的方法，确保跳转到正确的位置
            viewModel.navigateToSearchResult(displayedSearchIndex);
        }
        
        // 延迟重置导航状态，等待章节加载完成
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isSearchNavigating = false;
        }, SEARCH_NAVIGATION_DEBOUNCE);
    }

    /**
     * 显示TTS控制面板
     * 验证需求：10.1, 10.2, 10.3, 10.4
     * 问题3修复：在翻页模式下只显示当前页面的段落
     */
    private void showTTSControlDialog() {
        ttsControlDialog = TTSControlDialog.newInstance();
        
        // 获取当前页面的段落列表
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state != null && state.getDisplayContent() != null) {
            List<ParagraphInfo> paragraphs;
            
            // 根据翻页模式获取不同范围的段落
            if (currentPageMode == PageMode.PAGE && currentPages != null && !currentPages.isEmpty()) {
                // 翻页模式：只获取当前页面的段落
                int currentPageIndex = getCurrentPageIndexInChapter();
                if (currentPageIndex >= 0 && currentPageIndex < currentPages.size()) {
                    TextPaginator.PageInfo currentPageInfo = currentPages.get(currentPageIndex);
                    String pageContent = currentPageInfo.getContent();
                    int pageStartOffset = currentPageInfo.getStartIndex();
                    
                    // 解析当前页面的段落，并调整位置偏移量
                    paragraphs = parseParagraphsWithOffset(pageContent, pageStartOffset);
                } else {
                    // 回退到整章内容
                    paragraphs = TTSControlDialog.parseParagraphs(state.getDisplayContent());
                }
            } else {
                // 滚动模式：获取当前可见区域的段落
                paragraphs = getVisibleParagraphs(state.getDisplayContent());
            }
            
            ttsControlDialog.setParagraphs(paragraphs);
        }
        
        // 设置可用语音列表
        ttsControlDialog.setAvailableVoices(viewModel.getAvailableVoices());
        
        // 设置当前TTS状态
        if (state != null) {
            ttsControlDialog.setTTSState(state.getTtsState());
        }
        
        // 设置监听器
        ttsControlDialog.setListener(new TTSControlDialog.TTSControlListener() {
            @Override
            public void onParagraphSelected(ParagraphInfo paragraph) {
                // 从选中的段落位置开始朗读
                viewModel.startTTSFromPosition(paragraph.getStartPosition());
            }

            @Override
            public void onPlayPause() {
                viewModel.toggleTTS();
            }

            @Override
            public void onSpeedChanged(float speed) {
                viewModel.setTTSSpeechRate(speed);
            }

            @Override
            public void onVoiceChanged(String voiceId) {
                viewModel.setTTSVoice(voiceId);
            }
        });
        
        ttsControlDialog.show(getSupportFragmentManager(), "tts_control");
    }

    /**
     * 获取当前页面在章节中的索引
     */
    private int getCurrentPageIndexInChapter() {
        if (pageViewPager == null || pageAdapter == null) return -1;
        
        int currentPosition = pageViewPager.getCurrentItem();
        int currentChapterStartIndex = pageAdapter.getCurrentChapterStartIndex();
        
        return currentPosition - currentChapterStartIndex;
    }

    /**
     * 解析段落并添加位置偏移量
     * @param content 页面内容
     * @param startOffset 在原文中的起始偏移量
     * @return 段落列表
     */
    private List<ParagraphInfo> parseParagraphsWithOffset(String content, int startOffset) {
        List<ParagraphInfo> paragraphs = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return paragraphs;
        }

        String[] lines = content.split("\n");
        int position = 0;
        int paragraphIndex = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                String preview = ParagraphInfo.createPreview(trimmed, 50);
                // 添加偏移量，使位置指向原文中的实际位置
                ParagraphInfo paragraph = new ParagraphInfo(
                        preview,
                        trimmed,
                        startOffset + position,
                        paragraphIndex
                );
                paragraphs.add(paragraph);
                paragraphIndex++;
            }
            position += line.length() + 1;
        }

        return paragraphs;
    }

    /**
     * 获取滚动模式下可见区域的段落
     * @param fullContent 完整章节内容
     * @return 可见区域的段落列表
     */
    private List<ParagraphInfo> getVisibleParagraphs(String fullContent) {
        if (fullContent == null || fullContent.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取当前滚动位置和可见高度
        int scrollY = contentScrollView.getScrollY();
        int visibleHeight = contentScrollView.getHeight();
        
        // 估算每行高度（基于字体大小和行间距）
        ReaderUiState state = viewModel.getUiState().getValue();
        float fontSize = state != null ? state.getFontSize() : 18f;
        float lineSpacing = state != null ? state.getLineSpacing() : 1.5f;
        float lineHeight = fontSize * lineSpacing * getResources().getDisplayMetrics().density;
        
        // 估算可见区域的起始和结束行
        int startLine = (int) (scrollY / lineHeight);
        int endLine = (int) ((scrollY + visibleHeight) / lineHeight);
        
        // 解析所有段落
        List<ParagraphInfo> allParagraphs = TTSControlDialog.parseParagraphs(fullContent);
        
        // 如果段落数量不多，直接返回全部
        if (allParagraphs.size() <= 20) {
            return allParagraphs;
        }
        
        // 估算当前可见的段落范围（每个段落约2-5行）
        int avgLinesPerParagraph = 3;
        int startParagraph = Math.max(0, startLine / avgLinesPerParagraph - 2);
        int endParagraph = Math.min(allParagraphs.size(), endLine / avgLinesPerParagraph + 5);
        
        // 返回可见范围附近的段落
        return allParagraphs.subList(startParagraph, endParagraph);
    }

    /**
     * 显示摘要管理对话框
     */
    private void showSummaryManagerDialog() {
        // 创建 BottomSheetDialog
        summaryManagerDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_summary_manager, null);
        summaryManagerDialog.setContentView(dialogView);
        
        // 获取视图组件
        RecyclerView recyclerView = dialogView.findViewById(R.id.summary_list_recycler_view);
        TextView summaryCountText = dialogView.findViewById(R.id.summary_count_text);
        View emptyState = dialogView.findViewById(R.id.empty_state);
        
        // 设置 RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        summaryAdapter = new SummaryAdapter();
        recyclerView.setAdapter(summaryAdapter);
        
        // 设置点击监听 - 显示摘要详情
        summaryAdapter.setOnItemClickListener(chapter -> {
            showSummaryDetailDialog(chapter);
        });
        
        // 设置删除监听
        summaryAdapter.setOnDeleteListener((chapter, position) -> {
            // 显示确认对话框
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.summary_delete)
                    .setMessage(R.string.summary_delete_confirm)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // 删除摘要
                        viewModel.deleteSummary(chapter.getId(), () -> {
                            summaryAdapter.removeItem(position);
                            // 更新计数
                            int newCount = summaryAdapter.getItemCount();
                            summaryCountText.setText(getString(R.string.summary_manager_count, newCount));
                            // 检查是否为空
                            if (newCount == 0) {
                                recyclerView.setVisibility(View.GONE);
                                emptyState.setVisibility(View.VISIBLE);
                            }
                            Toast.makeText(this, R.string.summary_deleted, Toast.LENGTH_SHORT).show();
                        });
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
        
        // 加载摘要列表
        viewModel.loadChaptersWithSummaryList(summaryList -> {
            if (summaryList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
                summaryCountText.setText(getString(R.string.summary_manager_count, 0));
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
                summaryAdapter.setSummaryList(summaryList);
                summaryCountText.setText(getString(R.string.summary_manager_count, summaryList.size()));
            }
        });
        
        summaryManagerDialog.show();
    }

    /**
     * 显示摘要详情对话框
     * @param chapterInfo 章节信息（包含摘要）
     */
    private void showSummaryDetailDialog(com.example.read.data.entity.ChapterInfo chapterInfo) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_summary, null);
        
        TextView titleText = dialogView.findViewById(R.id.summary_title);
        View loadingContainer = dialogView.findViewById(R.id.loading_container);
        ScrollView contentContainer = dialogView.findViewById(R.id.content_container);
        TextView summaryContent = dialogView.findViewById(R.id.summary_content);
        View errorContainer = dialogView.findViewById(R.id.error_container);
        android.widget.Button btnRegenerate = dialogView.findViewById(R.id.btn_regenerate);
        android.widget.Button btnClose = dialogView.findViewById(R.id.btn_close);
        android.widget.Button btnJumpToChapter = dialogView.findViewById(R.id.btn_retry);
        
        // 设置标题
        titleText.setText(chapterInfo.getTitle());
        
        // 直接显示摘要内容（因为已经有缓存）
        loadingContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        summaryContent.setText(chapterInfo.getSummary());
        
        // 修改重试按钮为跳转章节按钮
        btnJumpToChapter.setText(R.string.summary_jump_to_chapter);
        btnJumpToChapter.setVisibility(View.VISIBLE);
        btnRegenerate.setVisibility(View.VISIBLE);
        
        // 创建对话框
        androidx.appcompat.app.AlertDialog detailDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // 关闭按钮
        btnClose.setOnClickListener(v -> detailDialog.dismiss());
        
        // 跳转章节按钮
        btnJumpToChapter.setOnClickListener(v -> {
            detailDialog.dismiss();
            summaryManagerDialog.dismiss();
            viewModel.loadChapter(chapterInfo.getId());
            animateChapterChange(false);
        });
        
        // 重新生成按钮
        btnRegenerate.setOnClickListener(v -> {
            // 转换为 Chapter 对象用于重新生成
            Chapter chapter = new Chapter();
            chapter.setId(chapterInfo.getId());
            chapter.setNovelId(chapterInfo.getNovelId());
            chapter.setTitle(chapterInfo.getTitle());
            chapter.setContent(""); // content 不需要，会在 ViewModel 中重新加载
            chapter.setChapterIndex(chapterInfo.getChapterIndex());
            chapter.setWordCount(chapterInfo.getWordCount());
            chapter.setSourceUrl(chapterInfo.getSourceUrl());
            
            // 显示加载状态
            loadingContainer.setVisibility(View.VISIBLE);
            contentContainer.setVisibility(View.GONE);
            btnRegenerate.setVisibility(View.GONE);
            btnJumpToChapter.setVisibility(View.GONE);
            
            viewModel.regenerateChapterSummary(chapter, new ReaderViewModel.SummaryCallback() {
                @Override
                public void onLoading() {
                    // 已经在上面设置了加载状态
                }

                @Override
                public void onSuccess(String summary, boolean fromCache) {
                    runOnUiThread(() -> {
                        loadingContainer.setVisibility(View.GONE);
                        contentContainer.setVisibility(View.VISIBLE);
                        summaryContent.setText(summary);
                        btnRegenerate.setVisibility(View.VISIBLE);
                        btnJumpToChapter.setVisibility(View.VISIBLE);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        loadingContainer.setVisibility(View.GONE);
                        errorContainer.setVisibility(View.VISIBLE);
                        TextView errorMessage = dialogView.findViewById(R.id.error_message);
                        errorMessage.setText(message);
                        btnJumpToChapter.setVisibility(View.VISIBLE);
                    });
                }
            });
        });
        
        detailDialog.show();
    }

    /**
     * 显示书签列表对话框
     * 验证需求：7.3 - 显示书签列表
     */
    private void showBookmarkListDialog() {
        BookmarkListDialog bookmarkDialog = new BookmarkListDialog(this);
        
        // 设置书签跳转监听
        bookmarkDialog.setOnBookmarkJumpListener(bookmark -> {
            viewModel.jumpToBookmark(bookmark);
        });
        
        // 设置书签删除监听
        bookmarkDialog.setOnBookmarkDeleteListener(bookmark -> {
            viewModel.deleteBookmark(bookmark.getId());
        });
        
        // 设置添加书签监听
        bookmarkDialog.setOnAddBookmarkListener(() -> {
            showAddBookmarkDialog();
        });
        
        // 观察书签列表
        viewModel.getBookmarks().observe(this, bookmarks -> {
            bookmarkDialog.setBookmarks(bookmarks);
        });
        
        bookmarkDialog.show();
    }

    /**
     * 显示添加书签对话框
     * 验证需求：7.1, 7.2 - 添加书签并支持备注
     */
    private void showAddBookmarkDialog() {
        ReaderUiState state = viewModel.getUiState().getValue();
        if (state == null || state.getCurrentChapter() == null) {
            Toast.makeText(this, "无法添加书签", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_bookmark, null);
        builder.setView(dialogView);

        TextView tvChapterInfo = dialogView.findViewById(R.id.tv_chapter_info);
        com.google.android.material.textfield.TextInputEditText etNote = dialogView.findViewById(R.id.et_note);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        // 显示当前章节信息
        String chapterInfo = getString(R.string.bookmark_chapter_info, state.getCurrentChapter().getTitle());
        tvChapterInfo.setText(chapterInfo);

        android.app.AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";
            int position = 0;
            String textPreview = "";
            String content = state.getDisplayContent();
            
            if (currentPageMode == PageMode.SCROLL) {
                // 滚动模式：根据滚动位置估算字符位置
                int scrollY = contentScrollView.getScrollY();
                View child = contentScrollView.getChildAt(0);
                if (child != null && child.getHeight() > 0 && content != null && !content.isEmpty()) {
                    float ratio = (float) scrollY / child.getHeight();
                    position = (int) (content.length() * ratio);
                    position = Math.max(0, Math.min(position, content.length() - 1));
                }
            } else {
                // 翻页模式：获取当前页面的起始字符位置
                int currentItem = pageViewPager.getCurrentItem();
                int startIndex = pageAdapter.getCurrentChapterStartIndex();
                int pageIndexInChapter = Math.max(0, currentItem - startIndex);
                
                // 从currentPages获取原始文本位置
                if (currentPages != null && pageIndexInChapter < currentPages.size()) {
                    TextPaginator.PageInfo pageInfo = currentPages.get(pageIndexInChapter);
                    position = pageInfo.getOriginalStartIndex();
                }
            }
            
            // 获取文本预览（从position位置开始截取50个字符）
            if (content != null && !content.isEmpty() && position < content.length()) {
                int endIndex = Math.min(position + 50, content.length());
                textPreview = content.substring(position, endIndex);
                // 清理文本预览：去除换行符，添加省略号
                textPreview = textPreview.replace("\n", " ").trim();
                if (endIndex < content.length()) {
                    textPreview += "...";
                }
            }
            
            viewModel.addBookmark(note, position, textPreview);
            dialog.dismiss();
        });

        dialog.show();
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
        // 刷新屏蔽词（从屏蔽词管理界面返回时生效）
        viewModel.refreshBlockedWords();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭分页后台线程
        if (paginationExecutor != null) {
            paginationExecutor.shutdownNow();
            paginationExecutor = null;
        }
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

    // ==================== AI摘要功能 ====================

    /**
     * 显示章节摘要对话框
     * 验证需求：8.1, 8.2, 8.3, 8.4
     * 
     * @param chapter 章节对象
     * @param hasSummary 是否已有摘要
     */
    private void showSummaryDialog(Chapter chapter, boolean hasSummary) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_summary, null);
        
        TextView titleText = dialogView.findViewById(R.id.summary_title);
        View loadingContainer = dialogView.findViewById(R.id.loading_container);
        ScrollView contentContainer = dialogView.findViewById(R.id.content_container);
        TextView summaryContent = dialogView.findViewById(R.id.summary_content);
        View errorContainer = dialogView.findViewById(R.id.error_container);
        TextView errorMessage = dialogView.findViewById(R.id.error_message);
        android.widget.Button btnRetry = dialogView.findViewById(R.id.btn_retry);
        android.widget.Button btnRegenerate = dialogView.findViewById(R.id.btn_regenerate);
        android.widget.Button btnClose = dialogView.findViewById(R.id.btn_close);
        
        // 设置标题
        titleText.setText(chapter.getTitle());
        
        // 创建对话框
        summaryDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // 关闭按钮
        btnClose.setOnClickListener(v -> summaryDialog.dismiss());
        
        // 重试按钮
        btnRetry.setOnClickListener(v -> {
            loadSummary(chapter, loadingContainer, contentContainer, summaryContent, 
                    errorContainer, errorMessage, btnRetry, btnRegenerate);
        });
        
        // 重新生成按钮
        btnRegenerate.setOnClickListener(v -> {
            regenerateSummary(chapter, loadingContainer, contentContainer, summaryContent, 
                    errorContainer, errorMessage, btnRetry, btnRegenerate);
        });
        
        // 加载摘要
        loadSummary(chapter, loadingContainer, contentContainer, summaryContent, 
                errorContainer, errorMessage, btnRetry, btnRegenerate);
        
        summaryDialog.show();
    }

    /**
     * 加载章节摘要
     */
    private void loadSummary(Chapter chapter, View loadingContainer, ScrollView contentContainer,
                             TextView summaryContent, View errorContainer, TextView errorMessage,
                             android.widget.Button btnRetry, android.widget.Button btnRegenerate) {
        // 显示加载状态
        loadingContainer.setVisibility(View.VISIBLE);
        contentContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
        btnRegenerate.setVisibility(View.GONE);
        
        viewModel.getChapterSummary(chapter, new ReaderViewModel.SummaryCallback() {
            @Override
            public void onLoading() {
                // 已经在上面设置了加载状态
            }

            @Override
            public void onSuccess(String summary, boolean fromCache) {
                runOnUiThread(() -> {
                    loadingContainer.setVisibility(View.GONE);
                    contentContainer.setVisibility(View.VISIBLE);
                    summaryContent.setText(summary);
                    btnRegenerate.setVisibility(View.VISIBLE);
                    // 更新章节列表中的摘要状态
                    chapterAdapter.updateSummaryStatus(chapter.getId(), true);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    loadingContainer.setVisibility(View.GONE);
                    errorContainer.setVisibility(View.VISIBLE);
                    errorMessage.setText(message);
                    btnRetry.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    /**
     * 重新生成章节摘要
     */
    private void regenerateSummary(Chapter chapter, View loadingContainer, ScrollView contentContainer,
                                   TextView summaryContent, View errorContainer, TextView errorMessage,
                                   android.widget.Button btnRetry, android.widget.Button btnRegenerate) {
        // 显示加载状态
        loadingContainer.setVisibility(View.VISIBLE);
        contentContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
        btnRegenerate.setVisibility(View.GONE);
        
        viewModel.regenerateChapterSummary(chapter, new ReaderViewModel.SummaryCallback() {
            @Override
            public void onLoading() {
                // 已经在上面设置了加载状态
            }

            @Override
            public void onSuccess(String summary, boolean fromCache) {
                runOnUiThread(() -> {
                    loadingContainer.setVisibility(View.GONE);
                    contentContainer.setVisibility(View.VISIBLE);
                    summaryContent.setText(summary);
                    btnRegenerate.setVisibility(View.VISIBLE);
                    // 更新章节列表中的摘要状态
                    chapterAdapter.updateSummaryStatus(chapter.getId(), true);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    loadingContainer.setVisibility(View.GONE);
                    errorContainer.setVisibility(View.VISIBLE);
                    errorMessage.setText(message);
                    btnRetry.setVisibility(View.VISIBLE);
                });
            }
        });
    }
}