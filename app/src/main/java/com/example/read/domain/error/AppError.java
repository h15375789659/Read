package com.example.read.domain.error;

/**
 * 应用错误基类
 */
public abstract class AppError extends Exception {
    
    public AppError(String message) {
        super(message);
    }
    
    public AppError(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 网络错误
     */
    public static class NetworkError extends AppError {
        private final int statusCode;
        private final boolean isTimeout;
        private final boolean isNoConnection;
        
        public NetworkError(String message) {
            super(message);
            this.statusCode = -1;
            this.isTimeout = false;
            this.isNoConnection = false;
        }
        
        public NetworkError(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = -1;
            this.isTimeout = false;
            this.isNoConnection = false;
        }
        
        public NetworkError(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
            this.isTimeout = false;
            this.isNoConnection = false;
        }
        
        public NetworkError(String message, boolean isTimeout, boolean isNoConnection) {
            super(message);
            this.statusCode = -1;
            this.isTimeout = isTimeout;
            this.isNoConnection = isNoConnection;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public boolean isTimeout() {
            return isTimeout;
        }
        
        public boolean isNoConnection() {
            return isNoConnection;
        }
        
        public boolean isClientError() {
            return statusCode >= 400 && statusCode < 500;
        }
        
        public boolean isServerError() {
            return statusCode >= 500;
        }
    }

    /**
     * 解析错误
     */
    public static class ParseError extends AppError {
        private final String url;
        
        public ParseError(String message, String url) {
            super(message);
            this.url = url;
        }
        
        public String getUrl() {
            return url;
        }
    }

    /**
     * 数据库错误
     */
    public static class DatabaseError extends AppError {
        public DatabaseError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 文件错误
     */
    public static class FileError extends AppError {
        private final String path;
        
        public FileError(String message, String path) {
            super(message);
            this.path = path;
        }
        
        public FileError(String message, String path, Throwable cause) {
            super(message, cause);
            this.path = path;
        }
        
        public String getPath() {
            return path;
        }
    }

    /**
     * 验证错误
     */
    public static class ValidationError extends AppError {
        private final String field;
        
        public ValidationError(String message, String field) {
            super(message);
            this.field = field;
        }
        
        public String getField() {
            return field;
        }
    }

    /**
     * AI服务错误
     */
    public static class AIServiceError extends AppError {
        private final boolean isTimeout;
        
        public AIServiceError(String message, boolean isTimeout) {
            super(message);
            this.isTimeout = isTimeout;
        }
        
        public boolean isTimeout() {
            return isTimeout;
        }
    }

    /**
     * 未知错误
     */
    public static class UnknownError extends AppError {
        public UnknownError(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
