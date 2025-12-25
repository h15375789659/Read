package com.example.read.presentation.bookshelf;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.read.R;
import com.example.read.domain.model.Novel;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * 小说列表适配器
 * 
 * 验证需求：4.2 - 展示封面、标题、作者、阅读进度信息
 */
public class NovelAdapter extends ListAdapter<Novel, NovelAdapter.NovelViewHolder> {

    private OnNovelClickListener clickListener;
    private OnNovelLongClickListener longClickListener;
    private OnSelectionChangedListener selectionChangedListener;
    
    // 批量模式状态
    private boolean isBatchMode = false;
    private Set<Long> selectedIds = new HashSet<>();
    
    // Glide 请求选项（复用，避免重复创建）
    private static final RequestOptions COVER_OPTIONS = new RequestOptions()
            .placeholder(R.drawable.ic_book_placeholder)
            .error(R.drawable.ic_book_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop();

    public NovelAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Novel> DIFF_CALLBACK = new DiffUtil.ItemCallback<Novel>() {
        @Override
        public boolean areItemsTheSame(@NonNull Novel oldItem, @NonNull Novel newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Novel oldItem, @NonNull Novel newItem) {
            return oldItem.getTitle().equals(newItem.getTitle())
                    && safeEquals(oldItem.getAuthor(), newItem.getAuthor())
                    && oldItem.getTotalChapters() == newItem.getTotalChapters()
                    && oldItem.getReadingProgress() == newItem.getReadingProgress()
                    && oldItem.isPinned() == newItem.isPinned()
                    && safeEquals(oldItem.getCurrentChapterTitle(), newItem.getCurrentChapterTitle())
                    && safeEquals(oldItem.getLatestChapterTitle(), newItem.getLatestChapterTitle())
                    && safeEquals(oldItem.getCoverPath(), newItem.getCoverPath());
        }
        
        private boolean safeEquals(String a, String b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return a.equals(b);
        }
    };

    @NonNull
    @Override
    public NovelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_novel, parent, false);
        return new NovelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NovelViewHolder holder, int position) {
        Novel novel = getItem(position);
        holder.bind(novel);
    }

    public void setOnNovelClickListener(OnNovelClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnNovelLongClickListener(OnNovelLongClickListener listener) {
        this.longClickListener = listener;
    }
    
    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }
    
    /**
     * 设置批量模式
     * @param batchMode 是否为批量模式
     */
    public void setBatchMode(boolean batchMode) {
        if (this.isBatchMode != batchMode) {
            this.isBatchMode = batchMode;
            if (!batchMode) {
                selectedIds.clear();
            }
            notifyDataSetChanged();
        }
    }
    
    /**
     * 设置选中的ID集合
     * @param selectedIds 选中的小说ID集合
     */
    public void setSelectedIds(Set<Long> selectedIds) {
        this.selectedIds = selectedIds != null ? new HashSet<>(selectedIds) : new HashSet<>();
        if (isBatchMode) {
            notifyDataSetChanged();
        }
    }
    
    /**
     * 检查是否为批量模式
     */
    public boolean isBatchMode() {
        return isBatchMode;
    }

    /**
     * 小说点击监听器
     */
    public interface OnNovelClickListener {
        void onNovelClick(Novel novel);
    }

    /**
     * 小说长按监听器
     * 验证需求：4.3 - 长按小说条目显示操作菜单
     */
    public interface OnNovelLongClickListener {
        void onNovelLongClick(Novel novel, View anchorView);
    }
    
    /**
     * 选择状态变化监听器
     */
    public interface OnSelectionChangedListener {
        void onSelectionChanged(long novelId, boolean isSelected);
    }

    /**
     * ViewHolder
     */
    class NovelViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgCover;
        private final TextView tvTitle;
        private final TextView tvAuthor;
        private final TextView tvCurrentChapter;
        private final TextView tvLatestChapter;
        private final TextView tvUnreadCount;
        private final CheckBox checkBox;

        public NovelViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvCurrentChapter = itemView.findViewById(R.id.tv_current_chapter);
            tvLatestChapter = itemView.findViewById(R.id.tv_latest_chapter);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
            checkBox = itemView.findViewById(R.id.checkbox_select);
        }

        public void bind(Novel novel) {
            // 设置标题
            tvTitle.setText(novel.getTitle());

            // 第一行：作者
            String author = novel.getAuthor();
            if (author == null || author.isEmpty()) {
                author = itemView.getContext().getString(R.string.unknown_author);
            }
            tvAuthor.setText(itemView.getContext().getString(R.string.author_prefix, author));

            // 第二行：当前阅读章节
            String currentChapter = novel.getCurrentChapterTitle();
            if (currentChapter != null && !currentChapter.isEmpty()) {
                tvCurrentChapter.setText(itemView.getContext().getString(
                        R.string.current_chapter_prefix, currentChapter));
                tvCurrentChapter.setVisibility(View.VISIBLE);
            } else {
                tvCurrentChapter.setText(itemView.getContext().getString(
                        R.string.current_chapter_prefix, 
                        itemView.getContext().getString(R.string.no_reading_record)));
                tvCurrentChapter.setVisibility(View.VISIBLE);
            }

            // 第三行：最新章节
            String latestChapter = novel.getLatestChapterTitle();
            if (latestChapter != null && !latestChapter.isEmpty()) {
                tvLatestChapter.setText(itemView.getContext().getString(
                        R.string.latest_chapter_prefix, latestChapter));
                tvLatestChapter.setVisibility(View.VISIBLE);
            } else {
                // 如果没有最新章节信息，显示总章节数
                int totalChapters = novel.getTotalChapters();
                if (totalChapters > 0) {
                    tvLatestChapter.setText(itemView.getContext().getString(
                            R.string.latest_chapter_prefix, "共" + totalChapters + "章"));
                    tvLatestChapter.setVisibility(View.VISIBLE);
                } else {
                    tvLatestChapter.setVisibility(View.GONE);
                }
            }

            // 未读章节数标签（如果有置顶则显示星号）
            if (novel.isPinned()) {
                tvUnreadCount.setVisibility(View.VISIBLE);
                tvUnreadCount.setText("★");
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }

            // 设置封面图片（使用 Glide 异步加载，带缓存）
            String coverPath = novel.getCoverPath();
            if (coverPath != null && !coverPath.isEmpty()) {
                File coverFile = new File(coverPath);
                if (coverFile.exists()) {
                    Glide.with(itemView.getContext())
                            .load(coverFile)
                            .apply(COVER_OPTIONS)
                            .into(imgCover);
                } else {
                    imgCover.setImageResource(R.drawable.ic_book_placeholder);
                }
            } else {
                // 默认封面
                imgCover.setImageResource(R.drawable.ic_book_placeholder);
            }
            
            // 批量模式处理
            if (isBatchMode) {
                // 显示复选框
                if (checkBox != null) {
                    checkBox.setVisibility(View.VISIBLE);
                    checkBox.setChecked(selectedIds.contains(novel.getId()));
                    
                    // 复选框点击事件
                    checkBox.setOnClickListener(v -> {
                        if (selectionChangedListener != null) {
                            selectionChangedListener.onSelectionChanged(novel.getId(), checkBox.isChecked());
                        }
                    });
                }
                
                // 批量模式下点击整个条目也切换选择
                itemView.setOnClickListener(v -> {
                    if (checkBox != null) {
                        checkBox.setChecked(!checkBox.isChecked());
                        if (selectionChangedListener != null) {
                            selectionChangedListener.onSelectionChanged(novel.getId(), checkBox.isChecked());
                        }
                    }
                });
                
                // 批量模式下禁用长按
                itemView.setOnLongClickListener(null);
            } else {
                // 隐藏复选框
                if (checkBox != null) {
                    checkBox.setVisibility(View.GONE);
                }
                
                // 正常模式点击事件
                itemView.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onNovelClick(novel);
                    }
                });

                // 长按事件
                itemView.setOnLongClickListener(v -> {
                    if (longClickListener != null) {
                        longClickListener.onNovelLongClick(novel, v);
                        return true;
                    }
                    return false;
                });
            }
        }
    }
}
