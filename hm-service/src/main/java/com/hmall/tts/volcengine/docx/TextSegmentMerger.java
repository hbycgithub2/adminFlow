package com.hmall.tts.volcengine.docx;

import com.hmall.tts.volcengine.dto.MergedSegment;
import com.hmall.tts.volcengine.dto.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本片段合并器
 * 将相同音色的连续文本片段合并，减少API调用次数
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Slf4j
@Component
public class TextSegmentMerger {
    
    /**
     * 合并相同音色的连续文本片段
     * 
     * @param segments 原始文本片段列表
     * @return 合并后的片段列表
     */
    public List<MergedSegment> merge(List<TextSegment> segments) {
        log.info("开始合并文本片段，原始片段数: {}", segments.size());
        
        List<MergedSegment> merged = new ArrayList<>();
        
        if (segments.isEmpty()) {
            log.warn("文本片段列表为空，无需合并");
            return merged;
        }
        
        // 初始化第一个合并片段
        MergedSegment current = new MergedSegment(segments.get(0).getSpeaker());
        current.addText(segments.get(0).getText());
        current.addOriginalSegment(segments.get(0));
        
        // 遍历剩余片段
        for (int i = 1; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            
            // 如果音色相同，合并到当前片段
            if (segment.getSpeaker().equals(current.getSpeaker())) {
                current.addText(segment.getText());
                current.addOriginalSegment(segment);
                log.debug("合并相同音色的文本: {}", segment.getText());
            } else {
                // 音色不同，保存当前片段，开始新片段
                merged.add(current);
                log.debug("保存合并片段，音色: {}, 文本长度: {}", 
                        current.getSpeaker(), current.getText().length());
                
                current = new MergedSegment(segment.getSpeaker());
                current.addText(segment.getText());
                current.addOriginalSegment(segment);
            }
        }
        
        // 添加最后一个片段
        merged.add(current);
        
        log.info("文本片段合并完成，合并后片段数: {}，压缩率: {:.2f}%", 
                merged.size(), 
                (1.0 - (double) merged.size() / segments.size()) * 100);
        
        return merged;
    }
    
    /**
     * 拆分过长的文本片段
     * 如果合并后的文本超过最大长度，需要拆分
     * 
     * @param segment 合并后的片段
     * @param maxLength 最大长度
     * @return 拆分后的片段列表
     */
    public List<MergedSegment> splitIfTooLong(MergedSegment segment, int maxLength) {
        List<MergedSegment> result = new ArrayList<>();
        
        String text = segment.getText();
        if (text.length() <= maxLength) {
            result.add(segment);
            return result;
        }
        
        log.info("文本片段过长({})，开始拆分，最大长度: {}", text.length(), maxLength);
        
        // 按句子分割
        String[] sentences = text.split("([。！？\\n]+)");
        
        MergedSegment current = new MergedSegment(segment.getSpeaker());
        
        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) {
                continue;
            }
            
            // 如果加上当前句子会超长，先保存当前片段
            if (current.getText().length() + sentence.length() > maxLength) {
                if (current.getText().length() > 0) {
                    result.add(current);
                    current = new MergedSegment(segment.getSpeaker());
                }
            }
            
            current.addText(sentence + "。");
        }
        
        // 添加最后一个片段
        if (current.getText().length() > 0) {
            result.add(current);
        }
        
        log.info("文本拆分完成，拆分为{}个片段", result.size());
        
        return result;
    }
}
