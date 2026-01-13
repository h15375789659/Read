package com.example.read.presentation.reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;

/**
 * 自定义快速滚动条
 * 特点：滑块有最小高度限制，即使内容很多也能保持可见和可拖动
 */
public class FixedThumbFastScroller extends View {

    // 滑块最小高度（dp）
    private static final int MIN_THUMB_HEIGHT_DP = 48;
    // 滚动条宽度（dp）
    private static final int SCROLLER_WIDTH_DP = 8;
    // 滑块圆角（dp）
    private static final int THUMB_CORNER_RADIUS_DP = 4;

    // 画笔
    private Paint thumbPaint;
    private Paint trackPaint;

    // 尺寸（像素）
    private int minThumbHeight;
    private int scrollerWidth;
    private int thumbCornerRadius;

    // 滑块位置和大小
    private float thumbTop = 0;
    private float thumbHeight;
    private RectF thumbRect = new RectF();
    private RectF trackRect = new RectF();

    // 关联的 RecyclerView
    private RecyclerView recyclerView;

    // 拖动状态
    private boolean isDragging = false;
    private float dragStartY;
    private float dragStartThumbTop;

    // 颜色
    private int thumbColor;
    private int thumbPressedColor;
    private int trackColor;

    public FixedThumbFastScroller(@NonNull Context context) {
        super(context);
        init(context);
    }

    public FixedThumbFastScroller(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        minThumbHeight = (int) (MIN_THUMB_HEIGHT_DP * density);
        scrollerWidth = (int) (SCROLLER_WIDTH_DP * density);
        thumbCornerRadius = (int) (THUMB_CORNER_RADIUS_DP * density);

        // 获取颜色
        thumbColor = ContextCompat.getColor(context, R.color.primary);
        thumbPressedColor = ContextCompat.getColor(context, R.color.primary_dark);
        trackColor = 0x20000000; // 12.5% 黑色

        // 初始化画笔
        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setColor(thumbColor);

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(trackColor);
    }

    /**
     * 绑定 RecyclerView
     */
    public void attachToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;

        // 监听滚动事件，更新滑块位置
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!isDragging) {
                    updateThumbPosition();
                    invalidate();
                }
            }
        });

        // 监听数据变化
        recyclerView.getAdapter().registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                post(() -> {
                    updateThumbPosition();
                    invalidate();
                });
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                post(() -> {
                    updateThumbPosition();
                    invalidate();
                });
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = scrollerWidth;
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        trackRect.set(0, 0, w, h);
        updateThumbPosition();
    }

    /**
     * 更新滑块位置和大小
     */
    private void updateThumbPosition() {
        if (recyclerView == null || getHeight() == 0) return;

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) return;

        LinearLayoutManager llm = (LinearLayoutManager) layoutManager;
        int itemCount = recyclerView.getAdapter() != null ? recyclerView.getAdapter().getItemCount() : 0;

        if (itemCount == 0) {
            thumbHeight = getHeight();
            thumbTop = 0;
            return;
        }

        // 计算可见范围
        int firstVisible = llm.findFirstVisibleItemPosition();
        int lastVisible = llm.findLastVisibleItemPosition();
        int visibleCount = lastVisible - firstVisible + 1;

        // 计算滑块高度（按比例，但有最小值限制）
        float ratio = (float) visibleCount / itemCount;
        thumbHeight = Math.max(minThumbHeight, getHeight() * ratio);

        // 计算滑块位置
        float scrollableHeight = getHeight() - thumbHeight;
        float scrollProgress = (float) firstVisible / Math.max(1, itemCount - visibleCount);
        thumbTop = scrollableHeight * Math.min(1f, Math.max(0f, scrollProgress));

        thumbRect.set(0, thumbTop, getWidth(), thumbTop + thumbHeight);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制轨道
        canvas.drawRoundRect(trackRect, thumbCornerRadius, thumbCornerRadius, trackPaint);

        // 绘制滑块
        thumbPaint.setColor(isDragging ? thumbPressedColor : thumbColor);
        canvas.drawRoundRect(thumbRect, thumbCornerRadius, thumbCornerRadius, thumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (recyclerView == null) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float y = event.getY();
                // 检查是否点击在滑块上或轨道上
                if (y >= thumbTop && y <= thumbTop + thumbHeight) {
                    // 点击在滑块上，开始拖动
                    isDragging = true;
                    dragStartY = y;
                    dragStartThumbTop = thumbTop;
                    invalidate();
                    return true;
                } else {
                    // 点击在轨道上，直接跳转到该位置
                    scrollToPosition(y);
                    return true;
                }

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float deltaY = event.getY() - dragStartY;
                    float newThumbTop = dragStartThumbTop + deltaY;

                    // 限制滑块范围
                    float maxTop = getHeight() - thumbHeight;
                    newThumbTop = Math.max(0, Math.min(maxTop, newThumbTop));

                    thumbTop = newThumbTop;
                    thumbRect.set(0, thumbTop, getWidth(), thumbTop + thumbHeight);

                    // 滚动 RecyclerView
                    scrollRecyclerView();
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    invalidate();
                    return true;
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * 根据滑块位置滚动 RecyclerView
     */
    private void scrollRecyclerView() {
        if (recyclerView == null || recyclerView.getAdapter() == null) return;

        int itemCount = recyclerView.getAdapter().getItemCount();
        if (itemCount == 0) return;

        float scrollableHeight = getHeight() - thumbHeight;
        if (scrollableHeight <= 0) return;

        float progress = thumbTop / scrollableHeight;
        int targetPosition = (int) (progress * (itemCount - 1));
        targetPosition = Math.max(0, Math.min(itemCount - 1, targetPosition));

        LinearLayoutManager llm = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (llm != null) {
            llm.scrollToPositionWithOffset(targetPosition, 0);
        }
    }

    /**
     * 点击轨道时跳转到指定位置
     */
    private void scrollToPosition(float y) {
        if (recyclerView == null || recyclerView.getAdapter() == null) return;

        int itemCount = recyclerView.getAdapter().getItemCount();
        if (itemCount == 0) return;

        float progress = y / getHeight();
        int targetPosition = (int) (progress * (itemCount - 1));
        targetPosition = Math.max(0, Math.min(itemCount - 1, targetPosition));

        recyclerView.scrollToPosition(targetPosition);
    }

    /**
     * 检查是否应该显示滚动条
     * 当内容不足以滚动时隐藏
     */
    public boolean shouldShow() {
        if (recyclerView == null || recyclerView.getAdapter() == null) return false;

        int itemCount = recyclerView.getAdapter().getItemCount();
        if (itemCount == 0) return false;

        LinearLayoutManager llm = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (llm == null) return false;

        int firstVisible = llm.findFirstCompletelyVisibleItemPosition();
        int lastVisible = llm.findLastCompletelyVisibleItemPosition();

        // 如果所有项目都完全可见，则不需要滚动条
        return !(firstVisible == 0 && lastVisible == itemCount - 1);
    }
}
