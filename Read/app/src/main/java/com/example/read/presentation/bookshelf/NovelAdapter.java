package com.example.read.presentation.bookshelf;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.Novel;

/**
 * 小说列表适配器
 * 
 * 验证需求：4.2 - 展示封面、标题、作者、阅读进度信息
 */
public class NovelAdapter extends ListAdapter<Novel, NovelAdapter.NovelViewHolder> {

    private OnNovelClickListener clickListener;
    private OnNovelLongClickListener longClickListener;

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
                    && safeEquals(oldItem.getLatestChapterTitle(), newItem.getLatestChapterTitle());
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
     * ViewHolder
     */
    class NovelViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgCover;
        private final TextView tvTitle;
        private final TextView tvAuthor;
        private final TextView tvCurrentChapter;
        private final TextView tvLatestChapter;
        private final TextView tvUnreadCount;

        public NovelViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvCurrentChapter = itemView.findViewById(R.id.tv_current_chapter);
            tvLatestChapter = itemView.findViewById(R.id.tv_latest_chapter);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
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

            // 设置封面图片
            String coverPath = novel.getCoverPath();
            if (coverPath != null && !coverPath.isEmpty()) {
                // TODO: 使用图片加载库加载封面
                imgCover.setImageResource(android.R.drawable.ic_menu_gallery);
            } else {
                // 默认封面
                imgCover.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // 点击事件
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
