package com.hmall.tts.service;

import com.hmall.tts.config.EdgeTTSProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 长文本 TTS 服务
 * 
 * 功能：
 * 1. 智能分割长文本（>5000字符）
 * 2. 批量生成音频
 * 3. 自动合并音频
 * 
 * @author Kiro
 * @since 2026-08-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LongTextTTSService {

    private final EdgeTTSCoreService coreService;
    private final TextSplitService textSplitService;
    private final AudioMergeService audioMergeService;
    private final EdgeTTSProperties properties;

    /**
     * 生成长文本语音
     * 
     * @param text 文本内容（可超过5000字符）
     * @param voice 音色
     * @param rate 语速
     * @param pitch 音调
     * @param maxSegmentLength 每段最大长度
     * @return 音频数据（MP3）
     */
    public byte[] generateLongTextSpeech(String text, String voice, String rate, String pitch, int maxSegmentLength) {
        long startTime = System.currentTimeMillis();
        
        log.info("🎤 [长文本 TTS] 开始处理: 文本长度={} 字符, 音色={}, 最大段长={}", 
                text.length(), voice, maxSegmentLength);
        
        // 1. 智能分割文本
        List<String> segments = textSplitService.smartSplit(text, maxSegmentLength);
        
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("文本分割失败");
        }
        
        // 2. 批量生成音频
        List<byte[]> audioDataList = new ArrayList<>();
        
        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            log.info("🎤 [长文本 TTS] 生成音频 {}/{}: {} 字符", 
                    i + 1, segments.size(), segment.length());
            
            try {
                byte[] audioData = coreService.generateSpeech(segment, voice, rate, pitch);
                audioDataList.add(audioData);
                log.info("✅ [长文本 TTS] 音频 {}/{} 生成成功: {} bytes", 
                        i + 1, segments.size(), audioData.length);
            } catch (Exception e) {
                log.error("❌ [长文本 TTS] 音频 {}/{} 生成失败: {}", 
                        i + 1, segments.size(), e.getMessage(), e);
                throw e;
            }
        }
        
        // 3. 合并音频
        byte[] mergedAudio = audioMergeService.merge(audioDataList);
        
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("✅ [长文本 TTS] 处理完成: 文本长度={} 字符, 分段数={}, 音频大小={} bytes, 耗时={} ms", 
                text.length(), segments.size(), mergedAudio.length, duration);
        
        return mergedAudio;
    }
}
