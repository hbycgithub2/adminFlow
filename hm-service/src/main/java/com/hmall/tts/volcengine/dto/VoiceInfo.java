package com.hmall.tts.volcengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 音色信息
 * 
 * @author Kiro
 * @since 2026-08-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceInfo {
    
    /**
     * 音色ID
     */
    private String voiceId;
    
    /**
     * 音色名称（中文）
     */
    private String voiceName;
    
    /**
     * 音色描述
     */
    private String description;
    
    /**
     * 性别（male/female）
     */
    private String gender;
    
    /**
     * 语言（zh-CN/en-US）
     */
    private String language;
    
    /**
     * 音色风格（如：calm/energetic/professional）
     */
    private String style;
    
    /**
     * 是否推荐
     */
    private Boolean recommended;
}
