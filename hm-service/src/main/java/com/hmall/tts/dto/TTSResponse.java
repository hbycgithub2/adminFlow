package com.hmall.tts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS 响应结果
 * 
 * @author Kiro
 * @since 2026-08-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TTSResponse {
    
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
     * 文本长度（字符）
     */
    private Integer textLength;
    
    /**
     * 耗时（毫秒）
     */
    private Long duration;
    
    /**
     * 音色
     */
    private String voice;
    
    public static TTSResponse success(String message, long audioSize, int textLength, long duration, String voice) {
        return new TTSResponse(true, message, audioSize, textLength, duration, voice);
    }
    
    public static TTSResponse error(String message) {
        return new TTSResponse(false, message, null, null, null, null);
    }
}
