package com.example.read.presentation.reader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.data.entity.ChapterInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 摘要列表适配器
 */
public class SummaryAdapter extends RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder> {

    private List<ChapterInfo> summaryList = new ArrayList<>();
    private OnSummaryItemClickListener onItemClickListener;
    private OnSummaryDeleteListener onDeleteListener;

    public interface OnSummaryItemClickListener {
        void onItemClick(ChapterInfo chapter);
    }

    public interface OnSummaryDeleteListener {
        void onDelete(ChapterInfo chapter, int position);
    }

    public void setOnItemClickListener(OnSummaryItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnDeleteListener(OnSummaryDeleteListener listener) {
        this.onDeleteListener = listener;
    }

    public void setSummaryList(List<ChapterInfo> list) {
        this.summaryList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < summaryList.size()) {
            summaryList.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public SummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_summary, parent, false);
        return new SummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SummaryViewHolder holder, int position) {
        ChapterInfo chapter = summaryList.get(position);
        holder.bind(chapter);
    }

    @Override
    public int getItemCount() {
        return summaryList.size();
    }

    class SummaryViewHolder extends RecyclerView.ViewHolder {
        private final TextView chapterIndexText;
        private final TextView chapterTitleText;
        private final TextView summaryPreviewText;
        private final ImageButton btnDelete;

        SummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterIndexText = itemView.findViewById(R.id.chapter_index_text);
            chapterTitleText = itemView.findViewById(R.id.chapter_title_text);
            summaryPreviewText = itemView.findViewById(R.id.summary_preview_text);
            btnDelete = itemView.findViewById(R.id.btn_delete);

            // 点击整个条目
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemClickListener != null) {
                    onItemClickListener.onItemClick(summaryList.get(position));
                }
            });

            // 删除按钮
            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onDeleteListener != null) {
                    onDeleteListener.onDelete(summaryList.get(position), position);
                }
            });
        }

        void bind(ChapterInfo chapter) {
            // 章节序号
            chapterIndexText.setText("第" + (chapter.getChapterIndex() + 1) + "章");
            
            // 章节标题（去掉可能的序号前缀）
            String title = chapter.getTitle();
            if (title != null && title.contains(" ")) {
                // 尝试去掉"第X章 "前缀
                int spaceIndex = title.indexOf(" ");
                if (spaceIndex > 0 && spaceIndex < title.length() - 1) {
                    title = title.substring(spaceIndex + 1);
                }
            }
            chapterTitleText.setText(title != null ? title : "");
            
            // 摘要预览
            String summary = chapter.getSummary();
            summaryPreviewText.setText(summary != null ? summary : "");
        }
    }
}
