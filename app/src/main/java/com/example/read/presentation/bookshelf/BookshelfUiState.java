package com.example.read.presentation.bookshelf;

import com.example.read.domain.model.Novel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 书架界面UI状态
 */
public class BookshelfUiState {
    private List<Novel> novels;
    private boolean isLoading;
    private String error;
    private String searchQuery;
    private String selectedCategory;
    private List<String> categories;
    
    // 文件导入状态
    private boolean isImporting;
    private String importFileName;
    private String importSuccessMessage;
    private String importErrorMessage;
    
    // 批量模式状态
    private boolean isBatchMode;
    private Set<Long> selectedNovelIds;

    public BookshelfUiState() {
        this.novels = new ArrayList<>();
        this.isLoading = false;
        this.error = null;
        this.searchQuery = "";
        this.selectedCategory = "全部";
        this.categories = new ArrayList<>();
        this.isImporting = false;
        this.importFileName = null;
        this.importSuccessMessage = null;
        this.importErrorMessage = null;
        this.isBatchMode = false;
        this.selectedNovelIds = new HashSet<>();
    }

    // 复制构造函数，用于创建不可变状态的副本
    public BookshelfUiState(BookshelfUiState other) {
        this.novels = new ArrayList<>(other.novels);
        this.isLoading = other.isLoading;
        this.error = other.error;
        this.searchQuery = other.searchQuery;
        this.selectedCategory = other.selectedCategory;
        this.categories = new ArrayList<>(other.categories);
        this.isImporting = other.isImporting;
        this.importFileName = other.importFileName;
        this.importSuccessMessage = other.importSuccessMessage;
        this.importErrorMessage = other.importErrorMessage;
        this.isBatchMode = other.isBatchMode;
        this.selectedNovelIds = new HashSet<>(other.selectedNovelIds);
    }

    // Getters
    public List<Novel> getNovels() { return novels; }
    public boolean isLoading() { return isLoading; }
    public String getError() { return error; }
    public String getSearchQuery() { return searchQuery; }
    public String getSelectedCategory() { return selectedCategory; }
    public List<String> getCategories() { return categories; }
    public boolean isImporting() { return isImporting; }
    public String getImportFileName() { return importFileName; }
    public String getImportSuccessMessage() { return importSuccessMessage; }
    public String getImportErrorMessage() { return importErrorMessage; }
    public boolean isBatchMode() { return isBatchMode; }
    public Set<Long> getSelectedNovelIds() { return selectedNovelIds; }
    
    /**
     * 获取选中的小说数量
     */
    public int getSelectedCount() { 
        return selectedNovelIds != null ? selectedNovelIds.size() : 0; 
    }
    
    /**
     * 检查是否全选
     */
    public boolean isAllSelected() {
        if (novels == null || novels.isEmpty() || selectedNovelIds == null) {
            return false;
        }
        return selectedNovelIds.size() == novels.size();
    }

    // Setters
    public void setNovels(List<Novel> novels) { this.novels = novels != null ? novels : new ArrayList<>(); }
    public void setLoading(boolean loading) { isLoading = loading; }
    public void setError(String error) { this.error = error; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery != null ? searchQuery : ""; }
    public void setSelectedCategory(String selectedCategory) { this.selectedCategory = selectedCategory != null ? selectedCategory : "全部"; }
    public void setCategories(List<String> categories) { this.categories = categories != null ? categories : new ArrayList<>(); }
    public void setImporting(boolean importing) { isImporting = importing; }
    public void setImportFileName(String importFileName) { this.importFileName = importFileName; }
    public void setImportSuccessMessage(String importSuccessMessage) { this.importSuccessMessage = importSuccessMessage; }
    public void setImportErrorMessage(String importErrorMessage) { this.importErrorMessage = importErrorMessage; }
    public void setBatchMode(boolean batchMode) { isBatchMode = batchMode; }
    public void setSelectedNovelIds(Set<Long> selectedNovelIds) { this.selectedNovelIds = selectedNovelIds != null ? selectedNovelIds : new HashSet<>(); }

    /**
     * 创建一个带有加载状态的新状态
     */
    public static BookshelfUiState loading() {
        BookshelfUiState state = new BookshelfUiState();
        state.setLoading(true);
        return state;
    }

    /**
     * 创建一个带有错误信息的新状态
     */
    public static BookshelfUiState error(String errorMessage) {
        BookshelfUiState state = new BookshelfUiState();
        state.setError(errorMessage);
        return state;
    }

    /**
     * 创建一个带有小说列表的新状态
     */
    public static BookshelfUiState success(List<Novel> novels) {
        BookshelfUiState state = new BookshelfUiState();
        state.setNovels(novels);
        return state;
    }
}
