package com.hmall.tts.volcengine.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 合并后的文本片段
 * 将相同音色的连续文本片段合并，减少API调用次数
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
public class MergedSegment {
    
    /**
     * 音色ID
     */
    private String speaker;
    
    /**
     * 合并后的文本
     */
    private StringBuilder text;
    
    /**
     * 原始片段列表（用于回溯）
     */
    private List<TextSegment> originalSegments;
    
    /**
     * 顺序
     */
    private Integer order;
    
    public MergedSegment(String speaker) {
        this.speaker = speaker;
        this.text = new StringBuilder();
        this.originalSegments = new ArrayList<>();
    }
    
    /**
     * 添加文本
     */
    public void addText(String text) {
        this.text.append(text);
    }
    
    /**
     * 添加原始片段
     */
    public void addOriginalSegment(TextSegment segment) {
        this.originalSegments.add(segment);
        if (this.order == null && segment.getOrder() != null) {
            this.order = segment.getOrder();
        }
    }
    
    /**
     * 获取合并后的文本
     */
    public String getText() {
        return text.toString();
    }
}
