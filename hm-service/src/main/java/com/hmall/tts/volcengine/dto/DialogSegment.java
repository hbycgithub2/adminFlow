package com.hmall.tts.volcengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 对话片段（用于实时进度显示）
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DialogSegment {
    
    /**
     * 片段序号（从0开始）
     */
    private Integer index;
    
    /**
     * 文本内容
     */
    private String text;
    
    /**
     * 是否加粗
     */
    private Boolean isBold;
    
    /**
     * 开始时间（秒）
     */
    private Double startTime;
    
    /**
     * 持续时间（秒）
     */
    private Double duration;
    
    /**
     * 使用的音色ID
     */
    private String voiceId;
    
    /**
     * 逐字时间戳（用于实现卡拉OK效果）
     */
    private List<CharTiming> charTimings;
}
