package com.example.read.presentation.bookshelf;

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

/**
 * 分类列表适配器 - 用于管理分类对话框中显示分类列表
 */
public class CategoryAdapter extends ListAdapter<String, CategoryAdapter.CategoryViewHolder> {

    private OnCategoryDeleteListener deleteListener;

    public CategoryAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<String> DIFF_CALLBACK = new DiffUtil.ItemCallback<String>() {
        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }
    };

    /**
     * 设置删除监听器
     */
    public void setOnCategoryDeleteListener(OnCategoryDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = getItem(position);
        holder.bind(category);
    }

    /**
     * 分类ViewHolder
     */
    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryName;
        private final ImageButton btnDelete;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.category_name);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(String category) {
            categoryName.setText(category);
            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(category);
                }
            });
        }
    }

    /**
     * 分类删除监听器接口
     */
    public interface OnCategoryDeleteListener {
        void onDelete(String category);
    }
}
