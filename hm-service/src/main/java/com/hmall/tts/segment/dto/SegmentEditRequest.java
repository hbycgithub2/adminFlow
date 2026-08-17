package com.hmall.tts.segment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 段落编辑请求
 * 
 * @author Kiro
 * @since 2026-08-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentEditRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 段落序号（0-based）
     */
    private Integer segmentIndex;
    
    /**
     * 新文本内容
     */
    private String newText;
    
    /**
     * 新音色ID（可选，不传则使用原音色）
     */
    private String voiceId;
    
    /**
     * 是否加粗（可选，不传则使用原设置）
     */
    private Boolean isBold;
    
    /**
     * 是否立即重新生成视频
     */
    @Builder.Default
    private Boolean regenerateVideo = true;
}
