package com.example.read.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.read.presentation.bookshelf.BookshelfActivity;
import com.example.read.presentation.parser.WebParserActivity;
import com.example.read.presentation.reader.ReaderActivity;

/**
 * 导航辅助类 - 统一管理Activity间的导航和数据传递
 * 
 * 验证需求：所有UI相关需求
 */
public final class NavigationHelper {

    // Intent Extra Keys
    public static final String EXTRA_NOVEL_ID = "novel_id";
    public static final String EXTRA_CHAPTER_ID = "chapter_id";
    public static final String EXTRA_SEARCH_KEYWORD = "search_keyword";
    public static final String EXTRA_BOOKMARK_ID = "bookmark_id";
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_FROM_NOTIFICATION = "from_notification";
    public static final String EXTRA_PARSER_RULE_ID = "parser_rule_id";
    public static final String EXTRA_URL = "url";

    // Request Codes
    public static final int REQUEST_CODE_READER = 1001;
    public static final int REQUEST_CODE_SETTINGS = 1002;
    public static final int REQUEST_CODE_PARSER = 1003;
    public static final int REQUEST_CODE_STATISTICS = 1004;
    public static final int REQUEST_CODE_BOOKMARK = 1005;

    // Result Codes
    public static final int RESULT_NOVEL_DELETED = 2001;
    public static final int RESULT_NOVEL_UPDATED = 2002;
    public static final int RESULT_SETTINGS_CHANGED = 2003;

    private NavigationHelper() {
        // 私有构造函数，防止实例化
    }

    // ==================== 书架导航 ====================

