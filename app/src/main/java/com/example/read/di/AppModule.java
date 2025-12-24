package com.example.read.di;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.read.data.api.DashScopeApi;
import com.example.read.data.service.AIServiceImpl;
import com.example.read.data.service.FileParserServiceImpl;
import com.example.read.data.service.WebParserServiceImpl;
import com.example.read.domain.service.AIService;
import com.example.read.domain.service.FileParserService;
import com.example.read.domain.service.WebParserService;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

/**
 * Hilt module for application-level dependencies.
 * Provides singleton instances of common dependencies.
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
    
    private static final String PREFS_NAME = "novel_reader_prefs";
    
    /**
     * Provides SharedPreferences instance for storing app settings.
     */
    @Provides
    @Singleton
    public SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Provides FileParserService instance for parsing novel files.
     */
    @Provides
    @Singleton
    public FileParserService provideFileParserService(@ApplicationContext Context context) {
        return new FileParserServiceImpl(context);
    }
    
    /**
     * Provides WebParserService instance for parsing web novel content.
     */
    @Provides
    @Singleton
    public WebParserService provideWebParserService() {
        return new WebParserServiceImpl();
    }
    
    /**
     * Provides AIService instance for AI-powered features.
     * 用于章节摘要生成等AI功能
     */
    @Provides
    @Singleton
    public AIService provideAIService(DashScopeApi dashScopeApi) {
        return new AIServiceImpl(dashScopeApi);
    }
}
