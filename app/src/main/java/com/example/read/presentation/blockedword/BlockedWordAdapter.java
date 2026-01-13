package com.example.read.presentation.blockedword;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.BlockedWord;

/**
 * 屏蔽词列表适配器
 * 
 * 验证需求：11.1, 11.3
 */
public class BlockedWordAdapter extends ListAdapter<BlockedWord, BlockedWordAdapter.ViewHolder> {

    private OnDeleteClickListener deleteClickListener;

    public BlockedWordAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<BlockedWord> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<BlockedWord>() {
        @Override
        public boolean areItemsTheSame(@NonNull BlockedWord oldItem, @NonNull BlockedWord newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull BlockedWord oldItem, @NonNull BlockedWord newItem) {
            return oldItem.getWord().equals(newItem.getWord());
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_blocked_word, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlockedWord word = getItem(position);
        holder.bind(word);
    }

    /**
     * 设置删除按钮点击监听器
     */
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    /**
     * 删除按钮点击监听器接口
     */
    public interface OnDeleteClickListener {
        void onDeleteClick(BlockedWord word);
    }

    /**
     * ViewHolder
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvWord;
        private final ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWord = itemView.findViewById(R.id.tv_word);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(BlockedWord word) {
            tvWord.setText(word.getWord());
            
            btnDelete.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(word);
                }
            });
        }
    }
}
