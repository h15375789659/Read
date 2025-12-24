package com.example.read.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.read.domain.model.ParserRule;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

/**
 * 解析规则Repository接口
 * 负责解析规则的CRUD操作和验证
 */
public interface ParserRuleRepository {
    
    /**
     * 获取所有解析规则（LiveData）
     * @return 解析规则列表
     */
    LiveData<List<ParserRule>> getAllRules();
    
    /**
     * 根据ID获取解析规则
     * @param ruleId 规则ID
     * @return 解析规则
     */
    ParserRule getRuleById(long ruleId);
    
    /**
     * 根据域名获取解析规则
     * @param domain 域名
     * @return 解析规则
     */
    ParserRule getRuleByDomain(String domain);
    
    /**
     * 插入或更新解析规则
     * @param rule 解析规则
     * @return 规则ID
     */
    Single<Long> insertRule(ParserRule rule);
    
    /**
     * 更新解析规则
     * @param rule 解析规则
     */
    void updateRule(ParserRule rule);
    
    /**
     * 删除解析规则
     * @param ruleId 规则ID
     */
    void deleteRule(long ruleId);
    
    /**
     * 验证解析规则的完整性
     * @param rule 解析规则
     * @return 验证结果，包含是否有效和缺失字段信息
     */
    ValidationResult validateRule(ParserRule rule);
    
    /**
     * 测试解析规则
     * @param rule 解析规则
     * @param testUrl 测试URL
     * @return 测试结果
     */
    Single<TestResult> testRule(ParserRule rule, String testUrl);
    
    /**
     * 验证结果类
     */
    class ValidationResult {
        private final boolean valid;
        private final List<String> missingFields;
        
        public ValidationResult(boolean valid, List<String> missingFields) {
            this.valid = valid;
            this.missingFields = missingFields;
        }
        
        public boolean isValid() { return valid; }
        public List<String> getMissingFields() { return missingFields; }
    }
    
    /**
     * 测试结果类
     */
    class TestResult {
        private final boolean success;
        private final String novelTitle;
        private final String novelAuthor;
        private final int chapterCount;
        private final String sampleContent;
        private final String errorMessage;
        
        private TestResult(boolean success, String novelTitle, String novelAuthor, 
                          int chapterCount, String sampleContent, String errorMessage) {
            this.success = success;
            this.novelTitle = novelTitle;
            this.novelAuthor = novelAuthor;
            this.chapterCount = chapterCount;
            this.sampleContent = sampleContent;
            this.errorMessage = errorMessage;
        }
        
        public static TestResult success(String novelTitle, String novelAuthor, 
                                         int chapterCount, String sampleContent) {
            return new TestResult(true, novelTitle, novelAuthor, chapterCount, sampleContent, null);
        }
        
        public static TestResult failure(String errorMessage) {
            return new TestResult(false, null, null, 0, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public String getNovelTitle() { return novelTitle; }
        public String getNovelAuthor() { return novelAuthor; }
        public int getChapterCount() { return chapterCount; }
        public String getSampleContent() { return sampleContent; }
        public String getErrorMessage() { return errorMessage; }
    }
}
