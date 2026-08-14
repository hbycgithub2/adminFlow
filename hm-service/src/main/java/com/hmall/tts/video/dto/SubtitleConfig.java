package com.hmall.tts.video.dto;

import com.hmall.tts.video.animation.AnimationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字幕配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleConfig {
    
    /**
     * 字体名称（默认使用微软雅黑，支持中文显示）
     */
    @Builder.Default
    private String fontName = "Microsoft YaHei";
    
    /**
     * 字体大小（建议56-72之间）
     */
    @Builder.Default
    private Integer fontSize = 64;
    
    /**
     * 字体颜色（HEX格式：#FFFFFF）
     */
    @Builder.Default
    private String fontColor = "#FFFFFF";
    
    /**
     * 边框颜色（HEX格式）
     */
    @Builder.Default
    private String borderColor = "#000000";
    
    /**
     * 边框粗细（增加到3，让字幕更清晰）
     */
    @Builder.Default
    private Integer borderWidth = 3;
    
    /**
     * 阴影距离
     */
    @Builder.Default
    private Integer shadowDistance = 2;
    
    /**
     * 字幕位置（1-9，九宫格，2=底部居中）
     */
    @Builder.Default
    private Integer position = 2;
    
    /**
     * 动画类型
     */
    @Builder.Default
    private AnimationType animationType = AnimationType.FADE;
    
    /**
     * 动画持续时间（毫秒）
     */
    @Builder.Default
    private Integer animationDuration = 300;
    
    /**
     * 是否加粗
     */
    @Builder.Default
    private Boolean bold = false;
    
    /**
     * 是否斜体
     */
    @Builder.Default
    private Boolean italic = false;
    
    /**
     * 将HEX颜色转换为ASS格式（&HBBGGRR&）
     */
    public String convertColorToASS(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#")) {
            return "&H00FFFFFF&"; // 默认白色
        }
        
        String hex = hexColor.substring(1); // 移除 #
        if (hex.length() != 6) {
            return "&H00FFFFFF&";
        }
        
        // HEX格式：#RRGGBB
        // ASS格式：&HBBGGRR&
        String rr = hex.substring(0, 2);
        String gg = hex.substring(2, 4);
        String bb = hex.substring(4, 6);
        
        return "&H00" + bb + gg + rr + "&";
    }
    
    /**
     * 获取字体颜色的ASS格式
     */
    public String getFontColorASS() {
        return convertColorToASS(fontColor);
    }
    
    /**
     * 获取边框颜色的ASS格式
     */
    public String getBorderColorASS() {
        return convertColorToASS(borderColor);
    }
}
