package com.example.read.presentation.reader;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.SearchResult;

/**
 * 搜索结果列表适配器
 * 
 * 验证需求：9.3 - 显示包含该关键词的所有位置列表（章节名称、段落预览）
 */
public class SearchResultAdapter extends ListAdapter<SearchResult, SearchResultAdapter.ViewHolder> {

    private OnSearchResultClickListener clickListener;
    private int selectedPosition = -1;

    public SearchResultAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<SearchResult> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<SearchResult>() {
        @Override
        public boolean areItemsTheSame(@NonNull SearchResult oldItem, @NonNull SearchResult newItem) {
            return oldItem.getChapterId() == newItem.getChapterId() 
                    && oldItem.getPosition() == newItem.getPosition();
        }

        @Override
        public boolean areContentsTheSame(@NonNull SearchResult oldItem, @NonNull SearchResult newItem) {
            return oldItem.getChapterId() == newItem.getChapterId()
                    && oldItem.getPosition() == newItem.getPosition()
                    && oldItem.getPreview().equals(newItem.getPreview());
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = getItem(position);
        holder.bind(result, position == selectedPosition);
    }

    /**
     * 设置选中位置
     */
    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        
        if (oldPosition >= 0 && oldPosition < getItemCount()) {
            notifyItemChanged(oldPosition);
        }
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position);
        }
    }

    /**
     * 设置点击监听器
     */
    public void setOnSearchResultClickListener(OnSearchResultClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * ViewHolder
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView chapterTitle;
        private final TextView previewText;
        private final View itemView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            chapterTitle = itemView.findViewById(R.id.chapter_title);
            previewText = itemView.findViewById(R.id.preview_text);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    setSelectedPosition(position);
                    clickListener.onSearchResultClick(getItem(position), position);
                }
            });
        }

        void bind(SearchResult result, boolean isSelected) {
            // 设置章节标题
            chapterTitle.setText(result.getChapterTitle());
            
            // 设置预览内容（高亮关键词）
            String preview = result.getPreview();
            String keyword = result.getKeyword();
            
            if (preview != null && keyword != null && !keyword.isEmpty()) {
                SpannableString spannablePreview = highlightKeyword(preview, keyword);
                previewText.setText(spannablePreview);
            } else {
                previewText.setText(preview != null ? preview : "");
            }
            
            // 设置选中状态背景
            if (isSelected) {
                itemView.setBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.search_result_selected));
            } else {
                itemView.setBackground(
                        ContextCompat.getDrawable(itemView.getContext(), 
                                android.R.attr.selectableItemBackground));
            }
        }

        /**
         * 高亮关键词
         * 验证需求：9.4 - 高亮显示关键词
         */
        private SpannableString highlightKeyword(String text, String keyword) {
            SpannableString spannable = new SpannableString(text);
            
            String lowerText = text.toLowerCase();
            String lowerKeyword = keyword.toLowerCase();
            
            int start = 0;
            while ((start = lowerText.indexOf(lowerKeyword, start)) != -1) {
                int end = start + keyword.length();
                
                // 设置高亮颜色
                spannable.setSpan(
                        new ForegroundColorSpan(
                                ContextCompat.getColor(itemView.getContext(), R.color.primary)),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                // 设置粗体
                spannable.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                start = end;
            }
            
            return spannable;
        }
    }

    /**
     * 搜索结果点击监听器接口
     */
    public interface OnSearchResultClickListener {
        void onSearchResultClick(SearchResult result, int position);
    }
}
