package com.hmall.tts.volcengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档TTS生成结果
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTTSResult {
    
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
     * 生成耗时（毫秒）
     */
    private Long generateTime;
    
    /**
     * 文本片段数量
     */
    private Integer segmentCount;
    
    /**
     * API调用次数
     */
    private Integer apiCallCount;
    
    /**
     * 对话片段列表（用于实时进度显示）
     */
    private List<DialogSegment> segments;
    
    /**
     * 音频总时长（秒）
     */
    private Double totalDuration;
    
    /**
     * 成功返回
     */
    public static DocumentTTSResult success(String taskId, String audioUrl, Long audioSize, Long generateTime) {
        return DocumentTTSResult.builder()
                .success(true)
                .message("文档TTS生成成功")
                .taskId(taskId)
                .audioUrl(audioUrl)
                .audioSize(audioSize)
                .generateTime(generateTime)
                .build();
    }
    
    /**
     * 成功返回（包含片段信息）
     */
    public static DocumentTTSResult success(String taskId, String audioUrl, Long audioSize, Long generateTime,
                                           List<DialogSegment> segments, Double totalDuration) {
        return DocumentTTSResult.builder()
                .success(true)
                .message("文档TTS生成成功")
                .taskId(taskId)
                .audioUrl(audioUrl)
                .audioSize(audioSize)
                .generateTime(generateTime)
                .segments(segments)
                .totalDuration(totalDuration)
                .build();
    }
    
    /**
     * 失败返回
     */
    public static DocumentTTSResult fail(String message) {
        return DocumentTTSResult.builder()
                .success(false)
                .message(message)
                .build();
    }
}
