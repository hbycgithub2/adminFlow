package com.hmall.tts.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 视频生成响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenerateResponse {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 视频URL
     */
    private String videoUrl;
    
    /**
     * 音频URL（新增：用于下载MP3）⭐
     */
    private String audioUrl;
    
    /**
     * 视频时长（秒）
     */
    private Double duration;
    
    /**
     * 是否重新对齐（仅从音频生成视频时有效）
     */
    private Boolean reAligned;
    
    /**
     * 视频大小（字节）
     */
    private Long videoSize;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 字幕列表（供前端预览）
     */
    private List<SubtitleSegment> subtitles;
}
