package com.example.read.presentation.blockedword;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.BlockedWord;
import com.example.read.domain.repository.BlockedWordRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 屏蔽词管理Activity
 * 
 * 验证需求：11.1, 11.2, 11.3
 */
@AndroidEntryPoint
public class BlockedWordActivity extends AppCompatActivity {

    public static final String EXTRA_NOVEL_ID = "novel_id";
    public static final String EXTRA_NOVEL_TITLE = "novel_title";

    @Inject
    BlockedWordRepository blockedWordRepository;

    private long novelId;
    private String novelTitle;

    // UI组件
    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private FloatingActionButton fabAdd;
    private TextView tvTitle;
    private ImageButton btnBack;

    private BlockedWordAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_blocked_word);

        // 设置窗口边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // 获取传入的小说ID和标题
        novelId = getIntent().getLongExtra(EXTRA_NOVEL_ID, -1);
        novelTitle = getIntent().getStringExtra(EXTRA_NOVEL_TITLE);

        if (novelId == -1) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadBlockedWords();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.empty_view);
        fabAdd = findViewById(R.id.fab_add);
        tvTitle = findViewById(R.id.tv_title);
        btnBack = findViewById(R.id.btn_back);

        // 设置标题（显示小说名称）
        if (novelTitle != null && !novelTitle.isEmpty()) {
            tvTitle.setText(getString(R.string.blocked_word_title) + " - " + novelTitle);
        }

        // 设置RecyclerView
        adapter = new BlockedWordAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        // 添加按钮
        fabAdd.setOnClickListener(v -> showAddDialog());

        // 删除按钮
        adapter.setOnDeleteClickListener(this::showDeleteConfirmDialog);
    }

    /**
     * 加载屏蔽词列表
     */
    private void loadBlockedWords() {
        blockedWordRepository.getBlockedWordsByNovelId(novelId).observe(this, words -> {
            adapter.submitList(words);
            updateEmptyView(words);
        });
    }

    /**
     * 更新空状态视图
     */
    private void updateEmptyView(List<BlockedWord> words) {
        boolean isEmpty = words == null || words.isEmpty();
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    /**
     * 显示添加屏蔽词对话框
     */
    private void showAddDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_blocked_word, null);
        EditText editWord = view.findViewById(R.id.edit_word);

        new AlertDialog.Builder(this)
                .setTitle(R.string.blocked_word_add_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    String word = editWord.getText().toString().trim();
                    if (word.isEmpty()) {
                        Toast.makeText(this, R.string.blocked_word_empty_input, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addBlockedWord(word);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    /**
     * 添加屏蔽词
     */
    private void addBlockedWord(String word) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // 检查是否已存在
            List<String> existingWords = blockedWordRepository.getBlockedWordStringsByNovelId(novelId);
            if (existingWords.contains(word)) {
                runOnUiThread(() -> 
                    Toast.makeText(this, R.string.blocked_word_exists, Toast.LENGTH_SHORT).show());
                return;
            }

            // 添加屏蔽词
            long id = blockedWordRepository.insertBlockedWord(novelId, word);
            runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(this, R.string.blocked_word_added, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(BlockedWord word) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_delete)
                .setMessage(R.string.blocked_word_delete_confirm)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> deleteBlockedWord(word))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    /**
     * 删除屏蔽词
     */
    private void deleteBlockedWord(BlockedWord word) {
        Executors.newSingleThreadExecutor().execute(() -> {
            blockedWordRepository.deleteBlockedWord(word.getId());
            runOnUiThread(() -> 
                Toast.makeText(this, R.string.blocked_word_deleted, Toast.LENGTH_SHORT).show());
        });
    }
}
