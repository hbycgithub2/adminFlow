package com.hmall.tts.video.dto;

import com.hmall.tts.volcengine.dto.DialogSegment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 从音频生成视频请求
 * 
 * @author Kiro
 * @since 2026-08-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFromAudioRequest {
    
    /**
     * 字幕数据（JSON格式）
     * 如果提供，尝试使用；如果MP3被编辑，会自动重新对齐
     */
    private List<DialogSegment> subtitles;
    
    /**
     * 原始文本（用于重新对齐）
     * 如果没有字幕数据，但有原始文本，可以通过Whisper识别+对齐生成字幕
     */
    private String originalText;
    
    /**
     * 视频配置
     */
    private VideoConfig videoConfig;
    
    /**
     * 字幕配置
     */
    private SubtitleConfig subtitleConfig;
    
    /**
     * 是否强制重新对齐
     * 默认false：自动检测MP3是否变化
     * 设为true：强制重新对齐字幕
     */
    @Builder.Default
    private Boolean forceReAlign = false;
}
