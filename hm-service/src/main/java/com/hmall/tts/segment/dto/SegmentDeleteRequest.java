package com.hmall.tts.segment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 段落删除请求
 * 
 * @author Kiro
 * @since 2026-08-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentDeleteRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 要删除的段落序号（0-based）
     */
    private Integer segmentIndex;
    
    /**
     * 是否立即重新生成视频
     */
    @Builder.Default
    private Boolean regenerateVideo = true;
}
