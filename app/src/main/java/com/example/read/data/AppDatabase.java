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
    version = 6,
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
    
    /**
     * 数据库迁移：版本3 -> 版本4
     * 添加 textPreview 字段到 bookmarks 表（用于显示书签位置的文本预览）
     */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加文本预览字段
            database.execSQL("ALTER TABLE bookmarks ADD COLUMN textPreview TEXT");
        }
    };

    /**
     * 数据库迁移：版本4 -> 版本5
     * 为 blocked_words 表添加 novelId 字段，支持每本小说独立的屏蔽词
     * 注意：不使用外键约束，避免迁移时的约束冲突
     */
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 创建新表（带 novelId 字段，不使用外键约束）
            database.execSQL("CREATE TABLE IF NOT EXISTS `blocked_words_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`novelId` INTEGER NOT NULL DEFAULT 0, " +
                    "`word` TEXT NOT NULL, " +
                    "`createTime` INTEGER NOT NULL)");
            
            // 复制旧数据到新表（旧数据的 novelId 设为 0，后续会被删除）
            database.execSQL("INSERT INTO blocked_words_new (id, novelId, word, createTime) " +
                    "SELECT id, 0, word, createTime FROM blocked_words");
            
            // 删除旧表
            database.execSQL("DROP TABLE blocked_words");
            
            // 重命名新表
            database.execSQL("ALTER TABLE blocked_words_new RENAME TO blocked_words");
            
            // 创建索引
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_blocked_words_novelId` ON `blocked_words` (`novelId`)");
            
            // 删除 novelId 为 0 的旧数据（因为没有关联的小说）
            database.execSQL("DELETE FROM blocked_words WHERE novelId = 0");
        }
    };

    /**
     * 数据库迁移：版本5 -> 版本6
     * 为 parser_rules 表添加 isDynamic 字段，支持强制使用动态解析
     */
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加 isDynamic 字段，默认值为 0（false）
            database.execSQL("ALTER TABLE parser_rules ADD COLUMN isDynamic INTEGER NOT NULL DEFAULT 0");
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
