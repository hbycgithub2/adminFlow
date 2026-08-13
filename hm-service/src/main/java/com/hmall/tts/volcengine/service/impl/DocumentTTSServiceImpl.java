package com.hmall.tts.volcengine.service.impl;

import com.hmall.tts.volcengine.config.VolcengineConfig;
import com.hmall.tts.volcengine.docx.*;
import com.hmall.tts.volcengine.dto.*;
import com.hmall.tts.volcengine.service.DocumentTTSService;
import com.hmall.tts.volcengine.service.VolcengineTTSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 文档TTS服务实现
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentTTSServiceImpl implements DocumentTTSService {
    
    private final WordDocumentParser documentParser;
    private final TextSegmentMerger segmentMerger;
    private final SmartPauseCalculator pauseCalculator;
    private final AudioMerger audioMerger;
    private final VolcengineTTSService ttsService;
    private final VolcengineConfig config;
    
    /**
     * 并发执行器（限制并发数为3）
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    
    /**
     * 最大文本长度（字符）
     */
    private static final int MAX_TEXT_LENGTH = 800;
    
    @Override
    public DocumentTTSResult generateDocumentSpeech(MultipartFile file, VoiceConfig voiceConfig) {
        long startTime = System.currentTimeMillis();
        String taskId = UUID.randomUUID().toString();
        
        try {
            log.info("开始生成文档TTS，任务ID: {}, 文件名: {}", taskId, file.getOriginalFilename());
            
            // 1. 验证文件
            validateFile(file);
            
            // 2. 生成音频
            byte[] audioData = generateDocumentSpeechBytes(file, voiceConfig);
            
            // 3. 保存音频文件
            String fileName = taskId + ".mp3";
            Path outputDir = Paths.get(config.getOutputDir(), "documents");
            Files.createDirectories(outputDir);
            
            Path audioFile = outputDir.resolve(fileName);
            Files.write(audioFile, audioData);
            
            long generateTime = System.currentTimeMillis() - startTime;
            
            log.info("文档TTS生成成功，任务ID: {}, 文件: {}, 大小: {} KB, 耗时: {} ms",
                    taskId, fileName, audioData.length / 1024.0, generateTime);
            
            return DocumentTTSResult.success(
                    taskId,
                    "/tts/documents/" + fileName,
                    (long) audioData.length,
                    generateTime
            );
            
        } catch (Exception e) {
            log.error("文档TTS生成失败，任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
            return DocumentTTSResult.fail("文档TTS生成失败: " + e.getMessage());
        }
    }
    
    @Override
    public byte[] generateDocumentSpeechBytes(MultipartFile file, VoiceConfig voiceConfig) throws Exception {
        log.info("开始生成文档TTS音频，文件名: {}", file.getOriginalFilename());
        
        try {
            // 1. 解析Word文档
            log.info("步骤1: 解析Word文档...");
            List<TextSegment> segments = documentParser.parse(file.getInputStream(), voiceConfig);
            
            if (segments.isEmpty()) {
                throw new Exception("文档中没有可用的文本内容");
            }
            
            log.info("解析完成，共{}个文本片段", segments.size());
            
            // 2. 合并相同音色的连续片段
            log.info("步骤2: 合并文本片段...");
            List<MergedSegment> mergedSegments = segmentMerger.merge(segments);
            
            // 3. 拆分过长的片段
            List<MergedSegment> finalSegments = new ArrayList<>();
            for (MergedSegment segment : mergedSegments) {
                List<MergedSegment> split = segmentMerger.splitIfTooLong(segment, MAX_TEXT_LENGTH);
                finalSegments.addAll(split);
            }
            
            log.info("合并完成，共{}个合并片段", finalSegments.size());
            
            // 4. 并发调用TTS API
            log.info("步骤3: 并发生成语音（{}个API调用）...", finalSegments.size());
            List<AudioSegment> audioSegments = synthesizeParallel(finalSegments, voiceConfig);
            
            // 5. 计算停顿并设置
            log.info("步骤4: 计算智能停顿...");
            calculatePauses(audioSegments, mergedSegments);
            
            // 6. 合并音频
            log.info("步骤5: 合并音频片段...");
            byte[] finalAudio = audioMerger.merge(audioSegments, voiceConfig.getSampleRate());
            
            log.info("文档TTS音频生成完成，总大小: {} KB", finalAudio.length / 1024.0);
            
            return finalAudio;
            
        } catch (Exception e) {
            log.error("生成文档TTS音频失败: {}", e.getMessage(), e);
            throw new Exception("生成文档TTS音频失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 并发调用TTS API
     */
    private List<AudioSegment> synthesizeParallel(List<MergedSegment> segments, VoiceConfig voiceConfig) throws Exception {
        List<CompletableFuture<AudioSegment>> futures = new ArrayList<>();
        
        for (MergedSegment segment : segments) {
            CompletableFuture<AudioSegment> future = CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("开始生成音频，音色: {}, 文本长度: {}", 
                            segment.getSpeaker(), segment.getText().length());
                    
                    TTSRequest request = TTSRequest.builder()
                            .text(segment.getText())
                            .speaker(segment.getSpeaker())
                            .format(voiceConfig.getFormat())
                            .sampleRate(voiceConfig.getSampleRate())
                            .build();
                    
                    byte[] audio = ttsService.generateSpeechBytes(request);
                    
                    log.debug("音频生成完成，大小: {} KB", audio.length / 1024.0);
                    
                    return new AudioSegment(audio, segment);
                    
                } catch (Exception e) {
                    log.error("TTS合成失败: {}", e.getMessage());
                    throw new RuntimeException("TTS合成失败: " + e.getMessage(), e);
                }
            }, executor);
            
            futures.add(future);
        }
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // 收集结果（保持原始顺序）
        List<AudioSegment> results = new ArrayList<>();
        for (CompletableFuture<AudioSegment> future : futures) {
            results.add(future.get());
        }
        
        log.info("并发TTS合成完成，共生成{}个音频片段", results.size());
        
        return results;
    }
    
    /**
     * 计算并设置停顿
     */
    private void calculatePauses(List<AudioSegment> audioSegments, List<MergedSegment> mergedSegments) {
        for (int i = 0; i < audioSegments.size(); i++) {
            AudioSegment current = audioSegments.get(i);
            AudioSegment next = (i < audioSegments.size() - 1) ? audioSegments.get(i + 1) : null;
            
            if (next != null) {
                int pauseDuration = pauseCalculator.calculatePause(
                        current.getMergedSegment(), 
                        next.getMergedSegment()
                );
                
                current.setNeedPause(pauseDuration > 0);
                current.setPauseDuration(pauseDuration);
            }
        }
    }
    
    /**
     * 验证上传的文件
     */
    private void validateFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".docx")) {
            throw new Exception("只支持.docx格式的Word文档");
        }
        
        // 限制文件大小（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new Exception("文件大小不能超过10MB");
        }
    }
}
