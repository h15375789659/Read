package com.example.read.di;

import com.example.read.data.service.TTSServiceImpl;
import com.example.read.domain.service.TTSService;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt模块 - 提供Service层的依赖注入绑定
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class ServiceModule {

    @Binds
    @Singleton
    public abstract TTSService bindTTSService(TTSServiceImpl impl);
}
