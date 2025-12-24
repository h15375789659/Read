package com.example.read.domain.service;

import android.net.Uri;

import com.example.read.domain.model.ParsedNovel;

import io.reactivex.rxjava3.core.Single;

/**
 * 文件解析服务接口
 * 负责解析本地小说文件（TXT、EPUB格式）
 */
public interface FileParserService {
    
    /**
     * 解析TXT格式文件
     * @param uri 文件URI
     * @return 解析后的小说数据
     */
    Single<ParsedNovel> parseTxtFile(Uri uri);
    
    /**
     * 解析EPUB格式文件
     * @param uri 文件URI
     * @return 解析后的小说数据
     */
    Single<ParsedNovel> parseEpubFile(Uri uri);
}
