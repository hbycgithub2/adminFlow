package com.hmall.tts.volcengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS 响应结果
 * 
 * @author Kiro
 * @since 2026-08-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TTSResponse {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 音频文件路径
     */
    private String audioPath;
    
    /**
     * 音频URL（可访问的URL）
     */
    private String audioUrl;
    
    /**
     * 音频数据（Base64编码）
     */
    private String audioData;
    
    /**
     * 音频文件大小（字节）
     */
    private Long audioSize;
    
    /**
     * 音频时长（秒）
     */
    private Double duration;
    
    /**
     * 生成时间（毫秒）
     */
    private Long generateTime;
    
    /**
     * 错误码
     */
    private Integer errorCode;
    
    /**
     * 创建成功响应
     */
    public static TTSResponse success(String audioPath, String audioUrl, Long audioSize) {
        return TTSResponse.builder()
                .success(true)
                .message("语音生成成功")
                .audioPath(audioPath)
                .audioUrl(audioUrl)
                .audioSize(audioSize)
                .build();
    }
    
    /**
     * 创建失败响应
     */
    public static TTSResponse fail(String message, Integer errorCode) {
        return TTSResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
