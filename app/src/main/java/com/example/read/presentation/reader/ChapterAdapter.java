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
import com.example.read.utils.PinyinHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 章节列表适配器
 * 用于在章节列表对话框中显示章节
 * 支持章节搜索过滤（中文、拼音全拼、拼音首字母）
 */
public class ChapterAdapter extends ListAdapter<Chapter, ChapterAdapter.ChapterViewHolder> {

    private long currentChapterId = -1;
    private OnChapterClickListener onChapterClickListener;
    
    // 原始章节列表（用于过滤）
    private List<Chapter> originalList = new ArrayList<>();
    // 当前搜索关键词
    private String currentFilter = "";

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
    
    /**
     * 设置原始章节列表
     * 在设置章节列表时调用，保存原始数据用于过滤
     */
    public void setOriginalList(List<Chapter> chapters) {
        this.originalList = new ArrayList<>(chapters);
        this.currentFilter = "";
        submitList(chapters);
    }
    
    /**
     * 过滤章节列表
     * 支持中文匹配、拼音全拼匹配、拼音首字母匹配
     * 
     * @param keyword 搜索关键词
     * @return 匹配的章节数量
     */
    public int filter(String keyword) {
        this.currentFilter = keyword == null ? "" : keyword.trim().toLowerCase();
        
        if (currentFilter.isEmpty()) {
            // 清空搜索，显示全部
            submitList(new ArrayList<>(originalList));
            return originalList.size();
        }
        
        List<Chapter> filteredList = new ArrayList<>();
        for (Chapter chapter : originalList) {
            // 使用PinyinHelper进行匹配（支持中文、拼音全拼、拼音首字母）
            if (PinyinHelper.matches(chapter.getTitle(), currentFilter)) {
                filteredList.add(chapter);
            }
        }
        
        submitList(filteredList);
        return filteredList.size();
    }
    
    /**
     * 清除过滤，显示全部章节
     */
    public void clearFilter() {
        this.currentFilter = "";
        submitList(new ArrayList<>(originalList));
    }
    
    /**
     * 获取当前是否有过滤
     */
    public boolean hasFilter() {
        return !currentFilter.isEmpty();
    }
    
    /**
     * 获取原始章节数量
     */
    public int getOriginalCount() {
        return originalList.size();
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
