package com.example.read.presentation.reader;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.example.read.domain.model.PageAnimation;

/**
 * 翻页动画转换器集合
 * 
 * 提供多种翻页动画效果：
 * - 覆盖（Cover）：新页面从右侧滑入覆盖旧页面
 * - 滑动（Slide）：两个页面同时滑动
 * - 仿真（Simulation）：模拟真实翻书效果
 * - 滚动（Scroll）：默认滚动效果
 */
public class PageTransformers {

    /**
     * 根据动画类型获取对应的PageTransformer
     */
    public static ViewPager2.PageTransformer getTransformer(PageAnimation animation) {
        if (animation == null) {
            return new SlideTransformer();
        }
        
        switch (animation) {
            case COVER:
                return new CoverTransformer();
            case SLIDE:
                return new SlideTransformer();
            case SIMULATION:
                return new SimulationTransformer();
            case SCROLL:
                return new ScrollTransformer();
            default:
                return new SlideTransformer();
        }
    }

    /**
     * 覆盖动画 - 新页面从右侧滑入覆盖旧页面
     */
    public static class CoverTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(@NonNull View page, float position) {
            int pageWidth = page.getWidth();
            
            // 重置所有变换
            page.setRotationY(0f);
            page.setScaleX(1f);
            page.setScaleY(1f);
            
            if (position < -1) {
                // 页面在屏幕左侧外
                page.setAlpha(0f);
                page.setTranslationX(0f);
                page.setTranslationZ(0f);
            } else if (position <= 0) {
                // 当前页面（向左滑动时保持不动）
                page.setAlpha(1f);
                page.setTranslationX(0f);
                page.setTranslationZ(0f);
            } else if (position <= 1) {
                // 下一页（从右侧滑入覆盖）
                page.setAlpha(1f);
                // 修复：不要修改 translationX，让 ViewPager2 处理滑动
                // 只设置 Z 轴让新页面在上层
                page.setTranslationX(0f);
                page.setTranslationZ(1f);
            } else {
                // 页面在屏幕右侧外
                page.setAlpha(0f);
                page.setTranslationX(0f);
                page.setTranslationZ(0f);
            }
        }
    }

    /**
     * 滑动动画 - 两个页面同时滑动（带轻微缩放效果）
     */
    public static class SlideTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.95f;
        private static final float MIN_ALPHA = 0.7f;

        @Override
        public void transformPage(@NonNull View page, float position) {
            // 重置旋转和Z轴
            page.setRotationY(0f);
            page.setTranslationZ(0f);
            page.setTranslationX(0f);
            
            if (position < -1 || position > 1) {
                // 页面在屏幕外
                page.setAlpha(0f);
                page.setScaleX(1f);
                page.setScaleY(1f);
            } else {
                // 计算缩放因子（越远离中心越小）
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position) * 0.05f);
                
                // 缩放
                page.setScaleX(scaleFactor);
                page.setScaleY(scaleFactor);
                
                // 透明度（越远离中心越透明）
                float alpha = Math.max(MIN_ALPHA, 1 - Math.abs(position) * 0.3f);
                page.setAlpha(alpha);
            }
        }
    }

    /**
     * 仿真动画 - 模拟真实翻书效果（3D翻转）
     */
    public static class SimulationTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(@NonNull View page, float position) {
            // 重置缩放和位移
            page.setScaleX(1f);
            page.setScaleY(1f);
            page.setTranslationX(0f);
            page.setTranslationZ(0f);
            
            // 设置相机距离以获得更好的3D效果
            page.setCameraDistance(page.getWidth() * 10);
            
            if (position < -1) {
                // 页面在屏幕左侧外
                page.setAlpha(0f);
                page.setRotationY(0f);
            } else if (position <= 0) {
                // 当前页面（向左翻）
                page.setAlpha(1f);
                page.setPivotX(page.getWidth());
                page.setPivotY(page.getHeight() / 2f);
                // 限制旋转角度，避免过度翻转
                page.setRotationY(Math.max(-45f, -45f * Math.abs(position)));
            } else if (position <= 1) {
                // 下一页（从右侧翻入）
                page.setAlpha(1f);
                page.setPivotX(0);
                page.setPivotY(page.getHeight() / 2f);
                // 限制旋转角度
                page.setRotationY(Math.min(45f, 45f * Math.abs(position)));
            } else {
                // 页面在屏幕右侧外
                page.setAlpha(0f);
                page.setRotationY(0f);
            }
        }
    }

    /**
     * 滚动动画 - 默认滚动效果（无额外变换）
     */
    public static class ScrollTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(@NonNull View page, float position) {
            // 重置所有变换，使用ViewPager2默认效果
            page.setAlpha(1f);
            page.setTranslationX(0f);
            page.setTranslationZ(0f);
            page.setScaleX(1f);
            page.setScaleY(1f);
            page.setRotationY(0f);
        }
    }
}
