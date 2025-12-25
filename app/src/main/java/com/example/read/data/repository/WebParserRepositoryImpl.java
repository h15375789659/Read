package com.example.read.data.repository;

import android.util.Log;

import com.example.read.data.dao.ChapterDao;
import com.example.read.data.dao.NovelDao;
import com.example.read.data.entity.ChapterEntity;
import com.example.read.data.entity.NovelEntity;
import com.example.read.domain.error.AppError;
import com.example.read.domain.mapper.NovelMapper;
import com.example.read.domain.model.Chapter;
import com.example.read.domain.model.ChapterInfo;
import com.example.read.domain.model.Novel;
import com.example.read.domain.model.NovelMetadata;
import com.example.read.domain.model.NovelSource;
import com.example.read.domain.model.ParserRule;
import com.example.read.domain.repository.WebParserRepository;
import com.example.read.domain.service.WebParserService;
import com.example.read.utils.NetworkRequestManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 网站解析Repository实现
 * 集成WebParserService和网络请求管理
 */
@Singleton
public class WebParserRepositoryImpl implements WebParserRepository {
    
    private static final String TAG = "WebParserRepository";
    
    // 并发下载配置
    private static final int CONCURRENT_DOWNLOADS = 10;  // 并发下载数（从5增加到10）
    private static final int DOWNLOAD_DELAY_MS = 50;     // 下载间隔（从100ms减少到50ms）
    private static final int BATCH_SAVE_SIZE = 50;       // 批量保存大小（从20增加到50）
    
