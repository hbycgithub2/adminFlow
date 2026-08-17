package com.hmall.tts.volcengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 任务元数据
 * 
 * 保存视频生成任务的完整信息，支持局部编辑
 * 
 * @author Kiro
 * @since 2026-08-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskMetadata implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 最后更新时间
     */
    private Long updateTime;
    
    /**
     * 分段列表
     */
    private List<SegmentMetadata> segments;
    
    /**
     * 总时长（秒）
     */
    private Double totalDuration;
    
    /**
     * 音色配置
     */
    private VoiceConfig voiceConfig;
    
    /**
     * 视频配置（可选）
     */
    private com.hmall.tts.video.dto.VideoConfig videoConfig;
    
    /**
     * 字幕配置（可选）
     */
    private com.hmall.tts.video.dto.SubtitleConfig subtitleConfig;
    
    /**
     * 完整音频文件路径（用于局部编辑时重新合并）
     */
    private String fullAudioPath;
}
