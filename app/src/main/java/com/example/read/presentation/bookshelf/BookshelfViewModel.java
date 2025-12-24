package com.example.read.presentation.bookshelf;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.read.domain.model.Novel;
import com.example.read.domain.repository.FileImportRepository;
import com.example.read.domain.repository.NovelRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 书架ViewModel - 管理书架界面的业务逻辑和UI状态
 * 
 * 验证需求：4.1, 4.4, 4.5, 4.6, 1.1, 1.2, 1.3, 1.4, 1.5
 */
@HiltViewModel
public class BookshelfViewModel extends ViewModel {

    private static final String TAG = "BookshelfViewModel";

    private final NovelRepository novelRepository;
    private final FileImportRepository fileImportRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // UI状态
    private final MutableLiveData<BookshelfUiState> _uiState = new MutableLiveData<>(new BookshelfUiState());
    public LiveData<BookshelfUiState> getUiState() { return _uiState; }

    // 当前数据源LiveData
    private LiveData<List<Novel>> currentNovelsSource;
    private final MediatorLiveData<List<Novel>> novelsMediator = new MediatorLiveData<>();

    // 分类列表
    private LiveData<List<String>> categoriesSource;

    @Inject
    public BookshelfViewModel(NovelRepository novelRepository, FileImportRepository fileImportRepository) {
        this.novelRepository = novelRepository;
        this.fileImportRepository = fileImportRepository;
        
        // 初始化加载
        loadNovels();
        loadCategories();
    }

    /**
     * 加载小说列表
     * 验证需求：4.1 - 打开书架界面显示所有已导入小说的列表
     */
    public void loadNovels() {
        Log.d(TAG, "loadNovels 开始");
        updateState(state -> {
            state.setLoading(true);
            state.setError(null);
        });

        // 移除旧的数据源
        if (currentNovelsSource != null) {
            novelsMediator.removeSource(currentNovelsSource);
        }

        // 根据当前状态决定数据源
        BookshelfUiState currentState = _uiState.getValue();
        String searchQuery = currentState != null ? currentState.getSearchQuery() : "";
        String category = currentState != null ? currentState.getSelectedCategory() : "全部";

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            // 搜索模式
            Log.d(TAG, "搜索模式: " + searchQuery);
            currentNovelsSource = novelRepository.searchNovels(searchQuery.trim());
        } else if (category != null && !category.isEmpty() && !"全部".equals(category)) {
            // 分类筛选模式
            Log.d(TAG, "分类筛选模式: " + category);
            currentNovelsSource = novelRepository.getNovelsByCategory(category);
        } else {
            // 默认加载所有小说
            Log.d(TAG, "加载所有小说");
            currentNovelsSource = novelRepository.getAllNovels();
        }

