package com.example.read.presentation.reader;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索对话框
 * 
 * 验证需求：9.1, 9.3, 9.4, 9.5, 9.7
 */
public class SearchDialog extends Dialog {

    // UI组件
    private ImageButton btnBack;
    private EditText searchInput;
    private ImageButton btnSearch;
    private TextView searchResultCount;
    private RecyclerView searchResultsRecyclerView;
    private FixedThumbFastScroller fastScroller;
    private LinearLayout emptyState;
    private TextView emptyStateText;
    private ProgressBar loadingProgress;

    // 适配器
    private SearchResultAdapter adapter;

    // 回调接口
    private OnSearchListener searchListener;

    // 当前状态（由外部设置，不在内部维护）
    private int currentIndex = -1;
    private int totalResults = 0;
    private List<SearchResult> currentResults = null;

    public SearchDialog(@NonNull Context context) {
        super(context, android.R.style.Theme_Material_Light_NoActionBar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_search);

        // 设置全屏
        Window window = getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, 
                    WindowManager.LayoutParams.MATCH_PARENT);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        initViews();
        setupListeners();
        showInitialState();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        searchInput = findViewById(R.id.search_input);
        btnSearch = findViewById(R.id.btn_search);
        searchResultCount = findViewById(R.id.search_result_count);
        searchResultsRecyclerView = findViewById(R.id.search_results_recycler_view);
        fastScroller = findViewById(R.id.fast_scroller);
        emptyState = findViewById(R.id.empty_state);
        emptyStateText = findViewById(R.id.empty_state_text);
        loadingProgress = findViewById(R.id.loading_progress);

        // 设置RecyclerView
        adapter = new SearchResultAdapter();
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsRecyclerView.setAdapter(adapter);

        // 绑定自定义快速滚动条
        fastScroller.attachToRecyclerView(searchResultsRecyclerView);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener(v -> dismiss());

        // 搜索按钮
        btnSearch.setOnClickListener(v -> performSearch());

        // 搜索输入框回车键
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // 输入框文本变化监听
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 清空时重置状态
                if (s.toString().trim().isEmpty()) {
                    showInitialState();
                }
            }
        });

        // 搜索结果点击 - 点击后关闭对话框，跳转到阅读界面
        adapter.setOnSearchResultClickListener((result, position) -> {
            if (searchListener != null) {
                searchListener.onSearchResultClick(result, position);
            }
            // 关闭对话框，让悬浮导航栏在阅读界面显示
            dismiss();
        });
    }

    /**
     * 执行搜索
     */
    private void performSearch() {
        String keyword = searchInput.getText().toString().trim();
        if (keyword.isEmpty()) {
            return;
        }

        // 隐藏键盘
        hideKeyboard();

        // 显示加载状态
        showLoading();

        // 回调搜索
        if (searchListener != null) {
            searchListener.onSearch(keyword);
        }
    }

    /**
     * 显示初始状态
     */
    private void showInitialState() {
        searchResultCount.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        fastScroller.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        emptyStateText.setText(R.string.search_empty_hint);
        loadingProgress.setVisibility(View.GONE);
        
        currentIndex = -1;
        totalResults = 0;
        currentResults = null;
    }

    /**
     * 显示加载状态
     */
    private void showLoading() {
        searchResultCount.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        fastScroller.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        loadingProgress.setVisibility(View.VISIBLE);
    }

    /**
     * 显示搜索结果
     */
    public void showSearchResults(List<SearchResult> results) {
        loadingProgress.setVisibility(View.GONE);
        
        if (results == null || results.isEmpty()) {
            showNoResults();
            return;
        }

        this.currentResults = results;
        this.totalResults = results.size();

        // 显示结果数量
        searchResultCount.setVisibility(View.VISIBLE);
        searchResultCount.setText(getContext().getString(R.string.search_result_count, totalResults));

        // 显示结果列表
        searchResultsRecyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        
        // 先提交 null 清空列表，再提交新列表，强制刷新
        // 这是因为 ListAdapter 的 DiffUtil 可能认为新旧列表相同而不更新
        adapter.submitList(null);
        adapter.submitList(new ArrayList<>(results));

        // 延迟显示滚动条（等待列表渲染完成）
        searchResultsRecyclerView.post(() -> {
            if (fastScroller.shouldShow()) {
                fastScroller.setVisibility(View.VISIBLE);
            } else {
                fastScroller.setVisibility(View.GONE);
            }
        });
    }

    /**
     * 显示无结果状态
     */
    private void showNoResults() {
        searchResultCount.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        fastScroller.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        emptyStateText.setText(R.string.search_no_results);
        
        currentIndex = -1;
        totalResults = 0;
        currentResults = null;
    }

    /**
     * 滚动到指定位置
     */
    private void scrollToPosition(int position) {
        if (position >= 0 && position < adapter.getItemCount()) {
            searchResultsRecyclerView.smoothScrollToPosition(position);
        }
    }

    /**
     * 隐藏键盘
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    /**
     * 设置搜索监听器
     */
    public void setOnSearchListener(OnSearchListener listener) {
        this.searchListener = listener;
    }

    /**
     * 设置当前搜索索引（由外部ViewModel状态驱动）
     */
    public void setCurrentIndex(int index) {
        this.currentIndex = index;
        adapter.setSelectedPosition(index);
        scrollToPosition(index);
    }

    /**
     * 获取当前搜索索引
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * 获取总结果数
     */
    public int getTotalResults() {
        return totalResults;
    }

    @Override
    public void show() {
        super.show();
        // 自动聚焦搜索框并显示键盘
        searchInput.requestFocus();
        Window window = getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    /**
     * 搜索监听器接口（简化版，移除导航回调）
     */
    public interface OnSearchListener {
        /**
         * 执行搜索
         */
        void onSearch(String keyword);

        /**
         * 点击搜索结果
         */
        void onSearchResultClick(SearchResult result, int position);
    }
}
