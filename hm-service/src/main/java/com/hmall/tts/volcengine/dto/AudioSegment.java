package com.hmall.tts.volcengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 音频片段
 * 包含音频数据和对应的文本片段信息
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AudioSegment {
    
    /**
     * 音频数据（字节数组）
     */
    private byte[] audioData;
    
    /**
     * 对应的合并片段
     */
    private MergedSegment mergedSegment;
    
    /**
     * 顺序
     */
    private Integer order;
    
    /**
     * 是否需要停顿
     */
    private Boolean needPause;
    
    /**
     * 停顿时长（毫秒）
     */
    private Integer pauseDuration;
    
    public AudioSegment(byte[] audioData, MergedSegment mergedSegment) {
        this.audioData = audioData;
        this.mergedSegment = mergedSegment;
        this.order = mergedSegment.getOrder();
    }
}
