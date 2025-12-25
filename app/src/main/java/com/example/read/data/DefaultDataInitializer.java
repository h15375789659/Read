package com.example.read.data;

import android.util.Log;

import com.example.read.data.dao.ParserRuleDao;
import com.example.read.data.entity.ParserRuleEntity;

import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 默认数据初始化器
 * 用于在应用启动时检查并添加默认解析规则
 */
@Singleton
public class DefaultDataInitializer {

    private static final String TAG = "DefaultDataInitializer";
    
    private final ParserRuleDao parserRuleDao;
    private volatile boolean initialized = false;

    @Inject
    public DefaultDataInitializer(ParserRuleDao parserRuleDao) {
        this.parserRuleDao = parserRuleDao;
    }

    /**
     * 初始化默认数据
     * 如果数据库中没有解析规则，则添加默认规则
     */
    public void initializeDefaultData() {
        if (initialized) {
            return;
        }
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 检查是否已有规则
                int ruleCount = parserRuleDao.getRuleCount();
                Log.d(TAG, "当前解析规则数量: " + ruleCount);
                
                if (ruleCount == 0) {
                    Log.d(TAG, "没有解析规则，开始添加默认规则");
                    insertDefaultRules();
                } else {
                    // 检查是否需要更新规则（如果没有"智能通用规则"则需要更新）
                    checkAndUpdateRules();
                }
                
                initialized = true;
            } catch (Exception e) {
                Log.e(TAG, "初始化默认数据失败", e);
            }
        });
    }
    
    /**
     * 检查并更新规则
     * 如果缺少新的智能通用规则，则添加
     */
    private void checkAndUpdateRules() {
        try {
            // 检查是否存在"智能通用规则"
            ParserRuleEntity existingRule = parserRuleDao.getRuleByName("智能通用规则");
            if (existingRule == null) {
                Log.d(TAG, "未找到智能通用规则，添加新规则");
                long currentTime = System.currentTimeMillis();
                
                // 添加智能通用规则作为第一个规则
                ParserRuleEntity ruleUniversal = new ParserRuleEntity(
                        "智能通用规则",
                        "*",
                        "#list dd a, .listmain dd a, #chapterlist a, .chapter-list a, " +
                        ".mulu a, .catalog a, .volume a, ul.list a, .chapters a, " +
                        "#catalog a, .booklist a, .ml_list a, .zjlist a, " +
                        ".dirlist a, #dir a, .chapterlist a",
                        "",
                        "",
                        "#content, #chaptercontent, #booktxt, #booktext, #htmlContent, " +
                        "#nr, #nr1, .content, .chapter-content, .booktxt, .booktext, " +
                        ".read-content, .novelcontent, .article-content, .txt, " +
                        ".yd_text2, .txtnav, .contentbox"
                );
                ruleUniversal.setRemoveSelectors("script,style,.ad,.ads,.advertisement,#ad,#ads,.banner,.popup,.comment,.comments,iframe,.copy,.bottem,.bottem2,.txtinfo,.review-wrap");
                ruleUniversal.setCreateTime(currentTime - 1000000); // 设置较早的时间确保排在前面
                parserRuleDao.insertRule(ruleUniversal);
                Log.d(TAG, "智能通用规则添加完成");
            }
        } catch (Exception e) {
            Log.e(TAG, "检查更新规则失败", e);
        }
    }

    /**
     * 插入默认解析规则
     * 规则按优先级排序：通用规则在前，特定网站规则在后
     */
    private void insertDefaultRules() {
        long currentTime = System.currentTimeMillis();

        // 1. 智能通用规则（默认首选）- 覆盖最广泛的网站结构
        ParserRuleEntity ruleUniversal = new ParserRuleEntity(
                "智能通用规则",
                "*",
                // 章节列表选择器：覆盖多种常见结构
                "#list dd a, .listmain dd a, #chapterlist a, .chapter-list a, " +
                ".mulu a, .catalog a, .volume a, ul.list a, .chapters a, " +
                "#catalog a, .booklist a, .ml_list a, .zjlist a, " +
                ".dirlist a, #dir a, .chapterlist a",
                "",
                "",
                // 内容选择器：覆盖多种常见ID和class
                "#content, #chaptercontent, #booktxt, #booktext, #htmlContent, " +
                "#nr, #nr1, .content, .chapter-content, .booktxt, .booktext, " +
                ".read-content, .novelcontent, .article-content, .txt, " +
                ".yd_text2, .txtnav, .contentbox"
        );
        ruleUniversal.setRemoveSelectors("script,style,.ad,.ads,.advertisement,#ad,#ads,.banner,.popup,.comment,.comments,iframe,.copy,.bottem,.bottem2,.txtinfo,.review-wrap");
        ruleUniversal.setCreateTime(currentTime);
        parserRuleDao.insertRule(ruleUniversal);

        // 2. 通用规则A - 适用于标准小说网站结构
        ParserRuleEntity ruleA = new ParserRuleEntity(
                "通用规则A",
                "*",
                "#list dd a, .listmain dd a, .chapter-list a, .mulu a, #chapterlist a",
                "",
                "",
                "#content, .content, #chaptercontent, .chapter-content, #booktxt, .booktxt"
        );
        ruleA.setRemoveSelectors("script,style,.ad,.ads,.banner,.popup");
        ruleA.setCreateTime(currentTime + 1);
        parserRuleDao.insertRule(ruleA);

        // 3. 通用规则B - 另一种常见结构
        ParserRuleEntity ruleB = new ParserRuleEntity(
                "通用规则B",
                "*",
                ".chapter a, .chapters a, .catalog a, ul.list a, .volume a, .zjlist a",
                "",
                "",
                "#content, .content, .article, .text, .read-content, #chaptercontent, .novelcontent"
        );
        ruleB.setRemoveSelectors("script,style,.ad,.ads,.copy,.banner");
        ruleB.setCreateTime(currentTime + 2);
        parserRuleDao.insertRule(ruleB);

        // 4. 笔趣阁类规则
        ParserRuleEntity ruleBiquge = new ParserRuleEntity(
                "笔趣阁类",
                "biquge",
                "#list dd a, .listmain dd a, #chapterlist a",
                "",
                "",
                "#content, #chaptercontent, .content"
        );
        ruleBiquge.setRemoveSelectors("script,style,.bottem,.bottem2,.ad");
        ruleBiquge.setCreateTime(currentTime + 3);
        parserRuleDao.insertRule(ruleBiquge);

        // 5. 起点类规则
        ParserRuleEntity ruleQidian = new ParserRuleEntity(
                "起点类",
                "qidian",
                ".volume-wrap .cf li a, .chapter-list a, .catalog a",
                "",
                "",
                ".read-content, .chapter-content, .content, #content"
        );
        ruleQidian.setRemoveSelectors("script,style,.review-wrap,.chapter-review,.ad");
        ruleQidian.setCreateTime(currentTime + 4);
        parserRuleDao.insertRule(ruleQidian);

        // 6. 69书吧类规则
        ParserRuleEntity rule69shu = new ParserRuleEntity(
                "69书吧类",
                "69shu",
                ".mu_contain li a, .mulu a, #catalog a",
                "",
                "",
                ".yd_text2, .txtnav, #content, .content"
        );
        rule69shu.setRemoveSelectors("script,style,.txtinfo,.ad");
        rule69shu.setCreateTime(currentTime + 5);
        parserRuleDao.insertRule(rule69shu);

        Log.d(TAG, "默认解析规则添加完成，共6条规则");
    }
}
