package com.hmall.tts.whisperx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WhisperX字符时间戳
 * 
 * @author Kiro
 * @since 2026-08-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharTimestamp {
    
    /**
     * 字符内容
     */
    private String character;
    
    /**
     * 开始时间（秒）
     */
    private Double startTime;
    
    /**
     * 结束时间（秒）
     */
    private Double endTime;
    
    /**
     * 时长（秒）
     */
    public Double getDuration() {
        if (startTime != null && endTime != null) {
            return endTime - startTime;
        }
        return 0.0;
    }
}
