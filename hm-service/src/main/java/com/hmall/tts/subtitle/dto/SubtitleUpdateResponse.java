package com.hmall.tts.subtitle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 字幕更新响应DTO
 * 
 * <p>用于返回字幕更新结果</p>
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleUpdateResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 操作是否成功
     */
    private Boolean success;
    
    /**
     * 操作结果消息
     * <p>中文友好提示</p>
     */
    private String message;
    
    /**
     * 新视频URL
     * <p>
     * 仅当regenerateVideo=true且重新生成成功时返回<br>
     * 例如: /tts/videos/uuid.mp4
     * </p>
     */
    private String videoUrl;
    
    /**
     * 视频生成耗时（秒）
     * <p>仅当重新生成视频时返回</p>
     */
    private Double duration;
    
    /**
     * 创建成功响应
     * @param message 消息
     * @return 响应对象
     */
    public static SubtitleUpdateResponse success(String message) {
        return SubtitleUpdateResponse.builder()
                .success(true)
                .message(message)
                .build();
    }
    
    /**
     * 创建成功响应（带视频URL）
     * @param message 消息
     * @param videoUrl 视频URL
     * @return 响应对象
     */
    public static SubtitleUpdateResponse success(String message, String videoUrl) {
        return SubtitleUpdateResponse.builder()
                .success(true)
                .message(message)
                .videoUrl(videoUrl)
                .build();
    }
    
    /**
     * 创建失败响应
     * @param message 错误消息
     * @return 响应对象
     */
    public static SubtitleUpdateResponse failure(String message) {
        return SubtitleUpdateResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
