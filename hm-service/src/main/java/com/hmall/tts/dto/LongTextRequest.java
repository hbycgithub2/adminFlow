package com.hmall.tts.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

/**
 * 长文本 TTS 请求参数
 * 
 * @author Kiro
 * @since 2026-08-12
 */
@Data
public class LongTextRequest {
    
    /**
     * 文本内容（必填，可超过5000字符）
     */
    @NotBlank(message = "文本内容不能为空")
    private String text;
    
    /**
     * 音色（可选，默认：晓晓）
     */
    private String voice = "zh-CN-XiaoxiaoNeural";
    
    /**
     * 语速（可选，默认：+0%）
     */
    private String rate = "+0%";
    
    /**
     * 音调（可选，默认：+0Hz）
     */
    private String pitch = "+0Hz";
    
    /**
     * 每段最大字符数（可选，默认：500，范围：100-1000）
     */
    @NotNull(message = "每段最大字符数不能为空")
    @Min(value = 100, message = "每段最大字符数不能小于100")
    @Max(value = 1000, message = "每段最大字符数不能大于1000")
    private Integer maxSegmentLength = 500;
}
