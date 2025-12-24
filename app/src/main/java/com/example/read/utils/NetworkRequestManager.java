package com.example.read.utils;

import com.example.read.domain.error.AppError;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;

/**
 * 网络请求管理器
 * 整合网络连接检查、请求队列控制等功能
 */
@Singleton
public class NetworkRequestManager {
    
    private final NetworkConnectivityChecker connectivityChecker;
    private final RequestQueueManager queueManager;
    
    @Inject
    public NetworkRequestManager(
            NetworkConnectivityChecker connectivityChecker,
            RequestQueueManager queueManager) {
        this.connectivityChecker = connectivityChecker;
        this.queueManager = queueManager;
    }
    
    /**
     * 执行网络请求
     * 在执行前检查网络状态，并通过队列控制并发
     * 
     * @param request 原始请求
     * @param <T> 返回类型
     * @return 受控的请求
     */
    public <T> Single<T> executeRequest(Single<T> request) {
        return Single.defer(() -> {
            // 检查网络连接状态
            if (!connectivityChecker.isNetworkAvailable()) {
                return Single.error(new AppError.NetworkError("网络不可用，请检查网络连接"));
            }
            
            // 通过队列管理器执行请求
            return queueManager.enqueue(request);
        });
    }
    
    /**
     * 执行网络请求（不检查网络状态）
     * 仅通过队列控制并发
     * 
     * @param request 原始请求
     * @param <T> 返回类型
     * @return 受控的请求
     */
    public <T> Single<T> executeRequestWithoutCheck(Single<T> request) {
        return queueManager.enqueue(request);
    }
    
    /**
     * 检查网络是否可用
     * @return true 如果网络可用
     */
    public boolean isNetworkAvailable() {
        return connectivityChecker.isNetworkAvailable();
    }
    
    /**
     * 获取当前活跃请求数
     * @return 活跃请求数
     */
    public int getActiveRequestCount() {
        return queueManager.getActiveRequestCount();
    }
    
    /**
     * 获取最大并发请求数
     * @return 最大并发请求数
     */
    public int getMaxConcurrentRequests() {
        return queueManager.getMaxConcurrentRequests();
    }
    
    /**
     * 检查是否可以立即执行请求
     * @return true 如果可以立即执行
     */
    public boolean canExecuteImmediately() {
        return queueManager.canExecuteImmediately();
    }
}
