package com.example.read.utils;

import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 拼音工具类 - 用于中文转拼音
 */
public class PinyinHelper {
    
    private static final HanyuPinyinOutputFormat format;
    
    static {
        format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }
    
    /**
     * 获取字符串的完整拼音
     * 例如：斗破苍穹 -> doupocanqiong
     */
    public static String getFullPinyin(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) {
                // 中文字符
                try {
                    String[] pinyinArray = net.sourceforge.pinyin4j.PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        result.append(pinyinArray[0]);
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    result.append(c);
                }
            } else {
                // 非中文字符保持原样
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
    
    /**
     * 获取字符串的拼音首字母
     * 例如：斗破苍穹 -> dpcq
     */
    public static String getPinyinInitials(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) {
                // 中文字符
                try {
                    String[] pinyinArray = net.sourceforge.pinyin4j.PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        result.append(pinyinArray[0].charAt(0));
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    // 忽略
                }
            } else if (Character.isLetter(c)) {
                // 英文字母
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
    
    /**
     * 检查搜索关键词是否匹配目标字符串
     * 支持：中文匹配、拼音全拼匹配、拼音首字母匹配
     */
    public static boolean matches(String target, String keyword) {
        if (target == null || keyword == null) {
            return false;
        }
        
        String lowerTarget = target.toLowerCase();
        String lowerKeyword = keyword.toLowerCase().trim();
        
        if (lowerKeyword.isEmpty()) {
            return true;
        }
        
        // 1. 直接匹配（中文或英文）
        if (lowerTarget.contains(lowerKeyword)) {
            return true;
        }
        
        // 2. 拼音全拼匹配
        String targetPinyin = getFullPinyin(target);
        if (targetPinyin.contains(lowerKeyword)) {
            return true;
        }
        
        // 3. 拼音首字母匹配
        String targetInitials = getPinyinInitials(target);
        if (targetInitials.contains(lowerKeyword)) {
            return true;
        }
        
        return false;
    }
}
