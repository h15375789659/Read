package com.example.read.presentation.reader;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.Bookmark;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

/**
 * 书签列表对话框
 * 验证需求：7.3 - 显示书签列表
 * 验证需求：7.4 - 点击书签跳转到对应位置
 * 验证需求：7.5 - 删除书签
 */
public class BookmarkListDialog {

    private final Context context;
    private BottomSheetDialog dialog;
    private BookmarkAdapter adapter;
    private LinearLayout emptyView;
    private RecyclerView recyclerView;

    private OnBookmarkJumpListener jumpListener;
    private OnBookmarkDeleteListener deleteListener;
    private OnAddBookmarkListener addListener;

    public BookmarkListDialog(Context context) {
        this.context = context;
        initDialog();
    }

    private void initDialog() {
        dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_bookmark_list, null);
        dialog.setContentView(view);

        // 初始化视图
        emptyView = view.findViewById(R.id.empty_view);
        recyclerView = view.findViewById(R.id.rv_bookmarks);
        ImageButton btnAdd = view.findViewById(R.id.btn_add_bookmark);

        // 设置RecyclerView
        adapter = new BookmarkAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);

        // 书签点击 - 跳转
        adapter.setOnBookmarkClickListener(bookmark -> {
            if (jumpListener != null) {
                jumpListener.onBookmarkJump(bookmark);
            }
            dialog.dismiss();
        });

        // 书签删除
        adapter.setOnBookmarkDeleteListener(bookmark -> {
            showDeleteConfirmDialog(bookmark);
        });

        // 添加书签按钮
        btnAdd.setOnClickListener(v -> {
            if (addListener != null) {
                addListener.onAddBookmark();
            }
        });
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(Bookmark bookmark) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.bookmark_delete)
                .setMessage(R.string.bookmark_delete_confirm)
                .setPositiveButton(R.string.confirm, (d, which) -> {
                    if (deleteListener != null) {
                        deleteListener.onBookmarkDelete(bookmark);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 设置书签列表
     */
    public void setBookmarks(List<Bookmark> bookmarks) {
        if (bookmarks == null || bookmarks.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(bookmarks);
        }
    }

    /**
     * 显示对话框
     */
    public void show() {
        dialog.show();
    }

    /**
     * 关闭对话框
     */
    public void dismiss() {
        dialog.dismiss();
    }

    /**
     * 设置书签跳转监听器
     */
    public void setOnBookmarkJumpListener(OnBookmarkJumpListener listener) {
        this.jumpListener = listener;
    }

    /**
     * 设置书签删除监听器
     */
    public void setOnBookmarkDeleteListener(OnBookmarkDeleteListener listener) {
        this.deleteListener = listener;
    }

    /**
     * 设置添加书签监听器
     */
    public void setOnAddBookmarkListener(OnAddBookmarkListener listener) {
        this.addListener = listener;
    }

    /**
     * 书签跳转监听器
     */
    public interface OnBookmarkJumpListener {
        void onBookmarkJump(Bookmark bookmark);
    }

    /**
     * 书签删除监听器
     */
    public interface OnBookmarkDeleteListener {
        void onBookmarkDelete(Bookmark bookmark);
    }

    /**
     * 添加书签监听器
     */
    public interface OnAddBookmarkListener {
        void onAddBookmark();
    }
}
