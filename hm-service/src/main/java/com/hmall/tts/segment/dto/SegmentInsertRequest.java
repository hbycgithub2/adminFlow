package com.hmall.tts.segment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 段落插入请求
 * 
 * @author Kiro
 * @since 2026-08-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentInsertRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 在哪个段落后插入（段落序号，0-based）
     * 例如：insertAfter=2 表示在第3段后插入
     */
    private Integer insertAfter;
    
    /**
     * 插入的文本内容
     */
    private String text;
    
    /**
     * 音色ID
     */
    private String voiceId;
    
    /**
     * 是否加粗
     */
    @Builder.Default
    private Boolean isBold = false;
    
    /**
     * 是否立即重新生成视频
     */
    @Builder.Default
    private Boolean regenerateVideo = true;
}