        // 添加新的数据源
        novelsMediator.addSource(currentNovelsSource, novels -> {
            Log.d(TAG, "收到小说列表更新，数量: " + (novels != null ? novels.size() : 0));
            // 直接更新MediatorLiveData的值，让观察者收到通知
            novelsMediator.setValue(novels);
            updateState(state -> {
                state.setNovels(novels != null ? novels : new ArrayList<>());
                state.setLoading(false);
            });
        });
    }


    /**
     * 加载分类列表
     */
    private void loadCategories() {
        categoriesSource = novelRepository.getAllCategories();
        // 注意：这里需要在Activity/Fragment中观察categoriesSource
    }

    /**
     * 获取分类列表LiveData
     */
    public LiveData<List<String>> getCategories() {
        return novelRepository.getAllCategories();
    }

    /**
     * 搜索小说
     * 验证需求：4.5 - 根据标题或作者关键词过滤显示结果
     * 
     * @param keyword 搜索关键词
     */
    public void searchNovels(String keyword) {
        Log.d(TAG, "searchNovels 被调用，关键词: " + keyword);
        
        // 使用setValue同步更新状态
        BookshelfUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new BookshelfUiState();
        }
        BookshelfUiState newState = new BookshelfUiState(currentState);
        newState.setSearchQuery(keyword);
        newState.setSelectedCategory("全部"); // 搜索时重置分类
        _uiState.setValue(newState);
        
        // 然后加载小说
        loadNovelsWithSearch(keyword);
    }
    
    /**
     * 根据搜索关键词加载小说（内部方法）
     */
    private void loadNovelsWithSearch(String keyword) {
        Log.d(TAG, "loadNovelsWithSearch 开始，关键词: " + keyword);
        
        // 设置加载状态
        BookshelfUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new BookshelfUiState();
        }
        BookshelfUiState newState = new BookshelfUiState(currentState);
        newState.setLoading(true);
        newState.setError(null);
        _uiState.setValue(newState);

        // 移除旧的数据源
        if (currentNovelsSource != null) {
            novelsMediator.removeSource(currentNovelsSource);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 搜索模式
            Log.d(TAG, "使用搜索: " + keyword);
            currentNovelsSource = novelRepository.searchNovels(keyword.trim());
        } else {
            // 默认加载所有小说
            Log.d(TAG, "加载所有小说");
            currentNovelsSource = novelRepository.getAllNovels();
        }

        // 添加新的数据源
        novelsMediator.addSource(currentNovelsSource, novels -> {
            Log.d(TAG, "收到小说列表更新，数量: " + (novels != null ? novels.size() : 0));
            novelsMediator.setValue(novels);
            
            BookshelfUiState state = _uiState.getValue();
            if (state == null) {
                state = new BookshelfUiState();
            }
            BookshelfUiState updatedState = new BookshelfUiState(state);
            updatedState.setNovels(novels != null ? novels : new ArrayList<>());
            updatedState.setLoading(false);
            _uiState.setValue(updatedState);
        });
    }

    /**
     * 清除搜索
     */
    public void clearSearch() {
        Log.d(TAG, "clearSearch 被调用");
        
        // 使用setValue同步更新状态
        BookshelfUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new BookshelfUiState();
        }
        BookshelfUiState newState = new BookshelfUiState(currentState);
        newState.setSearchQuery("");
        _uiState.setValue(newState);
        
        loadNovels();
    }

    /**
     * 按分类筛选小说
     * 验证需求：4.6 - 显示符合指定分类的小说
     * 
     * @param category 分类名称，"全部"表示不筛选
     */
    public void filterByCategory(String category) {
        Log.d(TAG, "filterByCategory 被调用，分类: " + category);
        
        // 直接使用setValue而不是postValue，确保状态立即更新
        BookshelfUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new BookshelfUiState();
        }
        BookshelfUiState newState = new BookshelfUiState(currentState);
        newState.setSelectedCategory(category);
        newState.setSearchQuery(""); // 筛选时清除搜索
        _uiState.setValue(newState);
        
        // 然后加载小说
        loadNovelsWithCategory(category);
    }
    
    /**
     * 根据指定分类加载小说（内部方法）
     */
    private void loadNovelsWithCategory(String category) {
        Log.d(TAG, "loadNovelsWithCategory 开始，分类: " + category);
        
        // 设置加载状态
        BookshelfUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new BookshelfUiState();
        }
        BookshelfUiState newState = new BookshelfUiState(currentState);
        newState.setLoading(true);
        newState.setError(null);
        _uiState.setValue(newState);

        // 移除旧的数据源
        if (currentNovelsSource != null) {
            novelsMediator.removeSource(currentNovelsSource);
        }

        if (category != null && !category.isEmpty() && !"全部".equals(category)) {
            // 分类筛选模式
            Log.d(TAG, "使用分类筛选: " + category);
            currentNovelsSource = novelRepository.getNovelsByCategory(category);
        } else {
            // 默认加载所有小说
            Log.d(TAG, "加载所有小说");
            currentNovelsSource = novelRepository.getAllNovels();
        }

        // 添加新的数据源
        novelsMediator.addSource(currentNovelsSource, novels -> {
            Log.d(TAG, "收到小说列表更新，数量: " + (novels != null ? novels.size() : 0));
            novelsMediator.setValue(novels);
            
            BookshelfUiState state = _uiState.getValue();
            if (state == null) {
                state = new BookshelfUiState();
            }
            BookshelfUiState updatedState = new BookshelfUiState(state);
            updatedState.setNovels(novels != null ? novels : new ArrayList<>());
            updatedState.setLoading(false);
            _uiState.setValue(updatedState);
        });
    }

    /**
     * 删除小说
     * 验证需求：4.4 - 从本地存储中移除该小说及其所有章节数据
     * 
     * @param novelId 小说ID
     */
    public void deleteNovel(long novelId) {
        disposables.add(
            io.reactivex.rxjava3.core.Completable.fromAction(() -> novelRepository.deleteNovel(novelId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {
                        // 删除成功后，LiveData会自动更新列表
                    },
                    error -> {
                        updateState(state -> {
                            state.setError("删除小说失败: " + error.getMessage());
                        });
                    }
                )
        );
    }

    /**
     * 置顶/取消置顶小说
     * 
     * @param novelId 小说ID
     * @param isPinned 是否置顶
     */
    public void togglePinned(long novelId, boolean isPinned) {
        disposables.add(
            io.reactivex.rxjava3.core.Completable.fromAction(() -> novelRepository.updatePinned(novelId, isPinned))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {
                        // 更新成功后，LiveData会自动更新列表
                    },
                    error -> {
                        updateState(state -> {
                            state.setError("操作失败: " + error.getMessage());
                        });
                    }
                )
        );
    }

    /**
     * 添加分类
     * 
     * @param categoryName 分类名称
     */
    public void addCategory(String categoryName) {
        disposables.add(
            io.reactivex.rxjava3.core.Completable.fromAction(() -> novelRepository.addCategory(categoryName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {
                        // 添加成功后，LiveData会自动更新分类列表
                    },
                    error -> {
                        updateState(state -> {
                            state.setError("添加分类失败: " + error.getMessage());
                        });
                    }
                )
        );
    }

    /**
     * 删除分类
     * 
     * @param categoryName 分类名称
     */
    public void deleteCategory(String categoryName) {
        disposables.add(
            io.reactivex.rxjava3.core.Completable.fromAction(() -> novelRepository.deleteCategory(categoryName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {
                        // 删除成功后，LiveData会自动更新分类列表
                    },
                    error -> {
                        updateState(state -> {
                            state.setError("删除分类失败: " + error.getMessage());
                        });
                    }
                )
        );
    }

    /**
     * 更新小说分类
     * 
     * @param novelId 小说ID
     * @param category 新分类
     */
    public void updateNovelCategory(long novelId, String category) {
        disposables.add(
            io.reactivex.rxjava3.core.Completable.fromAction(() -> novelRepository.updateCategory(novelId, category))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {
                        // 更新成功后，LiveData会自动更新列表
                    },
                    error -> {
                        updateState(state -> {
                            state.setError("设置分类失败: " + error.getMessage());
                        });
                    }
                )
        );
    }

    /**
     * 更新小说信息（标题、作者、封面）
     * 
     * @param novelId 小说ID
     * @param title 新标题
     * @param author 新作者
     * @param coverPath 新封面路径（可为null表示不更新）
     */
    public void updateNovelInfo(long novelId, String title, String author, String coverPath) {
        disposables.add(
            io.reactivex.rxjava3.core.Completable.fromAction(() -> {
                Novel novel = novelRepository.getNovelById(novelId);
                if (novel != null) {
                    novel.setTitle(title);
                    novel.setAuthor(author);
                    if (coverPath != null) {
                        novel.setCoverPath(coverPath);
                    }
                    novelRepository.updateNovel(novel);
                }
            })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {
                        // 更新成功后，LiveData会自动更新列表
                        Log.d(TAG, "小说信息更新成功");
                    },
                    error -> {
                        updateState(state -> {
                            state.setError("更新小说信息失败: " + error.getMessage());
                        });
                    }
                )
        );
    }

    /**
     * 导入文件
     * 验证需求：1.1, 1.2, 1.3, 1.4, 1.5
     * 
     * @param uri 文件URI
     * @param fileName 文件名
     */
    public void importFile(Uri uri, String fileName) {
        Log.d(TAG, "开始导入文件: " + fileName + ", URI: " + uri);
        
        // 设置导入中状态（在主线程）
        setImportingState(true, fileName);

        // 根据文件扩展名选择导入方式
        String lowerFileName = fileName != null ? fileName.toLowerCase() : "";
        
        io.reactivex.rxjava3.core.Single<Novel> importSingle;
        if (lowerFileName.endsWith(".epub")) {
            Log.d(TAG, "使用EPUB导入方式");
            // 验证需求：1.3 - 解析EPUB结构并提取章节信息
            importSingle = fileImportRepository.importEpubFile(uri);
        } else {
            Log.d(TAG, "使用TXT导入方式");
            // 验证需求：1.2 - 解析TXT文件内容并创建小说条目
            importSingle = fileImportRepository.importTxtFile(uri);
        }

        disposables.add(
            importSingle
                .doOnSubscribe(d -> Log.d(TAG, "订阅导入操作"))
                .doOnSuccess(novel -> Log.d(TAG, "导入成功: " + novel.getTitle()))
                .doOnError(e -> Log.e(TAG, "导入失败", e))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    novel -> {
                        Log.d(TAG, "更新UI - 导入成功，准备关闭对话框");
                        // 验证需求：1.5 - 导入成功完成，将小说添加到书架并显示成功提示
                        setImportSuccessState("《" + novel.getTitle() + "》导入成功");
                        // 刷新小说列表
                        loadNovels();
                    },
                    error -> {
                        Log.e(TAG, "更新UI - 导入失败", error);
                        // 验证需求：1.4 - 导入过程中发生错误，显示明确的错误提示信息
                        String msg = error.getMessage();
                        if (msg == null || msg.isEmpty()) {
                            msg = "导入失败，请检查文件格式";
                        }
                        setImportErrorState(msg);
                    }
                )
        );
    }
    
    /**
     * 设置导入中状态
     */
    private void setImportingState(boolean importing, String fileName) {
        BookshelfUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new BookshelfUiState();
        }
        BookshelfUiState newState = new BookshelfUiState(currentState);
        newState.setImporting(importing);
        newState.setImportFileName(fileName);
        newState.setImportSuccessMessage(null);
        newState.setImportErrorMessage(null);
        _uiState.setValue(newState);
        Log.d(TAG, "设置导入状态: importing=" + importing);
    }
    
    /**
     * 设置导入成功状态
     */
    private void setImportSuccessState(String message) {
        BookshelfUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new BookshelfUiState();
        }
        BookshelfUiState newState = new BookshelfUiState(currentState);
        newState.setImporting(false);
        newState.setImportFileName(null);
        newState.setImportSuccessMessage(message);
        newState.setImportErrorMessage(null);
        _uiState.setValue(newState);
        Log.d(TAG, "设置导入成功状态: " + message);
    }
    
    /**
     * 设置导入错误状态
     */
    private void setImportErrorState(String errorMessage) {
        BookshelfUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new BookshelfUiState();
        }
        BookshelfUiState newState = new BookshelfUiState(currentState);
        newState.setImporting(false);
        newState.setImportFileName(null);
        newState.setImportSuccessMessage(null);
        newState.setImportErrorMessage(errorMessage);
        _uiState.setValue(newState);
        Log.d(TAG, "设置导入错误状态: " + errorMessage);
    }

    /**
     * 清除导入成功消息
     */
    public void clearImportSuccessMessage() {
        updateState(state -> {
            state.setImportSuccessMessage(null);
        });
    }

    /**
     * 清除导入错误消息
     */
    public void clearImportErrorMessage() {
        updateState(state -> {
            state.setImportErrorMessage(null);
        });
    }

    /**
     * 清除错误信息
     */
    public void clearError() {
        updateState(state -> {
            state.setError(null);
        });
    }

    /**
     * 刷新小说列表
     */
    public void refresh() {
        loadNovels();
    }

    /**
     * 更新UI状态的辅助方法
     */
    private void updateState(StateUpdater updater) {
        BookshelfUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new BookshelfUiState();
        }
        BookshelfUiState newState = new BookshelfUiState(currentState);
        updater.update(newState);
        _uiState.postValue(newState);
    }

    /**
     * 状态更新接口
     */
    private interface StateUpdater {
        void update(BookshelfUiState state);
    }

    /**
     * 获取小说列表的MediatorLiveData，用于观察
     */
    public LiveData<List<Novel>> getNovelsLiveData() {
        return novelsMediator;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
