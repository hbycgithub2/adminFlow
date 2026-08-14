package com.hmall.tts.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoConfig {
    
    /**
     * 视频宽度
     */
    @Builder.Default
    private Integer width = 1920;
    
    /**
     * 视频高度
     */
    @Builder.Default
    private Integer height = 1080;
    
    /**
     * 帧率
     */
    @Builder.Default
    private Integer fps = 30;
    
    /**
     * 背景颜色（HEX格式：#FFFFFF）
     */
    @Builder.Default
    private String backgroundColor = "#FFFFFF";
    
    /**
     * 背景图片路径（可选）
     */
    private String backgroundImagePath;
    
    /**
     * 视频比特率（kbps）
     */
    @Builder.Default
    private Integer bitrate = 2000;
    
    /**
     * 视频编码格式
     */
    @Builder.Default
    private String codec = "libx264";
    
    /**
     * 音频编码格式
     */
    @Builder.Default
    private String audioCodec = "aac";
    
    /**
     * 将HEX颜色转换为FFmpeg格式
     */
    public String getBackgroundColorFFmpeg() {
        if (backgroundColor == null || !backgroundColor.startsWith("#")) {
            return "white";
        }
        return backgroundColor; // FFmpeg直接支持#RRGGBB格式
    }
}
