package com.hmall.tts.audio.service.impl;

import com.hmall.tts.audio.dto.AudioGenerateResponse;
import com.hmall.tts.audio.service.AudioGeneratorService;
import com.hmall.tts.volcengine.dto.DocumentTTSResult;
import com.hmall.tts.volcengine.dto.VoiceConfig;
import com.hmall.tts.volcengine.service.DocumentTTSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 音频生成服务实现类
 * 
 * 核心功能：从Word文档生成音频（复用DocumentTTSService）
 * 
 * @author Kiro
 * @since 2026-08-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioGeneratorServiceImpl implements AudioGeneratorService {
    
    private final DocumentTTSService documentTTSService;
    
    @Override
    public AudioGenerateResponse generateAudioFromDocument(MultipartFile file, VoiceConfig voiceConfig) throws Exception {
        log.info("开始生成音频，文件名：{}", file.getOriginalFilename());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // ⚡ Manual模式优化：跳过WhisperX对齐，使用智能算法（提速15倍）
            // 原因：Manual模式生成音频后，用户可能会编辑音频，所以不需要精确对齐
            // 等用户上传音频生成视频时，再进行精确对齐
            boolean skipAlignment = true;
            
            log.info("⚡ Manual模式优化：skipAlignment={}", skipAlignment);
            
            // 调用DocumentTTSService生成音频和字幕
            DocumentTTSResult ttsResult = documentTTSService.generateDocumentSpeech(file, voiceConfig, skipAlignment);
            
            if (!Boolean.TRUE.equals(ttsResult.getSuccess())) {
                log.error("音频生成失败：{}", ttsResult.getMessage());
                return AudioGenerateResponse.fail("音频生成失败：" + ttsResult.getMessage());
            }
            
            long generateTime = System.currentTimeMillis() - startTime;
            
            log.info("音频生成成功，任务ID：{}，音频URL：{}，时长：{}秒，耗时：{}ms",
                    ttsResult.getTaskId(), 
                    ttsResult.getAudioUrl(), 
                    String.format("%.2f", ttsResult.getTotalDuration()),
                    generateTime);
            
            return AudioGenerateResponse.success(
                    ttsResult.getTaskId(),
                    ttsResult.getAudioUrl(),
                    ttsResult.getAudioSize(),
                    ttsResult.getTotalDuration(),
                    ttsResult.getSegments(),
                    generateTime
            );
            
        } catch (Exception e) {
            log.error("音频生成异常：{}", e.getMessage(), e);
            return AudioGenerateResponse.fail("音频生成异常：" + e.getMessage());
        }
    }
}
