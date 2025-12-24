package com.example.read;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

/**
 * Application class for the Novel Reader app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
public class NovelReaderApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // Application initialization code will go here
    }
}
