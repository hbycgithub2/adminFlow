package com.hmall.tts.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * TTS 请求参数
 * 
 * @author Kiro
 * @since 2026-08-12
 */
@Data
public class TTSRequest {
    
    /**
     * 文本内容（必填，1-5000字符）
     */
    @NotBlank(message = "文本内容不能为空")
    @Size(max = 5000, message = "文本内容不能超过5000字符")
    private String text;
    
    /**
     * 音色（可选，默认：晓晓）
     */
    private String voice = "zh-CN-XiaoxiaoNeural";
    
    /**
     * 语速（可选，默认：+0%，范围：-50%到+100%）
     */
    private String rate = "+0%";
    
    /**
     * 音调（可选，默认：+0Hz，范围：-50Hz到+50Hz）
     */
    private String pitch = "+0Hz";
}
