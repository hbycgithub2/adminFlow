package com.hmall.tts.video.dto;

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
     * 加粗文本音色
     */
    private String boldVoice;
    
    /**
     * 非加粗文本音色
     */
    private String normalVoice;
    
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
