package com.example.read.presentation.bookshelf;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.read.R;
import com.example.read.domain.model.Novel;
import com.example.read.utils.NavigationHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 书架Activity - 显示和管理用户的小说集合
 * 
 * 验证需求：4.1, 4.2, 4.3, 4.5, 4.6
 */
@AndroidEntryPoint
public class BookshelfActivity extends AppCompatActivity {

    private BookshelfViewModel viewModel;
    private NovelAdapter novelAdapter;

    // UI组件
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyView;
    private TextView emptyText;
    private ProgressBar loadingProgress;
    private FloatingActionButton fabImport;

    // 顶部栏组件
    private LinearLayout topBar;
    private LinearLayout searchBar;
    private LinearLayout categoryContainer;
    private EditText searchEditText;
    private ImageButton btnSearch;
    private ImageButton btnSearchBack;
    private ImageButton btnMore;

    // 分类相关
    private List<String> categories = new ArrayList<>();
    private String selectedCategory = "全部";
    private TextView selectedCategoryTab;

    // 文件选择器
    private ActivityResultLauncher<Intent> filePickerLauncher;
    
    // 图片选择器（用于编辑封面）
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Novel editingNovel; // 当前正在编辑的小说
    private ImageView editCoverImageView; // 编辑对话框中的封面ImageView
    private String selectedCoverPath; // 选中的封面路径

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bookshelf);

        // 设置窗口边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinator_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initViewModel();
        initFilePickerLauncher();
        initImagePickerLauncher();
        setupListeners();
        observeData();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        recyclerView = findViewById(R.id.novels_recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        emptyView = findViewById(R.id.empty_view);
        emptyText = findViewById(R.id.empty_text);
        loadingProgress = findViewById(R.id.loading_progress);
        fabImport = findViewById(R.id.fab_import);

        // 顶部栏
        topBar = findViewById(R.id.top_bar);
        searchBar = findViewById(R.id.search_bar);
        categoryContainer = findViewById(R.id.category_container);
        searchEditText = findViewById(R.id.search_edit_text);
        btnSearch = findViewById(R.id.btn_search);
        btnSearchBack = findViewById(R.id.btn_search_back);
        btnMore = findViewById(R.id.btn_more);

        // 设置RecyclerView
        novelAdapter = new NovelAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(novelAdapter);
    }

    /**
     * 初始化ViewModel
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(BookshelfViewModel.class);
    }

    /**
     * 初始化文件选择器
     */
    private void initFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            handleFileImport(uri);
                        }
                    }
                }
        );
    }

    /**
     * 初始化图片选择器（用于编辑封面）
     */
    private void initImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            handleCoverImageSelected(uri);
                        }
                    }
                }
        );
    }

    /**
     * 处理选中的封面图片
     */
    private void handleCoverImageSelected(Uri uri) {
        try {
            // 复制图片到应用私有目录
            String fileName = "cover_" + System.currentTimeMillis() + ".jpg";
            java.io.File coverDir = new java.io.File(getFilesDir(), "covers");
            if (!coverDir.exists()) {
                coverDir.mkdirs();
            }
            java.io.File coverFile = new java.io.File(coverDir, fileName);
            
            // 复制文件
            try (java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream outputStream = new java.io.FileOutputStream(coverFile)) {
                if (inputStream != null) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
            
            selectedCoverPath = coverFile.getAbsolutePath();
            
            // 更新对话框中的封面预览
            if (editCoverImageView != null) {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(selectedCoverPath);
                if (bitmap != null) {
                    editCoverImageView.setImageBitmap(bitmap);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "封面图片加载失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 搜索按钮点击 - 显示搜索栏
        btnSearch.setOnClickListener(v -> showSearchBar());

        // 搜索返回按钮 - 隐藏搜索栏
        btnSearchBack.setOnClickListener(v -> hideSearchBar());

        // 搜索框文本变化监听
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    viewModel.clearSearch();
                } else {
                    viewModel.searchNovels(query);
                }
            }
        });

        // 搜索框回车键
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        // 更多按钮 - 管理分类
        btnMore.setOnClickListener(v -> showMoreMenu());

        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refresh());

        // 导入按钮点击
        fabImport.setOnClickListener(v -> showImportOptionsDialog());

        // 小说点击事件 - 跳转到阅读器界面
        novelAdapter.setOnNovelClickListener(novel -> {
            // 使用NavigationHelper进行导航
            Long currentChapterId = novel.getCurrentChapterId();
            if (currentChapterId != null && currentChapterId > 0) {
                NavigationHelper.navigateToReader(this, novel.getId(), currentChapterId);
            } else {
                NavigationHelper.navigateToReader(this, novel.getId());
            }
        });

        // 小说长按事件
        novelAdapter.setOnNovelLongClickListener((novel, anchorView) -> 
                showNovelContextMenu(novel, anchorView));
    }

    /**
     * 观察数据变化
     */
    private void observeData() {
        // 观察UI状态
        viewModel.getUiState().observe(this, state -> {
            // 更新加载状态
            swipeRefreshLayout.setRefreshing(state.isLoading());
            loadingProgress.setVisibility(state.isLoading() && state.getNovels().isEmpty() 
                    ? View.VISIBLE : View.GONE);

            // 更新错误信息
            if (state.getError() != null && !state.getError().isEmpty()) {
                Toast.makeText(this, state.getError(), Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        // 观察小说列表
        viewModel.getNovelsLiveData().observe(this, novels -> {
            novelAdapter.submitList(novels);
            updateEmptyView(novels);
        });

        // 观察分类列表
        viewModel.getCategories().observe(this, categoryList -> {
            categories.clear();
            categories.add(getString(R.string.category_all));
            if (categoryList != null) {
                categories.addAll(categoryList);
            }
            updateCategoryTabs();
        });
    }

    /**
     * 更新分类标签
     */
    private void updateCategoryTabs() {
        categoryContainer.removeAllViews();
        selectedCategoryTab = null;

        for (String category : categories) {
            TextView tab = createCategoryTab(category);
            categoryContainer.addView(tab);

            if (category.equals(selectedCategory)) {
                selectTab(tab);
            }
        }

        // 如果没有选中的标签，选中第一个
        if (selectedCategoryTab == null && categoryContainer.getChildCount() > 0) {
            selectTab((TextView) categoryContainer.getChildAt(0));
        }
    }

    /**
     * 创建分类标签
     */
    private TextView createCategoryTab(String category) {
        TextView tab = new TextView(this);
        tab.setText(category);
        tab.setTextSize(16);
        tab.setTextColor(getColor(R.color.bookshelf_tab_normal));
        tab.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        tab.setTag(category);

        tab.setOnClickListener(v -> {
            if (selectedCategoryTab != tab) {
                // 取消之前的选中状态
                if (selectedCategoryTab != null) {
                    selectedCategoryTab.setTextColor(getColor(R.color.bookshelf_tab_normal));
                    selectedCategoryTab.setTypeface(null, Typeface.NORMAL);
                }
                // 设置新的选中状态
                selectTab(tab);
                // 筛选
                selectedCategory = category;
                viewModel.filterByCategory(category);
            }
        });

        return tab;
    }

    /**
     * 选中标签
     */
    private void selectTab(TextView tab) {
        tab.setTextColor(getColor(R.color.bookshelf_tab_selected));
        tab.setTypeface(null, Typeface.BOLD);
        selectedCategoryTab = tab;
        selectedCategory = (String) tab.getTag();
    }

    /**
     * dp转px
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    /**
     * 显示搜索栏
     */
    private void showSearchBar() {
        topBar.setVisibility(View.GONE);
        searchBar.setVisibility(View.VISIBLE);
        searchEditText.requestFocus();
        showKeyboard();
    }

    /**
     * 隐藏搜索栏
     */
    private void hideSearchBar() {
        searchBar.setVisibility(View.GONE);
        topBar.setVisibility(View.VISIBLE);
        searchEditText.setText("");
        viewModel.clearSearch();
        hideKeyboard();
    }

    /**
     * 显示键盘
     */
    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * 隐藏键盘
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        }
    }

    /**
     * 显示更多菜单
     */
    private void showMoreMenu() {
        String[] menuItems = {
                getString(R.string.menu_manage_categories)
        };

        new AlertDialog.Builder(this)
                .setItems(menuItems, (dialog, which) -> {
                    if (which == 0) {
                        showManageCategoriesDialog();
                    }
                })
                .show();
    }

    /**
     * 显示管理分类对话框
     */
    private void showManageCategoriesDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_manage_categories, null);
        EditText editNewCategory = view.findViewById(R.id.edit_new_category);
        ImageButton btnAddCategory = view.findViewById(R.id.btn_add_category);
        RecyclerView categoryList = view.findViewById(R.id.category_list);
        TextView emptyHint = view.findViewById(R.id.empty_hint);

        // 设置分类列表
        CategoryAdapter categoryAdapter = new CategoryAdapter();
        categoryList.setLayoutManager(new LinearLayoutManager(this));
        categoryList.setAdapter(categoryAdapter);

        // 获取用户自定义的分类（排除"全部"）
        List<String> userCategories = new ArrayList<>();
        for (String category : categories) {
            if (!category.equals(getString(R.string.category_all))) {
                userCategories.add(category);
            }
        }
        categoryAdapter.submitList(new ArrayList<>(userCategories));

        // 更新空状态
        updateCategoryEmptyState(userCategories, categoryList, emptyHint);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.manage_categories_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_confirm, null)
                .create();

        // 添加分类按钮点击
        btnAddCategory.setOnClickListener(v -> {
            String categoryName = editNewCategory.getText().toString().trim();
            if (categoryName.isEmpty()) {
                Toast.makeText(this, R.string.category_name_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            if (categories.contains(categoryName)) {
                Toast.makeText(this, R.string.category_exists, Toast.LENGTH_SHORT).show();
                return;
            }
            // 添加分类
            viewModel.addCategory(categoryName);
            editNewCategory.setText("");
            Toast.makeText(this, R.string.category_added, Toast.LENGTH_SHORT).show();
        });

        // 删除分类监听
        categoryAdapter.setOnCategoryDeleteListener(category -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete_category)
                    .setMessage(getString(R.string.delete_category_message, category))
                    .setPositiveButton(R.string.dialog_confirm, (d, which) -> {
                        viewModel.deleteCategory(category);
                        // 如果删除的是当前选中的分类，切换到"全部"
                        if (category.equals(selectedCategory)) {
                            selectedCategory = getString(R.string.category_all);
                            viewModel.filterByCategory(selectedCategory);
                        }
                        Toast.makeText(this, R.string.category_deleted, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        });

        // 使用专门的观察者来更新对话框中的分类列表
        androidx.lifecycle.Observer<List<String>> categoryObserver = categoryListData -> {
            if (dialog.isShowing()) {
                List<String> updatedCategories = new ArrayList<>();
                if (categoryListData != null) {
                    updatedCategories.addAll(categoryListData);
                }
                // 使用新的列表实例来触发DiffUtil更新
                categoryAdapter.submitList(new ArrayList<>(updatedCategories));
                updateCategoryEmptyState(updatedCategories, categoryList, emptyHint);
            }
        };
        
        // 添加观察者
        viewModel.getCategories().observe(this, categoryObserver);
        
        // 对话框关闭时移除观察者
        dialog.setOnDismissListener(d -> {
            viewModel.getCategories().removeObserver(categoryObserver);
        });

        dialog.show();
    }

    /**
     * 更新分类列表空状态
     */
    private void updateCategoryEmptyState(List<String> categories, RecyclerView list, TextView emptyHint) {
        if (categories == null || categories.isEmpty()) {
            list.setVisibility(View.GONE);
            emptyHint.setVisibility(View.VISIBLE);
        } else {
            list.setVisibility(View.VISIBLE);
            emptyHint.setVisibility(View.GONE);
        }
    }

    /**
     * 显示添加分类对话框（独立使用）
     */
    private void showAddCategoryDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
        EditText editCategoryName = view.findViewById(R.id.edit_category_name);

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    String categoryName = editCategoryName.getText().toString().trim();
                    if (categoryName.isEmpty()) {
                        Toast.makeText(this, R.string.category_name_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (categories.contains(categoryName)) {
                        Toast.makeText(this, R.string.category_exists, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 添加分类
                    viewModel.addCategory(categoryName);
                    Toast.makeText(this, R.string.category_added, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    /**
     * 更新空状态视图
     */
    private void updateEmptyView(List<Novel> novels) {
        BookshelfUiState state = viewModel.getUiState().getValue();
        boolean isEmpty = novels == null || novels.isEmpty();
        boolean isSearching = state != null && state.getSearchQuery() != null 
                && !state.getSearchQuery().isEmpty();

        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            emptyText.setText(isSearching ? R.string.no_search_results : R.string.empty_bookshelf);
        }
    }

    /**
     * 显示导入选项对话框
     */
    private void showImportOptionsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_import_options, null);
        dialog.setContentView(view);

        // 从本地文件导入
        view.findViewById(R.id.option_local_file).setOnClickListener(v -> {
            dialog.dismiss();
            openFilePicker();
        });

        // 从网站URL导入
        view.findViewById(R.id.option_from_url).setOnClickListener(v -> {
            dialog.dismiss();
            // TODO: 跳转到网站解析界面
            Toast.makeText(this, "网站导入功能即将推出", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    /**
     * 打开文件选择器
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"text/plain", "application/epub+zip"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }

    /**
     * 处理文件导入
     */
    private void handleFileImport(Uri uri) {
        // 获取文件名
        String fileName = getFileNameFromUri(uri);
        
        // 显示导入进度对话框
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.import_progress_title)
                .setMessage(getString(R.string.import_progress_hint))
                .setCancelable(false)
                .create();
        progressDialog.show();
        
        // 调用ViewModel导入文件
        viewModel.importFile(uri, fileName);
        
        // 使用一次性观察者来处理导入结果
        // 创建一个标志来防止重复处理
        final boolean[] handled = {false};
        
        viewModel.getUiState().observe(this, state -> {
            if (handled[0]) return;
            
            // 导入成功
            if (state.getImportSuccessMessage() != null && !state.getImportSuccessMessage().isEmpty()) {
                handled[0] = true;
                progressDialog.dismiss();
                Toast.makeText(this, state.getImportSuccessMessage(), Toast.LENGTH_SHORT).show();
                viewModel.clearImportSuccessMessage();
            }
            
            // 导入失败
            if (state.getImportErrorMessage() != null && !state.getImportErrorMessage().isEmpty()) {
                handled[0] = true;
                progressDialog.dismiss();
                Toast.makeText(this, state.getImportErrorMessage(), Toast.LENGTH_LONG).show();
                viewModel.clearImportErrorMessage();
            }
        });
    }
    
    /**
     * 从URI获取文件名
     */
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        
        // 尝试从ContentResolver获取文件名
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(
                    uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                // 忽略错误
            }
        }
        
        // 如果无法获取，使用URI的最后一段
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        
        return fileName != null ? fileName : "unknown";
    }

    /**
     * 显示小说上下文菜单
     */
    private void showNovelContextMenu(Novel novel, View anchorView) {
        List<String> menuItemsList = new ArrayList<>();
        menuItemsList.add(novel.isPinned() ? getString(R.string.menu_unpin) : getString(R.string.menu_pin));
        menuItemsList.add(getString(R.string.menu_set_category));
        menuItemsList.add(getString(R.string.menu_edit));
        menuItemsList.add(getString(R.string.menu_delete));

        String[] menuItems = menuItemsList.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle(novel.getTitle())
                .setItems(menuItems, (dialog, which) -> {
                    switch (which) {
                        case 0: // 置顶/取消置顶
                            togglePinned(novel);
                            break;
                        case 1: // 设置分类
                            showSetCategoryDialog(novel);
                            break;
                        case 2: // 编辑
                            showEditNovelDialog(novel);
                            break;
                        case 3: // 删除
                            showDeleteConfirmDialog(novel);
                            break;
                    }
                })
                .show();
    }

    /**
     * 显示设置分类对话框
     */
    private void showSetCategoryDialog(Novel novel) {
        // 获取用户自定义的分类（排除"全部"）
        List<String> availableCategories = new ArrayList<>();
        for (String category : categories) {
            if (!category.equals(getString(R.string.category_all))) {
                availableCategories.add(category);
            }
        }

        if (availableCategories.isEmpty()) {
            Toast.makeText(this, "请先添加分类", Toast.LENGTH_SHORT).show();
            showAddCategoryDialog();
            return;
        }

        String[] items = availableCategories.toArray(new String[0]);
        int currentIndex = availableCategories.indexOf(novel.getCategory());

        new AlertDialog.Builder(this)
                .setTitle(R.string.set_category)
                .setSingleChoiceItems(items, currentIndex, (dialog, which) -> {
                    String newCategory = availableCategories.get(which);
                    viewModel.updateNovelCategory(novel.getId(), newCategory);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    /**
     * 切换置顶状态
     */
    private void togglePinned(Novel novel) {
        boolean newPinnedState = !novel.isPinned();
        viewModel.togglePinned(novel.getId(), newPinnedState);
        Toast.makeText(this, 
                newPinnedState ? R.string.toast_pin_success : R.string.toast_unpin_success, 
                Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(Novel novel) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, novel.getTitle()))
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    viewModel.deleteNovel(novel.getId());
                    Toast.makeText(this, R.string.toast_delete_success, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    /**
     * 显示编辑小说对话框
     */
    private void showEditNovelDialog(Novel novel) {
        editingNovel = novel;
        selectedCoverPath = novel.getCoverPath();
        
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_novel, null);
        
        editCoverImageView = view.findViewById(R.id.img_cover);
        TextView btnChangeCover = view.findViewById(R.id.btn_change_cover);
        com.google.android.material.textfield.TextInputEditText editTitle = view.findViewById(R.id.edit_title);
        com.google.android.material.textfield.TextInputEditText editAuthor = view.findViewById(R.id.edit_author);
        
        // 设置当前值
        editTitle.setText(novel.getTitle());
        editAuthor.setText(novel.getAuthor());
        
        // 加载当前封面
        if (novel.getCoverPath() != null && !novel.getCoverPath().isEmpty()) {
            java.io.File coverFile = new java.io.File(novel.getCoverPath());
            if (coverFile.exists()) {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(novel.getCoverPath());
                if (bitmap != null) {
                    editCoverImageView.setImageBitmap(bitmap);
                }
            }
        }
        
        // 更换封面按钮点击
        btnChangeCover.setOnClickListener(v -> openImagePicker());
        editCoverImageView.setOnClickListener(v -> openImagePicker());
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_novel_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_confirm, null) // 稍后设置点击事件
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newTitle = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
                String newAuthor = editAuthor.getText() != null ? editAuthor.getText().toString().trim() : "";
                
                if (newTitle.isEmpty()) {
                    Toast.makeText(this, R.string.edit_title_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 更新小说信息
                viewModel.updateNovelInfo(novel.getId(), newTitle, newAuthor, selectedCoverPath);
                Toast.makeText(this, R.string.edit_success, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                
                // 清理临时变量
                editingNovel = null;
                editCoverImageView = null;
                selectedCoverPath = null;
            });
        });
        
        dialog.setOnDismissListener(d -> {
            editingNovel = null;
            editCoverImageView = null;
            selectedCoverPath = null;
        });
        
        dialog.show();
    }

    /**
     * 打开图片选择器
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从阅读器返回时刷新数据，更新阅读进度显示
        viewModel.refresh();
    }
}
