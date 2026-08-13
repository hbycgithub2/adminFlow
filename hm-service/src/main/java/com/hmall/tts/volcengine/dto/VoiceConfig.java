package com.hmall.tts.volcengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 音色配置
 * 用户可以自由选择加粗和非加粗文本的音色
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceConfig {
    
    /**
     * 加粗文本的音色
     * 默认：云舟（沉稳男声）
     */
    @NotBlank(message = "加粗文本音色不能为空")
    private String boldVoice = "zh_male_m191_uranus_bigtts";
    
    /**
     * 非加粗文本的音色
     * 默认：薇薇（温柔女声）
     */
    @NotBlank(message = "非加粗文本音色不能为空")
    private String normalVoice = "zh_female_vv_uranus_bigtts";
    
    /**
     * 音频格式
     */
    private String format = "mp3";
    
    /**
     * 采样率
     */
    private Integer sampleRate = 24000;
}
