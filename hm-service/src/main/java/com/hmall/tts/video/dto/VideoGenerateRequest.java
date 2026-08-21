package com.hmall.tts.video.dto;

import com.hmall.tts.volcengine.dto.VoiceConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频生成请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenerateRequest {
    
    /**
     * 加粗文本音色（保留向后兼容）
     */
    private String boldVoice;
    
    /**
     * 非加粗文本音色（保留向后兼容）
     */
    private String normalVoice;
    
    /**
     * 完整的音色配置（支持多音色模式）
     * 如果提供了此字段，优先使用此字段，忽略boldVoice和normalVoice
     */
    private VoiceConfig voiceConfig;
    
    /**
     * 视频配置
     */
    private VideoConfig videoConfig;
    
    /**
     * 字幕配置
     */
    private SubtitleConfig subtitleConfig;
    
    /**
     * 音频格式
     */
    @Builder.Default
    private String audioFormat = "mp3";
    
    /**
     * 采样率
     */
    @Builder.Default
    private Integer sampleRate = 24000;
}
