package com.example.read.di;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.read.data.AppDatabase;
import com.example.read.data.dao.BlockedWordDao;
import com.example.read.data.dao.BookmarkDao;
import com.example.read.data.dao.CategoryDao;
import com.example.read.data.dao.ChapterDao;
import com.example.read.data.dao.NovelDao;
import com.example.read.data.dao.ParserRuleDao;
import com.example.read.data.dao.ReadingStatisticsDao;

import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt模块 - 提供数据库相关的依赖注入
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    /**
     * 数据库迁移：版本1 -> 版本2
     * 添加categories表
     */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 创建categories表
            database.execSQL("CREATE TABLE IF NOT EXISTS `categories` (" +
                    "`name` TEXT NOT NULL, " +
                    "`sortOrder` INTEGER NOT NULL, " +
                    "`createdTime` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`name`))");
        }
    };

    /**
     * 数据库创建回调 - 插入默认解析规则
     */
    private static final RoomDatabase.Callback DATABASE_CALLBACK = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            // 在后台线程插入默认解析规则
            Executors.newSingleThreadExecutor().execute(() -> {
                insertDefaultParserRules(db);
            });
        }
    };

    /**
     * 插入默认解析规则
     * 这些规则适用于常见的小说网站结构
     */
    private static void insertDefaultParserRules(SupportSQLiteDatabase db) {
        long currentTime = System.currentTimeMillis();
        
        // 通用规则1 - 适用于大多数小说网站
        db.execSQL("INSERT INTO parser_rules (name, domain, chapterListSelector, chapterTitleSelector, chapterLinkSelector, contentSelector, removeSelectors, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        "通用规则A",
                        "*",
                        "#list dd a, .listmain dd a, .chapter-list a, .mulu a",
                        "",
                        "",
                        "#content, .content, #chaptercontent, .chapter-content, #booktxt, .booktxt",
                        ".ad,.ads,script,style",
                        currentTime
                });

        // 通用规则2 - 另一种常见结构
        db.execSQL("INSERT INTO parser_rules (name, domain, chapterListSelector, chapterTitleSelector, chapterLinkSelector, contentSelector, removeSelectors, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        "通用规则B",
                        "*",
                        ".chapter a, .chapters a, .catalog a, ul.list a, .volume a",
                        "",
                        "",
                        "#content, .content, .article, .text, .read-content, #chaptercontent",
                        ".ad,.ads,script,style,.copy",
                        currentTime + 1
                });

        // 笔趣阁类规则
        db.execSQL("INSERT INTO parser_rules (name, domain, chapterListSelector, chapterTitleSelector, chapterLinkSelector, contentSelector, removeSelectors, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        "笔趣阁类",
                        "biquge",
                        "#list dd a",
                        "",
                        "",
                        "#content",
                        "script,style,.bottem,.bottem2",
                        currentTime + 2
                });

        // 起点类规则
        db.execSQL("INSERT INTO parser_rules (name, domain, chapterListSelector, chapterTitleSelector, chapterLinkSelector, contentSelector, removeSelectors, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        "起点类",
                        "qidian",
                        ".volume-wrap .cf li a, .chapter-list a",
                        "",
                        "",
                        ".read-content, .chapter-content, .content",
                        "script,style,.review-wrap,.chapter-review",
                        currentTime + 3
                });

        // 69书吧类规则
        db.execSQL("INSERT INTO parser_rules (name, domain, chapterListSelector, chapterTitleSelector, chapterLinkSelector, contentSelector, removeSelectors, createTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        "69书吧类",
                        "69shu",
                        ".mu_contain li a",
                        "",
                        "",
                        ".yd_text2, .txtnav",
                        "script,style,.txtinfo",
                        currentTime + 4
                });
    }

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                context,
                AppDatabase.class,
                AppDatabase.DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .addCallback(DATABASE_CALLBACK)
            .fallbackToDestructiveMigration()
            .build();
    }

    @Provides
    @Singleton
    public NovelDao provideNovelDao(AppDatabase database) {
        return database.novelDao();
    }

    @Provides
    @Singleton
    public ChapterDao provideChapterDao(AppDatabase database) {
        return database.chapterDao();
    }

    @Provides
    @Singleton
    public BookmarkDao provideBookmarkDao(AppDatabase database) {
        return database.bookmarkDao();
    }

    @Provides
    @Singleton
    public ParserRuleDao provideParserRuleDao(AppDatabase database) {
        return database.parserRuleDao();
    }

    @Provides
    @Singleton
    public ReadingStatisticsDao provideReadingStatisticsDao(AppDatabase database) {
        return database.readingStatisticsDao();
    }

    @Provides
    @Singleton
    public BlockedWordDao provideBlockedWordDao(AppDatabase database) {
        return database.blockedWordDao();
    }

    @Provides
    @Singleton
    public CategoryDao provideCategoryDao(AppDatabase database) {
        return database.categoryDao();
    }
}
