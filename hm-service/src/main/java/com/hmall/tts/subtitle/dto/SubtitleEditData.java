package com.hmall.tts.subtitle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 字幕编辑数据DTO
 * 
 * <p>用于前端加载字幕数据</p>
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleEditData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务ID
     * <p>视频生成任务的唯一标识符</p>
     */
    private String taskId;
    
    /**
     * 字幕片段列表
     * <p>按时间顺序排列</p>
     */
    private List<SubtitleSegment> subtitles;
    
    /**
     * 视频总时长（秒）
     * <p>用于前端显示和验证</p>
     */
    private Double totalDuration;
    
    /**
     * 视频URL
     * <p>用于前端预览视频</p>
     */
    private String videoUrl;
}
