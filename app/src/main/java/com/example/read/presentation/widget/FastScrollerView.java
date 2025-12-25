package com.example.read.presentation.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;

/**
 * 自定义快速滚动条
 * 用于章节列表的快速滚动
 */
public class FastScrollerView extends FrameLayout {

    private View scrollerThumb;
    private View scrollerTrack;
    private RecyclerView recyclerView;
    
    private boolean isDragging = false;
    private float thumbHeight = 0;
    private float trackHeight = 0;
    private int totalItemCount = 0;
    
    // 自动隐藏相关
    private ObjectAnimator hideAnimator;
    private static final int AUTO_HIDE_DELAY = 1500;
    private static final int ANIMATION_DURATION = 200;
    
    // 监听器引用（用于解绑）
    private RecyclerView.OnScrollListener scrollListener;
    private RecyclerView.AdapterDataObserver dataObserver;
    
    private final Runnable hideRunnable = () -> {
        if (!isDragging) {
            animateHide();
        }
    };

    public FastScrollerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public FastScrollerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FastScrollerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 创建轨道
        scrollerTrack = new View(context);
        scrollerTrack.setBackgroundResource(R.drawable.fastscroll_track);
        LayoutParams trackParams = new LayoutParams(
                dpToPx(6), 
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        trackParams.setMargins(0, dpToPx(8), dpToPx(4), dpToPx(8));
        addView(scrollerTrack, trackParams);
        
        // 创建滑块
        scrollerThumb = new View(context);
        scrollerThumb.setBackgroundResource(R.drawable.fastscroll_thumb);
        LayoutParams thumbParams = new LayoutParams(
                dpToPx(20),  // 更宽的滑块，便于触摸
                dpToPx(60)   // 更高的滑块
        );
        addView(scrollerThumb, thumbParams);
        
        // 初始隐藏
        setAlpha(0f);
    }

    /**
     * 绑定RecyclerView
     */
    public void attachToRecyclerView(RecyclerView recyclerView) {
        // 先解绑之前的 RecyclerView
        detachFromRecyclerView();
        
        this.recyclerView = recyclerView;
        
        // 创建滚动监听器
        scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateThumbPosition();
                showWithAutoHide();
            }
        };
        recyclerView.addOnScrollListener(scrollListener);
        
        // 创建数据观察者
        if (recyclerView.getAdapter() != null) {
            dataObserver = new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    updateThumbPosition();
                }
                
                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    updateThumbPosition();
                }
                
                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    updateThumbPosition();
                }
            };
            recyclerView.getAdapter().registerAdapterDataObserver(dataObserver);
        }
    }
    
    /**
     * 解绑RecyclerView，释放资源
     */
    public void detachFromRecyclerView() {
        if (recyclerView != null) {
            // 移除滚动监听器
            if (scrollListener != null) {
                recyclerView.removeOnScrollListener(scrollListener);
                scrollListener = null;
            }
            
            // 移除数据观察者
            if (dataObserver != null && recyclerView.getAdapter() != null) {
                try {
                    recyclerView.getAdapter().unregisterAdapterDataObserver(dataObserver);
                } catch (Exception ignored) {
                    // 可能已经被移除
                }
                dataObserver = null;
            }
            
            recyclerView = null;
        }
        
        // 取消所有待执行的回调
        removeCallbacks(hideRunnable);
        if (hideAnimator != null) {
            hideAnimator.cancel();
            hideAnimator = null;
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 视图从窗口移除时自动解绑
        detachFromRecyclerView();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        trackHeight = h - dpToPx(16); // 减去上下边距
        thumbHeight = scrollerThumb.getLayoutParams().height;
        updateThumbPosition();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (recyclerView == null) return false;
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 检查是否点击在滑块区域（扩大触摸区域）
                float thumbY = scrollerThumb.getY();
                float touchY = event.getY();
                if (touchY >= thumbY - dpToPx(20) && touchY <= thumbY + thumbHeight + dpToPx(20)) {
                    isDragging = true;
                    cancelAutoHide();
                    scrollToPosition(touchY);
                    return true;
                }
                // 点击轨道也可以跳转
                isDragging = true;
                cancelAutoHide();
                scrollToPosition(touchY);
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    scrollToPosition(event.getY());
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    scheduleAutoHide();
                    return true;
                }
                break;
        }
        
        return super.onTouchEvent(event);
    }

    /**
     * 根据触摸位置滚动到对应位置
     */
    private void scrollToPosition(float y) {
        if (recyclerView == null || trackHeight <= 0) return;
        
        totalItemCount = recyclerView.getAdapter() != null ? recyclerView.getAdapter().getItemCount() : 0;
        if (totalItemCount == 0) return;
        
        // 计算滚动比例
        float scrollableHeight = trackHeight - thumbHeight;
        float adjustedY = Math.max(0, Math.min(y - dpToPx(8), scrollableHeight));
        float ratio = adjustedY / scrollableHeight;
        
        // 计算目标位置
        int targetPosition = (int) (ratio * (totalItemCount - 1));
        targetPosition = Math.max(0, Math.min(targetPosition, totalItemCount - 1));
        
        // 滚动到目标位置
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            layoutManager.scrollToPositionWithOffset(targetPosition, 0);
        }
        
        // 更新滑块位置
        updateThumbPositionDirect(ratio);
    }

    /**
     * 更新滑块位置（基于RecyclerView滚动）
     */
    private void updateThumbPosition() {
        if (recyclerView == null || trackHeight <= 0) return;
        
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return;
        
        totalItemCount = recyclerView.getAdapter() != null ? recyclerView.getAdapter().getItemCount() : 0;
        if (totalItemCount == 0) {
            setVisibility(GONE);
            return;
        }
        
        // 如果项目数量少，不显示快速滚动
        if (totalItemCount < 30) {
            setVisibility(GONE);
            return;
        }
        
        setVisibility(VISIBLE);
        
        int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
        if (firstVisiblePosition == RecyclerView.NO_POSITION) return;
        
        // 计算滚动比例
        float ratio = (float) firstVisiblePosition / (totalItemCount - 1);
        updateThumbPositionDirect(ratio);
    }

    /**
     * 直接设置滑块位置
     */
    private void updateThumbPositionDirect(float ratio) {
        float scrollableHeight = trackHeight - thumbHeight;
        float thumbY = dpToPx(8) + ratio * scrollableHeight;
        scrollerThumb.setY(thumbY);
    }

    /**
     * 显示并设置自动隐藏
     */
    private void showWithAutoHide() {
        if (getAlpha() < 1f) {
            animate().alpha(1f).setDuration(ANIMATION_DURATION).start();
        }
        scheduleAutoHide();
    }

    /**
     * 安排自动隐藏
     */
    private void scheduleAutoHide() {
        removeCallbacks(hideRunnable);
        postDelayed(hideRunnable, AUTO_HIDE_DELAY);
    }

    /**
     * 取消自动隐藏
     */
    private void cancelAutoHide() {
        removeCallbacks(hideRunnable);
        if (hideAnimator != null) {
            hideAnimator.cancel();
        }
        setAlpha(1f);
    }

    /**
     * 动画隐藏
     */
    private void animateHide() {
        hideAnimator = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f);
        hideAnimator.setDuration(ANIMATION_DURATION);
        hideAnimator.start();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
