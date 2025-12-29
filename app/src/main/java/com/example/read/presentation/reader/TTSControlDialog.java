package com.example.read.presentation.reader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.read.R;
import com.example.read.domain.model.ParagraphInfo;
import com.example.read.domain.model.TTSState;
import com.example.read.domain.model.VoiceInfo;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * TTS控制面板Dialog
 * 提供段落选择、播放控制、语速调节、音色选择功能
 * 
 * 验证需求：10.1, 10.2, 10.3, 10.4
 */
public class TTSControlDialog extends BottomSheetDialogFragment {

    private RecyclerView paragraphList;
    private ParagraphAdapter paragraphAdapter;
    private ImageButton btnPlayPause;
    private SeekBar speedSeekbar;
    private TextView speedValue;
    private Spinner voiceSpinner;

    private TTSControlListener listener;
    private List<ParagraphInfo> paragraphs = new ArrayList<>();
    private List<VoiceInfo> availableVoices = new ArrayList<>();
    private TTSState currentTTSState;
    private float currentSpeed = 1.0f;
    private int selectedVoiceIndex = 0;

    /**
     * TTS控制监听器接口
     */
    public interface TTSControlListener {
        void onParagraphSelected(ParagraphInfo paragraph);
        void onPlayPause();
        void onSpeedChanged(float speed);
        void onVoiceChanged(String voiceId);
    }

    public static TTSControlDialog newInstance() {
        return new TTSControlDialog();
    }

    public void setListener(TTSControlListener listener) {
        this.listener = listener;
    }

    public void setParagraphs(List<ParagraphInfo> paragraphs) {
        this.paragraphs = paragraphs != null ? paragraphs : new ArrayList<>();
        if (paragraphAdapter != null) {
            paragraphAdapter.setParagraphs(this.paragraphs);
        }
    }

    public void setAvailableVoices(List<VoiceInfo> voices) {
        this.availableVoices = voices != null ? voices : new ArrayList<>();
        updateVoiceSpinner();
    }

    public void setTTSState(TTSState state) {
        this.currentTTSState = state;
        updatePlayPauseButton();
        if (state != null) {
            this.currentSpeed = state.getSpeechRate();
            updateSpeedUI();
        }
    }

    /**
     * 更新正在朗读的位置
     * @param textPosition 在原文中的字符位置
     */
    public void updateReadingPosition(int textPosition) {
        if (paragraphAdapter != null) {
            paragraphAdapter.updateReadingPositionByTextPosition(textPosition);
            
            // 自动滚动到正在朗读的段落
            int readingPosition = paragraphAdapter.getReadingPosition();
            if (readingPosition >= 0 && paragraphList != null) {
                paragraphList.smoothScrollToPosition(readingPosition);
            }
        }
    }

    /**
     * 清除朗读高亮
     */
    public void clearReadingHighlight() {
        if (paragraphAdapter != null) {
            paragraphAdapter.setReadingPosition(-1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_tts_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
        updateUI();
    }

    private void initViews(View view) {
        paragraphList = view.findViewById(R.id.paragraph_list);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        speedSeekbar = view.findViewById(R.id.speed_seekbar);
        speedValue = view.findViewById(R.id.speed_value);
        voiceSpinner = view.findViewById(R.id.voice_spinner);

        // 设置段落列表
        paragraphAdapter = new ParagraphAdapter();
        paragraphList.setLayoutManager(new LinearLayoutManager(getContext()));
        paragraphList.setAdapter(paragraphAdapter);
        paragraphAdapter.setParagraphs(paragraphs);
    }

    private void setupListeners() {
        // 段落选择
        paragraphAdapter.setOnParagraphClickListener(paragraph -> {
            if (listener != null) {
                listener.onParagraphSelected(paragraph);
            }
        });

        // 播放/暂停按钮
        btnPlayPause.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayPause();
            }
        });

        // 语速调节
        speedSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // progress: 0-150 对应 0.5x-2.0x
                    currentSpeed = 0.5f + (progress / 100f);
                    updateSpeedText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (listener != null) {
                    listener.onSpeedChanged(currentSpeed);
                }
            }
        });

        // 音色选择
        voiceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != selectedVoiceIndex && position < availableVoices.size()) {
                    selectedVoiceIndex = position;
                    if (listener != null) {
                        listener.onVoiceChanged(availableVoices.get(position).getVoiceId());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateUI() {
        updatePlayPauseButton();
        updateSpeedUI();
        updateVoiceSpinner();
    }

    private void updatePlayPauseButton() {
        if (btnPlayPause == null) return;
        
        if (currentTTSState != null && currentTTSState.isPlaying()) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void updateSpeedUI() {
        if (speedSeekbar == null || speedValue == null) return;
        
        // 将速度转换为进度值 (0.5-2.0 -> 0-150)
        int progress = (int) ((currentSpeed - 0.5f) * 100);
        speedSeekbar.setProgress(Math.max(0, Math.min(150, progress)));
        updateSpeedText();
    }

    private void updateSpeedText() {
        if (speedValue != null) {
            speedValue.setText(getString(R.string.tts_speed_value, currentSpeed));
        }
    }

    private void updateVoiceSpinner() {
        if (voiceSpinner == null || getContext() == null) return;
        
        List<String> voiceNames = new ArrayList<>();
        for (VoiceInfo voice : availableVoices) {
            voiceNames.add(voice.getName());
        }
        
        if (voiceNames.isEmpty()) {
            voiceNames.add("默认语音");
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                voiceNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceSpinner.setAdapter(adapter);
        
        // 设置当前选中的语音
        if (currentTTSState != null && currentTTSState.getCurrentVoiceId() != null) {
            for (int i = 0; i < availableVoices.size(); i++) {
                if (availableVoices.get(i).getVoiceId().equals(currentTTSState.getCurrentVoiceId())) {
                    selectedVoiceIndex = i;
                    voiceSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    /**
     * 从文本内容解析段落列表
     * @param content 章节内容
     * @return 段落列表
     */
    public static List<ParagraphInfo> parseParagraphs(String content) {
        List<ParagraphInfo> paragraphs = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return paragraphs;
        }

        // 按换行符分割段落
        String[] lines = content.split("\n");
        int position = 0;
        int paragraphIndex = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                // 创建段落信息
                String preview = ParagraphInfo.createPreview(trimmed, 50);
                ParagraphInfo paragraph = new ParagraphInfo(
                        preview,
                        trimmed,
                        position,
                        paragraphIndex
                );
                paragraphs.add(paragraph);
                paragraphIndex++;
            }
            position += line.length() + 1; // +1 for \n
        }

        return paragraphs;
    }
}
