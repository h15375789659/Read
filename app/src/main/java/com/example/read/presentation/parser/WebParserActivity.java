package com.example.read.presentation.parser;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.ChapterInfo;
import com.example.read.domain.model.NovelMetadata;
import com.example.read.domain.model.ParserRule;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 网站解析Activity - 从网站URL导入小说
 * 
 * 验证需求：2.1, 2.2, 2.3, 2.4, 2.5, 14.6
 */
@AndroidEntryPoint
public class WebParserActivity extends AppCompatActivity {

    private ParserViewModel viewModel;
    private ParserChapterAdapter chapterAdapter;

    // UI组件
    private Toolbar toolbar;
    private TextInputLayout urlInputLayout;
    private TextInputEditText editUrl;
    private Spinner spinnerRule;
    private MaterialButton btnParse;
    private LinearLayout parseProgressContainer;
    private TextView textParseStatus;
    private MaterialCardView cardNovelInfo;
    private TextView textNovelTitle;
    private TextView textNovelAuthor;
    private TextView textNovelDescription;
    private LinearLayout chapterListContainer;
    private TextView textChapterCount;
    private RecyclerView recyclerChapters;
    private LinearLayout downloadContainer;
    private MaterialButton btnDownload;
    private LinearLayout downloadProgressContainer;
    private TextView textDownloadProgress;
    private TextView textDownloadPercent;
    private ProgressBar progressDownload;
    private TextView textCurrentChapter;
    private MaterialButton btnCancelDownload;
    private TextView textError;

