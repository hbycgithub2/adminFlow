package com.hmall.tts.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字幕片段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleSegment {
    
    /**
     * 字幕文本
     */
    private String text;
    
    /**
     * 开始时间（秒）
     */
    private Double startTime;
    
    /**
     * 持续时间（秒）
     */
    private Double duration;
    
    /**
     * 是否加粗（区分角色）
     */
    private Boolean isBold;
    
    /**
     * 说话人（可选）
     */
    private String speaker;
    
    /**
     * 获取结束时间
     */
    public Double getEndTime() {
        return startTime + duration;
    }
}
