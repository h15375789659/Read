package com.example.read.presentation.settings;

import com.example.read.domain.model.BlockedWord;
import com.example.read.domain.model.ParserRule;
import com.example.read.domain.model.ReaderTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * 设置界面UI状态
 * 
 * 验证需求：3.1, 6.1, 11.1
 */
public class SettingsUiState {

    // 主题相关状态
    private ReaderTheme currentTheme;
    private List<ReaderTheme> allThemes;
    private boolean autoThemeSwitchEnabled;
    private int nightModeStartHour;
    private int nightModeEndHour;

    // 阅读设置状态
    private float fontSize;
    private float lineSpacing;
    private boolean keepScreenOn;
    private boolean volumeKeyPageTurn;
    private boolean fullScreenReading;

    // 解析规则状态
    private List<ParserRule> parserRules;
    private ParserRule editingRule;
    private boolean isTestingRule;
    private String ruleTestResult;

    // 屏蔽词状态
    private List<BlockedWord> blockedWords;

    // 通用状态
    private boolean isLoading;
    private String error;
    private String successMessage;

    public SettingsUiState() {
        this.allThemes = new ArrayList<>();
        this.parserRules = new ArrayList<>();
        this.blockedWords = new ArrayList<>();
        this.fontSize = 18f;
        this.lineSpacing = 1.5f;
        this.nightModeStartHour = 22;
        this.nightModeEndHour = 6;
        this.isLoading = false;
        this.isTestingRule = false;
    }

    /**
     * 复制构造函数
     */
    public SettingsUiState(SettingsUiState other) {
        this.currentTheme = other.currentTheme;
        this.allThemes = new ArrayList<>(other.allThemes);
        this.autoThemeSwitchEnabled = other.autoThemeSwitchEnabled;
        this.nightModeStartHour = other.nightModeStartHour;
        this.nightModeEndHour = other.nightModeEndHour;
        this.fontSize = other.fontSize;
        this.lineSpacing = other.lineSpacing;
        this.keepScreenOn = other.keepScreenOn;
        this.volumeKeyPageTurn = other.volumeKeyPageTurn;
        this.fullScreenReading = other.fullScreenReading;
        this.parserRules = new ArrayList<>(other.parserRules);
        this.editingRule = other.editingRule;
        this.isTestingRule = other.isTestingRule;
        this.ruleTestResult = other.ruleTestResult;
        this.blockedWords = new ArrayList<>(other.blockedWords);
        this.isLoading = other.isLoading;
        this.error = other.error;
        this.successMessage = other.successMessage;
    }

    // ==================== 主题相关 Getters/Setters ====================

    public ReaderTheme getCurrentTheme() { return currentTheme; }
    public void setCurrentTheme(ReaderTheme currentTheme) { this.currentTheme = currentTheme; }

    public List<ReaderTheme> getAllThemes() { return allThemes; }
    public void setAllThemes(List<ReaderTheme> allThemes) { this.allThemes = allThemes; }

    public boolean isAutoThemeSwitchEnabled() { return autoThemeSwitchEnabled; }
    public void setAutoThemeSwitchEnabled(boolean autoThemeSwitchEnabled) { 
        this.autoThemeSwitchEnabled = autoThemeSwitchEnabled; 
    }

    public int getNightModeStartHour() { return nightModeStartHour; }
    public void setNightModeStartHour(int nightModeStartHour) { 
        this.nightModeStartHour = nightModeStartHour; 
    }

    public int getNightModeEndHour() { return nightModeEndHour; }
    public void setNightModeEndHour(int nightModeEndHour) { 
        this.nightModeEndHour = nightModeEndHour; 
    }

    // ==================== 阅读设置 Getters/Setters ====================

    public float getFontSize() { return fontSize; }
    public void setFontSize(float fontSize) { this.fontSize = fontSize; }

    public float getLineSpacing() { return lineSpacing; }
    public void setLineSpacing(float lineSpacing) { this.lineSpacing = lineSpacing; }

    public boolean isKeepScreenOn() { return keepScreenOn; }
    public void setKeepScreenOn(boolean keepScreenOn) { this.keepScreenOn = keepScreenOn; }

    public boolean isVolumeKeyPageTurn() { return volumeKeyPageTurn; }
    public void setVolumeKeyPageTurn(boolean volumeKeyPageTurn) { 
        this.volumeKeyPageTurn = volumeKeyPageTurn; 
    }

    public boolean isFullScreenReading() { return fullScreenReading; }
    public void setFullScreenReading(boolean fullScreenReading) { 
        this.fullScreenReading = fullScreenReading; 
    }

    // ==================== 解析规则 Getters/Setters ====================

    public List<ParserRule> getParserRules() { return parserRules; }
    public void setParserRules(List<ParserRule> parserRules) { this.parserRules = parserRules; }

    public ParserRule getEditingRule() { return editingRule; }
    public void setEditingRule(ParserRule editingRule) { this.editingRule = editingRule; }

    public boolean isTestingRule() { return isTestingRule; }
    public void setTestingRule(boolean testingRule) { isTestingRule = testingRule; }

    public String getRuleTestResult() { return ruleTestResult; }
    public void setRuleTestResult(String ruleTestResult) { this.ruleTestResult = ruleTestResult; }

    // ==================== 屏蔽词 Getters/Setters ====================

    public List<BlockedWord> getBlockedWords() { return blockedWords; }
    public void setBlockedWords(List<BlockedWord> blockedWords) { this.blockedWords = blockedWords; }

    // ==================== 通用状态 Getters/Setters ====================

    public boolean isLoading() { return isLoading; }
    public void setLoading(boolean loading) { isLoading = loading; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getSuccessMessage() { return successMessage; }
    public void setSuccessMessage(String successMessage) { this.successMessage = successMessage; }
}