    // 解析规则列表
    private List<ParserRule> parserRules = new ArrayList<>();
    private ArrayAdapter<String> ruleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_web_parser);

        // 设置窗口边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinator_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initViewModel();
        setupListeners();
        observeData();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        urlInputLayout = findViewById(R.id.url_input_layout);
        editUrl = findViewById(R.id.edit_url);
        spinnerRule = findViewById(R.id.spinner_rule);
        btnParse = findViewById(R.id.btn_parse);
        parseProgressContainer = findViewById(R.id.parse_progress_container);
        textParseStatus = findViewById(R.id.text_parse_status);
        cardNovelInfo = findViewById(R.id.card_novel_info);
        textNovelTitle = findViewById(R.id.text_novel_title);
        textNovelAuthor = findViewById(R.id.text_novel_author);
        textNovelDescription = findViewById(R.id.text_novel_description);
        chapterListContainer = findViewById(R.id.chapter_list_container);
        textChapterCount = findViewById(R.id.text_chapter_count);
        recyclerChapters = findViewById(R.id.recycler_chapters);
        downloadContainer = findViewById(R.id.download_container);
        btnDownload = findViewById(R.id.btn_download);
        downloadProgressContainer = findViewById(R.id.download_progress_container);
        textDownloadProgress = findViewById(R.id.text_download_progress);
        textDownloadPercent = findViewById(R.id.text_download_percent);
        progressDownload = findViewById(R.id.progress_download);
        textCurrentChapter = findViewById(R.id.text_current_chapter);
        btnCancelDownload = findViewById(R.id.btn_cancel_download);
        textError = findViewById(R.id.text_error);

        // 设置Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 设置章节列表
        chapterAdapter = new ParserChapterAdapter();
        recyclerChapters.setLayoutManager(new LinearLayoutManager(this));
        recyclerChapters.setAdapter(chapterAdapter);

        // 设置规则Spinner适配器
        ruleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        ruleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRule.setAdapter(ruleAdapter);
    }

    /**
     * 初始化ViewModel
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ParserViewModel.class);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 返回按钮
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // URL输入监听
        editUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.updateUrl(s.toString().trim());
            }
        });

        // 规则选择监听
        spinnerRule.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < parserRules.size()) {
                    viewModel.selectRule(parserRules.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 解析按钮
        btnParse.setOnClickListener(v -> {
            if (validateInput()) {
                viewModel.parseNovelMetadata();
            }
        });

        // 下载按钮
        btnDownload.setOnClickListener(v -> viewModel.startDownload());

        // 取消下载按钮
        btnCancelDownload.setOnClickListener(v -> {
            viewModel.cancelDownload();
            Toast.makeText(this, R.string.web_parser_download_canceled, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 观察数据变化
     */
    private void observeData() {
        // 观察解析规则
        viewModel.getRulesLiveData().observe(this, rules -> {
            parserRules.clear();
            List<String> ruleNames = new ArrayList<>();
            
            if (rules != null && !rules.isEmpty()) {
                parserRules.addAll(rules);
                for (ParserRule rule : rules) {
                    ruleNames.add(rule.getName());
                }
            } else {
                ruleNames.add(getString(R.string.web_parser_no_rules));
            }
            
            ruleAdapter.clear();
            ruleAdapter.addAll(ruleNames);
            ruleAdapter.notifyDataSetChanged();
            
            // 更新ViewModel中的规则列表
            viewModel.updateRules(rules);
        });

        // 观察UI状态
        viewModel.getUiState().observe(this, this::updateUi);
    }

    /**
     * 更新UI状态
     */
    private void updateUi(ParserUiState state) {
        // 更新URL验证状态
        if (!state.isUrlValid() && !state.getUrl().isEmpty()) {
            urlInputLayout.setError(getString(R.string.web_parser_invalid_url));
        } else {
            urlInputLayout.setError(null);
        }

        // 更新解析进度
        boolean isParsing = state.isParsing();
        parseProgressContainer.setVisibility(isParsing ? View.VISIBLE : View.GONE);
        btnParse.setEnabled(!isParsing && !state.isDownloading());

        // 更新解析状态文本
        switch (state.getParseState()) {
            case PARSING_META:
                textParseStatus.setText(R.string.web_parser_parsing_meta);
                break;
            case PARSING_LIST:
                textParseStatus.setText(R.string.web_parser_parsing_list);
                break;
            default:
                textParseStatus.setText(R.string.web_parser_parsing);
                break;
        }

        // 更新小说信息卡片
        NovelMetadata metadata = state.getNovelMetadata();
        if (metadata != null) {
            cardNovelInfo.setVisibility(View.VISIBLE);
            textNovelTitle.setText(metadata.getTitle());
            textNovelAuthor.setText(getString(R.string.web_parser_author, 
                    metadata.getAuthor() != null && !metadata.getAuthor().isEmpty() 
                            ? metadata.getAuthor() : getString(R.string.unknown_author)));
            
            String description = metadata.getDescription();
            if (description != null && !description.isEmpty()) {
                textNovelDescription.setVisibility(View.VISIBLE);
                textNovelDescription.setText(description);
            } else {
                textNovelDescription.setVisibility(View.GONE);
            }
        } else {
            cardNovelInfo.setVisibility(View.GONE);
        }

        // 更新章节列表
        List<ChapterInfo> chapters = state.getChapters();
        if (chapters != null && !chapters.isEmpty()) {
            chapterListContainer.setVisibility(View.VISIBLE);
            textChapterCount.setText(getString(R.string.web_parser_chapter_count, chapters.size()));
            chapterAdapter.submitList(chapters);
            
            // 显示下载区域
            downloadContainer.setVisibility(View.VISIBLE);
        } else {
            chapterListContainer.setVisibility(View.GONE);
            downloadContainer.setVisibility(View.GONE);
        }

        // 更新下载状态
        boolean isDownloading = state.isDownloading();
        btnDownload.setVisibility(isDownloading ? View.GONE : View.VISIBLE);
        downloadProgressContainer.setVisibility(isDownloading ? View.VISIBLE : View.GONE);

        // 更新下载进度
        ParserUiState.DownloadProgress progress = state.getDownloadProgress();
        if (progress != null) {
            textDownloadProgress.setText(getString(R.string.web_parser_downloading, 
                    progress.getCurrent(), progress.getTotal()));
            textDownloadPercent.setText(getString(R.string.web_parser_download_percent, 
                    progress.getProgressPercent()));
            progressDownload.setProgress(progress.getProgressPercent());
            
            String currentChapter = progress.getCurrentChapterTitle();
            if (currentChapter != null && !currentChapter.isEmpty()) {
                textCurrentChapter.setVisibility(View.VISIBLE);
                textCurrentChapter.setText(getString(R.string.web_parser_current_chapter, currentChapter));
            } else {
                textCurrentChapter.setVisibility(View.GONE);
            }
        }

        // 更新错误信息
        String error = state.getError();
        if (error != null && !error.isEmpty()) {
            textError.setVisibility(View.VISIBLE);
            textError.setText(error);
        } else {
            textError.setVisibility(View.GONE);
        }
        
        // 显示断点续传对话框
        if (state.isShowResumeDialog()) {
            showResumeDownloadDialog(state);
        }

        // 下载完成处理
        if (state.getParseState() == ParserUiState.ParseState.COMPLETED) {
            Toast.makeText(this, R.string.web_parser_download_complete, Toast.LENGTH_SHORT).show();
            finish(); // 返回书架
        }
    }

    /**
     * 验证输入
     */
    private boolean validateInput() {
        ParserUiState state = viewModel.getUiState().getValue();
        if (state == null) return false;

        // 验证URL
        if (!state.isUrlValid()) {
            Toast.makeText(this, R.string.web_parser_invalid_url, Toast.LENGTH_SHORT).show();
            return false;
        }

        // 验证规则
        if (state.getSelectedRule() == null || parserRules.isEmpty()) {
            Toast.makeText(this, R.string.web_parser_select_rule, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
    
    // 用于防止对话框重复显示
    private androidx.appcompat.app.AlertDialog resumeDialog;
    
    /**
     * 显示断点续传对话框
     */
    private void showResumeDownloadDialog(ParserUiState state) {
        // 如果对话框已经在显示，不重复创建
        if (resumeDialog != null && resumeDialog.isShowing()) {
            return;
        }
        
        // 先关闭状态中的标志，防止重复触发
        viewModel.dismissResumeDialog();
        
        String title = state.getExistingNovelTitle();
        int downloaded = state.getExistingDownloadedCount();
        int total = state.getChapters() != null ? state.getChapters().size() : 0;
        boolean isComplete = state.isExistingNovelComplete();
        
        String dialogTitle;
        String message;
        
        if (isComplete) {
            // 小说已完成下载
            dialogTitle = "小说已存在";
            message = String.format("《%s》已下载完成（%d章），是否重新下载？", 
                    title != null ? title : "未知", downloaded);
        } else {
            // 小说未完成下载
            dialogTitle = "发现未完成的下载";
            message = String.format("发现《%s》已下载 %d/%d 章，是否继续下载？", 
                    title != null ? title : "未知", downloaded, total);
        }
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(dialogTitle)
                .setMessage(message)
                .setCancelable(true)
                .setOnCancelListener(dialog -> {
                    // 用户点击外部或返回键关闭对话框
                    resumeDialog = null;
                })
                .setOnDismissListener(dialog -> {
                    resumeDialog = null;
                });
        
        if (isComplete) {
            // 已完成：只显示"重新下载"和"取消"
            builder.setPositiveButton("重新下载", (dialog, which) -> {
                viewModel.restartDownload();
            })
            .setNegativeButton("取消", (dialog, which) -> {
                // 对话框会自动关闭
            });
        } else {
            // 未完成：显示"继续下载"、"重新下载"和"取消"
            builder.setPositiveButton("继续下载", (dialog, which) -> {
                viewModel.resumeDownload();
            })
            .setNegativeButton("重新下载", (dialog, which) -> {
                viewModel.restartDownload();
            })
            .setNeutralButton("取消", (dialog, which) -> {
                // 对话框会自动关闭
            });
        }
        
        resumeDialog = builder.create();
        resumeDialog.show();
    }

    @Override
    public void onBackPressed() {
        // 如果正在下载，提示用户
        ParserUiState state = viewModel.getUiState().getValue();
        if (state != null && state.isDownloading()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.web_parser_title)
                    .setMessage("正在下载中，确定要取消吗？")
                    .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                        viewModel.cancelDownload();
                        super.onBackPressed();
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}
