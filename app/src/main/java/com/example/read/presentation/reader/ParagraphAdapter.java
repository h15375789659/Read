package com.example.read.presentation.reader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.ParagraphInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 段落列表适配器
 * 用于TTS控制面板中显示可选择的段落
 * 支持选中状态和正在朗读状态的高亮显示
 */
public class ParagraphAdapter extends RecyclerView.Adapter<ParagraphAdapter.ParagraphViewHolder> {

    private List<ParagraphInfo> paragraphs = new ArrayList<>();
    private OnParagraphClickListener listener;
    private int selectedPosition = -1;
    private int readingPosition = -1;  // 正在朗读的段落位置

    public interface OnParagraphClickListener {
        void onParagraphClick(ParagraphInfo paragraph);
    }

    public void setOnParagraphClickListener(OnParagraphClickListener listener) {
        this.listener = listener;
    }

    public void setParagraphs(List<ParagraphInfo> paragraphs) {
        this.paragraphs = paragraphs != null ? paragraphs : new ArrayList<>();
        this.selectedPosition = -1;
        this.readingPosition = -1;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = this.selectedPosition;
        this.selectedPosition = position;
        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition);
        }
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    /**
     * 设置正在朗读的段落位置
     * @param position 段落位置，-1表示没有正在朗读
     */
    public void setReadingPosition(int position) {
        int oldPosition = this.readingPosition;
        this.readingPosition = position;
        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition);
        }
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    /**
     * 根据文本位置更新正在朗读的段落
     * @param textPosition 在原文中的字符位置
     */
    public void updateReadingPositionByTextPosition(int textPosition) {
        int newReadingPosition = -1;
        
        // 查找包含当前位置的段落
        for (int i = 0; i < paragraphs.size(); i++) {
            ParagraphInfo paragraph = paragraphs.get(i);
            int start = paragraph.getStartPosition();
            int end = start + paragraph.getFullText().length();
            
            if (textPosition >= start && textPosition < end) {
                newReadingPosition = i;
                break;
            }
            // 如果位置在两个段落之间，选择下一个段落
            if (i < paragraphs.size() - 1) {
                ParagraphInfo nextParagraph = paragraphs.get(i + 1);
                if (textPosition >= end && textPosition < nextParagraph.getStartPosition()) {
                    newReadingPosition = i + 1;
                    break;
                }
            }
        }
        
        if (newReadingPosition != readingPosition) {
            setReadingPosition(newReadingPosition);
        }
    }

    /**
     * 获取正在朗读的段落位置
     */
    public int getReadingPosition() {
        return readingPosition;
    }

    @NonNull
    @Override
    public ParagraphViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paragraph, parent, false);
        return new ParagraphViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParagraphViewHolder holder, int position) {
        ParagraphInfo paragraph = paragraphs.get(position);
        boolean isSelected = position == selectedPosition;
        boolean isReading = position == readingPosition;
        holder.bind(paragraph, isSelected, isReading);
        
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                setSelectedPosition(adapterPosition);
                listener.onParagraphClick(paragraphs.get(adapterPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return paragraphs.size();
    }

    static class ParagraphViewHolder extends RecyclerView.ViewHolder {
        private final TextView indexText;
        private final TextView previewText;
        private final View readingIndicator;

        public ParagraphViewHolder(@NonNull View itemView) {
            super(itemView);
            indexText = itemView.findViewById(R.id.paragraph_index);
            previewText = itemView.findViewById(R.id.paragraph_preview);
            readingIndicator = itemView.findViewById(R.id.reading_indicator);
        }

        public void bind(ParagraphInfo paragraph, boolean isSelected, boolean isReading) {
            indexText.setText(String.valueOf(paragraph.getParagraphIndex() + 1));
            previewText.setText(paragraph.getPreview());
            
            // 正在朗读状态 - 最高优先级
            if (isReading) {
                itemView.setBackgroundResource(R.drawable.bg_paragraph_reading);
                if (readingIndicator != null) {
                    readingIndicator.setVisibility(View.VISIBLE);
                }
            } else if (isSelected) {
                // 选中状态
                itemView.setBackgroundResource(R.drawable.bg_paragraph_selected);
                if (readingIndicator != null) {
                    readingIndicator.setVisibility(View.GONE);
                }
            } else {
                // 普通状态
                itemView.setBackgroundResource(R.drawable.bg_paragraph_normal);
                if (readingIndicator != null) {
                    readingIndicator.setVisibility(View.GONE);
                }
            }
            
            itemView.setSelected(isSelected || isReading);
        }
    }
}
