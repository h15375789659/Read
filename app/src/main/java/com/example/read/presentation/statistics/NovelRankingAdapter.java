package com.example.read.presentation.statistics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.NovelReadingStats;

/**
 * 小说阅读排行适配器
 * 
 * 验证需求：12.5
 */
public class NovelRankingAdapter extends ListAdapter<NovelReadingStats, NovelRankingAdapter.ViewHolder> {

    public NovelRankingAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<NovelReadingStats> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<NovelReadingStats>() {
        @Override
        public boolean areItemsTheSame(@NonNull NovelReadingStats oldItem, 
                                       @NonNull NovelReadingStats newItem) {
            return oldItem.getNovelId() == newItem.getNovelId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull NovelReadingStats oldItem, 
                                          @NonNull NovelReadingStats newItem) {
            return oldItem.getNovelId() == newItem.getNovelId()
                    && oldItem.getTotalDuration() == newItem.getTotalDuration()
                    && oldItem.getReadingProgress() == newItem.getReadingProgress()
                    && oldItem.getTotalWordCount() == newItem.getTotalWordCount();
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_novel_ranking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NovelReadingStats stats = getItem(position);
        holder.bind(stats, position + 1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRank;
        private final TextView tvNovelTitle;
        private final TextView tvNovelAuthor;
        private final TextView tvDuration;
        private final TextView tvProgressInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvNovelTitle = itemView.findViewById(R.id.tv_novel_title);
            tvNovelAuthor = itemView.findViewById(R.id.tv_novel_author);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvProgressInfo = itemView.findViewById(R.id.tv_progress_info);
        }

        void bind(NovelReadingStats stats, int rank) {
            tvRank.setText(String.valueOf(rank));
            tvNovelTitle.setText(stats.getNovelTitle());
            tvNovelAuthor.setText(stats.getNovelAuthor());
            tvDuration.setText(stats.getFormattedDuration());
            
            // 显示进度和总字数
            String progressInfo = "进度 " + stats.getFormattedProgress() + " · " + stats.getFormattedWordCount();
            tvProgressInfo.setText(progressInfo);
        }
    }
}
