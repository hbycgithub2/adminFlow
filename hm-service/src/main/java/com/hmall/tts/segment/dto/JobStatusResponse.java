package com.hmall.tts.segment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 任务状态响应
 * 
 * @author Kiro
 * @since 2026-08-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务ID
     */
    private String jobId;
    
    /**
     * 任务状态
     * pending: 等待中
     * processing: 处理中
     * completed: 已完成
     * failed: 失败
     */
    private String status;
    
    /**
     * 进度百分比（0-100）
     */
    private Integer progress;
    
    /**
     * 当前步骤描述
     */
    private String currentStep;
    
    /**
     * 视频URL（完成后才有）
     */
    private String videoUrl;
    
    /**
     * 错误信息（失败时才有）
     */
    private String errorMessage;
}
