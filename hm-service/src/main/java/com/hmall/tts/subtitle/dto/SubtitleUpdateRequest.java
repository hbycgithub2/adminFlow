package com.hmall.tts.subtitle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 字幕更新请求DTO
 * 
 * <p>用于前端提交字幕修改</p>
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleUpdateRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务ID
     * <p>视频生成任务的唯一标识符</p>
     */
    private String taskId;
    
    /**
     * 更新后的字幕列表
     * <p>包含所有修改</p>
     */
    private List<SubtitleSegment> subtitles;
    
    /**
     * 是否立即重新生成视频
     * <p>
     * true: 更新字幕后立即重新生成视频<br>
     * false: 仅保存字幕，不重新生成视频
     * </p>
     */
    @Builder.Default
    private Boolean regenerateVideo = false;
}
