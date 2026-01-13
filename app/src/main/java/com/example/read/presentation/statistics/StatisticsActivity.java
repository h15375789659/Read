package com.example.read.presentation.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.StatisticsPeriod;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 阅读统计Activity
 * 
 * 验证需求：12.2, 12.3, 12.5, 12.6
 */
@AndroidEntryPoint
public class StatisticsActivity extends AppCompatActivity {

    private StatisticsViewModel viewModel;

    // UI组件
    private TextView tvTotalDuration;
    private MaterialButton btnDay;
    private MaterialButton btnWeek;
    private MaterialButton btnMonth;
    private BarChart chartDuration;
    private RecyclerView recyclerRanking;
    private TextView tvRankingEmpty;
    private ProgressBar loadingProgress;

    private NovelRankingAdapter rankingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statistics);

        // 设置窗口边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();
        initViewModel();
        setupListeners();
        setupChart();
        observeData();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        tvTotalDuration = findViewById(R.id.tv_total_duration);
        btnDay = findViewById(R.id.btn_day);
        btnWeek = findViewById(R.id.btn_week);
        btnMonth = findViewById(R.id.btn_month);
        chartDuration = findViewById(R.id.chart_duration);
        recyclerRanking = findViewById(R.id.recycler_ranking);
        tvRankingEmpty = findViewById(R.id.tv_ranking_empty);
        loadingProgress = findViewById(R.id.loading_progress);

        // 返回按钮
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 标题长按生成测试数据（调试用）
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setOnLongClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("生成测试数据")
                    .setMessage("是否为所有小说生成过去30天的随机阅读记录？\n\n此操作仅用于测试和演示。")
                    .setPositiveButton("生成", (dialog, which) -> {
                        Toast.makeText(this, "正在生成测试数据...", Toast.LENGTH_SHORT).show();
                        viewModel.generateTestData();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        // 设置排行榜RecyclerView
        rankingAdapter = new NovelRankingAdapter();
        recyclerRanking.setLayoutManager(new LinearLayoutManager(this));
        recyclerRanking.setAdapter(rankingAdapter);
    }

    /**
     * 初始化ViewModel
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        btnDay.setOnClickListener(v -> {
            viewModel.switchToDay();
            updatePeriodButtons(StatisticsPeriod.DAY);
        });

        btnWeek.setOnClickListener(v -> {
            viewModel.switchToWeek();
            updatePeriodButtons(StatisticsPeriod.WEEK);
        });

        btnMonth.setOnClickListener(v -> {
            viewModel.switchToMonth();
            updatePeriodButtons(StatisticsPeriod.MONTH);
        });
    }

    /**
     * 配置图表
     */
    private void setupChart() {
        // 基本配置
        chartDuration.getDescription().setEnabled(false);
        chartDuration.setDrawGridBackground(false);
        chartDuration.setDrawBarShadow(false);
        chartDuration.setHighlightFullBarEnabled(false);
        chartDuration.setDrawValueAboveBar(true);
        chartDuration.getLegend().setEnabled(false);
        chartDuration.setScaleEnabled(false);
        chartDuration.setPinchZoom(false);
        chartDuration.setDoubleTapToZoomEnabled(false);

        // X轴配置
        XAxis xAxis = chartDuration.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getColor(R.color.text_secondary));

        // Y轴配置
        YAxis leftAxis = chartDuration.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(getColor(R.color.text_secondary));

        YAxis rightAxis = chartDuration.getAxisRight();
        rightAxis.setEnabled(false);

        // 设置空数据
        chartDuration.setNoDataText(getString(R.string.statistics_no_data));
        chartDuration.setNoDataTextColor(getColor(R.color.text_hint));
    }

    /**
     * 观察数据变化
     */
    private void observeData() {
        viewModel.getUiState().observe(this, state -> {
            // 更新加载状态
            loadingProgress.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

            // 更新错误信息
            if (state.getError() != null && !state.getError().isEmpty()) {
                Toast.makeText(this, state.getError(), Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }

            // 更新总计数据
            tvTotalDuration.setText(state.getFormattedTotalDuration());

            // 更新周期按钮状态
            updatePeriodButtons(state.getCurrentPeriod());

            // 更新图表
            updateChart(state.getDurationChartData());

            // 更新排行榜
            rankingAdapter.submitList(state.getNovelRanking());
            boolean hasRanking = state.getNovelRanking() != null && !state.getNovelRanking().isEmpty();
            tvRankingEmpty.setVisibility(hasRanking ? View.GONE : View.VISIBLE);
            recyclerRanking.setVisibility(hasRanking ? View.VISIBLE : View.GONE);
        });
    }


    /**
     * 更新周期按钮状态
     */
    private void updatePeriodButtons(StatisticsPeriod period) {
        // 重置所有按钮样式
        btnDay.setBackgroundTintList(null);
        btnWeek.setBackgroundTintList(null);
        btnMonth.setBackgroundTintList(null);
        btnDay.setTextColor(getColor(R.color.primary));
        btnWeek.setTextColor(getColor(R.color.primary));
        btnMonth.setTextColor(getColor(R.color.primary));

        // 设置选中按钮样式
        MaterialButton selectedBtn;
        switch (period) {
            case DAY:
                selectedBtn = btnDay;
                break;
            case MONTH:
                selectedBtn = btnMonth;
                break;
            case WEEK:
            default:
                selectedBtn = btnWeek;
                break;
        }
        selectedBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.primary)));
        selectedBtn.setTextColor(Color.WHITE);
    }

    /**
     * 更新图表数据
     */
    private void updateChart(List<StatisticsUiState.ChartEntry> chartData) {
        if (chartData == null || chartData.isEmpty()) {
            chartDuration.clear();
            chartDuration.invalidate();
            return;
        }

        // 转换为BarEntry
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        for (StatisticsUiState.ChartEntry entry : chartData) {
            entries.add(new BarEntry(entry.getX(), entry.getY()));
            labels.add(entry.getLabel());
        }

        // 创建数据集
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(getColor(R.color.primary));
        dataSet.setValueTextColor(getColor(R.color.text_secondary));
        dataSet.setValueTextSize(10f);

        // 设置数据
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        chartDuration.setData(barData);

        // 设置X轴标签
        XAxis xAxis = chartDuration.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(Math.min(labels.size(), 7));

        // 刷新图表
        chartDuration.invalidate();
    }
}
