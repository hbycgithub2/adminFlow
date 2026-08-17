package com.hmall.tts.segment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 段落编辑响应
 * 
 * @author Kiro
 * @since 2026-08-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentEditResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
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
     * 异步任务ID（用于查询进度）
     */
    private String jobId;
    
    /**
     * 预计耗时（秒）
     */
    private Integer estimatedTime;
    
    /**
     * 视频URL（完成后才有）
     */
    private String videoUrl;
    
    /**
     * 便捷构造方法
     */
    public static SegmentEditResponse success(String message, String taskId, String jobId) {
        return SegmentEditResponse.builder()
                .success(true)
                .message(message)
                .taskId(taskId)
                .jobId(jobId)
                .estimatedTime(12)  // 默认预计12秒
                .build();
    }
    
    public static SegmentEditResponse failure(String message) {
        return SegmentEditResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
