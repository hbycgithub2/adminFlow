package com.hmall.tts.subtitle.service;

import com.hmall.tts.volcengine.dto.DialogSegment;

import java.util.List;

/**
 * 字幕对齐服务接口
 * 
 * 职责：处理字幕对齐逻辑，支持多种对齐策略
 * 
 * @author Kiro
 * @since 2026-08-16
 */
public interface SubtitleAlignmentService {
    
    /**
     * 智能对齐字幕
     * 
     * 策略（三层降级）：
     * 1. 如果有原始字幕且MP3未变化 → 直接使用原始字幕
     * 2. 如果有原始字幕但MP3变化 → 调用WhisperX重新对齐
     * 3. 如果无原始字幕 → 调用Whisper识别 + WhisperX对齐
     * 
     * @param audioData 音频数据（字节数组）
     * @param originalSubtitles 原始字幕数据（可选）
     * @param originalText 原始文本（可选，用于重新对齐）
     * @param forceReAlign 是否强制重新对齐
     * @return 对齐后的字幕列表
     * @throws Exception 对齐失败时抛出异常
     */
    List<DialogSegment> alignSubtitles(
            byte[] audioData,
            List<DialogSegment> originalSubtitles,
            String originalText,
            boolean forceReAlign
    ) throws Exception;
    
    /**
     * 检测音频是否被修改
     * 
     * 通过比较音频时长判断是否被编辑
     * 
     * @param audioData 音频数据
     * @param originalSubtitles 原始字幕数据
     * @return true=音频被修改，false=未修改
     */
    boolean isAudioModified(byte[] audioData, List<DialogSegment> originalSubtitles);
}
