package com.hmall.tts.subtitle.service.impl;

import com.hmall.tts.subtitle.service.SubtitleAlignmentService;
import com.hmall.tts.volcengine.dto.CharTiming;
import com.hmall.tts.volcengine.dto.DialogSegment;
import com.hmall.tts.whisper.service.WhisperService;
import com.hmall.tts.whisperx.dto.CharTimestamp;
import com.hmall.tts.whisperx.service.WhisperXService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字幕对齐服务实现类
 * 
 * @author Kiro
 * @since 2026-08-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleAlignmentServiceImpl implements SubtitleAlignmentService {
    
    private final WhisperXService whisperXService;
    private final WhisperService whisperService;
    
    /**
     * 音频时长误差阈值（秒）
     * 如果音频时长差异小于此值，认为未修改
     */
    private static final double DURATION_TOLERANCE = 1.0;
    
    @Override
    public List<DialogSegment> alignSubtitles(
            byte[] audioData,
            List<DialogSegment> originalSubtitles,
            String originalText,
            boolean forceReAlign
    ) throws Exception {
        
        log.info("[字幕对齐] 开始对齐，音频大小：{} KB，有原始字幕：{}，有原始文本：{}，强制重对齐：{}",
                audioData.length / 1024.0,
                originalSubtitles != null && !originalSubtitles.isEmpty(),
                originalText != null && !originalText.isEmpty(),
                forceReAlign);
        
        // 策略1：如果有原始字幕且MP3未变化（且不强制重对齐），直接使用
        if (!forceReAlign && originalSubtitles != null && !originalSubtitles.isEmpty()) {
            boolean isModified = isAudioModified(audioData, originalSubtitles);
            
            if (!isModified) {
                log.info("[字幕对齐] MP3未修改，直接使用原始字幕（{}个segment）", originalSubtitles.size());
                return originalSubtitles;
            } else {
                log.info("[字幕对齐] MP3已修改，需要重新对齐");
            }
        }
        
        // 策略2：如果有原始字幕，使用WhisperX重新对齐
        if (originalSubtitles != null && !originalSubtitles.isEmpty()) {
            // 如果没有原始文本，从字幕中提取
            String textForAlign = originalText;
            if (textForAlign == null || textForAlign.isEmpty()) {
                textForAlign = originalSubtitles.stream()
                        .map(DialogSegment::getText)
                        .collect(Collectors.joining());
                log.info("[字幕对齐] 从字幕数据中提取文本，长度：{}", textForAlign.length());
            }
            return reAlignWithWhisperX(audioData, textForAlign, originalSubtitles);
        }
        
        // 策略3：如果只有原始文本，使用Whisper识别 + WhisperX对齐
        if (originalText != null && !originalText.isEmpty()) {
            return recognizeAndAlign(audioData, originalText);
        }
        
        // 策略4：兜底 - 使用Whisper纯识别（不推荐，准确率较低）
        log.warn("[字幕对齐] 无原始字幕和原始文本，使用Whisper纯识别（准确率较低）");
        return recognizeOnly(audioData);
    }
    
    @Override
    public boolean isAudioModified(byte[] audioData, List<DialogSegment> originalSubtitles) {
        if (originalSubtitles == null || originalSubtitles.isEmpty()) {
            return true;
        }
        
        // 计算原始字幕的总时长
        double originalDuration = calculateTotalDuration(originalSubtitles);
        
        // 估算音频时长（MP3格式，假设128kbps）
        double audioDuration = estimateAudioDuration(audioData);
        
        double diff = Math.abs(audioDuration - originalDuration);
        
        log.debug("[字幕对齐] 时长比较：原始字幕={}秒，音频={}秒，差异={}秒，阈值={}秒",
                String.format("%.2f", originalDuration),
                String.format("%.2f", audioDuration),
                String.format("%.2f", diff),
                DURATION_TOLERANCE);
        
        return diff > DURATION_TOLERANCE;
    }
    
    /**
     * 使用WhisperX重新对齐
     */
    private List<DialogSegment> reAlignWithWhisperX(byte[] audioData, String originalText, 
                                                     List<DialogSegment> originalSubtitles) throws Exception {
        log.info("[字幕对齐] 使用WhisperX重新对齐");
        
        try {
            // 调用WhisperX对齐
            List<CharTimestamp> charTimestamps = whisperXService.align(audioData, originalText);
            
            if (charTimestamps == null || charTimestamps.isEmpty()) {
                log.warn("[字幕对齐] WhisperX返回空结果，使用原始字幕");
                return originalSubtitles;
            }
            
            // 将字符时间戳映射到DialogSegment
            List<DialogSegment> newSubtitles = mapCharTimestampsToDialogSegments(
                    charTimestamps, 
                    originalSubtitles
            );
            
            log.info("[字幕对齐] WhisperX对齐完成，生成{}个字幕片段", newSubtitles.size());
            
            return newSubtitles;
            
        } catch (Exception e) {
            log.error("[字幕对齐] WhisperX对齐失败，回退到原始字幕：{}", e.getMessage());
            return originalSubtitles;
        }
    }
    
    /**
     * 识别并对齐（Whisper识别 + WhisperX对齐）
     */
    private List<DialogSegment> recognizeAndAlign(byte[] audioData, String originalText) throws Exception {
        log.info("[字幕对齐] 使用Whisper识别 + WhisperX对齐");
        
        try {
            // 先用WhisperX对齐（更快更准确）
            List<CharTimestamp> charTimestamps = whisperXService.align(audioData, originalText);
            
            if (charTimestamps == null || charTimestamps.isEmpty()) {
                log.warn("[字幕对齐] WhisperX对齐失败，回退到Whisper纯识别");
                return recognizeOnly(audioData);
            }
            
            // 转换为DialogSegment（按句子分段）
            List<DialogSegment> subtitles = convertCharTimestampsToDialogSegments(charTimestamps, originalText);
            
            log.info("[字幕对齐] 识别对齐完成，生成{}个字幕片段", subtitles.size());
            
            return subtitles;
            
        } catch (Exception e) {
            log.error("[字幕对齐] WhisperX对齐失败，回退到Whisper纯识别：{}", e.getMessage());
            return recognizeOnly(audioData);
        }
    }
    
    /**
     * 纯识别（兜底方案）
     */
    private List<DialogSegment> recognizeOnly(byte[] audioData) throws Exception {
        log.info("[字幕对齐] 使用Whisper纯识别（兜底方案）");
        
        // 这里需要实现Whisper纯识别逻辑
        // 暂时返回空列表，实际项目中需要完善
        log.warn("[字幕对齐] Whisper纯识别功能暂未实现");
        
        throw new Exception("无法生成字幕：缺少原始字幕或原始文本");
    }
    
    /**
     * 计算字幕总时长
     */
    private double calculateTotalDuration(List<DialogSegment> subtitles) {
        if (subtitles.isEmpty()) {
            return 0.0;
        }
        
        DialogSegment lastSegment = subtitles.get(subtitles.size() - 1);
        return lastSegment.getStartTime() + lastSegment.getDuration();
    }
    
    /**
     * 估算音频时长（MP3格式）
     */
    private double estimateAudioDuration(byte[] audioData) {
        // MP3格式：假设128kbps比特率
        int bitrate = 128000;
        return (audioData.length * 8.0) / bitrate * 1.05; // 修正系数1.05
    }
    
    /**
     * 将字符时间戳映射到DialogSegment
     */
    private List<DialogSegment> mapCharTimestampsToDialogSegments(
            List<CharTimestamp> charTimestamps,
            List<DialogSegment> originalSubtitles) {
        
        List<DialogSegment> newSubtitles = new ArrayList<>();
        
        int charIndex = 0;
        
        for (DialogSegment originalSegment : originalSubtitles) {
            String text = originalSegment.getText();
            
            if (text == null || text.isEmpty()) {
                continue;
            }
            
            List<CharTiming> charTimings = new ArrayList<>();
            double startTime = -1.0;
            double endTime = 0.0;
            
            // 收集这个segment的所有字符时间戳
            for (int i = 0; i < text.length() && charIndex < charTimestamps.size(); i++, charIndex++) {
                CharTimestamp whisperXChar = charTimestamps.get(charIndex);
                
                if (startTime < 0) {
                    startTime = whisperXChar.getStartTime();
                }
                endTime = whisperXChar.getEndTime();
                
                CharTiming charTiming = new CharTiming();
                charTiming.setCharacter(whisperXChar.getCharacter());
                charTiming.setStartTime(whisperXChar.getStartTime());
                charTiming.setDuration(whisperXChar.getDuration());
                
                charTimings.add(charTiming);
            }
            
            if (startTime < 0) {
                startTime = 0.0;
            }
            
            DialogSegment newSegment = new DialogSegment();
            newSegment.setText(text);
            newSegment.setVoiceId(originalSegment.getVoiceId());
            newSegment.setIsBold(originalSegment.getIsBold());
            newSegment.setStartTime(startTime);
            newSegment.setDuration(endTime - startTime);
            newSegment.setCharTimings(charTimings);
            
            newSubtitles.add(newSegment);
        }
        
        return newSubtitles;
    }
    
    /**
     * 将字符时间戳转换为DialogSegment（按句子分段）
     */
    private List<DialogSegment> convertCharTimestampsToDialogSegments(
            List<CharTimestamp> charTimestamps,
            String originalText) {
        
        // 按句子分段（简单实现：按标点符号切分）
        List<String> sentences = splitIntoSentences(originalText);
        
        List<DialogSegment> subtitles = new ArrayList<>();
        
        int charIndex = 0;
        
        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) {
                continue;
            }
            
            List<CharTiming> charTimings = new ArrayList<>();
            double startTime = -1.0;
            double endTime = 0.0;
            
            for (int i = 0; i < sentence.length() && charIndex < charTimestamps.size(); i++, charIndex++) {
                CharTimestamp whisperXChar = charTimestamps.get(charIndex);
                
                if (startTime < 0) {
                    startTime = whisperXChar.getStartTime();
                }
                endTime = whisperXChar.getEndTime();
                
                CharTiming charTiming = new CharTiming();
                charTiming.setCharacter(whisperXChar.getCharacter());
                charTiming.setStartTime(whisperXChar.getStartTime());
                charTiming.setDuration(whisperXChar.getDuration());
                
                charTimings.add(charTiming);
            }
            
            if (startTime < 0) {
                startTime = 0.0;
            }
            
            DialogSegment segment = new DialogSegment();
            segment.setText(sentence);
            segment.setStartTime(startTime);
            segment.setDuration(endTime - startTime);
            segment.setCharTimings(charTimings);
            
            subtitles.add(segment);
        }
        
        return subtitles;
    }
    
    /**
     * 按句子切分文本
     */
    private List<String> splitIntoSentences(String text) {
        // 简单实现：按句号、问号、感叹号切分
        String[] parts = text.split("[。？！.?!]");
        
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                sentences.add(part.trim());
            }
        }
        
        return sentences;
    }
}
