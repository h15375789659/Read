package com.example.read.di;

import com.example.read.config.AIConfig;
import com.example.read.data.api.DashScopeApi;
import com.example.read.utils.NetworkConnectivityChecker;
import com.example.read.utils.NetworkRequestManager;
import com.example.read.utils.RequestQueueManager;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Hilt module for network-related dependencies.
 * 提供 Retrofit, OkHttp 和网络管理相关的依赖
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {
    
    // 网络请求超时时间（秒）
    public static final int CONNECT_TIMEOUT = 15;
    public static final int READ_TIMEOUT = 15;
    public static final int WRITE_TIMEOUT = 15;
    
    // AI服务超时时间（秒）- 30秒
    public static final int AI_TIMEOUT = 30;
    
    // 最大并发请求数
    public static final int MAX_CONCURRENT_REQUESTS = 5;
    
    // 默认基础URL（用于AI服务等）
    private static final String BASE_URL = "https://api.example.com/";
    
    // 通义千问API基础URL
    private static final String DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/api/v1/";
    
    /**
     * 提供 HttpLoggingInterceptor
     * 用于记录网络请求日志
     */
    @Provides
    @Singleton
    public HttpLoggingInterceptor provideLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return interceptor;
    }
    
    /**
     * 提供 OkHttpClient
     * 配置超时时间和拦截器
     */
    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(HttpLoggingInterceptor loggingInterceptor) {
        return new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .build();
    }
    
    /**
     * 提供AI服务专用的OkHttpClient
     * 配置30秒超时
     */
    @Provides
    @Singleton
    @Named("aiClient")
    public OkHttpClient provideAIHttpClient(HttpLoggingInterceptor loggingInterceptor) {
        return new OkHttpClient.Builder()
                .connectTimeout(AI_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(AI_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(AI_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(false) // AI请求不自动重试
                .build();
    }
    
    /**
     * 提供 Retrofit 实例
     */
    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }
    
    /**
     * 提供通义千问API的Retrofit实例
     */
    @Provides
    @Singleton
    @Named("dashscope")
    public Retrofit provideDashScopeRetrofit(@Named("aiClient") OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(DASHSCOPE_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }
    
    /**
     * 提供通义千问API接口
     */
    @Provides
    @Singleton
    public DashScopeApi provideDashScopeApi(@Named("dashscope") Retrofit retrofit) {
        return retrofit.create(DashScopeApi.class);
    }
    
    /**
     * 提供请求队列管理器
     */
    @Provides
    @Singleton
    public RequestQueueManager provideRequestQueueManager() {
        return new RequestQueueManager(MAX_CONCURRENT_REQUESTS);
    }
    
    /**
     * 提供网络请求管理器
     */
    @Provides
    @Singleton
    public NetworkRequestManager provideNetworkRequestManager(
            NetworkConnectivityChecker connectivityChecker,
            RequestQueueManager queueManager) {
        return new NetworkRequestManager(connectivityChecker, queueManager);
    }
}
