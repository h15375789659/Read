package com.example.read.presentation.reader;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.read.domain.model.Bookmark;
import com.example.read.domain.model.Chapter;
import com.example.read.domain.model.Novel;
import com.example.read.domain.model.PageAnimation;
import com.example.read.domain.model.PageMode;
import com.example.read.domain.model.ReaderFont;
import com.example.read.domain.model.ReaderTheme;
import com.example.read.domain.model.ReadingPosition;
import com.example.read.domain.model.SearchResult;
import com.example.read.domain.model.TTSState;
import com.example.read.domain.model.VoiceInfo;
import com.example.read.domain.repository.AIServiceRepository;
import com.example.read.domain.repository.BlockedWordRepository;
import com.example.read.domain.repository.BookmarkRepository;
import com.example.read.domain.repository.NovelRepository;
import com.example.read.domain.repository.SettingsRepository;
import com.example.read.domain.repository.StatisticsRepository;
import com.example.read.domain.repository.ThemeRepository;
import com.example.read.domain.repository.TTSRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * 阅读器ViewModel - 管理阅读界面的业务逻辑和UI状态
 * 
 * 验证需求：5.1, 5.4, 5.5, 5.6, 5.7, 6.2, 9.2, 10.1, 10.2, 11.4
 */
@HiltViewModel
public class ReaderViewModel extends ViewModel {

    private final NovelRepository novelRepository;
    private final SettingsRepository settingsRepository;
    private final ThemeRepository themeRepository;
    private final BlockedWordRepository blockedWordRepository;
    private final StatisticsRepository statisticsRepository;
    private final TTSRepository ttsRepository;
    private final BookmarkRepository bookmarkRepository;
    private final AIServiceRepository aiServiceRepository;
    private final ExecutorService executorService;
    
    // RxJava订阅管理
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // UI状态
    private final MutableLiveData<ReaderUiState> _uiState = new MutableLiveData<>(new ReaderUiState());
    public LiveData<ReaderUiState> getUiState() { return _uiState; }

    // 当前小说ID
    private long currentNovelId = -1;
    
    // 屏蔽词列表缓存
    private List<String> blockedWords = new ArrayList<>();
    
    // TTS状态观察者
    private Observer<TTSState> ttsStateObserver;
    private Observer<Integer> ttsPositionObserver;

    @Inject
    public ReaderViewModel(
            NovelRepository novelRepository,
            SettingsRepository settingsRepository,
            ThemeRepository themeRepository,
            BlockedWordRepository blockedWordRepository,
            StatisticsRepository statisticsRepository,
            TTSRepository ttsRepository,
            BookmarkRepository bookmarkRepository,
            AIServiceRepository aiServiceRepository) {
        this.novelRepository = novelRepository;
        this.settingsRepository = settingsRepository;
        this.themeRepository = themeRepository;
        this.blockedWordRepository = blockedWordRepository;
        this.statisticsRepository = statisticsRepository;
        this.ttsRepository = ttsRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.aiServiceRepository = aiServiceRepository;
        this.executorService = Executors.newSingleThreadExecutor();
        
        // 初始化设置
        initializeSettings();
        // 初始化TTS观察
        initializeTTSObservers();
    }

    /**
     * 初始化设置
     */
    private void initializeSettings() {
        // 从 SharedPreferences 读取保存的设置
        String savedPageMode = settingsRepository.getPageMode();
        float savedFontSize = settingsRepository.getFontSize();
        float savedLineSpacing = settingsRepository.getLineSpacing();
        String savedFontFamily = settingsRepository.getFontFamily();
        String savedPageAnimation = settingsRepository.getPageAnimation();
        
        // 使用 setValue 同步更新初始状态，确保在 Activity 观察之前状态已经正确
        ReaderUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new ReaderUiState();
        }
        ReaderUiState newState = new ReaderUiState(currentState);
        newState.setFontSize(savedFontSize);
        newState.setLineSpacing(savedLineSpacing);
        newState.setTheme(themeRepository.getCurrentTheme());
        newState.setPageMode(PageMode.fromId(savedPageMode));
        newState.setPageAnimation(PageAnimation.fromId(savedPageAnimation));
        newState.setFont(ReaderFont.fromId(savedFontFamily));
        newState.setAvailableVoices(ttsRepository.getAvailableVoices());
        _uiState.setValue(newState);
        
