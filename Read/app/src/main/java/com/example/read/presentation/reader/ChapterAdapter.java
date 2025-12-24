package com.example.read.presentation.reader;

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
import com.example.read.domain.model.Chapter;

/**
 * 章节列表适配器
 * 用于在章节列表对话框中显示章节
 */
public class ChapterAdapter extends ListAdapter<Chapter, ChapterAdapter.ChapterViewHolder> {

    private long currentChapterId = -1;
    private OnChapterClickListener onChapterClickListener;

    public ChapterAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Chapter> DIFF_CALLBACK = new DiffUtil.ItemCallback<Chapter>() {
        @Override
        public boolean areItemsTheSame(@NonNull Chapter oldItem, @NonNull Chapter newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Chapter oldItem, @NonNull Chapter newItem) {
            return oldItem.getId() == newItem.getId() 
                    && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getChapterIndex() == newItem.getChapterIndex();
        }
    };

    /**
     * 设置当前阅读的章节ID
     */
    public void setCurrentChapterId(long chapterId) {
        long oldChapterId = this.currentChapterId;
        this.currentChapterId = chapterId;
        
        // 刷新旧的和新的当前章节项
        for (int i = 0; i < getItemCount(); i++) {
            Chapter chapter = getItem(i);
            if (chapter.getId() == oldChapterId || chapter.getId() == chapterId) {
                notifyItemChanged(i);
            }
        }
    }

    /**
     * 设置章节点击监听器
     */
    public void setOnChapterClickListener(OnChapterClickListener listener) {
        this.onChapterClickListener = listener;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Chapter chapter = getItem(position);
        holder.bind(chapter, chapter.getId() == currentChapterId);
    }

    /**
     * 章节点击监听器接口
     */
    public interface OnChapterClickListener {
        void onChapterClick(Chapter chapter);
    }

    /**
     * 章节ViewHolder
     */
    class ChapterViewHolder extends RecyclerView.ViewHolder {
        private final TextView indexText;
        private final TextView titleText;
        private final ImageView currentIndicator;

        ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            indexText = itemView.findViewById(R.id.chapter_index_text);
            titleText = itemView.findViewById(R.id.chapter_title_text);
            currentIndicator = itemView.findViewById(R.id.current_indicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onChapterClickListener != null) {
                    onChapterClickListener.onChapterClick(getItem(position));
                }
            });
        }

        void bind(Chapter chapter, boolean isCurrent) {
            indexText.setText(String.valueOf(chapter.getChapterIndex() + 1));
            titleText.setText(chapter.getTitle());
            currentIndicator.setVisibility(isCurrent ? View.VISIBLE : View.GONE);
            
            // 当前章节高亮显示
            if (isCurrent) {
                titleText.setTextColor(itemView.getContext().getResources().getColor(R.color.primary, null));
            } else {
                titleText.setTextColor(itemView.getContext().getResources().getColor(R.color.text_primary, null));
            }
        }
    }
}
