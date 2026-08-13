package com.hmall.tts.volcengine.asr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ASR识别结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ASRResult {
    
    /**
     * 词级别时间戳列表
     */
    private List<WordTiming> wordTimings;
    
    /**
     * 音频总时长（秒）
     */
    private Double duration;
    
    /**
     * 识别文本（完整）
     */
    private String text;
    
    /**
     * 是否识别成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}
