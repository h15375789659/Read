package com.example.read.di;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
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

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                context,
                AppDatabase.class,
                AppDatabase.DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
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