        // 加载屏蔽词
        loadBlockedWords();
    }

    /**
     * 初始化TTS观察者
     */
    private void initializeTTSObservers() {
        // 观察TTS状态变化
        ttsStateObserver = ttsState -> {
            updateState(state -> state.setTtsState(ttsState));
        };
        ttsRepository.getTTSState().observeForever(ttsStateObserver);
        
        // 观察TTS位置变化 - 用于实时更新高亮
        ttsPositionObserver = position -> {
            // 更新TTSState中的位置
            ReaderUiState currentState = _uiState.getValue();
            if (currentState != null && currentState.getTtsState() != null) {
                TTSState ttsState = currentState.getTtsState().copy();
                ttsState.setCurrentPosition(position);
                updateState(state -> state.setTtsState(ttsState));
            }
        };
        ttsRepository.getCurrentPosition().observeForever(ttsPositionObserver);
        
        // 设置章节完成监听器
        ttsRepository.setOnChapterCompleteListener(chapterId -> {
            // 验证需求：10.5 - 自动开始朗读下一章节
            // 使用executorService确保在后台线程执行，避免阻塞主线程
            executorService.execute(() -> {
                ReaderUiState currentState = _uiState.getValue();
                if (currentState == null || !currentState.canGoNextChapter()) {
                    // 没有下一章，停止朗读
                    return;
                }
                
                // 切换到下一章
                int currentIndex = currentState.getCurrentChapterIndex();
                List<Chapter> chapters = currentState.getChapters();
                if (chapters != null && currentIndex < chapters.size() - 1) {
                    Chapter nextChapter = chapters.get(currentIndex + 1);
                    
                    // 加载下一章内容
                    try {
                        Chapter fullChapter = novelRepository.getChapterById(nextChapter.getId());
                        if (fullChapter != null) {
                            final String displayContent = getFilteredContent(fullChapter);
                            
                            // 预加载相邻章节
                            Chapter prevChapter = currentState.getCurrentChapter();
                            String prevContent = currentState.getDisplayContent();
                            
                            Chapter nextNextChapter = null;
                            String nextNextContent = "";
                            if (currentIndex + 2 < chapters.size()) {
                                long nextNextId = chapters.get(currentIndex + 2).getId();
                                nextNextChapter = novelRepository.getChapterById(nextNextId);
                                nextNextContent = getFilteredContent(nextNextChapter);
                            }
                            
                            final Chapter finalPrevChapter = prevChapter;
                            final String finalPrevContent = prevContent;
                            final Chapter finalNextChapter = nextNextChapter;
                            final String finalNextContent = nextNextContent;
                            
                            // 更新UI状态
                            updateState(state -> {
                                state.setCurrentChapter(fullChapter);
                                state.setDisplayContent(displayContent);
                                state.setPreviousChapter(finalPrevChapter);
                                state.setNextChapter(finalNextChapter);
                                state.setPreviousChapterContent(finalPrevContent);
                                state.setNextChapterContent(finalNextContent);
                            });
                            
                            // 更新阅读进度
                            if (currentNovelId > 0) {
                                novelRepository.updateReadingProgress(currentNovelId, fullChapter.getId(), 0);
                            }
                            
                            // 更新TTS当前章节并开始朗读
                            ttsRepository.setCurrentChapterId(fullChapter.getId());
                            
                            // 延迟一小段时间确保UI更新完成后再开始朗读
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            
                            if (displayContent != null && !displayContent.isEmpty()) {
                                ttsRepository.startReading(displayContent, 0);
                            }
                        }
                    } catch (Exception e) {
                        // 加载失败，静默处理
                    }
                }
            });
        });
    }

    /**
     * 加载屏蔽词列表（按小说ID）
     */
    private void loadBlockedWords() {
        if (currentNovelId <= 0) {
            blockedWords = new ArrayList<>();
            return;
        }
        executorService.execute(() -> {
            blockedWords = blockedWordRepository.getBlockedWordStringsByNovelId(currentNovelId);
        });
    }


    // ==================== 小说和章节加载 ====================

    /**
     * 加载小说
     * 验证需求：5.1 - 打开小说时显示上次阅读位置的章节内容
     * 
     * @param novelId 小说ID
     */
    public void loadNovel(long novelId) {
        this.currentNovelId = novelId;
        
        updateState(state -> {
            state.setLoading(true);
            state.setError(null);
        });

        executorService.execute(() -> {
            try {
                // 先同步加载屏蔽词（必须在加载章节内容之前）
                blockedWords = blockedWordRepository.getBlockedWordStringsByNovelId(novelId);
                
                // 加载小说信息
                Novel novel = novelRepository.getNovelById(novelId);
                if (novel == null) {
                    updateState(state -> {
                        state.setLoading(false);
                        state.setError("小说不存在");
                    });
                    return;
                }

                // 加载章节列表
                List<Chapter> chapters = novelRepository.getChaptersByNovelIdSync(novelId);
                
                // 检查章节列表是否为空
                if (chapters == null || chapters.isEmpty()) {
                    updateState(state -> {
                        state.setLoading(false);
                        state.setError("该小说没有章节内容，可能下载未完成");
                    });
                    return;
                }
                
                // 确定要显示的章节（上次阅读位置）
                Chapter currentChapter = null;
                if (novel.getCurrentChapterId() != null) {
                    currentChapter = novelRepository.getChapterById(novel.getCurrentChapterId());
                }
                if (currentChapter == null && !chapters.isEmpty()) {
                    // 从列表获取第一章时，需要通过getChapterById获取完整内容
                    currentChapter = novelRepository.getChapterById(chapters.get(0).getId());
                }

                final Chapter finalChapter = currentChapter;
                final String displayContent = getFilteredContent(finalChapter);
                
                // 检查章节内容是否为空
                if (displayContent == null || displayContent.trim().isEmpty()) {
                    // 当前章节内容为空，尝试检查其他章节
                    // 注意：chapters列表中的Chapter对象是从ChapterInfo转换的，没有content
                    // 需要通过getChapterById获取完整内容来检查
                    boolean foundValidChapter = false;
                    for (Chapter ch : chapters) {
                        Chapter fullChapter = novelRepository.getChapterById(ch.getId());
                        if (fullChapter != null && fullChapter.getContent() != null 
                            && !fullChapter.getContent().trim().isEmpty()) {
                            foundValidChapter = true;
                            break;
                        }
                        // 只检查前几章，避免遍历太多
                        if (ch.getChapterIndex() > 5) {
                            break;
                        }
                    }
                    if (!foundValidChapter) {
                        updateState(state -> {
                            state.setLoading(false);
                            state.setError("章节内容解析失败，请尝试更换解析规则重新下载");
                        });
                        return;
                    }
                }
                
                // 预加载相邻章节
                Chapter prevChapter = null;
                Chapter nextChapter = null;
                String prevContent = "";
                String nextContent = "";
                
                if (finalChapter != null && !chapters.isEmpty()) {
                    // 在章节列表中查找当前章节的位置
                    int currentIndex = -1;
                    for (int i = 0; i < chapters.size(); i++) {
                        if (chapters.get(i).getId() == finalChapter.getId()) {
                            currentIndex = i;
                            break;
                        }
                    }
                    
                    if (currentIndex >= 0) {
                        // 加载上一章（需要从数据库获取完整内容）
                        if (currentIndex > 0) {
                            long prevChapterId = chapters.get(currentIndex - 1).getId();
                            prevChapter = novelRepository.getChapterById(prevChapterId);
                            prevContent = getFilteredContent(prevChapter);
                        }
                        
                        // 加载下一章（需要从数据库获取完整内容）
                        if (currentIndex < chapters.size() - 1) {
                            long nextChapterId = chapters.get(currentIndex + 1).getId();
                            nextChapter = novelRepository.getChapterById(nextChapterId);
                            nextContent = getFilteredContent(nextChapter);
                        }
                    }
                }
                
                final Chapter finalPrevChapter = prevChapter;
                final Chapter finalNextChapter = nextChapter;
                final String finalPrevContent = prevContent;
                final String finalNextContent = nextContent;
                
                // 获取保存的阅读位置
                final int savedPosition = novel.getCurrentPosition();

                updateState(state -> {
                    state.setNovel(novel);
                    state.setChapters(chapters);
                    state.setCurrentChapter(finalChapter);
                    state.setDisplayContent(displayContent);
                    state.setPreviousChapter(finalPrevChapter);
                    state.setNextChapter(finalNextChapter);
                    state.setPreviousChapterContent(finalPrevContent);
                    state.setNextChapterContent(finalNextContent);
                    state.setLoading(false);
                    // 恢复阅读位置
                    // 注意：savedPosition 可能是滚动位置（大值）或页码（小值）
                    // 根据值的大小来判断：如果 > 100，认为是滚动位置；否则认为是页码
                    if (savedPosition > 100) {
                        // 可能是滚动位置
                        state.setSavedScrollPosition(savedPosition);
                        state.setSavedPageIndex(0); // 页码从0开始
                    } else {
                        // 可能是页码
                        state.setSavedScrollPosition(0);
                        state.setSavedPageIndex(savedPosition);
                    }
                    // 开始记录阅读时间
                    state.setReadingStartTime(System.currentTimeMillis());
                });

            } catch (Exception e) {
                updateState(state -> {
                    state.setLoading(false);
                    state.setError("加载小说失败: " + e.getMessage());
                });
            }
        });
    }

    /**
     * 切换到指定章节
     * 验证需求：5.6 - 加载新章节内容并更新阅读进度
     * 
     * @param chapterId 章节ID
     */
    public void loadChapter(long chapterId) {
        loadChapter(chapterId, -1); // -1 表示不跳转到特定位置
    }

    /**
     * 切换到指定章节并跳转到指定位置
     * 验证需求：5.6 - 加载新章节内容并更新阅读进度
     * 验证需求：7.4 - 跳转到书签对应的位置
     * 
     * @param chapterId 章节ID
     * @param jumpPosition 跳转位置（-1表示不跳转）
     */
    public void loadChapter(long chapterId, int jumpPosition) {
        executorService.execute(() -> {
            try {
                Chapter chapter = novelRepository.getChapterById(chapterId);
                if (chapter == null) {
                    updateState(state -> state.setError("章节不存在"));
                    return;
                }

                final String displayContent = getFilteredContent(chapter);
                
                // 预加载相邻章节
                ReaderUiState currentState = _uiState.getValue();
                List<Chapter> chapters = currentState != null ? currentState.getChapters() : null;
                
                Chapter prevChapter = null;
                Chapter nextChapter = null;
                String prevContent = "";
                String nextContent = "";
                
                if (chapters != null && !chapters.isEmpty()) {
                    // 在章节列表中查找当前章节的位置
                    int currentIndex = -1;
                    for (int i = 0; i < chapters.size(); i++) {
                        if (chapters.get(i).getId() == chapter.getId()) {
                            currentIndex = i;
                            break;
                        }
                    }
                    
                    if (currentIndex >= 0) {
                        // 加载上一章（需要从数据库获取完整内容）
                        if (currentIndex > 0) {
                            long prevChapterId = chapters.get(currentIndex - 1).getId();
                            prevChapter = novelRepository.getChapterById(prevChapterId);
                            prevContent = getFilteredContent(prevChapter);
                        }
                        
                        // 加载下一章（需要从数据库获取完整内容）
                        if (currentIndex < chapters.size() - 1) {
                            long nextChapterId = chapters.get(currentIndex + 1).getId();
                            nextChapter = novelRepository.getChapterById(nextChapterId);
                            nextContent = getFilteredContent(nextChapter);
                        }
                    }
                }
                
                final Chapter finalPrevChapter = prevChapter;
                final Chapter finalNextChapter = nextChapter;
                final String finalPrevContent = prevContent;
                final String finalNextContent = nextContent;
                final int finalJumpPosition = jumpPosition;

                updateState(state -> {
                    state.setCurrentChapter(chapter);
                    state.setDisplayContent(displayContent);
                    state.setPreviousChapter(finalPrevChapter);
                    state.setNextChapter(finalNextChapter);
                    state.setPreviousChapterContent(finalPrevContent);
                    state.setNextChapterContent(finalNextContent);
                    // 设置跳转位置（在章节加载完成后设置）
                    if (finalJumpPosition >= 0) {
                        state.setJumpToPosition(finalJumpPosition);
                    }
                });

                // 更新阅读进度
                if (currentNovelId > 0) {
                    novelRepository.updateReadingProgress(currentNovelId, chapterId, 0);
                }

                // 注意：不要在这里更新TTS当前章节ID
                // TTS章节ID应该只在开始朗读时设置，而不是切换阅读章节时
                // 这样用户可以在朗读时浏览其他章节，然后跳转回朗读位置

            } catch (Exception e) {
                updateState(state -> state.setError("加载章节失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 切换到上一章
     */
    public void goToPreviousChapter() {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState == null || !currentState.canGoPreviousChapter()) {
            return;
        }

        int currentIndex = currentState.getCurrentChapterIndex();
        List<Chapter> chapters = currentState.getChapters();
        if (currentIndex > 0 && chapters != null && !chapters.isEmpty()) {
            Chapter previousChapter = chapters.get(currentIndex - 1);
            loadChapter(previousChapter.getId());
        }
    }

    /**
     * 切换到下一章
     */
    public void goToNextChapter() {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState == null || !currentState.canGoNextChapter()) {
            return;
        }

        int currentIndex = currentState.getCurrentChapterIndex();
        List<Chapter> chapters = currentState.getChapters();
        if (chapters != null && currentIndex < chapters.size() - 1) {
            Chapter nextChapter = chapters.get(currentIndex + 1);
            loadChapter(nextChapter.getId());
        }
    }

    /**
     * 预加载上上章内容（当用户翻到上一章中间位置时调用）
     * 用于提前准备数据，使章节切换更流畅
     */
    public void preloadPreviousPreviousChapter() {
        executorService.execute(() -> {
            try {
                ReaderUiState currentState = _uiState.getValue();
                if (currentState == null) return;
                
                List<Chapter> chapters = currentState.getChapters();
                int currentIndex = currentState.getCurrentChapterIndex();
                
                // 需要加载上上章，即 currentIndex - 2
                if (chapters != null && currentIndex >= 2) {
                    long prevPrevChapterId = chapters.get(currentIndex - 2).getId();
                    Chapter prevPrevChapter = novelRepository.getChapterById(prevPrevChapterId);
                    if (prevPrevChapter != null) {
                        // 预加载内容到缓存（这里只是触发数据库读取，内容会被缓存）
                        getFilteredContent(prevPrevChapter);
                        Log.d("ReaderChapterSwitch", "[预加载] 上上章预加载完成: " + prevPrevChapter.getTitle());
                    }
                }
            } catch (Exception e) {
                Log.e("ReaderChapterSwitch", "[预加载] 上上章预加载失败: " + e.getMessage());
            }
        });
    }

    /**
     * 预加载下下章内容（当用户翻到下一章中间位置时调用）
     * 用于提前准备数据，使章节切换更流畅
     */
    public void preloadNextNextChapter() {
        executorService.execute(() -> {
            try {
                ReaderUiState currentState = _uiState.getValue();
                if (currentState == null) return;
                
                List<Chapter> chapters = currentState.getChapters();
                int currentIndex = currentState.getCurrentChapterIndex();
                
                // 需要加载下下章，即 currentIndex + 2
                if (chapters != null && currentIndex + 2 < chapters.size()) {
                    long nextNextChapterId = chapters.get(currentIndex + 2).getId();
                    Chapter nextNextChapter = novelRepository.getChapterById(nextNextChapterId);
                    if (nextNextChapter != null) {
                        // 预加载内容到缓存（这里只是触发数据库读取，内容会被缓存）
                        getFilteredContent(nextNextChapter);
                        Log.d("ReaderChapterSwitch", "[预加载] 下下章预加载完成: " + nextNextChapter.getTitle());
                    }
                }
            } catch (Exception e) {
                Log.e("ReaderChapterSwitch", "[预加载] 下下章预加载失败: " + e.getMessage());
            }
        });
    }

    /**
     * 获取过滤后的章节内容（应用屏蔽词）
     * 验证需求：11.4 - 将所有屏蔽词替换为星号
     */
    private String getFilteredContent(Chapter chapter) {
        if (chapter == null || chapter.getContent() == null) {
            return "";
        }
        
        if (blockedWords.isEmpty()) {
            return chapter.getContent();
        }
        
        return blockedWordRepository.applyBlockedWords(chapter.getContent(), blockedWords);
    }


    // ==================== 阅读进度管理 ====================

    /**
     * 更新阅读位置
     * 验证需求：5.7 - 自动保存当前阅读位置
     * 
     * @param position 当前位置
     */
    public void updateReadingPosition(int position) {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState == null || currentState.getCurrentChapter() == null) {
            return;
        }

        long chapterId = currentState.getCurrentChapter().getId();
        
        executorService.execute(() -> {
            try {
                novelRepository.updateReadingProgress(currentNovelId, chapterId, position);
            } catch (Exception e) {
                // 静默处理保存失败
            }
        });
    }

    /**
     * 保存阅读进度并退出
     * 验证需求：5.7 - 用户退出阅读时自动保存当前阅读位置
     * 
     * @param position 当前位置
     */
    public void saveAndExit(int position) {
        // 停止TTS
        stopTTS();
        
        // 记录阅读统计
        recordReadingStatistics();
        
        // 保存阅读位置
        updateReadingPosition(position);
    }

    /**
     * 记录阅读统计
     * 验证需求：12.1 - 记录阅读时长到本地存储
     */
    private void recordReadingStatistics() {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState == null || currentState.getReadingStartTime() <= 0) {
            return;
        }

        long duration = System.currentTimeMillis() - currentState.getReadingStartTime();
        int charCount = currentState.getReadCharCount();
        
        // 只记录有效的阅读时长（至少1秒）
        if (duration >= 1000 && currentNovelId > 0) {
            executorService.execute(() -> {
                try {
                    statisticsRepository.recordReadingSession(currentNovelId, duration, charCount);
                } catch (Exception e) {
                    // 静默处理
                }
            });
        }
    }

    /**
     * 更新阅读字数
     * 
     * @param charCount 阅读字数
     */
    public void updateReadCharCount(int charCount) {
        updateState(state -> state.setReadCharCount(charCount));
    }

    // ==================== 字体和行间距设置 ====================

    /**
     * 设置字体大小
     * 验证需求：5.4 - 立即应用新的字体大小设置
     * 
     * @param fontSize 字体大小（sp）
     */
    public void setFontSize(float fontSize) {
        // 限制范围 12-32
        float validSize = Math.max(12f, Math.min(32f, fontSize));
        
        settingsRepository.setFontSize(validSize);
        updateState(state -> state.setFontSize(validSize));
    }

    /**
     * 设置行间距
     * 验证需求：5.5 - 立即应用新的行间距设置
     * 
     * @param lineSpacing 行间距倍数
     */
    public void setLineSpacing(float lineSpacing) {
        // 限制范围 1.0-3.0
        float validSpacing = Math.max(1.0f, Math.min(3.0f, lineSpacing));
        
        settingsRepository.setLineSpacing(validSpacing);
        updateState(state -> state.setLineSpacing(validSpacing));
    }

    // ==================== 翻页设置 ====================

    /**
     * 设置翻页模式
     * 
     * @param pageMode 翻页模式
     */
    public void setPageMode(PageMode pageMode) {
        if (pageMode == null) return;
        
        settingsRepository.setPageMode(pageMode.getId());
        updateState(state -> state.setPageMode(pageMode));
    }

    /**
     * 设置翻页动画
     * 
     * @param pageAnimation 翻页动画类型
     */
    public void setPageAnimation(PageAnimation pageAnimation) {
        if (pageAnimation == null) return;
        
        settingsRepository.setPageAnimation(pageAnimation.getId());
        updateState(state -> state.setPageAnimation(pageAnimation));
    }

    /**
     * 获取当前翻页模式
     */
    public PageMode getPageMode() {
        ReaderUiState state = _uiState.getValue();
        return state != null ? state.getPageMode() : PageMode.SCROLL;
    }

    /**
     * 获取当前翻页动画
     */
    public PageAnimation getPageAnimation() {
        ReaderUiState state = _uiState.getValue();
        return state != null ? state.getPageAnimation() : PageAnimation.SLIDE;
    }

    /**
     * 设置字体
     * 
     * @param font 字体
     */
    public void setFont(ReaderFont font) {
        if (font == null) return;
        
        settingsRepository.setFontFamily(font.getId());
        updateState(state -> state.setFont(font));
    }

    /**
     * 获取当前字体
     */
    public ReaderFont getFont() {
        ReaderUiState state = _uiState.getValue();
        return state != null ? state.getFont() : ReaderFont.DEFAULT;
    }

    // ==================== 主题切换 ====================

    /**
     * 设置主题
     * 验证需求：6.2 - 立即应用该主题的背景色和文字色
     * 
     * @param theme 主题
     */
    public void setTheme(ReaderTheme theme) {
        if (theme == null) return;
        
        themeRepository.setCurrentTheme(theme);
        updateState(state -> state.setTheme(theme));
    }

    /**
     * 根据ID设置主题
     * 
     * @param themeId 主题ID
     */
    public void setThemeById(String themeId) {
        ReaderTheme theme = themeRepository.getThemeById(themeId);
        if (theme != null) {
            setTheme(theme);
        }
    }

    /**
     * 获取所有可用主题
     */
    public List<ReaderTheme> getAllThemes() {
        return themeRepository.getAllThemes();
    }

    /**
     * 保存自定义主题
     * 验证需求：6.5 - 将该主题添加到主题列表
     * 
     * @param theme 自定义主题
     */
    public void saveCustomTheme(ReaderTheme theme) {
        if (theme == null) return;
        themeRepository.saveCustomTheme(theme);
    }

    /**
     * 是否启用自动主题切换
     * 验证需求：6.6
     * 
     * @return 是否启用
     */
    public boolean isAutoThemeSwitchEnabled() {
        return themeRepository.isAutoThemeSwitchEnabled();
    }

    /**
     * 设置自动主题切换
     * 验证需求：6.6 - 自动切换到夜间模式
     * 
     * @param enabled 是否启用
     */
    public void setAutoThemeSwitchEnabled(boolean enabled) {
        themeRepository.setAutoThemeSwitchEnabled(enabled);
        if (enabled) {
            // 立即应用自动主题切换
            themeRepository.applyAutoThemeSwitch();
            // 更新UI状态
            updateState(state -> state.setTheme(themeRepository.getCurrentTheme()));
        }
    }


    // ==================== 关键词搜索 ====================

    /**
     * 在小说中搜索关键词
     * 验证需求：9.2 - 在当前小说的所有章节中查找该关键词
     * 
     * @param keyword 搜索关键词
     */
    public void searchInNovel(String keyword) {
        searchInNovel(keyword, 0);
    }
    
    /**
     * 在小说中搜索关键词（带当前位置）
     * 验证需求：9.2 - 在当前小说的所有章节中查找该关键词
     * 
     * @param keyword 搜索关键词
     * @param currentPosition 当前阅读位置（用于返回时恢复）
     */
    public void searchInNovel(String keyword, int currentPosition) {
        if (keyword == null || keyword.trim().isEmpty()) {
            clearSearchResults();
            return;
        }

        // 保存当前阅读位置，并清空旧的搜索结果
        ReaderUiState currentState = _uiState.getValue();
        if (currentState != null && currentState.getCurrentChapter() != null) {
            ReadingPosition savedPosition = new ReadingPosition(
                    currentState.getCurrentChapter().getId(),
                    currentPosition // 保存当前位置
            );
            updateState(state -> {
                state.setSavedPosition(savedPosition);
                state.setSearchKeyword(keyword.trim());
                // 清空旧的搜索结果，避免显示旧数据
                state.setSearchResults(new ArrayList<>());
                state.setCurrentSearchIndex(-1);
            });
        }

        executorService.execute(() -> {
            try {
                List<SearchResult> results = novelRepository.searchInNovel(currentNovelId, keyword.trim());
                
                updateState(state -> {
                    state.setSearchResults(results);
                    state.setCurrentSearchIndex(results.isEmpty() ? -1 : 0);
                });

                // 如果有结果，跳转到第一个
                if (!results.isEmpty()) {
                    navigateToSearchResult(0);
                }

            } catch (Exception e) {
                updateState(state -> state.setError("搜索失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 导航到指定搜索结果
     * 验证需求：9.4 - 跳转到该关键词所在位置
     * 
     * @param index 结果索引
     */
    public void navigateToSearchResult(int index) {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState == null || !currentState.hasSearchResults()) {
            return;
        }

        List<SearchResult> results = currentState.getSearchResults();
        if (index < 0 || index >= results.size()) {
            return;
        }

        SearchResult result = results.get(index);
        updateState(state -> state.setCurrentSearchIndex(index));

        // 获取搜索结果的位置
        int searchPosition = result.getPosition();
        
        // 检查是否需要切换章节
        if (currentState.getCurrentChapter() != null 
                && currentState.getCurrentChapter().getId() == result.getChapterId()) {
            // 同一章节，直接设置跳转位置
            updateState(state -> state.setJumpToPosition(searchPosition));
        } else {
            // 不同章节，加载目标章节并设置跳转位置
            loadChapterWithPosition(result.getChapterId(), searchPosition);
        }
    }
    
    /**
     * 加载章节并跳转到指定位置
     * 
     * @param chapterId 章节ID
     * @param position 跳转位置
     */
    private void loadChapterWithPosition(long chapterId, int position) {
        updateState(state -> state.setLoading(true));

        executorService.execute(() -> {
            try {
                Chapter chapter = novelRepository.getChapterById(chapterId);
                if (chapter == null) {
                    updateState(state -> {
                        state.setLoading(false);
                        state.setError("章节不存在");
                    });
                    return;
                }

                String filteredContent = getFilteredContent(chapter);

                // 预加载相邻章节
                ReaderUiState currentState = _uiState.getValue();
                List<Chapter> chapters = currentState != null ? currentState.getChapters() : null;
                
                Chapter prevChapter = null;
                Chapter nextChapter = null;
                String prevContent = "";
                String nextContent = "";
                
                if (chapters != null && !chapters.isEmpty()) {
                    // 在章节列表中查找当前章节的位置
                    int currentIndex = -1;
                    for (int i = 0; i < chapters.size(); i++) {
                        if (chapters.get(i).getId() == chapter.getId()) {
                            currentIndex = i;
                            break;
                        }
                    }
                    
                    if (currentIndex >= 0) {
                        // 加载上一章
                        if (currentIndex > 0) {
                            long prevChapterId = chapters.get(currentIndex - 1).getId();
                            prevChapter = novelRepository.getChapterById(prevChapterId);
                            prevContent = getFilteredContent(prevChapter);
                        }
                        
                        // 加载下一章
                        if (currentIndex < chapters.size() - 1) {
                            long nextChapterId = chapters.get(currentIndex + 1).getId();
                            nextChapter = novelRepository.getChapterById(nextChapterId);
                            nextContent = getFilteredContent(nextChapter);
                        }
                    }
                }
                
                final Chapter finalPrevChapter = prevChapter;
                final Chapter finalNextChapter = nextChapter;
                final String finalPrevContent = prevContent;
                final String finalNextContent = nextContent;

                updateState(state -> {
                    state.setCurrentChapter(chapter);
                    state.setDisplayContent(filteredContent);
                    state.setPreviousChapter(finalPrevChapter);
                    state.setNextChapter(finalNextChapter);
                    state.setPreviousChapterContent(finalPrevContent);
                    state.setNextChapterContent(finalNextContent);
                    state.setLoading(false);
                    state.setJumpToPosition(position); // 设置跳转位置
                });
            } catch (Exception e) {
                updateState(state -> {
                    state.setLoading(false);
                    state.setError("加载章节失败: " + e.getMessage());
                });
            }
        });
    }

    /**
     * 导航到上一个搜索结果
     * 验证需求：9.7 - 支持上一个结果的快速导航
     * 
     * @param currentDisplayIndex 当前显示的索引（由 UI 层维护）
     */
    public void navigateToPreviousResult(int currentDisplayIndex) {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState != null && currentState.hasSearchResults() && currentDisplayIndex > 0) {
            navigateToSearchResult(currentDisplayIndex - 1);
        }
    }

    /**
     * 导航到上一个搜索结果（兼容旧接口）
     */
    public void navigateToPreviousResult() {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState != null && currentState.canNavigateToPreviousResult()) {
            navigateToSearchResult(currentState.getCurrentSearchIndex() - 1);
        }
    }

    /**
     * 导航到下一个搜索结果
     * 验证需求：9.7 - 支持下一个结果的快速导航
     * 
     * @param currentDisplayIndex 当前显示的索引（由 UI 层维护）
     */
    public void navigateToNextResult(int currentDisplayIndex) {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState != null && currentState.hasSearchResults()) {
            int total = currentState.getSearchResults().size();
            if (currentDisplayIndex < total - 1) {
                navigateToSearchResult(currentDisplayIndex + 1);
            }
        }
    }

    /**
     * 导航到下一个搜索结果（兼容旧接口）
     */
    public void navigateToNextResult() {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState != null && currentState.canNavigateToNextResult()) {
            navigateToSearchResult(currentState.getCurrentSearchIndex() + 1);
        }
    }

    /**
     * 返回搜索前的位置
     * 验证需求：9.5 - 恢复到搜索前的原始阅读进度
     */
    public void returnToSavedPosition() {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState == null || currentState.getSavedPosition() == null) {
            clearSearchResults();
            return;
        }

        ReadingPosition savedPosition = currentState.getSavedPosition();
        
        // 检查是否需要切换章节
        if (currentState.getCurrentChapter() != null 
                && currentState.getCurrentChapter().getId() == savedPosition.getChapterId()) {
            // 同一章节，直接设置跳转位置
            updateState(state -> state.setJumpToPosition(savedPosition.getPosition()));
        } else {
            // 不同章节，加载目标章节并设置跳转位置
            loadChapterWithPosition(savedPosition.getChapterId(), savedPosition.getPosition());
        }
        
        clearSearchResults();
    }

    /**
     * 清除搜索结果
     */
    public void clearSearchResults() {
        updateState(state -> {
            state.setSearchResults(new ArrayList<>());
            state.setCurrentSearchIndex(-1);
            state.setSearchKeyword("");
            state.setSavedPosition(null);
        });
    }


    // ==================== 屏蔽词过滤 ====================

    /**
     * 刷新屏蔽词并重新应用（按小说ID）
     * 验证需求：11.5 - 修改后立即刷新当前页面应用新的屏蔽规则
     */
    public void refreshBlockedWords() {
        if (currentNovelId <= 0) {
            return;
        }
        executorService.execute(() -> {
            blockedWords = blockedWordRepository.getBlockedWordStringsByNovelId(currentNovelId);
            
            // 重新应用屏蔽词到当前章节
            ReaderUiState currentState = _uiState.getValue();
            if (currentState != null && currentState.getCurrentChapter() != null) {
                String filteredContent = getFilteredContent(currentState.getCurrentChapter());
                updateState(state -> state.setDisplayContent(filteredContent));
            }
        });
    }

    // ==================== TTS语音朗读 ====================

    /**
     * 开始语音朗读
     * 验证需求：10.1 - 使用TTS引擎朗读当前章节内容
     */
    public void startTTS() {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState == null || currentState.getCurrentChapter() == null) {
            return;
        }

        String content = currentState.getDisplayContent();
        if (content == null || content.isEmpty()) {
            return;
        }

        ttsRepository.setCurrentChapterId(currentState.getCurrentChapter().getId());
        ttsRepository.startReading(content, 0);
    }

    /**
     * 从指定位置开始朗读
     * 
     * @param position 开始位置
     */
    public void startTTSFromPosition(int position) {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState == null || currentState.getCurrentChapter() == null) {
            return;
        }

        String content = currentState.getDisplayContent();
        if (content == null || content.isEmpty()) {
            return;
        }

        ttsRepository.setCurrentChapterId(currentState.getCurrentChapter().getId());
        ttsRepository.startReading(content, position);
    }

    /**
     * 暂停语音朗读
     * 验证需求：10.6 - 暂停朗读时保存当前朗读位置
     */
    public void pauseTTS() {
        ttsRepository.pauseReading();
    }

    /**
     * 恢复语音朗读
     * 验证需求：10.7 - 从暂停位置继续朗读
     */
    public void resumeTTS() {
        ttsRepository.resumeReading();
    }

    /**
     * 停止语音朗读
     * 验证需求：10.2 - 显示播放控制界面（停止）
     */
    public void stopTTS() {
        ttsRepository.stopReading();
    }

    /**
     * 切换TTS播放状态
     */
    public void toggleTTS() {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState == null) return;

        TTSState ttsState = currentState.getTtsState();
        if (ttsState == null || ttsState.isIdle()) {
            startTTS();
        } else if (ttsState.isPlaying()) {
            pauseTTS();
        } else if (ttsState.isPaused()) {
            resumeTTS();
        }
    }

    /**
     * 设置TTS语速
     * 验证需求：10.3 - 立即应用新的语速设置
     * 
     * @param rate 语速（0.5-2.0）
     */
    public void setTTSSpeechRate(float rate) {
        float validRate = Math.max(0.5f, Math.min(2.0f, rate));
        ttsRepository.setSpeechRate(validRate);
    }

    /**
     * 设置TTS语音
     * 验证需求：10.4 - 切换到指定的语音引擎
     * 
     * @param voiceId 语音ID
     */
    public void setTTSVoice(String voiceId) {
        ttsRepository.setVoice(voiceId);
    }

    /**
     * 获取可用语音列表
     */
    public List<VoiceInfo> getAvailableVoices() {
        return ttsRepository.getAvailableVoices();
    }


    // ==================== 工具栏控制 ====================

    /**
     * 切换工具栏显示状态
     * 验证需求：5.3 - 点击屏幕中央显示或隐藏顶部和底部工具栏
     */
    public void toggleToolbar() {
        updateState(state -> state.setShowToolbar(!state.isShowToolbar()));
    }

    /**
     * 显示工具栏
     */
    public void showToolbar() {
        updateState(state -> state.setShowToolbar(true));
    }

    /**
     * 隐藏工具栏
     */
    public void hideToolbar() {
        updateState(state -> state.setShowToolbar(false));
    }

    // ==================== 错误处理 ====================

    /**
     * 清除错误信息
     */
    public void clearError() {
        updateState(state -> state.setError(null));
    }

    // ==================== 辅助方法 ====================

    /**
     * 更新UI状态的辅助方法
     */
    private void updateState(StateUpdater updater) {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new ReaderUiState();
        }
        ReaderUiState newState = new ReaderUiState(currentState);
        updater.update(newState);
        _uiState.postValue(newState);
    }

    /**
     * 状态更新接口
     */
    private interface StateUpdater {
        void update(ReaderUiState state);
    }

    /**
     * 获取当前小说ID
     */
    public long getCurrentNovelId() {
        return currentNovelId;
    }

    /**
     * 获取章节列表LiveData
     */
    public LiveData<List<Chapter>> getChaptersLiveData() {
        if (currentNovelId > 0) {
            return novelRepository.getChaptersByNovelId(currentNovelId);
        }
        return new MutableLiveData<>(new ArrayList<>());
    }

    // ==================== 书签功能 ====================

    /**
     * 获取当前小说的书签列表
     * 验证需求：7.3 - 显示书签列表
     */
    public LiveData<List<Bookmark>> getBookmarks() {
        if (currentNovelId > 0) {
            return bookmarkRepository.getBookmarksByNovelId(currentNovelId);
        }
        return new MutableLiveData<>(new ArrayList<>());
    }

    /**
     * 添加书签
     * 验证需求：7.1 - 保存当前章节和段落位置
     * 验证需求：7.2 - 允许用户为书签添加备注文字
     * 
     * @param note 书签备注（可选）
     * @param position 当前阅读位置（字符位置）
     * @param textPreview 文本预览（书签位置附近的文本）
     * @return 是否添加成功
     */
    public void addBookmark(String note, int position, String textPreview) {
        ReaderUiState currentState = _uiState.getValue();
        if (currentState == null || currentState.getCurrentChapter() == null) {
            return;
        }

        Chapter currentChapter = currentState.getCurrentChapter();
        
        executorService.execute(() -> {
            try {
                Bookmark bookmark = new Bookmark(
                        currentNovelId,
                        currentChapter.getId(),
                        currentChapter.getTitle(),
                        position
                );
                bookmark.setNote(note);
                bookmark.setTextPreview(textPreview);
                
                long id = bookmarkRepository.insertBookmark(bookmark);
                if (id > 0) {
                    // 书签添加成功，通过UI状态通知
                    updateState(state -> state.setBookmarkAdded(true));
                }
            } catch (Exception e) {
                updateState(state -> state.setError("添加书签失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 删除书签
     * 验证需求：7.5 - 从本地存储中移除该书签记录
     * 
     * @param bookmarkId 书签ID
     */
    public void deleteBookmark(long bookmarkId) {
        executorService.execute(() -> {
            try {
                bookmarkRepository.deleteBookmark(bookmarkId);
                updateState(state -> state.setBookmarkDeleted(true));
            } catch (Exception e) {
                updateState(state -> state.setError("删除书签失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 跳转到书签位置
     * 验证需求：7.4 - 跳转到该书签对应的位置
     * 
     * @param bookmark 书签对象
     */
    public void jumpToBookmark(Bookmark bookmark) {
        if (bookmark == null) return;
        
        ReaderUiState currentState = _uiState.getValue();
        
        // 检查是否在同一章节
        if (currentState != null && currentState.getCurrentChapter() != null 
                && currentState.getCurrentChapter().getId() == bookmark.getChapterId()) {
            // 同一章节，直接设置跳转位置，不需要重新加载章节
            updateState(state -> state.setJumpToPosition(bookmark.getPosition()));
        } else {
            // 不同章节，加载书签对应的章节，并传递跳转位置
            loadChapter(bookmark.getChapterId(), bookmark.getPosition());
        }
    }

    /**
     * 清除书签添加状态
     */
    public void clearBookmarkAddedState() {
        updateState(state -> state.setBookmarkAdded(false));
    }

    /**
     * 清除书签删除状态
     */
    public void clearBookmarkDeletedState() {
        updateState(state -> state.setBookmarkDeleted(false));
    }

    /**
     * 清除跳转位置状态
     */
    public void clearJumpToPosition() {
        updateState(state -> state.setJumpToPosition(-1));
    }

    // ==================== AI摘要功能 ====================

    /**
     * 获取章节摘要（优先使用缓存）
     * 验证需求：8.1, 8.4 - 调用AI服务API或使用缓存
     * 
     * @param chapter 章节对象
     * @param callback 结果回调
     */
    public void getChapterSummary(Chapter chapter, SummaryCallback callback) {
        if (chapter == null) {
            callback.onError("章节不存在");
            return;
        }

        // 在后台线程检查缓存和生成摘要
        executorService.execute(() -> {
            try {
                // 先检查缓存
                String cachedSummary = aiServiceRepository.getCachedSummary(chapter.getId());
                if (cachedSummary != null && !cachedSummary.isEmpty()) {
                    // 在主线程回调
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(cachedSummary, true);
                    });
                    return;
                }

                // 需要生成新摘要，先在主线程显示加载状态
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onLoading();
                });
                
                // 获取章节内容
                Chapter fullChapter = novelRepository.getChapterById(chapter.getId());
                if (fullChapter == null || fullChapter.getContent() == null || fullChapter.getContent().isEmpty()) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError("章节内容为空");
                    });
                    return;
                }

                Disposable disposable = aiServiceRepository.getOrGenerateSummary(fullChapter)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                summary -> callback.onSuccess(summary, false),
                                error -> callback.onError(error.getMessage())
                        );
                compositeDisposable.add(disposable);
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("获取章节内容失败: " + e.getMessage());
                });
            }
        });
    }

    /**
     * 重新生成章节摘要（忽略缓存）
     * 验证需求：8.1 - 调用AI服务API并传递章节文本
     * 
     * @param chapter 章节对象
     * @param callback 结果回调
     */
    public void regenerateChapterSummary(Chapter chapter, SummaryCallback callback) {
        if (chapter == null) {
            callback.onError("章节不存在");
            return;
        }

        callback.onLoading();

        executorService.execute(() -> {
            try {
                Chapter fullChapter = novelRepository.getChapterById(chapter.getId());
                if (fullChapter == null || fullChapter.getContent() == null || fullChapter.getContent().isEmpty()) {
                    callback.onError("章节内容为空");
                    return;
                }

                final long chapterId = chapter.getId();
                Disposable disposable = aiServiceRepository.generateSummary(fullChapter.getContent())
                        .subscribe(
                                summary -> {
                                    // 在后台线程保存到缓存
                                    aiServiceRepository.saveSummaryCache(chapterId, summary);
                                    // 切换到主线程回调
                                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                        callback.onSuccess(summary, false);
                                    });
                                },
                                error -> {
                                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                        callback.onError(error.getMessage());
                                    });
                                }
                        );
                compositeDisposable.add(disposable);
            } catch (Exception e) {
                callback.onError("获取章节内容失败: " + e.getMessage());
            }
        });
    }

    /**
     * 获取已有摘要的章节ID集合（异步加载）
     * 
     * @param callback 回调函数
     */
    public void loadChaptersWithSummary(java.util.function.Consumer<Set<Long>> callback) {
        executorService.execute(() -> {
            Set<Long> result = new HashSet<>();
            ReaderUiState currentState = _uiState.getValue();
            if (currentState != null && currentState.getChapters() != null) {
                for (Chapter chapter : currentState.getChapters()) {
                    String cached = aiServiceRepository.getCachedSummary(chapter.getId());
                    if (cached != null && !cached.isEmpty()) {
                        result.add(chapter.getId());
                    }
                }
            }
            // 在主线程回调
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                callback.accept(result);
            });
        });
    }

    /**
     * 获取已有摘要的章节ID集合（同步方法，仅在后台线程调用）
     * 
     * @return 已有摘要的章节ID集合
     */
    public Set<Long> getChaptersWithSummary() {
        Set<Long> result = new HashSet<>();
        ReaderUiState currentState = _uiState.getValue();
        if (currentState != null && currentState.getChapters() != null) {
            for (Chapter chapter : currentState.getChapters()) {
                String cached = aiServiceRepository.getCachedSummary(chapter.getId());
                if (cached != null && !cached.isEmpty()) {
                    result.add(chapter.getId());
                }
            }
        }
        return result;
    }

    /**
     * 检查章节是否有缓存的摘要
     * 
     * @param chapterId 章节ID
     * @return 是否有缓存
     */
    public boolean hasCachedSummary(long chapterId) {
        String cached = aiServiceRepository.getCachedSummary(chapterId);
        return cached != null && !cached.isEmpty();
    }

    /**
     * 摘要回调接口
     */
    public interface SummaryCallback {
        void onLoading();
        void onSuccess(String summary, boolean fromCache);
        void onError(String message);
    }

    /**
     * 获取有摘要的章节列表（异步）
     * 
     * @param callback 回调函数
     */
    public void loadChaptersWithSummaryList(java.util.function.Consumer<List<com.example.read.data.entity.ChapterInfo>> callback) {
        executorService.execute(() -> {
            List<com.example.read.data.entity.ChapterInfo> result = novelRepository.getChaptersWithSummary(currentNovelId);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                callback.accept(result != null ? result : new java.util.ArrayList<>());
            });
        });
    }

    /**
     * 删除章节摘要
     * 
     * @param chapterId 章节ID
     * @param callback 完成回调
     */
    public void deleteSummary(long chapterId, Runnable callback) {
        executorService.execute(() -> {
            novelRepository.deleteChapterSummary(chapterId);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(callback);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        
        // 记录阅读统计
        recordReadingStatistics();
        
        // 停止TTS
        ttsRepository.stopReading();
        
        // 移除TTS观察者
        if (ttsStateObserver != null) {
            ttsRepository.getTTSState().removeObserver(ttsStateObserver);
        }
        if (ttsPositionObserver != null) {
            ttsRepository.getCurrentPosition().removeObserver(ttsPositionObserver);
        }
        
        // 清理RxJava订阅
        compositeDisposable.clear();
        
        // 关闭线程池
        executorService.shutdown();
    }
}
