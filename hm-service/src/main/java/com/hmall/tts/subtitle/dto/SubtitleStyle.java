package com.hmall.tts.subtitle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 字幕样式数据模型
 * 
 * <p>包含字幕的所有视觉属性配置</p>
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleStyle implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 字体名称
     * <p>例如: Microsoft YaHei, Arial, SimHei</p>
     */
    @Builder.Default
    private String fontName = "Microsoft YaHei";
    
    /**
     * 字体大小（像素）
     * <p>范围: 24-96px，推荐: 64px</p>
     */
    @Builder.Default
    private Integer fontSize = 64;
    
    /**
     * 字体颜色（十六进制）
     * <p>格式: #RRGGBB，例如: #FFFFFF（白色）</p>
     */
    @Builder.Default
    private String fontColor = "#FFFFFF";
    
    /**
     * 边框颜色（十六进制）
     * <p>格式: #RRGGBB，例如: #000000（黑色）</p>
     */
    @Builder.Default
    private String borderColor = "#000000";
    
    /**
     * 边框粗细（像素）
     * <p>范围: 1-5px，推荐: 3px</p>
     */
    @Builder.Default
    private Integer borderWidth = 3;
    
    /**
     * 字幕位置（九宫格）
     * <p>
     * 位置编号:
     * <pre>
     * 1 2 3    1=左上   2=顶部居中  3=右上
     * 4 5 6    4=左中   5=正中      6=右中
     * 7 8 9    7=左下   8=底部居中  9=右下
     * </pre>
     * 推荐: 2（底部居中）
     * </p>
     */
    @Builder.Default
    private Integer position = 2;
    
    /**
     * 动画类型
     * <p>
     * 可选值:
     * <ul>
     *   <li>fade - 渐入渐出</li>
     *   <li>slide_up - 从下飞入</li>
     *   <li>slide_down - 从上飞入</li>
     *   <li>slide_left - 从左飞入</li>
     *   <li>slide_right - 从右飞入</li>
     *   <li>zoom_in - 缩放进入</li>
     *   <li>bounce - 弹跳效果</li>
     *   <li>none - 无动画</li>
     * </ul>
     * </p>
     */
    @Builder.Default
    private String animationType = "fade";
}
