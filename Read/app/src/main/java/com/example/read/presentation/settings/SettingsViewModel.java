package com.example.read.presentation.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.read.domain.model.BlockedWord;
import com.example.read.domain.model.ParserRule;
import com.example.read.domain.model.ReaderTheme;
import com.example.read.domain.repository.BlockedWordRepository;
import com.example.read.domain.repository.ParserRuleRepository;
import com.example.read.domain.repository.SettingsRepository;
import com.example.read.domain.repository.ThemeRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 设置ViewModel - 管理设置界面的业务逻辑和UI状态
 * 
 * 验证需求：3.1, 6.1, 11.1
 */
@HiltViewModel
public class SettingsViewModel extends ViewModel {

    private final ThemeRepository themeRepository;
    private final SettingsRepository settingsRepository;
    private final ParserRuleRepository parserRuleRepository;
    private final BlockedWordRepository blockedWordRepository;
    
    private final ExecutorService executorService;
    private final CompositeDisposable disposables;

    // UI状态
    private final MutableLiveData<SettingsUiState> _uiState = new MutableLiveData<>(new SettingsUiState());
    public LiveData<SettingsUiState> getUiState() { return _uiState; }

    // 数据源观察者
    private Observer<ReaderTheme> themeObserver;
    private Observer<List<ParserRule>> rulesObserver;
    private Observer<List<BlockedWord>> blockedWordsObserver;

    @Inject
    public SettingsViewModel(
            ThemeRepository themeRepository,
            SettingsRepository settingsRepository,
            ParserRuleRepository parserRuleRepository,
            BlockedWordRepository blockedWordRepository) {
        this.themeRepository = themeRepository;
        this.settingsRepository = settingsRepository;
        this.parserRuleRepository = parserRuleRepository;
        this.blockedWordRepository = blockedWordRepository;
        this.executorService = Executors.newSingleThreadExecutor();
        this.disposables = new CompositeDisposable();

        // 初始化加载
        loadInitialData();
        setupObservers();
    }

    /**
     * 加载初始数据
     */
    private void loadInitialData() {
        updateState(state -> {
            state.setLoading(true);
            
            // 加载主题数据
            state.setCurrentTheme(themeRepository.getCurrentTheme());
            state.setAllThemes(themeRepository.getAllThemes());
            state.setAutoThemeSwitchEnabled(themeRepository.isAutoThemeSwitchEnabled());
            state.setNightModeStartHour(themeRepository.getNightModeStartHour());
            state.setNightModeEndHour(themeRepository.getNightModeEndHour());
            
            // 加载阅读设置
            state.setFontSize(settingsRepository.getFontSize());
            state.setLineSpacing(settingsRepository.getLineSpacing());
            state.setKeepScreenOn(settingsRepository.isKeepScreenOn());
            state.setVolumeKeyPageTurn(settingsRepository.isVolumeKeyPageTurn());
            state.setFullScreenReading(settingsRepository.isFullScreenReading());
            
            state.setLoading(false);
        });
    }

    /**
     * 设置数据观察者
     */
    private void setupObservers() {
        // 观察主题变化
        themeObserver = theme -> {
            if (theme != null) {
                updateState(state -> state.setCurrentTheme(theme));
            }
        };
        themeRepository.getCurrentThemeLiveData().observeForever(themeObserver);

        // 观察解析规则变化
        rulesObserver = rules -> {
            updateState(state -> state.setParserRules(rules != null ? rules : List.of()));
        };
        parserRuleRepository.getAllRules().observeForever(rulesObserver);

        // 观察屏蔽词变化
        blockedWordsObserver = words -> {
            updateState(state -> state.setBlockedWords(words != null ? words : List.of()));
        };
        blockedWordRepository.getAllBlockedWords().observeForever(blockedWordsObserver);
    }

    // ==================== 主题管理 ====================

    /**
     * 设置当前主题
     * 验证需求：6.1 - 提供至少三种预设主题
     * 
     * @param theme 要应用的主题
     */
    public void setCurrentTheme(ReaderTheme theme) {
        if (theme == null) return;
        
        executorService.execute(() -> {
            themeRepository.setCurrentTheme(theme);
            updateState(state -> {
                state.setCurrentTheme(theme);
                state.setSuccessMessage("主题已切换为: " + theme.getName());
            });
        });
    }

    /**
     * 根据ID设置主题
     * 
     * @param themeId 主题ID
     */
    public void setCurrentThemeById(String themeId) {
        ReaderTheme theme = themeRepository.getThemeById(themeId);
        if (theme != null) {
            setCurrentTheme(theme);
        }
    }

