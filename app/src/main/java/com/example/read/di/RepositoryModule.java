package com.example.read.di;

import com.example.read.data.repository.AIServiceRepositoryImpl;
import com.example.read.data.repository.BlockedWordRepositoryImpl;
import com.example.read.data.repository.BookmarkRepositoryImpl;
import com.example.read.data.repository.FileImportRepositoryImpl;
import com.example.read.data.repository.NovelRepositoryImpl;
import com.example.read.data.repository.ParserRuleRepositoryImpl;
import com.example.read.data.repository.SettingsRepositoryImpl;
import com.example.read.data.repository.StatisticsRepositoryImpl;
import com.example.read.data.repository.TTSRepositoryImpl;
import com.example.read.data.repository.ThemeRepositoryImpl;
import com.example.read.data.repository.WebParserRepositoryImpl;
import com.example.read.domain.repository.AIServiceRepository;
import com.example.read.domain.repository.BlockedWordRepository;
import com.example.read.domain.repository.BookmarkRepository;
import com.example.read.domain.repository.FileImportRepository;
import com.example.read.domain.repository.NovelRepository;
import com.example.read.domain.repository.ParserRuleRepository;
import com.example.read.domain.repository.SettingsRepository;
import com.example.read.domain.repository.StatisticsRepository;
import com.example.read.domain.repository.TTSRepository;
import com.example.read.domain.repository.ThemeRepository;
import com.example.read.domain.repository.WebParserRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt模块 - 提供Repository层的依赖注入绑定
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    @Singleton
    public abstract FileImportRepository bindFileImportRepository(
            FileImportRepositoryImpl impl);
    
    @Binds
    @Singleton
    public abstract WebParserRepository bindWebParserRepository(
            WebParserRepositoryImpl impl);
    
    @Binds
    @Singleton
    public abstract ParserRuleRepository bindParserRuleRepository(
            ParserRuleRepositoryImpl impl);
    
    @Binds
    @Singleton
    public abstract NovelRepository bindNovelRepository(
            NovelRepositoryImpl impl);
    
    @Binds
    @Singleton
    public abstract BlockedWordRepository bindBlockedWordRepository(
            BlockedWordRepositoryImpl impl);
    
    @Binds
    @Singleton
    public abstract BookmarkRepository bindBookmarkRepository(
            BookmarkRepositoryImpl impl);
    
    @Binds
    @Singleton
    public abstract AIServiceRepository bindAIServiceRepository(
            AIServiceRepositoryImpl impl);
    
    @Binds
    @Singleton
    public abstract StatisticsRepository bindStatisticsRepository(
            StatisticsRepositoryImpl impl);
    
    @Binds
    @Singleton
    public abstract TTSRepository bindTTSRepository(
            TTSRepositoryImpl impl);
    
    @Binds
    @Singleton
    public abstract ThemeRepository bindThemeRepository(
            ThemeRepositoryImpl impl);
    
    @Binds
    @Singleton
    public abstract SettingsRepository bindSettingsRepository(
            SettingsRepositoryImpl impl);
}