    /**
     * 导航到书架界面
     * 
     * @param context 上下文
     */
    public static void navigateToBookshelf(@NonNull Context context) {
        Intent intent = new Intent(context, BookshelfActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    /**
     * 导航到书架界面并清除返回栈
     * 
     * @param context 上下文
     */
    public static void navigateToBookshelfClearStack(@NonNull Context context) {
        Intent intent = new Intent(context, BookshelfActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    // ==================== 阅读器导航 ====================

    /**
     * 导航到阅读器界面
     * 
     * @param context 上下文
     * @param novelId 小说ID
     */
    public static void navigateToReader(@NonNull Context context, long novelId) {
        navigateToReader(context, novelId, -1, -1);
    }

    /**
     * 导航到阅读器界面，指定章节
     * 
     * @param context   上下文
     * @param novelId   小说ID
     * @param chapterId 章节ID（-1表示使用上次阅读位置）
     */
    public static void navigateToReader(@NonNull Context context, long novelId, long chapterId) {
        navigateToReader(context, novelId, chapterId, -1);
    }

    /**
     * 导航到阅读器界面，指定章节和位置
     * 
     * @param context   上下文
     * @param novelId   小说ID
     * @param chapterId 章节ID（-1表示使用上次阅读位置）
     * @param position  阅读位置（-1表示从头开始）
     */
    public static void navigateToReader(@NonNull Context context, long novelId, long chapterId, int position) {
        Intent intent = createReaderIntent(context, novelId, chapterId, position);
        context.startActivity(intent);
    }

    /**
     * 导航到阅读器界面（带返回结果）
     * 
     * @param activity    Activity
     * @param novelId     小说ID
     * @param requestCode 请求码
     */
    public static void navigateToReaderForResult(@NonNull Activity activity, long novelId, int requestCode) {
        Intent intent = createReaderIntent(activity, novelId, -1, -1);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 创建阅读器Intent
     */
    private static Intent createReaderIntent(@NonNull Context context, long novelId, long chapterId, int position) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(EXTRA_NOVEL_ID, novelId);
        if (chapterId > 0) {
            intent.putExtra(EXTRA_CHAPTER_ID, chapterId);
        }
        if (position >= 0) {
            intent.putExtra(EXTRA_POSITION, position);
        }
        return intent;
    }

    /**
     * 导航到阅读器界面，跳转到搜索结果位置
     * 
     * @param context       上下文
     * @param novelId       小说ID
     * @param chapterId     章节ID
     * @param searchKeyword 搜索关键词
     */
    public static void navigateToReaderWithSearch(@NonNull Context context, long novelId, 
            long chapterId, @NonNull String searchKeyword) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(EXTRA_NOVEL_ID, novelId);
        intent.putExtra(EXTRA_CHAPTER_ID, chapterId);
        intent.putExtra(EXTRA_SEARCH_KEYWORD, searchKeyword);
        context.startActivity(intent);
    }

    /**
     * 导航到阅读器界面，跳转到书签位置
     * 
     * @param context    上下文
     * @param novelId    小说ID
     * @param chapterId  章节ID
     * @param bookmarkId 书签ID
     * @param position   书签位置
     */
    public static void navigateToReaderWithBookmark(@NonNull Context context, long novelId,
            long chapterId, long bookmarkId, int position) {
        Intent intent = new Intent(context, ReaderActivity.class);
        intent.putExtra(EXTRA_NOVEL_ID, novelId);
        intent.putExtra(EXTRA_CHAPTER_ID, chapterId);
        intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId);
        intent.putExtra(EXTRA_POSITION, position);
        context.startActivity(intent);
    }

    // ==================== 网站解析导航 ====================

    /**
     * 导航到网站解析界面
     * 
     * @param context 上下文
     */
    public static void navigateToWebParser(@NonNull Context context) {
        Intent intent = new Intent(context, WebParserActivity.class);
        context.startActivity(intent);
    }

    /**
     * 导航到网站解析界面（带URL）
     * 
     * @param context 上下文
     * @param url     预填充的URL
     */
    public static void navigateToWebParser(@NonNull Context context, @NonNull String url) {
        Intent intent = new Intent(context, WebParserActivity.class);
        intent.putExtra(EXTRA_URL, url);
        context.startActivity(intent);
    }

    /**
     * 导航到网站解析界面（带返回结果）
     * 
     * @param activity    Activity
     * @param requestCode 请求码
     */
    public static void navigateToWebParserForResult(@NonNull Activity activity, int requestCode) {
        Intent intent = new Intent(activity, WebParserActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    // ==================== Intent数据提取 ====================

    /**
     * 从Intent中提取小说ID
     * 
     * @param intent Intent
     * @return 小说ID，如果不存在返回-1
     */
    public static long getNovelId(@Nullable Intent intent) {
        if (intent == null) return -1;
        return intent.getLongExtra(EXTRA_NOVEL_ID, -1);
    }

    /**
     * 从Intent中提取章节ID
     * 
     * @param intent Intent
     * @return 章节ID，如果不存在返回-1
     */
    public static long getChapterId(@Nullable Intent intent) {
        if (intent == null) return -1;
        return intent.getLongExtra(EXTRA_CHAPTER_ID, -1);
    }

    /**
     * 从Intent中提取阅读位置
     * 
     * @param intent Intent
     * @return 阅读位置，如果不存在返回-1
     */
    public static int getPosition(@Nullable Intent intent) {
        if (intent == null) return -1;
        return intent.getIntExtra(EXTRA_POSITION, -1);
    }

    /**
     * 从Intent中提取搜索关键词
     * 
     * @param intent Intent
     * @return 搜索关键词，如果不存在返回null
     */
    @Nullable
    public static String getSearchKeyword(@Nullable Intent intent) {
        if (intent == null) return null;
        return intent.getStringExtra(EXTRA_SEARCH_KEYWORD);
    }

    /**
     * 从Intent中提取书签ID
     * 
     * @param intent Intent
     * @return 书签ID，如果不存在返回-1
     */
    public static long getBookmarkId(@Nullable Intent intent) {
        if (intent == null) return -1;
        return intent.getLongExtra(EXTRA_BOOKMARK_ID, -1);
    }

    /**
     * 从Intent中提取URL
     * 
     * @param intent Intent
     * @return URL，如果不存在返回null
     */
    @Nullable
    public static String getUrl(@Nullable Intent intent) {
        if (intent == null) return null;
        return intent.getStringExtra(EXTRA_URL);
    }

    /**
     * 检查是否从通知启动
     * 
     * @param intent Intent
     * @return 是否从通知启动
     */
    public static boolean isFromNotification(@Nullable Intent intent) {
        if (intent == null) return false;
        return intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false);
    }

    // ==================== 返回栈管理 ====================

    /**
     * 结束当前Activity并返回结果
     * 
     * @param activity   Activity
     * @param resultCode 结果码
     */
    public static void finishWithResult(@NonNull Activity activity, int resultCode) {
        activity.setResult(resultCode);
        activity.finish();
    }

    /**
     * 结束当前Activity并返回结果（带数据）
     * 
     * @param activity   Activity
     * @param resultCode 结果码
     * @param data       返回数据
     */
    public static void finishWithResult(@NonNull Activity activity, int resultCode, @Nullable Intent data) {
        activity.setResult(resultCode, data);
        activity.finish();
    }

    /**
     * 创建返回数据Intent
     * 
     * @param novelId 小说ID
     * @return Intent
     */
    public static Intent createResultIntent(long novelId) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_NOVEL_ID, novelId);
        return intent;
    }

    /**
     * 创建返回数据Intent（带额外数据）
     * 
     * @param novelId 小说ID
     * @param extras  额外数据
     * @return Intent
     */
    public static Intent createResultIntent(long novelId, @Nullable Bundle extras) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_NOVEL_ID, novelId);
        if (extras != null) {
            intent.putExtras(extras);
        }
        return intent;
    }
}
