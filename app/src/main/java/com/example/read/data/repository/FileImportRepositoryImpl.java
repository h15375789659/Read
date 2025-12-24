package com.example.read.data.repository;

import android.net.Uri;
import android.util.Log;

import com.example.read.data.AppDatabase;
import com.example.read.data.dao.ChapterDao;
import com.example.read.data.dao.NovelDao;
import com.example.read.data.entity.ChapterEntity;
import com.example.read.data.entity.NovelEntity;
import com.example.read.domain.error.AppError;
import com.example.read.domain.mapper.NovelMapper;
import com.example.read.domain.model.Novel;
import com.example.read.domain.model.NovelSource;
import com.example.read.domain.model.ParsedNovel;
import com.example.read.domain.repository.FileImportRepository;
import com.example.read.domain.service.FileParserService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 文件导入Repository实现
 * 集成FileParserService，实现文件解析和数据库保存
 */
@Singleton
public class FileImportRepositoryImpl implements FileImportRepository {

    private static final String TAG = "FileImportRepository";
    
    private final FileParserService fileParserService;
    private final NovelDao novelDao;
    private final ChapterDao chapterDao;
    private final AppDatabase database;
    
    // 批量插入的分块大小，避免一次性插入太多数据
    private static final int BATCH_SIZE = 50;

    @Inject
    public FileImportRepositoryImpl(
            FileParserService fileParserService,
            NovelDao novelDao,
            ChapterDao chapterDao,
            AppDatabase database) {
        this.fileParserService = fileParserService;
        this.novelDao = novelDao;
        this.chapterDao = chapterDao;
        this.database = database;
    }

    @Override
    public Single<Novel> importTxtFile(Uri uri) {
        Log.d(TAG, "开始导入TXT文件: " + uri);
        return fileParserService.parseTxtFile(uri)
                .doOnSuccess(parsed -> Log.d(TAG, "TXT解析完成，章节数: " + 
                        (parsed.getChapters() != null ? parsed.getChapters().size() : 0)))
                .doOnError(e -> Log.e(TAG, "TXT解析失败", e))
                .flatMap(parsedNovel -> saveNovelToDatabase(parsedNovel, uri.toString()))
                .doOnSuccess(novel -> Log.d(TAG, "TXT导入完成: " + novel.getTitle()))
                .doOnError(e -> Log.e(TAG, "TXT保存失败", e))
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(error -> {
                    Log.e(TAG, "导入TXT文件失败", error);
                    if (error instanceof AppError) {
                        return Single.error(error);
                    }
                    return Single.error(new AppError.FileError(
                            "导入TXT文件失败: " + error.getMessage(),
                            uri.toString(),
                            error));
                });
    }

    @Override
    public Single<Novel> importEpubFile(Uri uri) {
        Log.d(TAG, "开始导入EPUB文件: " + uri);
        return fileParserService.parseEpubFile(uri)
                .doOnSuccess(parsed -> Log.d(TAG, "EPUB解析完成，章节数: " + 
                        (parsed.getChapters() != null ? parsed.getChapters().size() : 0)))
                .doOnError(e -> Log.e(TAG, "EPUB解析失败", e))
                .flatMap(parsedNovel -> saveNovelToDatabase(parsedNovel, uri.toString()))
                .doOnSuccess(novel -> Log.d(TAG, "EPUB导入完成: " + novel.getTitle()))
                .doOnError(e -> Log.e(TAG, "EPUB保存失败", e))
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(error -> {
                    Log.e(TAG, "导入EPUB文件失败", error);
                    if (error instanceof AppError) {
                        return Single.error(error);
                    }
                    return Single.error(new AppError.FileError(
                            "导入EPUB文件失败: " + error.getMessage(),
                            uri.toString(),
                            error));
                });
    }

    /**
     * 将解析后的小说保存到数据库
     * 使用事务和分批插入优化性能
     * @param parsedNovel 解析后的小说数据
     * @param sourceUrl 源文件路径
     * @return 保存后的小说对象（包含数据库生成的ID）
     */
    private Single<Novel> saveNovelToDatabase(ParsedNovel parsedNovel, String sourceUrl) {
        return Single.fromCallable(() -> {
            // 验证解析结果
            if (parsedNovel == null) {
                throw new AppError.FileError("解析结果为空", sourceUrl);
            }
            
            if (parsedNovel.getChapters() == null || parsedNovel.getChapters().isEmpty()) {
                throw new AppError.FileError("未能解析出任何章节", sourceUrl);
            }

            // 创建小说实体
            NovelEntity novelEntity = new NovelEntity(
                    parsedNovel.getTitle() != null ? parsedNovel.getTitle() : "未知标题",
                    parsedNovel.getAuthor() != null ? parsedNovel.getAuthor() : "未知作者"
            );
            novelEntity.setDescription(parsedNovel.getDescription());
            novelEntity.setCoverPath(parsedNovel.getCoverPath());
            novelEntity.setSource(NovelSource.LOCAL.getValue());
            novelEntity.setSourceUrl(sourceUrl);
            novelEntity.setTotalChapters(parsedNovel.getChapters().size());

            // 使用事务保存小说和章节
            final long[] novelIdHolder = new long[1];
            
            database.runInTransaction(() -> {
                // 保存小说到数据库
                novelIdHolder[0] = novelDao.insertNovel(novelEntity);
                
                if (novelIdHolder[0] <= 0) {
                    throw new RuntimeException("保存小说失败");
                }

                // 分批创建和插入章节实体
                List<ChapterEntity> batch = new ArrayList<>(BATCH_SIZE);
                for (ParsedNovel.ParsedChapter parsedChapter : parsedNovel.getChapters()) {
                    ChapterEntity chapterEntity = new ChapterEntity(
                            novelIdHolder[0],
                            parsedChapter.getTitle() != null ? parsedChapter.getTitle() : "未知章节",
                            parsedChapter.getContent() != null ? parsedChapter.getContent() : "",
                            parsedChapter.getIndex()
                    );
                    batch.add(chapterEntity);
                    
                    // 达到批量大小时插入
                    if (batch.size() >= BATCH_SIZE) {
                        chapterDao.insertChapters(batch);
                        batch.clear();
                    }
                }
                
                // 插入剩余的章节
                if (!batch.isEmpty()) {
                    chapterDao.insertChapters(batch);
                }
            });
            
            if (novelIdHolder[0] <= 0) {
                throw new AppError.DatabaseError("保存小说失败", null);
            }

            // 获取保存后的小说（包含ID）
            NovelEntity savedNovel = novelDao.getNovelById(novelIdHolder[0]);
            
            if (savedNovel == null) {
                throw new AppError.DatabaseError("无法获取保存的小说", null);
            }

            return NovelMapper.toDomain(savedNovel);
        });
    }
}