    /**
     * 保存自定义主题
     * 
     * @param name 主题名称
     * @param backgroundColor 背景色
     * @param textColor 文字色
     */
    public void saveCustomTheme(String name, int backgroundColor, int textColor) {
        executorService.execute(() -> {
            try {
                ReaderTheme customTheme = ReaderTheme.createCustom(name, backgroundColor, textColor);
                themeRepository.saveCustomTheme(customTheme);
                
                updateState(state -> {
                    state.setAllThemes(themeRepository.getAllThemes());
                    state.setSuccessMessage("自定义主题已保存: " + name);
                });
            } catch (Exception e) {
                updateState(state -> state.setError("保存主题失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 删除自定义主题
     * 
     * @param themeId 主题ID
     */
    public void deleteCustomTheme(String themeId) {
        executorService.execute(() -> {
            try {
                themeRepository.deleteCustomTheme(themeId);
                updateState(state -> {
                    state.setAllThemes(themeRepository.getAllThemes());
                    state.setSuccessMessage("主题已删除");
                });
            } catch (Exception e) {
                updateState(state -> state.setError("删除主题失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 设置自动主题切换
     * 
     * @param enabled 是否启用
     */
    public void setAutoThemeSwitchEnabled(boolean enabled) {
        executorService.execute(() -> {
            themeRepository.setAutoThemeSwitchEnabled(enabled);
            updateState(state -> {
                state.setAutoThemeSwitchEnabled(enabled);
                state.setSuccessMessage(enabled ? "已启用自动主题切换" : "已关闭自动主题切换");
            });
        });
    }

    /**
     * 设置夜间模式时间范围
     * 
     * @param startHour 开始时间（0-23）
     * @param endHour 结束时间（0-23）
     */
    public void setNightModeTimeRange(int startHour, int endHour) {
        executorService.execute(() -> {
            themeRepository.setNightModeTimeRange(startHour, endHour);
            updateState(state -> {
                state.setNightModeStartHour(startHour);
                state.setNightModeEndHour(endHour);
            });
        });
    }

    // ==================== 阅读设置管理 ====================

    /**
     * 设置字体大小
     * 
     * @param size 字体大小（sp）
     */
    public void setFontSize(float size) {
        settingsRepository.setFontSize(size);
        updateState(state -> state.setFontSize(size));
    }

    /**
     * 设置行间距
     * 
     * @param spacing 行间距倍数
     */
    public void setLineSpacing(float spacing) {
        settingsRepository.setLineSpacing(spacing);
        updateState(state -> state.setLineSpacing(spacing));
    }

    /**
     * 设置屏幕常亮
     * 
     * @param keepOn 是否屏幕常亮
     */
    public void setKeepScreenOn(boolean keepOn) {
        settingsRepository.setKeepScreenOn(keepOn);
        updateState(state -> state.setKeepScreenOn(keepOn));
    }

    /**
     * 设置音量键翻页
     * 
     * @param enabled 是否启用
     */
    public void setVolumeKeyPageTurn(boolean enabled) {
        settingsRepository.setVolumeKeyPageTurn(enabled);
        updateState(state -> state.setVolumeKeyPageTurn(enabled));
    }

    /**
     * 设置全屏阅读
     * 
     * @param fullScreen 是否全屏
     */
    public void setFullScreenReading(boolean fullScreen) {
        settingsRepository.setFullScreenReading(fullScreen);
        updateState(state -> state.setFullScreenReading(fullScreen));
    }

    /**
     * 重置所有设置为默认值
     */
    public void resetToDefaults() {
        executorService.execute(() -> {
            settingsRepository.resetToDefaults();
            loadInitialData();
            updateState(state -> state.setSuccessMessage("设置已重置为默认值"));
        });
    }

    // ==================== 解析规则管理 ====================

    /**
     * 获取解析规则列表LiveData
     * 验证需求：3.1 - 显示已有规则列表
     */
    public LiveData<List<ParserRule>> getParserRulesLiveData() {
        return parserRuleRepository.getAllRules();
    }

    /**
     * 开始编辑规则（新建或编辑）
     * 
     * @param rule 要编辑的规则，null表示新建
     */
    public void startEditingRule(ParserRule rule) {
        updateState(state -> {
            if (rule == null) {
                // 新建规则
                state.setEditingRule(new ParserRule());
            } else {
                // 编辑现有规则
                state.setEditingRule(rule);
            }
            state.setRuleTestResult(null);
        });
    }

    /**
     * 取消编辑规则
     */
    public void cancelEditingRule() {
        updateState(state -> {
            state.setEditingRule(null);
            state.setRuleTestResult(null);
        });
    }

    /**
     * 保存解析规则
     * 验证需求：3.1 - 配置网站域名、章节列表选择器、正文选择器
     * 
     * @param rule 要保存的规则
     */
    public void saveParserRule(ParserRule rule) {
        if (rule == null) return;

        // 先验证规则
        ParserRuleRepository.ValidationResult validation = parserRuleRepository.validateRule(rule);
        if (!validation.isValid()) {
            updateState(state -> {
                state.setError("规则配置不完整，缺少字段: " + 
                    String.join(", ", validation.getMissingFields()));
            });
            return;
        }

        updateState(state -> state.setLoading(true));

        disposables.add(
            parserRuleRepository.insertRule(rule)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    ruleId -> {
                        updateState(state -> {
                            state.setLoading(false);
                            state.setEditingRule(null);
                            state.setSuccessMessage("规则已保存");
                        });
                    },
                    error -> {
                        updateState(state -> {
                            state.setLoading(false);
                            state.setError("保存规则失败: " + error.getMessage());
                        });
                    }
                )
        );
    }

    /**
     * 删除解析规则
     * 
     * @param ruleId 规则ID
     */
    public void deleteParserRule(long ruleId) {
        executorService.execute(() -> {
            try {
                parserRuleRepository.deleteRule(ruleId);
                updateState(state -> state.setSuccessMessage("规则已删除"));
            } catch (Exception e) {
                updateState(state -> state.setError("删除规则失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 测试解析规则
     * 
     * @param rule 要测试的规则
     * @param testUrl 测试URL
     */
    public void testParserRule(ParserRule rule, String testUrl) {
        if (rule == null || testUrl == null || testUrl.trim().isEmpty()) {
            updateState(state -> state.setError("请输入测试URL"));
            return;
        }

        updateState(state -> {
            state.setTestingRule(true);
            state.setRuleTestResult(null);
        });

        disposables.add(
            parserRuleRepository.testRule(rule, testUrl)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        updateState(state -> {
                            state.setTestingRule(false);
                            if (result.isSuccess()) {
                                state.setRuleTestResult(
                                    "测试成功!\n" +
                                    "标题: " + result.getNovelTitle() + "\n" +
                                    "作者: " + result.getNovelAuthor() + "\n" +
                                    "章节数: " + result.getChapterCount() + "\n" +
                                    "示例内容: " + result.getSampleContent()
                                );
                            } else {
                                state.setRuleTestResult("测试失败: " + result.getErrorMessage());
                            }
                        });
                    },
                    error -> {
                        updateState(state -> {
                            state.setTestingRule(false);
                            state.setRuleTestResult("测试出错: " + error.getMessage());
                        });
                    }
                )
        );
    }

    // ==================== 屏蔽词管理 ====================

    /**
     * 获取屏蔽词列表LiveData
     * 验证需求：11.1 - 显示当前已设置的屏蔽词列表
     */
    public LiveData<List<BlockedWord>> getBlockedWordsLiveData() {
        return blockedWordRepository.getAllBlockedWords();
    }

    /**
     * 添加屏蔽词
     * 验证需求：11.1 - 添加新屏蔽词
     * 
     * @param word 屏蔽词
     */
    public void addBlockedWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            updateState(state -> state.setError("屏蔽词不能为空"));
            return;
        }

        executorService.execute(() -> {
            try {
                long id = blockedWordRepository.insertBlockedWord(word.trim());
                if (id > 0) {
                    updateState(state -> state.setSuccessMessage("屏蔽词已添加: " + word.trim()));
                } else {
                    updateState(state -> state.setError("添加屏蔽词失败"));
                }
            } catch (Exception e) {
                updateState(state -> state.setError("添加屏蔽词失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 删除屏蔽词
     * 
     * @param wordId 屏蔽词ID
     */
    public void deleteBlockedWord(long wordId) {
        executorService.execute(() -> {
            try {
                blockedWordRepository.deleteBlockedWord(wordId);
                updateState(state -> state.setSuccessMessage("屏蔽词已删除"));
            } catch (Exception e) {
                updateState(state -> state.setError("删除屏蔽词失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 清空所有屏蔽词
     */
    public void clearAllBlockedWords() {
        executorService.execute(() -> {
            try {
                blockedWordRepository.deleteAllBlockedWords();
                updateState(state -> state.setSuccessMessage("所有屏蔽词已清空"));
            } catch (Exception e) {
                updateState(state -> state.setError("清空屏蔽词失败: " + e.getMessage()));
            }
        });
    }

    // ==================== 通用方法 ====================

    /**
     * 清除错误信息
     */
    public void clearError() {
        updateState(state -> state.setError(null));
    }

    /**
     * 清除成功消息
     */
    public void clearSuccessMessage() {
        updateState(state -> state.setSuccessMessage(null));
    }

    /**
     * 更新UI状态的辅助方法
     */
    private void updateState(StateUpdater updater) {
        SettingsUiState currentState = _uiState.getValue();
        if (currentState == null) {
            currentState = new SettingsUiState();
        }
        SettingsUiState newState = new SettingsUiState(currentState);
        updater.update(newState);
        _uiState.postValue(newState);
    }

    /**
     * 状态更新接口
     */
    private interface StateUpdater {
        void update(SettingsUiState state);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        
        // 移除观察者
        if (themeObserver != null) {
            themeRepository.getCurrentThemeLiveData().removeObserver(themeObserver);
        }
        if (rulesObserver != null) {
            parserRuleRepository.getAllRules().removeObserver(rulesObserver);
        }
        if (blockedWordsObserver != null) {
            blockedWordRepository.getAllBlockedWords().removeObserver(blockedWordsObserver);
        }
        
        // 清理资源
        disposables.dispose();
        executorService.shutdown();
    }
}
