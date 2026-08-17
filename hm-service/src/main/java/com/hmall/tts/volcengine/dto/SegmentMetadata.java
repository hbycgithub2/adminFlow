package com.hmall.tts.volcengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分段元数据
 * 
 * 用于保存每个音频片段的详细信息，支持局部编辑
 * 
 * @author Kiro
 * @since 2026-08-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentMetadata implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 段落序号（0-based）
     */
    private Integer index;
    
    /**
     * 文本内容
     */
    private String text;
    
    /**
     * 是否加粗
     */
    private Boolean isBold;
    
    /**
     * 音色ID
     */
    private String voiceId;
    
    /**
     * 开始时间（秒，绝对时间）
     */
    private Double startTime;
    
    /**
     * 结束时间（秒，绝对时间）
     */
    private Double endTime;
    
    /**
     * 时长（秒）
     */
    private Double duration;
    
    /**
     * 原始音频数据（Base64编码）
     * 用于局部编辑时重新合并
     */
    private String audioDataBase64;
    
    /**
     * 是否需要停顿
     */
    private Boolean needPause;
    
    /**
     * 停顿时长（毫秒）
     */
    private Integer pauseDuration;
}
