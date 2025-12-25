package com.example.read;

import android.app.Application;

import com.example.read.data.DefaultDataInitializer;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Application class for the Novel Reader app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
public class NovelReaderApplication extends Application {
    
    @Inject
    DefaultDataInitializer defaultDataInitializer;
    
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化默认数据（如解析规则）
        defaultDataInitializer.initializeDefaultData();
    }
}
