package com.example.read.data;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.read.data.dao.BlockedWordDao;
import com.example.read.data.dao.BookmarkDao;
import com.example.read.data.dao.CategoryDao;
import com.example.read.data.dao.ChapterDao;
import com.example.read.data.dao.NovelDao;
import com.example.read.data.dao.ParserRuleDao;
import com.example.read.data.dao.ReadingStatisticsDao;
import com.example.read.data.entity.BlockedWordEntity;
import com.example.read.data.entity.BookmarkEntity;
import com.example.read.data.entity.CategoryEntity;
import com.example.read.data.entity.ChapterEntity;
import com.example.read.data.entity.NovelEntity;
import com.example.read.data.entity.ParserRuleEntity;
import com.example.read.data.entity.ReadingStatisticsEntity;

/**
 * Room数据库类 - 应用的主数据库
 * 包含所有实体表和DAO访问接口
 */
@Database(
    entities = {
        NovelEntity.class,
        ChapterEntity.class,
        BookmarkEntity.class,
        ParserRuleEntity.class,
        ReadingStatisticsEntity.class,
        BlockedWordEntity.class,
        CategoryEntity.class
    },
    version = 3,
    exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {
    
    public static final String DATABASE_NAME = "novel_reader_db";
    
    /**
     * 数据库迁移：版本2 -> 版本3
     * 添加 currentChapterTitle 和 latestChapterTitle 字段到 novels 表
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加当前阅读章节标题字段
            database.execSQL("ALTER TABLE novels ADD COLUMN currentChapterTitle TEXT");
            // 添加最新章节标题字段
            database.execSQL("ALTER TABLE novels ADD COLUMN latestChapterTitle TEXT");
        }
    };

    // DAO访问方法
    public abstract NovelDao novelDao();
    public abstract ChapterDao chapterDao();
    public abstract BookmarkDao bookmarkDao();
    public abstract ParserRuleDao parserRuleDao();
    public abstract ReadingStatisticsDao readingStatisticsDao();
    public abstract BlockedWordDao blockedWordDao();
    public abstract CategoryDao categoryDao();
}
