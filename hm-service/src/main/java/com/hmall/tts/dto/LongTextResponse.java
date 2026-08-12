package com.hmall.tts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 长文本 TTS 响应结果
 * 
 * @author Kiro
 * @since 2026-08-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LongTextResponse {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 音频大小（字节）
     */
    private Long audioSize;
    
    /**
     * 文本总长度（字符）
     */
    private Integer totalTextLength;
    
    /**
     * 分段数量
     */
    private Integer segmentCount;
    
    /**
     * 总耗时（毫秒）
     */
    private Long totalDuration;
    
    /**
     * 音色
     */
    private String voice;
    
    public static LongTextResponse success(String message, long audioSize, int totalTextLength, 
                                           int segmentCount, long totalDuration, String voice) {
        return new LongTextResponse(true, message, audioSize, totalTextLength, segmentCount, totalDuration, voice);
    }
    
    public static LongTextResponse error(String message) {
        return new LongTextResponse(false, message, null, null, null, null, null);
    }
}
