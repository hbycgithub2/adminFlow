package com.hmall.tts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本智能分割服务
 * 
 * @author Kiro
 * @since 2026-08-12
 */
@Slf4j
@Service
public class TextSplitService {
    
    /**
     * 句子结束符：句号、问号、感叹号、省略号
     */
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile("[。？！…；;]");
    
    /**
     * 智能分割文本
     * 
     * 规则：
     * 1. 优先按句子边界分割（句号、问号、感叹号）
     * 2. 每段不超过 maxLength 字符
     * 3. 保持句子完整性，不在句子中间截断
     * 4. 如果单句超过 maxLength，强制分割
     * 
     * @param text 原始文本
     * @param maxLength 每段最大长度（字符）
     * @return 分割后的文本段列表
     */
    public List<String> smartSplit(String text, int maxLength) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        text = text.trim();
        List<String> segments = new ArrayList<>();
        
        // 如果文本短于 maxLength，直接返回
        if (text.length() <= maxLength) {
            segments.add(text);
            log.info("📝 [文本分割] 文本较短，无需分割: {} 字符", text.length());
            return segments;
        }
        
        log.info("📝 [文本分割] 开始分割: 总长度={} 字符, 最大段长={} 字符", text.length(), maxLength);
        
        // 按句子分割
        List<String> sentences = splitBySentence(text);
        log.info("📝 [文本分割] 按句子分割: {} 个句子", sentences.size());
        
        // 合并短句子，直到接近 maxLength
        StringBuilder currentSegment = new StringBuilder();
        
        for (String sentence : sentences) {
            // 如果当前段为空，直接添加句子
            if (currentSegment.length() == 0) {
                currentSegment.append(sentence);
            }
            // 如果添加当前句子不会超过 maxLength，合并
            else if (currentSegment.length() + sentence.length() <= maxLength) {
                currentSegment.append(sentence);
            }
            // 否则，保存当前段，开始新段
            else {
                segments.add(currentSegment.toString());
                currentSegment = new StringBuilder(sentence);
            }
            
            // 如果单句超过 maxLength，强制分割
            if (currentSegment.length() > maxLength) {
                String longSentence = currentSegment.toString();
                List<String> forceSplit = forceSplit(longSentence, maxLength);
                segments.addAll(forceSplit.subList(0, forceSplit.size() - 1));
                currentSegment = new StringBuilder(forceSplit.get(forceSplit.size() - 1));
            }
        }
        
        // 添加最后一段
        if (currentSegment.length() > 0) {
            segments.add(currentSegment.toString());
        }
        
        log.info("✅ [文本分割] 分割完成: {} 个段落", segments.size());
        for (int i = 0; i < segments.size(); i++) {
            log.debug("   段落 {}: {} 字符", i + 1, segments.get(i).length());
        }
        
        return segments;
    }
    
    /**
     * 按句子分割文本
     * 
     * @param text 原始文本
     * @return 句子列表
     */
    private List<String> splitBySentence(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_END_PATTERN.matcher(text);
        
        int lastEnd = 0;
        while (matcher.find()) {
            int end = matcher.end();
            String sentence = text.substring(lastEnd, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            lastEnd = end;
        }
        
        // 添加最后一句（可能没有标点符号）
        if (lastEnd < text.length()) {
            String lastSentence = text.substring(lastEnd).trim();
            if (!lastSentence.isEmpty()) {
                sentences.add(lastSentence);
            }
        }
        
        return sentences;
    }
    
    /**
     * 强制分割（当单句超过 maxLength 时）
     * 
     * @param text 文本
     * @param maxLength 最大长度
     * @return 分割后的段落
     */
    private List<String> forceSplit(String text, int maxLength) {
        List<String> segments = new ArrayList<>();
        
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            segments.add(text.substring(start, end));
            start = end;
        }
        
        return segments;
    }
}
