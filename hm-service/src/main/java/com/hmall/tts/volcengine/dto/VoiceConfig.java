package com.hmall.tts.volcengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 音色配置
 * 用户可以自由选择加粗和非加粗文本的音色
 * 支持多音色模式（6种音色）：加粗、非加粗、蓝色、红色、绿色、紫色
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
    
    // ==================== 多音色模式支持 ====================
    
    /**
     * 是否启用多音色模式
     * true=6种音色（加粗、非加粗、蓝、红、绿、紫）
     * false=2种音色（加粗、非加粗）
     */
    private boolean multiVoiceMode = false;
    
    /**
     * 蓝色文本音色（RGB: 0070C0）
     */
    private String blueVoice;
    
    /**
     * 红色文本音色（RGB: C00000 或 FF0000）
     */
    private String redVoice;
    
    /**
     * 绿色文本音色（RGB: 00B050）
     */
    private String greenVoice;
    
    /**
     * 紫色文本音色（RGB: 7030A0）
     */
    private String purpleVoice;
    
    // ==================== 字幕对齐控制 ====================
    
    /**
     * 是否需要字幕对齐（WhisperX）
     * true=生成字符级时间戳（用于视频字幕），耗时约30秒
     * false=只生成音频文件（用于试听/下载），耗时约8秒
     * 默认值：true（兼容旧版本）
     */
    @Builder.Default
    private Boolean alignSubtitles = true;
    
    /**
     * 根据文本颜色和是否加粗获取对应的音色ID
     * 
     * @param colorRGB 颜色RGB值（例如："0070C0"），可能为null
     * @param isBold 是否加粗
     * @return 音色ID
     */
    public String getVoiceId(String colorRGB, boolean isBold) {
        // 如果启用了多音色模式且有颜色
        if (multiVoiceMode && colorRGB != null && !colorRGB.isEmpty()) {
            String color = colorRGB.toUpperCase();
            
            // ⭐ 诊断日志：打印颜色匹配过程
            System.out.println(String.format("[VoiceConfig] 颜色匹配: colorRGB=%s, isBold=%s, multiVoiceMode=%s", 
                                            colorRGB, isBold, multiVoiceMode));
            System.out.println(String.format("[VoiceConfig] 可用音色: blue=%s, red=%s, green=%s, purple=%s", 
                                            blueVoice != null ? "✓" : "✗", 
                                            redVoice != null ? "✓" : "✗",
                                            greenVoice != null ? "✓" : "✗",
                                            purpleVoice != null ? "✓" : "✗"));
            
            // 匹配彩色音色（优先使用颜色音色，如果颜色音色为null则降级到加粗/非加粗）
            if ("0070C0".equals(color) && blueVoice != null) {
                System.out.println("[VoiceConfig] ✅ 匹配蓝色音色: " + blueVoice);
                return blueVoice;
            } else if (("C00000".equals(color) || "FF0000".equals(color)) && redVoice != null) {
                // 支持两种常见的红色：C00000（深红）和 FF0000（纯红）
                System.out.println("[VoiceConfig] ✅ 匹配红色音色: " + redVoice);
                return redVoice;
            } else if ("00B050".equals(color) && greenVoice != null) {
                System.out.println("[VoiceConfig] ✅ 匹配绿色音色: " + greenVoice);
                return greenVoice;
            } else if ("7030A0".equals(color) && purpleVoice != null) {
                System.out.println("[VoiceConfig] ✅ 匹配紫色音色: " + purpleVoice);
                return purpleVoice;
            } else {
                System.out.println(String.format("[VoiceConfig] ⚠️ 颜色不匹配或音色为null，降级到基础模式 (color=%s)", color));
            }
        } else {
            System.out.println(String.format("[VoiceConfig] 基础模式: multiVoiceMode=%s, colorRGB=%s", multiVoiceMode, colorRGB));
        }
        
        // 降级到基础模式（根据是否加粗）
        String fallbackVoice = isBold ? boldVoice : normalVoice;
        System.out.println(String.format("[VoiceConfig] 返回基础音色: isBold=%s, voice=%s", isBold, fallbackVoice));
        return fallbackVoice;
    }
}
