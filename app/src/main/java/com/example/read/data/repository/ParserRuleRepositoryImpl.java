package com.example.read.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.read.data.dao.ParserRuleDao;
import com.example.read.data.entity.ParserRuleEntity;
import com.example.read.domain.error.AppError;
import com.example.read.domain.mapper.ParserRuleMapper;
import com.example.read.domain.model.ChapterInfo;
import com.example.read.domain.model.NovelMetadata;
import com.example.read.domain.model.ParserRule;
import com.example.read.domain.repository.ParserRuleRepository;
import com.example.read.domain.service.WebParserService;
import com.example.read.utils.NetworkRequestManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 解析规则Repository实现
 * 实现规则CRUD操作、验证和测试功能
 */
@Singleton
public class ParserRuleRepositoryImpl implements ParserRuleRepository {
    
    private final ParserRuleDao parserRuleDao;
    private final WebParserService webParserService;
    private final NetworkRequestManager networkRequestManager;
    
    @Inject
    public ParserRuleRepositoryImpl(
            ParserRuleDao parserRuleDao,
            WebParserService webParserService,
            NetworkRequestManager networkRequestManager) {
        this.parserRuleDao = parserRuleDao;
        this.webParserService = webParserService;
        this.networkRequestManager = networkRequestManager;
    }
    
    @Override
    public LiveData<List<ParserRule>> getAllRules() {
        return Transformations.map(
            parserRuleDao.getAllRules(),
            ParserRuleMapper::toDomainList
        );
    }
    
    @Override
    public ParserRule getRuleById(long ruleId) {
        ParserRuleEntity entity = parserRuleDao.getRuleById(ruleId);
        return ParserRuleMapper.toDomain(entity);
    }
    
    @Override
    public ParserRule getRuleByDomain(String domain) {
        ParserRuleEntity entity = parserRuleDao.getRuleByDomain(domain);
        return ParserRuleMapper.toDomain(entity);
    }
    
    @Override
    public Single<Long> insertRule(ParserRule rule) {
        return Single.fromCallable(() -> {
            // 验证规则
            ValidationResult validation = validateRule(rule);
            if (!validation.isValid()) {
                throw new AppError.ValidationError(
                    "规则配置不完整，缺少字段: " + String.join(", ", validation.getMissingFields()),
                    String.join(", ", validation.getMissingFields())
                );
            }
            
            // 转换并保存
            ParserRuleEntity entity = ParserRuleMapper.toEntity(rule);
            return parserRuleDao.insertRule(entity);
        }).subscribeOn(Schedulers.io());
    }
    
    @Override
    public void updateRule(ParserRule rule) {
        ParserRuleEntity entity = ParserRuleMapper.toEntity(rule);
        parserRuleDao.updateRule(entity);
    }
    
    @Override
    public void deleteRule(long ruleId) {
        parserRuleDao.deleteRuleById(ruleId);
    }

    @Override
    public ValidationResult validateRule(ParserRule rule) {
        List<String> missingFields = new ArrayList<>();
        
        if (rule == null) {
            missingFields.add("规则对象");
            return new ValidationResult(false, missingFields);
        }
        
        // 检查必需字段
        if (rule.getDomain() == null || rule.getDomain().trim().isEmpty()) {
            missingFields.add("域名(domain)");
        }
        
        if (rule.getChapterListSelector() == null || rule.getChapterListSelector().trim().isEmpty()) {
            missingFields.add("章节列表选择器(chapterListSelector)");
        }
        
        if (rule.getContentSelector() == null || rule.getContentSelector().trim().isEmpty()) {
            missingFields.add("内容选择器(contentSelector)");
        }
        
        // 可选但建议填写的字段
        if (rule.getName() == null || rule.getName().trim().isEmpty()) {
            // 名称不是必需的，但如果为空则使用域名作为名称
        }
        
        return new ValidationResult(missingFields.isEmpty(), missingFields);
    }
    
    @Override
    public Single<TestResult> testRule(ParserRule rule, String testUrl) {
        // 先验证规则
        ValidationResult validation = validateRule(rule);
        if (!validation.isValid()) {
            return Single.just(TestResult.failure(
                "规则配置不完整，缺少字段: " + String.join(", ", validation.getMissingFields())
            ));
        }
        
        // 验证URL
        if (testUrl == null || testUrl.trim().isEmpty()) {
            return Single.just(TestResult.failure("测试URL不能为空"));
        }
        
        return networkRequestManager.executeRequest(
            webParserService.fetchHtml(testUrl)
        )
        .map(html -> {
            try {
                // 提取元数据
                NovelMetadata metadata = webParserService.extractNovelInfo(html, rule);
                
                // 提取章节列表
                List<ChapterInfo> chapters = webParserService.extractChapterList(html, rule);
                
                // 获取示例内容（如果有章节的话）
                String sampleContent = "";
                if (!chapters.isEmpty()) {
                    // 尝试获取第一章的内容作为示例
                    ChapterInfo firstChapter = chapters.get(0);
                    if (firstChapter.getUrl() != null && !firstChapter.getUrl().isEmpty()) {
                        try {
                            String chapterHtml = webParserService.fetchHtml(firstChapter.getUrl()).blockingGet();
                            sampleContent = webParserService.extractChapterContent(chapterHtml, rule);
                            // 截取前200个字符作为示例
                            if (sampleContent.length() > 200) {
                                sampleContent = sampleContent.substring(0, 200) + "...";
                            }
                        } catch (Exception e) {
                            sampleContent = "[无法获取章节内容: " + e.getMessage() + "]";
                        }
                    }
                }
                
                return TestResult.success(
                    metadata.getTitle() != null ? metadata.getTitle() : "未能提取标题",
                    metadata.getAuthor() != null ? metadata.getAuthor() : "未能提取作者",
                    chapters.size(),
                    sampleContent
                );
                
            } catch (Exception e) {
                return TestResult.failure("解析失败: " + e.getMessage());
            }
        })
        .onErrorReturn(error -> TestResult.failure("网络请求失败: " + error.getMessage()))
        .subscribeOn(Schedulers.io());
    }
}