    // URL验证正则表达式
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?" +                           // 协议（可选）
        "((([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,})|" +  // 域名
        "localhost|" +                              // localhost
        "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})" + // IP地址
        "(:\\d+)?" +                                // 端口（可选）
        "(/[\\w\\-.~:/?#\\[\\]@!$&'()*+,;=%]*)?$"  // 路径（可选）
    );
    
    private final WebParserService webParserService;
    private final NetworkRequestManager networkRequestManager;
    private final NovelDao novelDao;
    private final ChapterDao chapterDao;
    
    // 下载取消标志
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private final AtomicBoolean isDownloading = new AtomicBoolean(false);
    
    // 并发下载线程池
    private ExecutorService downloadExecutor;
    
    @Inject
    public WebParserRepositoryImpl(
            WebParserService webParserService,
            NetworkRequestManager networkRequestManager,
            NovelDao novelDao,
            ChapterDao chapterDao) {
        this.webParserService = webParserService;
        this.networkRequestManager = networkRequestManager;
        this.novelDao = novelDao;
        this.chapterDao = chapterDao;
    }

    @Override
    public Single<NovelMetadata> parseNovelMetadata(String url, ParserRule rule) {
        return validateUrlAndExecute(url, () ->
            networkRequestManager.executeRequest(
                webParserService.fetchHtml(url)
                    .map(html -> webParserService.extractNovelInfo(html, rule))
            )
        );
    }
    
    @Override
    public Single<List<ChapterInfo>> parseChapterList(String url, ParserRule rule) {
        return validateUrlAndExecute(url, () ->
            networkRequestManager.executeRequest(
                webParserService.fetchHtml(url)
                    .map(html -> webParserService.extractChapterList(html, rule))
            )
        );
    }
    
    @Override
    public Single<String> parseChapterContent(String url, ParserRule rule) {
        return validateUrlAndExecute(url, () ->
            networkRequestManager.executeRequest(
                webParserService.fetchHtml(url)
                    .map(html -> webParserService.extractChapterContent(html, rule))
            )
        );
    }
    
    @Override
    public Single<Novel> downloadNovel(String url, ParserRule rule, ProgressCallback callback) {
        if (!isValidUrl(url)) {
            return Single.error(new AppError.ValidationError("无效的URL格式", "url"));
        }
        
        // 重置取消标志
        isCancelled.set(false);
        isDownloading.set(true);
        
        return networkRequestManager.executeRequest(
            webParserService.fetchHtml(url)
        )
        .flatMap(html -> {
            // 检查是否已取消
            if (isCancelled.get()) {
                return Single.error(new AppError.NetworkError("下载已取消"));
            }
            
            // 提取元数据
            NovelMetadata metadata = webParserService.extractNovelInfo(html, rule);
            
            // 提取章节列表
            List<ChapterInfo> chapterList = webParserService.extractChapterList(html, rule);
            
            if (chapterList.isEmpty()) {
                return Single.error(new AppError.ParseError("未能解析出章节列表", url));
            }
            
            // 创建并保存小说
            return saveNovelAndDownloadChapters(metadata, chapterList, url, rule, callback, 0);
        })
        .doFinally(() -> isDownloading.set(false))
        .subscribeOn(Schedulers.io());
    }
    
    @Override
    public Single<Novel> resumeDownload(long novelId, String url, ParserRule rule, ProgressCallback callback) {
        if (!isValidUrl(url)) {
            return Single.error(new AppError.ValidationError("无效的URL格式", "url"));
        }
        
        // 重置取消标志
        isCancelled.set(false);
        isDownloading.set(true);
        
        return Single.fromCallable(() -> {
            // 获取已存在的小说
            NovelEntity existingNovel = novelDao.getNovelById(novelId);
            if (existingNovel == null) {
                throw new AppError.DatabaseError("小说不存在", null);
            }
            
            // 获取已下载的章节数
            int downloadedCount = chapterDao.getChapterCount(novelId);
            return new ResumeInfo(existingNovel, downloadedCount);
        })
        .subscribeOn(Schedulers.io())
        .flatMap(resumeInfo -> 
            networkRequestManager.executeRequest(webParserService.fetchHtml(url))
                .flatMap(html -> {
                    if (isCancelled.get()) {
                        return Single.error(new AppError.NetworkError("下载已取消"));
                    }
                    
                    List<ChapterInfo> chapterList = webParserService.extractChapterList(html, rule);
                    
                    if (chapterList.isEmpty()) {
                        return Single.error(new AppError.ParseError("未能解析出章节列表", url));
                    }
                    
                    // 从断点处继续下载
                    return downloadChaptersFromIndex(
                        resumeInfo.novel.getId(),
                        chapterList,
                        rule,
                        callback,
                        resumeInfo.downloadedCount
                    );
                })
        )
        .doFinally(() -> isDownloading.set(false));
    }

    @Override
    public void cancelDownload() {
        isCancelled.set(true);
    }
    
    @Override
    public boolean isDownloading() {
        return isDownloading.get();
    }
    
    @Override
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return URL_PATTERN.matcher(url.trim()).matches();
    }
    
    @Override
    public Single<ExistingNovelInfo> checkExistingNovel(String sourceUrl) {
        return Single.fromCallable(() -> {
            NovelEntity existingNovel = novelDao.getNovelBySourceUrl(sourceUrl);
            if (existingNovel == null) {
                // 返回一个特殊值表示不存在，而不是null
                return new ExistingNovelInfo(-1, null, 0, 0);
            }
            
            int downloadedCount = chapterDao.getChapterCount(existingNovel.getId());
            return new ExistingNovelInfo(
                existingNovel.getId(),
                existingNovel.getTitle(),
                downloadedCount,
                existingNovel.getTotalChapters()
            );
        }).subscribeOn(Schedulers.io());
    }
    
    /**
     * 验证URL并执行请求
     */
    private <T> Single<T> validateUrlAndExecute(String url, RequestSupplier<T> supplier) {
        if (!isValidUrl(url)) {
            return Single.error(new AppError.ValidationError("无效的URL格式", "url"));
        }
        return supplier.get();
    }
    
    /**
     * 保存小说并下载章节
     */
    private Single<Novel> saveNovelAndDownloadChapters(
            NovelMetadata metadata,
            List<ChapterInfo> chapterList,
            String sourceUrl,
            ParserRule rule,
            ProgressCallback callback,
            int startIndex) {
        
        return Single.fromCallable(() -> {
            // 创建小说实体
            NovelEntity novelEntity = new NovelEntity(
                metadata.getTitle() != null ? metadata.getTitle() : "未知标题",
                metadata.getAuthor() != null ? metadata.getAuthor() : "未知作者"
            );
            novelEntity.setDescription(metadata.getDescription());
            novelEntity.setSource(NovelSource.WEB.getValue());
            novelEntity.setSourceUrl(sourceUrl);
            novelEntity.setTotalChapters(chapterList.size());
            
            // 保存小说
            long novelId = novelDao.insertNovel(novelEntity);
            if (novelId <= 0) {
                throw new AppError.DatabaseError("保存小说失败", null);
            }
            
            return novelId;
        })
        .subscribeOn(Schedulers.io())
        .flatMap(novelId -> downloadChaptersFromIndex(novelId, chapterList, rule, callback, startIndex));
    }
    
    /**
     * 从指定索引开始下载章节（并发下载优化版）
     */
    private Single<Novel> downloadChaptersFromIndex(
            long novelId,
            List<ChapterInfo> chapterList,
            ParserRule rule,
            ProgressCallback callback,
            int startIndex) {
        
        return Single.create(emitter -> {
            try {
                int total = chapterList.size();
                int remaining = total - startIndex;
                
                if (remaining <= 0) {
                    // 没有需要下载的章节
                    NovelEntity savedNovel = novelDao.getNovelById(novelId);
                    if (savedNovel != null) {
                        emitter.onSuccess(NovelMapper.toDomain(savedNovel));
                    } else {
                        emitter.onError(new AppError.DatabaseError("无法获取保存的小说", null));
                    }
                    return;
                }
                
                // 创建线程池
                downloadExecutor = Executors.newFixedThreadPool(CONCURRENT_DOWNLOADS);
                
                // 用于存储下载结果，保持章节顺序
                ConcurrentHashMap<Integer, ChapterEntity> downloadedMap = new ConcurrentHashMap<>();
                
                // 进度计数器
                AtomicInteger completedCount = new AtomicInteger(startIndex);
                
                // 提交所有下载任务
                CountDownLatch latch = new CountDownLatch(remaining);
                
                for (int i = startIndex; i < total; i++) {
                    final int index = i;
                    final ChapterInfo chapterInfo = chapterList.get(i);
                    
                    downloadExecutor.submit(() -> {
                        try {
                            // 检查是否已取消
                            if (isCancelled.get()) {
                                latch.countDown();
                                return;
                            }
                            
                            // 添加延迟，避免请求过快被封
                            Thread.sleep(DOWNLOAD_DELAY_MS * (index % CONCURRENT_DOWNLOADS));
                            
                            // 下载章节内容
                            String content = "";
                            try {
                                Log.d(TAG, "开始下载章节: " + chapterInfo.getTitle() + ", URL: " + chapterInfo.getUrl());
                                content = networkRequestManager.executeRequest(
                                    webParserService.fetchHtml(chapterInfo.getUrl())
                                        .map(html -> webParserService.extractChapterContent(html, rule))
                                ).blockingGet();
                                Log.d(TAG, "章节下载完成: " + chapterInfo.getTitle() + ", 内容长度: " + (content != null ? content.length() : 0));
                            } catch (Exception e) {
                                Log.w(TAG, "章节下载失败: " + chapterInfo.getTitle() + ", " + e.getMessage());
                                content = "[下载失败: " + e.getMessage() + "]";
                            }
                            
                            // 创建章节实体
                            ChapterEntity chapterEntity = new ChapterEntity(
                                novelId,
                                chapterInfo.getTitle(),
                                content != null ? content : "",
                                index
                            );
                            chapterEntity.setSourceUrl(chapterInfo.getUrl());
                            
                            // 存储结果
                            downloadedMap.put(index, chapterEntity);
                            
                            // 更新进度
                            int completed = completedCount.incrementAndGet();
                            if (callback != null) {
                                callback.onProgress(completed, total, chapterInfo.getTitle());
                            }
                            
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                
                // 等待所有任务完成或取消
                try {
                    // 每秒检查一次是否取消
                    while (!latch.await(1, TimeUnit.SECONDS)) {
                        if (isCancelled.get()) {
                            downloadExecutor.shutdownNow();
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 关闭线程池
                downloadExecutor.shutdown();
                
                // 按顺序保存章节
                List<ChapterEntity> sortedChapters = new ArrayList<>();
                int emptyContentCount = 0;
                for (int i = startIndex; i < total; i++) {
                    ChapterEntity chapter = downloadedMap.get(i);
                    if (chapter != null) {
                        sortedChapters.add(chapter);
                        if (chapter.getContent() == null || chapter.getContent().isEmpty() 
                                || chapter.getContent().startsWith("[下载失败")) {
                            emptyContentCount++;
                        }
                    }
                    
                    // 批量保存
                    if (sortedChapters.size() >= BATCH_SAVE_SIZE) {
                        chapterDao.insertChapters(sortedChapters);
                        Log.d(TAG, "批量保存章节: " + sortedChapters.size() + " 章");
                        sortedChapters.clear();
                    }
                }
                
                // 保存剩余章节
                if (!sortedChapters.isEmpty()) {
                    chapterDao.insertChapters(sortedChapters);
                    Log.d(TAG, "保存剩余章节: " + sortedChapters.size() + " 章");
                }
                
                Log.d(TAG, "下载完成统计: 总章节=" + total + ", 已下载=" + downloadedMap.size() 
                        + ", 空内容/失败=" + emptyContentCount);
                
                // 检查是否取消
                if (isCancelled.get()) {
                    int savedCount = startIndex + downloadedMap.size();
                    novelDao.updateTotalChapters(novelId, savedCount);
                    emitter.onError(new AppError.NetworkError("下载已取消，已保存 " + savedCount + " 章"));
                    return;
                }
                
                // 更新小说总章节数
                novelDao.updateTotalChapters(novelId, total);
                
                // 获取保存后的小说
                NovelEntity savedNovel = novelDao.getNovelById(novelId);
                if (savedNovel == null) {
                    emitter.onError(new AppError.DatabaseError("无法获取保存的小说", null));
                    return;
                }
                
                emitter.onSuccess(NovelMapper.toDomain(savedNovel));
                
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
    
    /**
     * 请求提供者接口
     */
    @FunctionalInterface
    private interface RequestSupplier<T> {
        Single<T> get();
    }
    
    /**
     * 断点续传信息
     */
    private static class ResumeInfo {
        final NovelEntity novel;
        final int downloadedCount;
        
        ResumeInfo(NovelEntity novel, int downloadedCount) {
            this.novel = novel;
            this.downloadedCount = downloadedCount;
        }
    }
}
