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
import com.google.android.material.button.MaterialButton;

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
    private LinearLayout emptyState;
    private TextView emptyStateText;
    private ProgressBar loadingProgress;
    private LinearLayout navigationBar;
    private ImageButton btnPreviousResult;
    private TextView currentResultPosition;
    private ImageButton btnNextResult;
    private MaterialButton btnReturnPosition;

    // 适配器
    private SearchResultAdapter adapter;

    // 回调接口
    private OnSearchListener searchListener;

    // 当前状态
    private int currentIndex = -1;
    private int totalResults = 0;

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
        emptyState = findViewById(R.id.empty_state);
        emptyStateText = findViewById(R.id.empty_state_text);
        loadingProgress = findViewById(R.id.loading_progress);
        navigationBar = findViewById(R.id.navigation_bar);
        btnPreviousResult = findViewById(R.id.btn_previous_result);
        currentResultPosition = findViewById(R.id.current_result_position);
        btnNextResult = findViewById(R.id.btn_next_result);
        btnReturnPosition = findViewById(R.id.btn_return_position);

        // 设置RecyclerView
        adapter = new SearchResultAdapter();
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsRecyclerView.setAdapter(adapter);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener(v -> dismiss());

        // 搜索按钮
        // 验证需求：9.1 - 显示搜索输入框
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

        // 搜索结果点击
        // 验证需求：9.4 - 点击搜索结果条目跳转到该关键词所在位置
        adapter.setOnSearchResultClickListener((result, position) -> {
            currentIndex = position;
            updateNavigationState();
            
            if (searchListener != null) {
                searchListener.onSearchResultClick(result, position);
            }
        });

        // 上一个结果
        // 验证需求：9.7 - 支持上一个结果的快速导航
        btnPreviousResult.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                adapter.setSelectedPosition(currentIndex);
                scrollToPosition(currentIndex);
                updateNavigationState();
                
                if (searchListener != null) {
                    searchListener.onNavigateToPrevious();
                }
            }
        });

        // 下一个结果
        // 验证需求：9.7 - 支持下一个结果的快速导航
        btnNextResult.setOnClickListener(v -> {
            if (currentIndex < totalResults - 1) {
                currentIndex++;
                adapter.setSelectedPosition(currentIndex);
                scrollToPosition(currentIndex);
                updateNavigationState();
                
                if (searchListener != null) {
                    searchListener.onNavigateToNext();
                }
            }
        });

        // 返回原位置
        // 验证需求：9.5 - 点击返回恢复到搜索前的原始阅读进度
        btnReturnPosition.setOnClickListener(v -> {
            if (searchListener != null) {
                searchListener.onReturnToOriginalPosition();
            }
            dismiss();
        });
    }

    /**
     * 执行搜索
     * 验证需求：9.2 - 在当前小说的所有章节中查找该关键词
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
        emptyState.setVisibility(View.VISIBLE);
        emptyStateText.setText(R.string.search_empty_hint);
        loadingProgress.setVisibility(View.GONE);
        navigationBar.setVisibility(View.GONE);
        
        currentIndex = -1;
        totalResults = 0;
    }

    /**
     * 显示加载状态
     */
    private void showLoading() {
        searchResultCount.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        loadingProgress.setVisibility(View.VISIBLE);
        navigationBar.setVisibility(View.GONE);
    }

    /**
     * 显示搜索结果
     * 验证需求：9.3 - 显示包含该关键词的所有位置列表
     */
    public void showSearchResults(List<SearchResult> results) {
        loadingProgress.setVisibility(View.GONE);
        
        if (results == null || results.isEmpty()) {
            showNoResults();
            return;
        }

        totalResults = results.size();
        currentIndex = 0;

        // 显示结果数量
        searchResultCount.setVisibility(View.VISIBLE);
        searchResultCount.setText(getContext().getString(R.string.search_result_count, totalResults));

        // 显示结果列表
        searchResultsRecyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        adapter.submitList(results);
        adapter.setSelectedPosition(0);

        // 显示导航栏
        navigationBar.setVisibility(View.VISIBLE);
        updateNavigationState();
    }

    /**
     * 显示无结果状态
     * 验证需求：9.6 - 显示"未找到相关内容"的提示信息
     */
    private void showNoResults() {
        searchResultCount.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        emptyStateText.setText(R.string.search_no_results);
        navigationBar.setVisibility(View.GONE);
        
        currentIndex = -1;
        totalResults = 0;
    }

    /**
     * 更新导航状态
     */
    private void updateNavigationState() {
        // 更新位置显示
        if (totalResults > 0) {
            currentResultPosition.setText(
                    getContext().getString(R.string.search_position, currentIndex + 1, totalResults));
        }

        // 更新按钮状态
        btnPreviousResult.setEnabled(currentIndex > 0);
        btnPreviousResult.setAlpha(currentIndex > 0 ? 1.0f : 0.3f);
        
        btnNextResult.setEnabled(currentIndex < totalResults - 1);
        btnNextResult.setAlpha(currentIndex < totalResults - 1 ? 1.0f : 0.3f);
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
     * 设置当前搜索索引
     */
    public void setCurrentIndex(int index) {
        this.currentIndex = index;
        adapter.setSelectedPosition(index);
        updateNavigationState();
    }

    /**
     * 获取当前搜索索引
     */
    public int getCurrentIndex() {
        return currentIndex;
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
     * 搜索监听器接口
     */
    public interface OnSearchListener {
        /**
         * 执行搜索
         * 验证需求：9.2
         */
        void onSearch(String keyword);

        /**
         * 点击搜索结果
         * 验证需求：9.4
         */
        void onSearchResultClick(SearchResult result, int position);

        /**
         * 导航到上一个结果
         * 验证需求：9.7
         */
        void onNavigateToPrevious();

        /**
         * 导航到下一个结果
         * 验证需求：9.7
         */
        void onNavigateToNext();

        /**
         * 返回原位置
         * 验证需求：9.5
         */
        void onReturnToOriginalPosition();
    }
}
