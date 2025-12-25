package com.example.read.presentation.parser;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.read.domain.model.ChapterInfo;
import com.example.read.domain.model.Novel;
import com.example.read.domain.model.NovelMetadata;
import com.example.read.domain.model.ParserRule;
import com.example.read.domain.repository.ParserRuleRepository;
import com.example.read.domain.repository.WebParserRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 网站解析ViewModel - 管理网站解析界面的业务逻辑和UI状态
 * 
 * 验证需求：2.1, 2.2, 2.3, 2.4
 */
@HiltViewModel
public class ParserViewModel extends ViewModel {

    private final WebParserRepository webParserRepository;
    private final ParserRuleRepository parserRuleRepository;
    private final ExecutorService executorService;
    private final CompositeDisposable disposables;

    // UI状态
    private final MutableLiveData<ParserUiState> _uiState = new MutableLiveData<>(new ParserUiState());
    public LiveData<ParserUiState> getUiState() { return _uiState; }

    // 解析规则LiveData
    private LiveData<List<ParserRule>> rulesLiveData;
    private final Observer<List<ParserRule>> rulesObserver;

    @Inject
    public ParserViewModel(WebParserRepository webParserRepository, 
                          ParserRuleRepository parserRuleRepository) {
        this.webParserRepository = webParserRepository;
        this.parserRuleRepository = parserRuleRepository;
        this.executorService = Executors.newSingleThreadExecutor();
        this.disposables = new CompositeDisposable();

        // 创建规则观察者
        this.rulesObserver = rules -> {
            updateState(state -> {
                state.setAvailableRules(rules);
                // 如果当前没有选中规则且有可用规则，自动选择第一个
                if (state.getSelectedRule() == null && rules != null && !rules.isEmpty()) {
                    state.setSelectedRule(rules.get(0));
                }
            });
        };

        // 加载解析规则
        loadParserRules();
    }

    /**
     * 加载解析规则列表
     */
    private void loadParserRules() {
        rulesLiveData = parserRuleRepository.getAllRules();
    }

    /**
     * 获取解析规则LiveData，供Activity/Fragment观察
     */
    public LiveData<List<ParserRule>> getRulesLiveData() {
        return rulesLiveData;
    }

    /**
     * 更新解析规则列表
     */
    public void updateRules(List<ParserRule> rules) {
        rulesObserver.onChanged(rules);
    }


    /**
     * 更新URL输入
     * 验证需求：2.1 - 验证URL格式的有效性
     * 
     * @param url 用户输入的URL
     */
    public void updateUrl(String url) {
        boolean isValid = webParserRepository.isValidUrl(url);
        updateState(state -> {
            state.setUrl(url);
            state.setUrlValid(isValid);
            // 清除之前的解析结果
            if (!url.equals(state.getUrl())) {
                state.setNovelMetadata(null);
                state.setChapters(null);
                state.setParseState(ParserUiState.ParseState.IDLE);
            }
        });
    }

    /**
     * 选择解析规则
     * 
     * @param rule 选中的解析规则
     */
    public void selectRule(ParserRule rule) {
        updateState(state -> {
            state.setSelectedRule(rule);
            // 清除之前的解析结果
            state.setNovelMetadata(null);
            state.setChapters(null);
            state.setParseState(ParserUiState.ParseState.IDLE);
        });
    }

