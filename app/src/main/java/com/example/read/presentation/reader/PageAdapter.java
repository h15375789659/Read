package com.example.read.presentation.reader;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.ReaderFont;

import java.util.ArrayList;
import java.util.List;

/**
 * 翻页适配器 - 用于ViewPager2显示分页内容
 * 
 * 支持跨章节翻页：
 * - 上一章最后一页（如果有上一章）
 * - 当前章节所有页
 * - 下一章第一页（如果有下一章）
 * 
 * 支持点击区域检测：
 * - 左侧1/3区域：上一页
 * - 中间1/3区域：显示/隐藏工具栏
 * - 右侧1/3区域：下一页
 */
public class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageViewHolder> {

    /**
     * 页面数据，包含章节信息
     */
    public static class PageData {
        public static final int TYPE_PREVIOUS_CHAPTER = 0; // 上一章页面
        public static final int TYPE_CURRENT_CHAPTER = 1;  // 当前章节页面
        public static final int TYPE_NEXT_CHAPTER = 2;     // 下一章页面
        
        private final String content;           // 页面内容
        private final String chapterTitle;      // 章节标题
        private final int pageType;             // 页面类型
        private final int pageNumber;           // 在当前章节中的页码
        private final int totalPages;           // 当前章节总页数
        private final boolean isFirstPage;      // 是否是章节第一页
        private final long chapterId;           // 章节ID
        private final int startIndex;           // 页面内容在章节中的起始位置（缩进后）
        private final int originalStartIndex;   // 原始文本起始位置（用于TTS高亮）
        private final int originalEndIndex;     // 原始文本结束位置（用于TTS高亮）
        
        public PageData(String content, String chapterTitle, int pageType, 
                       int pageNumber, int totalPages, boolean isFirstPage, long chapterId, int startIndex) {
            this(content, chapterTitle, pageType, pageNumber, totalPages, isFirstPage, 
                 chapterId, startIndex, startIndex, startIndex + (content != null ? content.length() : 0));
        }
        
        public PageData(String content, String chapterTitle, int pageType, 
                       int pageNumber, int totalPages, boolean isFirstPage, long chapterId, 
                       int startIndex, int originalStartIndex, int originalEndIndex) {
            this.content = content;
            this.chapterTitle = chapterTitle;
            this.pageType = pageType;
            this.pageNumber = pageNumber;
            this.totalPages = totalPages;
            this.isFirstPage = isFirstPage;
            this.chapterId = chapterId;
            this.startIndex = startIndex;
            this.originalStartIndex = originalStartIndex;
            this.originalEndIndex = originalEndIndex;
        }
        
        public String getContent() { return content; }
        public String getChapterTitle() { return chapterTitle; }
        public int getPageType() { return pageType; }
        public int getPageNumber() { return pageNumber; }
        public int getTotalPages() { return totalPages; }
        public boolean isFirstPage() { return isFirstPage; }
        public long getChapterId() { return chapterId; }
        public int getStartIndex() { return startIndex; }
        /** 获取原始文本起始位置（用于TTS高亮匹配） */
        public int getOriginalStartIndex() { return originalStartIndex; }
        /** 获取原始文本结束位置（用于TTS高亮匹配） */
        public int getOriginalEndIndex() { return originalEndIndex; }
    }

    // 页面数据列表（包含上一章所有页、当前章节所有页、下一章所有页）
    private List<PageData> allPages = new ArrayList<>();
    
    // ==================== 章节索引范围 ====================
    // 上一章在 allPages 中的索引范围
    private int prevChapterStartIndex = -1;
    private int prevChapterEndIndex = -1;
    
    // 当前章节在 allPages 中的索引范围
    private int currentChapterStartIndex = 0;
    private int currentChapterEndIndex = -1;
    
    // 下一章在 allPages 中的索引范围
    private int nextChapterStartIndex = -1;
    private int nextChapterEndIndex = -1;
    
