package com.example.read.presentation.parser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.ChapterInfo;

/**
 * 网站解析章节列表适配器
 * 用于预览解析到的章节列表
 */
public class ParserChapterAdapter extends ListAdapter<ChapterInfo, ParserChapterAdapter.ChapterViewHolder> {

    public ParserChapterAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<ChapterInfo> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<ChapterInfo>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChapterInfo oldItem, @NonNull ChapterInfo newItem) {
            return oldItem.getIndex() == newItem.getIndex();
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChapterInfo oldItem, @NonNull ChapterInfo newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) 
                    && oldItem.getUrl().equals(newItem.getUrl());
        }
    };

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parser_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        ChapterInfo chapter = getItem(position);
        holder.bind(chapter);
    }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        private final TextView textIndex;
        private final TextView textTitle;

        ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            textIndex = itemView.findViewById(R.id.text_chapter_index);
            textTitle = itemView.findViewById(R.id.text_chapter_title);
        }

        void bind(ChapterInfo chapter) {
            textIndex.setText((chapter.getIndex() + 1) + ".");
            textTitle.setText(chapter.getTitle());
        }
    }
}
