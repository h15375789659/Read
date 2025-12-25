package com.example.read.presentation.reader;

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
import com.example.read.domain.model.Bookmark;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 书签列表适配器
 * 验证需求：7.3 - 显示书签列表
 */
public class BookmarkAdapter extends ListAdapter<Bookmark, BookmarkAdapter.BookmarkViewHolder> {

    private OnBookmarkClickListener clickListener;
    private OnBookmarkDeleteListener deleteListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public BookmarkAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Bookmark> DIFF_CALLBACK = new DiffUtil.ItemCallback<Bookmark>() {
        @Override
        public boolean areItemsTheSame(@NonNull Bookmark oldItem, @NonNull Bookmark newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Bookmark oldItem, @NonNull Bookmark newItem) {
            return oldItem.getChapterTitle().equals(newItem.getChapterTitle())
                    && (oldItem.getNote() == null ? newItem.getNote() == null : oldItem.getNote().equals(newItem.getNote()))
                    && oldItem.getCreateTime() == newItem.getCreateTime()
                    && oldItem.getPosition() == newItem.getPosition();
        }
    };

    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bookmark, parent, false);
        return new BookmarkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
        Bookmark bookmark = getItem(position);
        holder.bind(bookmark);
    }

    public void setOnBookmarkClickListener(OnBookmarkClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnBookmarkDeleteListener(OnBookmarkDeleteListener listener) {
        this.deleteListener = listener;
    }

    class BookmarkViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvChapterTitle;
        private final TextView tvPageInfo;
        private final TextView tvNote;
        private final TextView tvCreateTime;
        private final ImageButton btnDelete;

        BookmarkViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChapterTitle = itemView.findViewById(R.id.tv_chapter_title);
            tvPageInfo = itemView.findViewById(R.id.tv_page_info);
            tvNote = itemView.findViewById(R.id.tv_note);
            tvCreateTime = itemView.findViewById(R.id.tv_create_time);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(Bookmark bookmark) {
            // 设置章节标题
            tvChapterTitle.setText(bookmark.getChapterTitle());

            // 设置页数信息（position + 1，因为页码从0开始）
            int pageNumber = bookmark.getPosition() + 1;
            String pageInfo = itemView.getContext().getString(R.string.bookmark_page_info, pageNumber);
            tvPageInfo.setText(pageInfo);

            // 设置备注（如果有）
            String note = bookmark.getNote();
            if (note != null && !note.trim().isEmpty()) {
                tvNote.setText(note);
                tvNote.setVisibility(View.VISIBLE);
            } else {
                tvNote.setVisibility(View.GONE);
            }

            // 设置创建时间
            String timeStr = dateFormat.format(new Date(bookmark.getCreateTime()));
            tvCreateTime.setText(timeStr);

            // 点击跳转
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onBookmarkClick(bookmark);
                }
            });

            // 删除按钮
            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onBookmarkDelete(bookmark);
                }
            });
        }
    }

    /**
     * 书签点击监听器
     */
    public interface OnBookmarkClickListener {
        void onBookmarkClick(Bookmark bookmark);
    }

    /**
     * 书签删除监听器
     */
    public interface OnBookmarkDeleteListener {
        void onBookmarkDelete(Bookmark bookmark);
    }
}
