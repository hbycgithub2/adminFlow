package com.hmall.tts.volcengine.asr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 词级别时间戳（ASR识别结果）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordTiming {
    
    /**
     * 词文本
     */
    private String word;
    
    /**
     * 开始时间（秒）
     */
    private Double startTime;
    
    /**
     * 结束时间（秒）
     */
    private Double endTime;
    
    /**
     * 识别置信度（0-1）
     */
    private Double confidence;
    
    /**
     * 获取词的时长
     */
    public Double getDuration() {
        return endTime - startTime;
    }
}
