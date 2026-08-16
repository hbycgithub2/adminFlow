package com.hmall.tts.audio.dto;

import com.hmall.tts.volcengine.dto.VoiceConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 音频生成请求
 * 
 * @author Kiro
 * @since 2026-08-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioGenerateRequest {
    
    /**
     * 粗体文本音色（用于标题、重点内容）
     */
    @Builder.Default
    private String boldVoice = "zh_male_m191_uranus_bigtts";
    
    /**
     * 普通文本音色（用于正文）
     */
    @Builder.Default
    private String normalVoice = "zh_female_vv_uranus_bigtts";
    
    /**
     * 音频格式（mp3、wav等）
     */
    @Builder.Default
    private String audioFormat = "mp3";
    
    /**
     * 采样率
     */
    @Builder.Default
    private Integer sampleRate = 24000;
    
    /**
     * 转换为VoiceConfig
     */
    public VoiceConfig toVoiceConfig() {
        return VoiceConfig.builder()
                .boldVoice(boldVoice)
                .normalVoice(normalVoice)
                .format(audioFormat)
                .sampleRate(sampleRate)
                .build();
    }
}
