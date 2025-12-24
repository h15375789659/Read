package com.example.read.utils;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 请求队列管理器
 * 用于控制并发请求数量，防止同时发起过多请求
 */
@Singleton
public class RequestQueueManager {
    
    // 默认最大并发请求数
    public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 5;
    
    private final Semaphore semaphore;
    private final AtomicInteger activeRequests;
    private final int maxConcurrentRequests;
    
    @Inject
    public RequestQueueManager() {
        this(DEFAULT_MAX_CONCURRENT_REQUESTS);
    }
    
    /**
     * 创建请求队列管理器
     * @param maxConcurrentRequests 最大并发请求数
     */
    public RequestQueueManager(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.semaphore = new Semaphore(maxConcurrentRequests, true);
        this.activeRequests = new AtomicInteger(0);
    }
    
    /**
     * 包装一个请求，使其受并发控制
     * @param request 原始请求
     * @param <T> 返回类型
     * @return 受控的请求
     */
    public <T> Single<T> enqueue(Single<T> request) {
        return Single.<T>create(emitter -> {
            try {
                // 获取许可，如果没有可用许可则阻塞
                semaphore.acquire();
                activeRequests.incrementAndGet();
                
                request.subscribeOn(Schedulers.io())
                    .subscribe(
                        result -> {
                            releasePermit();
                            if (!emitter.isDisposed()) {
                                emitter.onSuccess(result);
                            }
                        },
                        error -> {
                            releasePermit();
                            if (!emitter.isDisposed()) {
                                emitter.onError(error);
                            }
                        }
                    );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }
    
    /**
     * 释放许可
     */
    private void releasePermit() {
        activeRequests.decrementAndGet();
        semaphore.release();
    }
    
    /**
     * 获取当前活跃请求数
     * @return 活跃请求数
     */
    public int getActiveRequestCount() {
        return activeRequests.get();
    }
    
    /**
     * 获取最大并发请求数
     * @return 最大并发请求数
     */
    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }
    
    /**
     * 获取可用许可数
     * @return 可用许可数
     */
    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }
    
    /**
     * 检查是否可以立即执行请求（不阻塞）
     * @return true 如果可以立即执行，false 否则
     */
    public boolean canExecuteImmediately() {
        return semaphore.availablePermits() > 0;
    }
    
    /**
     * 尝试获取许可（非阻塞）
     * @return true 如果成功获取许可，false 否则
     */
    public boolean tryAcquire() {
        boolean acquired = semaphore.tryAcquire();
        if (acquired) {
            activeRequests.incrementAndGet();
        }
        return acquired;
    }
    
    /**
     * 释放许可（用于手动管理）
     */
    public void release() {
        releasePermit();
    }
}