    // ==================== 章节ID ====================
    private long prevChapterId = -1;
    private long currentChapterId = -1;
    private long nextChapterId = -1;
    
    // ==================== 章节页数 ====================
    private int prevChapterPageCount = 0;
    private int currentChapterPageCount = 0;
    private int nextChapterPageCount = 0;
    
    // 显示设置
    private float fontSize = 18f;
    private float lineSpacing = 1.5f;
    private int textColor = 0xFF333333;
    private int backgroundColor = 0xFFFFFFFF;
    private ReaderFont font = ReaderFont.DEFAULT;
    
    // 状态信息
    private String statusChapterName = "";
    private String statusTimeBattery = "";
    
    // TTS高亮位置（在章节中的位置）
    private int ttsHighlightPosition = -1;
    
    // 搜索关键词（用于高亮显示）
    private String searchKeyword = "";
    
    // 点击监听器
    private OnPageClickListener clickListener;
    private OnPageTurnListener pageTurnListener;
    private OnChapterChangeListener chapterChangeListener;

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_page_content, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        if (position < allPages.size()) {
            PageData pageData = allPages.get(position);
            
            // 设置页面内容
            holder.pageContentView.setPageContent(pageData.getContent());
            holder.pageContentView.setChapterTitle(pageData.getChapterTitle());
            holder.pageContentView.setShowTitle(pageData.isFirstPage());
            holder.pageContentView.setPageInfo(String.format("%d / %d", 
                    pageData.getPageNumber(), pageData.getTotalPages()));
            
            // 应用显示设置
            holder.pageContentView.setFontSize(fontSize);
            holder.pageContentView.setLineSpacing(lineSpacing);
            holder.pageContentView.setTextColor(textColor);
            holder.pageContentView.setBackgroundColor(backgroundColor);
            holder.pageContentView.setFont(font);
            
            // 设置状态信息
            holder.pageContentView.setStatusChapterName(statusChapterName);
            holder.pageContentView.setStatusTimeBattery(statusTimeBattery);
            
            // 设置TTS高亮（仅当前章节页面）
            holder.pageContentView.setPageStartIndex(pageData.getStartIndex());
            holder.pageContentView.setOriginalTextRange(pageData.getOriginalStartIndex(), pageData.getOriginalEndIndex());
            if (pageData.getPageType() == PageData.TYPE_CURRENT_CHAPTER && ttsHighlightPosition >= 0) {
                holder.pageContentView.setTTSHighlight(ttsHighlightPosition);
            } else {
                holder.pageContentView.clearTTSHighlight();
            }
            
            // 设置搜索关键词高亮
            holder.pageContentView.setSearchKeyword(searchKeyword);
            
            // 设置点击监听 - 支持区域点击
            holder.itemView.setOnClickListener(v -> {
                float x = lastTouchX;
                int width = v.getWidth();
                
                // 左侧1/3区域 - 上一页
                if (x >= 0 && x < width / 3f) {
                    if (pageTurnListener != null) {
                        pageTurnListener.onPreviousPage();
                    }
                }
                // 右侧1/3区域 - 下一页
                else if (x > width * 2 / 3f) {
                    if (pageTurnListener != null) {
                        pageTurnListener.onNextPage();
                    }
                }
                // 中间1/3区域 - 工具栏
                else {
                    if (clickListener != null) {
                        clickListener.onPageClick();
                    }
                }
            });
            
            // 记录触摸位置，用于点击时判断区域
            holder.itemView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    lastTouchX = event.getX();
                }
                return false;
            });
        }
    }
    
    // 记录最后一次触摸的 X 坐标（-1 表示无效）
    private float lastTouchX = -1;

    @Override
    public int getItemCount() {
        return allPages.size();
    }

    /**
     * 设置页面数据（旧接口，兼容性保留）
     */
    public void setPages(List<TextPaginator.PageInfo> pages, String chapterTitle) {
        setPages(pages, chapterTitle, 0, null, null, null, null);
    }
    
    /**
     * 设置页面数据（包含相邻章节）
     * 
     * @param currentPages 当前章节页面
     * @param currentTitle 当前章节标题
     * @param currentChapterId 当前章节ID
     * @param prevLastPage 上一章最后一页（可为null）
     * @param prevTitle 上一章标题
     * @param nextFirstPage 下一章第一页（可为null）
     * @param nextTitle 下一章标题
     */
    public void setPages(List<TextPaginator.PageInfo> currentPages, String currentTitle, long currentChapterId,
                        TextPaginator.PageInfo prevLastPage, String prevTitle,
                        TextPaginator.PageInfo nextFirstPage, String nextTitle) {
        allPages.clear();
        
        int currentTotalPages = currentPages != null ? currentPages.size() : 0;
        
        // 添加上一章最后一页
        if (prevLastPage != null && prevTitle != null) {
            allPages.add(new PageData(
                    prevLastPage.getContent(),
                    prevTitle,
                    PageData.TYPE_PREVIOUS_CHAPTER,
                    prevLastPage.getPageNumber(),
                    prevLastPage.getPageNumber(), // 上一章总页数（这里用页码代替，因为只有最后一页）
                    false, // 不是第一页
                    0, // 上一章ID（暂不需要）
                    prevLastPage.getStartIndex(), // 页面起始位置（缩进后）
                    prevLastPage.getOriginalStartIndex(), // 原始文本起始位置
                    prevLastPage.getOriginalEndIndex() // 原始文本结束位置
            ));
            currentChapterStartIndex = 1;
        } else {
            currentChapterStartIndex = 0;
        }
        
        // 添加当前章节所有页
        if (currentPages != null) {
            for (int i = 0; i < currentPages.size(); i++) {
                TextPaginator.PageInfo pageInfo = currentPages.get(i);
                allPages.add(new PageData(
                        pageInfo.getContent(),
                        currentTitle != null ? currentTitle : "",
                        PageData.TYPE_CURRENT_CHAPTER,
                        i + 1,
                        currentTotalPages,
                        pageInfo.isFirstPage(),
                        currentChapterId,
                        pageInfo.getStartIndex(), // 页面起始位置（缩进后）
                        pageInfo.getOriginalStartIndex(), // 原始文本起始位置
                        pageInfo.getOriginalEndIndex() // 原始文本结束位置
                ));
            }
        }
        currentChapterPageCount = currentTotalPages;
        
        // 添加下一章第一页
        if (nextFirstPage != null && nextTitle != null) {
            allPages.add(new PageData(
                    nextFirstPage.getContent(),
                    nextTitle,
                    PageData.TYPE_NEXT_CHAPTER,
                    1,
                    1, // 下一章总页数（这里暂时设为1，因为只有第一页）
                    true, // 是第一页
                    0, // 下一章ID（暂不需要）
                    nextFirstPage.getStartIndex(), // 页面起始位置（缩进后）
                    nextFirstPage.getOriginalStartIndex(), // 原始文本起始位置
                    nextFirstPage.getOriginalEndIndex() // 原始文本结束位置
            ));
        }
        
        notifyDataSetChanged();
    }

    /**
     * 获取当前章节在 allPages 中的起始索引
     */
    public int getCurrentChapterStartIndex() {
        return currentChapterStartIndex;
    }
    
    /**
     * 获取当前章节页数
     */
    public int getCurrentChapterPageCount() {
        return currentChapterPageCount;
    }
    
    /**
     * 获取当前章节最后一页的索引
     */
    public int getCurrentChapterEndIndex() {
        return currentChapterStartIndex + currentChapterPageCount - 1;
    }
    
    /**
     * 检查指定位置是否是上一章页面
     */
    public boolean isPreviousChapterPage(int position) {
        if (position >= 0 && position < allPages.size()) {
            return allPages.get(position).getPageType() == PageData.TYPE_PREVIOUS_CHAPTER;
        }
        return false;
    }
    
    /**
     * 检查指定位置是否是下一章页面
     */
    public boolean isNextChapterPage(int position) {
        if (position >= 0 && position < allPages.size()) {
            return allPages.get(position).getPageType() == PageData.TYPE_NEXT_CHAPTER;
        }
        return false;
    }
    
    /**
     * 获取总页数
     */
    public int getTotalPages() {
        return allPages.size();
    }

    /**
     * 设置字体大小
     */
    public void setFontSize(float fontSize) {
        if (this.fontSize != fontSize) {
            this.fontSize = fontSize;
            notifyItemRangeChanged(0, getItemCount(), "fontSize");
        }
    }

    /**
     * 设置行间距
     */
    public void setLineSpacing(float lineSpacing) {
        if (this.lineSpacing != lineSpacing) {
            this.lineSpacing = lineSpacing;
            notifyItemRangeChanged(0, getItemCount(), "lineSpacing");
        }
    }

    /**
     * 设置文本颜色
     */
    public void setTextColor(int color) {
        if (this.textColor != color) {
            this.textColor = color;
            notifyItemRangeChanged(0, getItemCount(), "textColor");
        }
    }

    /**
     * 设置背景颜色
     */
    public void setBackgroundColor(int color) {
        if (this.backgroundColor != color) {
            this.backgroundColor = color;
            notifyItemRangeChanged(0, getItemCount(), "backgroundColor");
        }
    }

    /**
     * 设置字体
     */
    public void setFont(ReaderFont font) {
        if (this.font != font) {
            this.font = font;
            notifyItemRangeChanged(0, getItemCount(), "font");
        }
    }
    
    /**
     * 设置左上角章节名
     */
    public void setStatusChapterName(String name) {
        String newName = name != null ? name : "";
        if (!this.statusChapterName.equals(newName)) {
            this.statusChapterName = newName;
            notifyItemRangeChanged(0, getItemCount(), "statusChapterName");
        }
    }
    
    /**
     * 设置右下角时间电量
     */
    public void setStatusTimeBattery(String info) {
        String newInfo = info != null ? info : "";
        if (!this.statusTimeBattery.equals(newInfo)) {
            this.statusTimeBattery = newInfo;
            notifyItemRangeChanged(0, getItemCount(), "statusTimeBattery");
        }
    }

    /**
     * 设置点击监听器（中间区域）
     */
    public void setOnPageClickListener(OnPageClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * 设置翻页监听器（左右区域）
     */
    public void setOnPageTurnListener(OnPageTurnListener listener) {
        this.pageTurnListener = listener;
    }
    
    /**
     * 设置章节切换监听器
     */
    public void setOnChapterChangeListener(OnChapterChangeListener listener) {
        this.chapterChangeListener = listener;
    }

    /**
     * 页面点击监听器（中间区域）
     */
    public interface OnPageClickListener {
        void onPageClick();
    }

    /**
     * 翻页监听器（左右区域）
     */
    public interface OnPageTurnListener {
        void onPreviousPage();
        void onNextPage();
    }
    
    /**
     * 章节切换监听器
     */
    public interface OnChapterChangeListener {
        void onPreviousChapter();
        void onNextChapter();
    }

    /**
     * ViewHolder
     */
    static class PageViewHolder extends RecyclerView.ViewHolder {
        PageContentView pageContentView;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            pageContentView = itemView.findViewById(R.id.page_content_view);
        }
    }
    
    /**
     * 更新TTS高亮位置
     * @param position TTS在章节中的当前位置，-1表示清除高亮
     */
    public void setTTSHighlightPosition(int position) {
        if (this.ttsHighlightPosition != position) {
            this.ttsHighlightPosition = position;
            // 只更新当前章节的页面
            if (currentChapterPageCount > 0) {
                notifyItemRangeChanged(currentChapterStartIndex, currentChapterPageCount, "ttsHighlight");
            }
        }
    }
    
    /**
     * 清除TTS高亮
     */
    public void clearTTSHighlight() {
        setTTSHighlightPosition(-1);
    }
    
    /**
     * 设置搜索关键词（用于高亮显示）
     * @param keyword 搜索关键词，空字符串或null表示清除高亮
     */
    public void setSearchKeyword(String keyword) {
        String newKeyword = keyword != null ? keyword : "";
        if (!this.searchKeyword.equals(newKeyword)) {
            this.searchKeyword = newKeyword;
            // 更新所有页面
            notifyDataSetChanged();
        }
    }
    
    /**
     * 清除搜索关键词高亮
     */
    public void clearSearchKeyword() {
        setSearchKeyword("");
    }
    
    // ==================== 任务2: 完整章节预加载新方法 ====================
    
    /**
     * 设置完整的三章页面数据（新接口）
     * 
     * @param prevPages 上一章所有页面（可为null或空）
     * @param prevTitle 上一章标题
     * @param prevChapterId 上一章ID
     * @param currentPages 当前章节所有页面
     * @param currentTitle 当前章节标题
     * @param currentChapterId 当前章节ID
     * @param nextPages 下一章所有页面（可为null或空）
     * @param nextTitle 下一章标题
     * @param nextChapterId 下一章ID
     */
    public void setFullChapterPages(
            List<TextPaginator.PageInfo> prevPages, String prevTitle, long prevChapterId,
            List<TextPaginator.PageInfo> currentPages, String currentTitle, long currentChapterId,
            List<TextPaginator.PageInfo> nextPages, String nextTitle, long nextChapterId) {
        
        allPages.clear();
        
        // 重置所有索引
        this.prevChapterStartIndex = -1;
        this.prevChapterEndIndex = -1;
        this.currentChapterStartIndex = 0;
        this.currentChapterEndIndex = -1;
        this.nextChapterStartIndex = -1;
        this.nextChapterEndIndex = -1;
        
        // 保存章节ID
        this.prevChapterId = prevChapterId;
        this.currentChapterId = currentChapterId;
        this.nextChapterId = nextChapterId;
        
        int index = 0;
        
        // 1. 添加上一章所有页面
        int prevCount = (prevPages != null) ? prevPages.size() : 0;
        this.prevChapterPageCount = prevCount;
        if (prevCount > 0) {
            this.prevChapterStartIndex = index;
            for (int i = 0; i < prevCount; i++) {
                TextPaginator.PageInfo pageInfo = prevPages.get(i);
                allPages.add(new PageData(
                        pageInfo.getContent(),
                        prevTitle != null ? prevTitle : "",
                        PageData.TYPE_PREVIOUS_CHAPTER,
                        i + 1,
                        prevCount,
                        pageInfo.isFirstPage(),
                        prevChapterId,
                        pageInfo.getStartIndex(),
                        pageInfo.getOriginalStartIndex(),
                        pageInfo.getOriginalEndIndex()
                ));
                index++;
            }
            this.prevChapterEndIndex = index - 1;
        }
        
        // 2. 添加当前章节所有页面
        int currentCount = (currentPages != null) ? currentPages.size() : 0;
        this.currentChapterPageCount = currentCount;
        this.currentChapterStartIndex = index;
        if (currentCount > 0) {
            for (int i = 0; i < currentCount; i++) {
                TextPaginator.PageInfo pageInfo = currentPages.get(i);
                allPages.add(new PageData(
                        pageInfo.getContent(),
                        currentTitle != null ? currentTitle : "",
                        PageData.TYPE_CURRENT_CHAPTER,
                        i + 1,
                        currentCount,
                        pageInfo.isFirstPage(),
                        currentChapterId,
                        pageInfo.getStartIndex(),
                        pageInfo.getOriginalStartIndex(),
                        pageInfo.getOriginalEndIndex()
                ));
                index++;
            }
            this.currentChapterEndIndex = index - 1;
        } else {
            this.currentChapterEndIndex = this.currentChapterStartIndex - 1;
        }
        
        // 3. 添加下一章所有页面
        int nextCount = (nextPages != null) ? nextPages.size() : 0;
        this.nextChapterPageCount = nextCount;
        if (nextCount > 0) {
            this.nextChapterStartIndex = index;
            for (int i = 0; i < nextCount; i++) {
                TextPaginator.PageInfo pageInfo = nextPages.get(i);
                allPages.add(new PageData(
                        pageInfo.getContent(),
                        nextTitle != null ? nextTitle : "",
                        PageData.TYPE_NEXT_CHAPTER,
                        i + 1,
                        nextCount,
                        pageInfo.isFirstPage(),
                        nextChapterId,
                        pageInfo.getStartIndex(),
                        pageInfo.getOriginalStartIndex(),
                        pageInfo.getOriginalEndIndex()
                ));
                index++;
            }
            this.nextChapterEndIndex = index - 1;
        }
        
        notifyDataSetChanged();
    }
    
    /**
     * 判断页面属于哪个章节
     * 
     * @param position 页面在 allPages 中的位置
     * @return PageData.TYPE_PREVIOUS_CHAPTER / TYPE_CURRENT_CHAPTER / TYPE_NEXT_CHAPTER
     */
    public int getChapterTypeForPosition(int position) {
        if (position < 0 || position >= allPages.size()) {
            return PageData.TYPE_CURRENT_CHAPTER; // 默认返回当前章节
        }
        return allPages.get(position).getPageType();
    }
    
    /**
     * 判断是否在上一章的中间位置（用于触发预加载上上章）
     * 定义：位于上一章页面范围内，且不是最后一页
     * 
     * @param position 页面位置
     * @return true 如果在上一章中间
     */
    public boolean isInPrevChapterMiddle(int position) {
        if (prevChapterStartIndex < 0 || prevChapterEndIndex < 0) {
            return false;
        }
        // 在上一章范围内，且不是上一章最后一页
        return position >= prevChapterStartIndex && position < prevChapterEndIndex;
    }
    
    /**
     * 判断是否在下一章的中间位置（用于触发预加载下下章）
     * 定义：位于下一章页面范围内，且不是第一页也不是最后一页
     * 
     * @param position 页面位置
     * @return true 如果在下一章中间
     */
    public boolean isInNextChapterMiddle(int position) {
        if (nextChapterStartIndex < 0 || nextChapterEndIndex < 0) {
            return false;
        }
        // 在下一章范围内，且不是下一章第一页，也不是最后一页
        return position > nextChapterStartIndex && position < nextChapterEndIndex;
    }
    
    /**
     * 获取上一章起始索引
     */
    public int getPrevChapterStartIndex() {
        return prevChapterStartIndex;
    }
    
    /**
     * 获取上一章结束索引
     */
    public int getPrevChapterEndIndex() {
        return prevChapterEndIndex;
    }
    
    /**
     * 获取下一章起始索引
     */
    public int getNextChapterStartIndex() {
        return nextChapterStartIndex;
    }
    
    /**
     * 获取下一章结束索引
     */
    public int getNextChapterEndIndex() {
        return nextChapterEndIndex;
    }
    
    /**
     * 获取上一章ID
     */
    public long getPrevChapterId() {
        return prevChapterId;
    }
    
    /**
     * 获取当前章节ID
     */
    public long getCurrentChapterId() {
        return currentChapterId;
    }
    
    /**
     * 获取下一章ID
     */
    public long getNextChapterId() {
        return nextChapterId;
    }
    
    /**
     * 获取上一章页数
     */
    public int getPrevChapterPageCount() {
        return prevChapterPageCount;
    }
    
    /**
     * 获取下一章页数
     */
    public int getNextChapterPageCount() {
        return nextChapterPageCount;
    }
    
    /**
     * 检查是否有上一章页面
     */
    public boolean hasPrevChapterPages() {
        return prevChapterPageCount > 0;
    }
    
    /**
     * 检查是否有下一章页面
     */
    public boolean hasNextChapterPages() {
        return nextChapterPageCount > 0;
    }
}
