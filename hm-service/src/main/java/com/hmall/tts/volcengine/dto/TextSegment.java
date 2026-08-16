package com.hmall.tts.volcengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文本片段
 * 代表Word文档中的一段文本及其音色配置
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextSegment {
    
    /**
     * 文本内容
     */
    private String text;
    
    /**
     * 音色ID
     */
    private String speaker;
    
    /**
     * 是否加粗
     */
    private Boolean isBold;
    
    /**
     * 原始顺序（用于排序）
     */
    private Integer order;
    
    /**
     * 段落ID（同一段落的Run有相同的ID）
     * 用于视频生成时按段落分行显示
     */
    private Integer paragraphId;
}
