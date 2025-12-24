package com.example.read.domain.repository;

import android.net.Uri;

import com.example.read.domain.model.Novel;

import io.reactivex.rxjava3.core.Single;

/**
 * 文件导入Repository接口
 * 负责处理本地文件的导入操作，包括TXT和EPUB格式
 */
public interface FileImportRepository {
    
    /**
     * 导入TXT格式文件
     * @param uri 文件URI
     * @return 导入成功后的小说对象（包含ID）
     */
    Single<Novel> importTxtFile(Uri uri);
    
    /**
     * 导入EPUB格式文件
     * @param uri 文件URI
     * @return 导入成功后的小说对象（包含ID）
     */
    Single<Novel> importEpubFile(Uri uri);
}