    /**
     * 解析小说元数据
     * 验证需求：2.2 - 访问网页并提取小说元数据（标题、作者、简介）
     */
    public void parseNovelMetadata() {
        ParserUiState currentState = _uiState.getValue();
        if (currentState == null) return;

        String url = currentState.getUrl();
        ParserRule rule = currentState.getSelectedRule();

        // 验证输入
        if (!currentState.isUrlValid()) {
            updateState(state -> state.setError("请输入有效的URL"));
            return;
        }

        if (rule == null) {
            updateState(state -> state.setError("请选择解析规则"));
            return;
        }

        // 开始解析
        updateState(state -> {
            state.setParsing(true);
            state.setError(null);
            state.setParseState(ParserUiState.ParseState.PARSING_META);
        });

        Disposable disposable = webParserRepository.parseNovelMetadata(url, rule)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        metadata -> {
                            updateState(state -> {
                                state.setNovelMetadata(metadata);
                                state.setParsing(false);
                            });
                            // 自动解析章节列表
                            parseChapterList();
                        },
                        error -> {
                            updateState(state -> {
                                state.setParsing(false);
                                state.setError("解析元数据失败: " + error.getMessage());
                                state.setParseState(ParserUiState.ParseState.ERROR);
                            });
                        }
                );
        disposables.add(disposable);
    }

    /**
     * 解析章节列表
     * 验证需求：2.3 - 提取所有章节的标题和链接
     */
    public void parseChapterList() {
        ParserUiState currentState = _uiState.getValue();
        if (currentState == null) return;

        String url = currentState.getUrl();
        ParserRule rule = currentState.getSelectedRule();

        if (url == null || url.isEmpty() || rule == null) {
            return;
        }

        updateState(state -> {
            state.setParsing(true);
            state.setParseState(ParserUiState.ParseState.PARSING_LIST);
        });

        Disposable disposable = webParserRepository.parseChapterList(url, rule)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        chapters -> {
                            updateState(state -> {
                                state.setChapters(chapters);
                                state.setParsing(false);
                                state.setParseState(ParserUiState.ParseState.PARSED);
                            });
                        },
                        error -> {
                            updateState(state -> {
                                state.setParsing(false);
                                state.setError("解析章节列表失败: " + error.getMessage());
                                state.setParseState(ParserUiState.ParseState.ERROR);
                            });
                        }
                );
        disposables.add(disposable);
    }


    /**
     * 开始下载小说
     * 验证需求：2.4 - 按顺序获取每个章节的正文内容
     */
    public void startDownload() {
        ParserUiState currentState = _uiState.getValue();
        if (currentState == null) return;

        String url = currentState.getUrl();
        ParserRule rule = currentState.getSelectedRule();
        List<ChapterInfo> chapters = currentState.getChapters();

        // 验证状态
        if (url == null || url.isEmpty()) {
            updateState(state -> state.setError("URL不能为空"));
            return;
        }

        if (rule == null) {
            updateState(state -> state.setError("请选择解析规则"));
            return;
        }

        if (chapters == null || chapters.isEmpty()) {
            updateState(state -> state.setError("请先解析章节列表"));
            return;
        }

        // 检查是否已存在相同URL的小说
        final int currentChapterCount = chapters.size();
        Disposable disposable = webParserRepository.checkExistingNovel(url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        existingInfo -> {
                            // novelId == -1 表示不存在
                            if (existingInfo != null && existingInfo.getNovelId() > 0) {
                                // 小说已存在，比较已下载章节数和当前解析出的章节数
                                int downloadedCount = existingInfo.getDownloadedChapters();
                                boolean isComplete = downloadedCount >= currentChapterCount;
                                
                                updateState(state -> {
                                    state.setExistingNovelId(existingInfo.getNovelId());
                                    state.setExistingNovelTitle(existingInfo.getTitle());
                                    state.setExistingDownloadedCount(downloadedCount);
                                    state.setExistingNovelComplete(isComplete);
                                    state.setShowResumeDialog(true);
                                });
                            } else {
                                // 不存在，直接开始新下载
                                doStartDownload(url, rule, chapters);
                            }
                        },
                        error -> {
                            // 检查失败，直接开始新下载
                            doStartDownload(url, rule, chapters);
                        }
                );
        disposables.add(disposable);
    }
    
    /**
     * 继续下载（断点续传）
     */
    public void resumeDownload() {
        ParserUiState currentState = _uiState.getValue();
        if (currentState == null) return;
        
        Long novelId = currentState.getExistingNovelId();
        String url = currentState.getUrl();
        ParserRule rule = currentState.getSelectedRule();
        List<ChapterInfo> chapters = currentState.getChapters();
        
        if (novelId == null || url == null || rule == null) {
            updateState(state -> state.setError("续传参数无效"));
            return;
        }
        
        // 关闭对话框
        updateState(state -> state.setShowResumeDialog(false));
        
        // 开始续传下载
        updateState(state -> {
            state.setDownloading(true);
            state.setError(null);
            state.setParseState(ParserUiState.ParseState.DOWNLOADING);
            state.setDownloadProgress(new ParserUiState.DownloadProgress(
                    currentState.getExistingDownloadedCount(), 
                    chapters != null ? chapters.size() : 0, 
                    ""));
        });
        
        WebParserRepository.ProgressCallback progressCallback = (current, total, currentChapterTitle) -> {
            updateState(state -> {
                state.setDownloadProgress(new ParserUiState.DownloadProgress(current, total, currentChapterTitle));
            });
        };
        
        Disposable disposable = webParserRepository.resumeDownload(novelId, url, rule, progressCallback)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        novel -> {
                            updateState(state -> {
                                state.setDownloading(false);
                                state.setParseState(ParserUiState.ParseState.COMPLETED);
                            });
                            onDownloadCompleted(novel);
                        },
                        error -> {
                            updateState(state -> {
                                state.setDownloading(false);
                                state.setError("续传下载失败: " + error.getMessage());
                                state.setParseState(ParserUiState.ParseState.ERROR);
                            });
                        }
                );
        disposables.add(disposable);
    }
    
    /**
     * 重新下载（忽略已存在的小说）
     */
    public void restartDownload() {
        ParserUiState currentState = _uiState.getValue();
        if (currentState == null) return;
        
        // 关闭对话框
        updateState(state -> state.setShowResumeDialog(false));
        
        // 开始新下载
        doStartDownload(currentState.getUrl(), currentState.getSelectedRule(), currentState.getChapters());
    }
    
    /**
     * 取消续传对话框
     */
    public void dismissResumeDialog() {
        updateState(state -> state.setShowResumeDialog(false));
    }
    
    /**
     * 执行下载
     */
    private void doStartDownload(String url, ParserRule rule, List<ChapterInfo> chapters) {
        // 开始下载
        updateState(state -> {
            state.setDownloading(true);
            state.setError(null);
            state.setParseState(ParserUiState.ParseState.DOWNLOADING);
            state.setDownloadProgress(new ParserUiState.DownloadProgress(0, chapters.size(), ""));
        });

        // 创建进度回调
        WebParserRepository.ProgressCallback progressCallback = (current, total, currentChapterTitle) -> {
            updateState(state -> {
                state.setDownloadProgress(new ParserUiState.DownloadProgress(current, total, currentChapterTitle));
            });
        };

        Disposable disposable = webParserRepository.downloadNovel(url, rule, progressCallback)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        novel -> {
                            updateState(state -> {
                                state.setDownloading(false);
                                state.setParseState(ParserUiState.ParseState.COMPLETED);
                            });
                            // 通知下载完成
                            onDownloadCompleted(novel);
                        },
                        error -> {
                            updateState(state -> {
                                state.setDownloading(false);
                                state.setError("下载失败: " + error.getMessage());
                                state.setParseState(ParserUiState.ParseState.ERROR);
                            });
                        }
                );
        disposables.add(disposable);
    }

    /**
     * 取消下载
     */
    public void cancelDownload() {
        webParserRepository.cancelDownload();
        updateState(state -> {
            state.setDownloading(false);
            state.setParseState(ParserUiState.ParseState.PARSED);
            state.setDownloadProgress(null);
        });
    }

    /**
     * 下载完成回调
     * 子类或Activity可以重写此方法处理下载完成事件
     */
    protected void onDownloadCompleted(Novel novel) {
        // 默认实现为空，可由子类或通过LiveData事件处理
    }

    /**
     * 重置状态
     */
    public void reset() {
        // 取消正在进行的下载
        if (webParserRepository.isDownloading()) {
            webParserRepository.cancelDownload();
        }
        
        updateState(state -> {
            state.setUrl("");
            state.setUrlValid(false);
            state.setNovelMetadata(null);
            state.setChapters(null);
            state.setDownloadProgress(null);
            state.setLoading(false);
            state.setParsing(false);
            state.setDownloading(false);
            state.setError(null);
            state.setParseState(ParserUiState.ParseState.IDLE);
        });
    }

    /**
     * 清除错误信息
     */
    public void clearError() {
        updateState(state -> state.setError(null));
    }

    /**
     * 检查是否可以开始解析
     */
    public boolean canParse() {
        ParserUiState state = _uiState.getValue();
        if (state == null) return false;
        return state.isUrlValid() && state.getSelectedRule() != null && !state.isParsing();
    }

    /**
     * 检查是否可以开始下载
     */
    public boolean canDownload() {
        ParserUiState state = _uiState.getValue();
        if (state == null) return false;
        return state.getChapters() != null && !state.getChapters().isEmpty() 
                && !state.isDownloading();
    }

    /**
     * 更新UI状态的辅助方法
     */
    private void updateState(StateUpdater updater) {
        ParserUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new ParserUiState();
        }
        ParserUiState newState = new ParserUiState(currentState);
        updater.update(newState);
        _uiState.postValue(newState);
    }

    /**
     * 状态更新接口
     */
    private interface StateUpdater {
        void update(ParserUiState state);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        executorService.shutdown();
        
        // 取消正在进行的下载
        if (webParserRepository.isDownloading()) {
            webParserRepository.cancelDownload();
        }
    }
}
