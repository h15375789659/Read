package com.example.read.utils;

import com.example.read.domain.error.AppError;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.HttpException;

/**
 * 网络错误处理工具类
 * 将各种网络异常转换为统一的 AppError.NetworkError
 */
public class NetworkErrorHandler {
    
    /**
     * 将异常转换为 NetworkError
     * @param throwable 原始异常
     * @return NetworkError
     */
    public static AppError.NetworkError handleError(Throwable throwable) {
        if (throwable instanceof AppError.NetworkError) {
            return (AppError.NetworkError) throwable;
        }
        
        if (throwable instanceof SocketTimeoutException) {
            return new AppError.NetworkError("网络请求超时，请稍后重试", true, false);
        }
        
        if (throwable instanceof UnknownHostException) {
            return new AppError.NetworkError("无法连接到服务器，请检查网络连接", false, true);
        }
        
        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            int statusCode = httpException.code();
            String message = getHttpErrorMessage(statusCode);
            return new AppError.NetworkError(message, statusCode);
        }
        
        if (throwable instanceof IOException) {
            return new AppError.NetworkError("网络连接异常: " + throwable.getMessage(), throwable);
        }
        
        return new AppError.NetworkError("网络请求失败: " + throwable.getMessage(), throwable);
    }
    
    /**
     * 根据 HTTP 状态码获取错误信息
     * @param statusCode HTTP 状态码
     * @return 错误信息
     */
    public static String getHttpErrorMessage(int statusCode) {
        switch (statusCode) {
            case 400:
                return "请求参数错误 (400)";
            case 401:
                return "未授权访问 (401)";
            case 403:
                return "访问被禁止 (403)";
            case 404:
                return "请求的资源不存在 (404)";
            case 408:
                return "请求超时 (408)";
            case 429:
                return "请求过于频繁，请稍后重试 (429)";
            case 500:
                return "服务器内部错误 (500)";
            case 502:
                return "网关错误 (502)";
            case 503:
                return "服务暂时不可用 (503)";
            case 504:
                return "网关超时 (504)";
            default:
                if (statusCode >= 400 && statusCode < 500) {
                    return "客户端错误 (" + statusCode + ")";
                } else if (statusCode >= 500) {
                    return "服务器错误 (" + statusCode + ")";
                }
                return "网络错误 (" + statusCode + ")";
        }
    }
    
    /**
     * 检查是否是超时错误
     * @param throwable 异常
     * @return true 如果是超时错误
     */
    public static boolean isTimeoutError(Throwable throwable) {
        if (throwable instanceof SocketTimeoutException) {
            return true;
        }
        if (throwable instanceof AppError.NetworkError) {
            return ((AppError.NetworkError) throwable).isTimeout();
        }
        return false;
    }
    
    /**
     * 检查是否是网络连接错误
     * @param throwable 异常
     * @return true 如果是网络连接错误
     */
    public static boolean isConnectionError(Throwable throwable) {
        if (throwable instanceof UnknownHostException) {
            return true;
        }
        if (throwable instanceof AppError.NetworkError) {
            return ((AppError.NetworkError) throwable).isNoConnection();
        }
        return false;
    }
    
    /**
     * 检查是否可以重试
     * @param throwable 异常
     * @return true 如果可以重试
     */
    public static boolean isRetryable(Throwable throwable) {
        if (throwable instanceof SocketTimeoutException) {
            return true;
        }
        if (throwable instanceof HttpException) {
            int code = ((HttpException) throwable).code();
            // 5xx 错误和 429 可以重试
            return code >= 500 || code == 429;
        }
        if (throwable instanceof AppError.NetworkError) {
            AppError.NetworkError error = (AppError.NetworkError) throwable;
            return error.isTimeout() || error.isServerError() || error.getStatusCode() == 429;
        }
        return false;
    }
}
