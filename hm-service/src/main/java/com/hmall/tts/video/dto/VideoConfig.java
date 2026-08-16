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
     * 帧率（降低到24fps，减少编码负担）
     */
    @Builder.Default
    private Integer fps = 24;  // 从30降到24，减少20%编码时间
    
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
     * 视频比特率（kbps）（GPU编码可以用更低的码率）
     */
    @Builder.Default
    private Integer bitrate = 1500;  // 从2000降到1500，GPU编码质量仍很好
    
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
