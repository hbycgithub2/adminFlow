package com.hmall.tts.volcengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * TTS 请求参数
 * 
 * @author Kiro
 * @since 2026-08-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TTSRequest {
    
    /**
     * 需要转换的文本（必填）
     */
    @NotBlank(message = "文本内容不能为空")
    private String text;
    
    /**
     * 音色（可选，不填则使用默认音色）
     * 可选值：
     * - zh_female_vv_uranus_bigtts（女声-晓晓）
     * - zh_male_vv_uranus_bigtts（男声-云扬）
     * - zh_female_calm_uranus_bigtts（女声-平静）
     * - zh_male_calm_uranus_bigtts（男声-平静）
     */
    private String speaker;
    
    /**
     * 音频格式（可选，默认mp3）
     * 可选值：mp3, wav, pcm
     */
    private String format;
    
    /**
     * 采样率（可选，默认24000）
     * 可选值：8000, 16000, 24000
     */
    @Min(value = 8000, message = "采样率最小为8000")
    @Max(value = 24000, message = "采样率最大为24000")
    private Integer sampleRate;
    
    /**
     * 语速（可选，默认1.0）
     * 范围：0.5 - 2.0
     */
    @Min(value = 0, message = "语速最小为0.5")
    @Max(value = 2, message = "语速最大为2.0")
    private Double speed;
    
    /**
     * 音量（可选，默认1.0）
     * 范围：0.5 - 2.0
     */
    @Min(value = 0, message = "音量最小为0.5")
    @Max(value = 2, message = "音量最大为2.0")
    private Double volume;
    
    /**
     * 音调（可选，默认1.0）
     * 范围：0.5 - 2.0
     */
    @Min(value = 0, message = "音调最小为0.5")
    @Max(value = 2, message = "音调最大为2.0")
    private Double pitch;
}
