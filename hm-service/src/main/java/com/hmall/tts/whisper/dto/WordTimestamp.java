package com.hmall.tts.whisper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 词时间戳
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordTimestamp {
    
    /**
     * 文本内容
     */
    private String text;
    
    /**
     * 开始时间（秒）
     */
    private Double startTime;
    
    /**
     * 结束时间（秒）
     */
    private Double endTime;
    
    /**
     * 获取时长（秒）
     */
    public Double getDuration() {
        if (startTime == null || endTime == null) {
            return 0.0;
        }
        return endTime - startTime;
    }
}
