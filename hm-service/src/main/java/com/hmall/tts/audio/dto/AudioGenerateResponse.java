package com.hmall.tts.audio.dto;

import com.hmall.tts.volcengine.dto.DialogSegment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 音频生成响应
 * 
 * @author Kiro
 * @since 2026-08-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioGenerateResponse {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 音频URL
     */
    private String audioUrl;
    
    /**
     * 音频大小（字节）
     */
    private Long audioSize;
    
    /**
     * 音频时长（秒）
     */
    private Double duration;
    
    /**
     * 字幕数据（DialogSegments，包含时间戳）
     */
    private List<DialogSegment> subtitles;
    
    /**
     * 生成耗时（毫秒）
     */
    private Long generateTime;
    
    /**
     * 成功响应构建器
     */
    public static AudioGenerateResponse success(String taskId, String audioUrl, Long audioSize, 
                                                Double duration, List<DialogSegment> subtitles, Long generateTime) {
        return AudioGenerateResponse.builder()
                .success(true)
                .message("音频生成成功")
                .taskId(taskId)
                .audioUrl(audioUrl)
                .audioSize(audioSize)
                .duration(duration)
                .subtitles(subtitles)
                .generateTime(generateTime)
                .build();
    }
    
    /**
     * 失败响应构建器
     */
    public static AudioGenerateResponse fail(String message) {
        return AudioGenerateResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
