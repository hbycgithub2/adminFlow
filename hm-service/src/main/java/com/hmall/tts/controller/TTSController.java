package com.hmall.tts.controller;

import com.hmall.tts.dto.*;
import com.hmall.tts.service.EdgeTTSCoreService;
import com.hmall.tts.service.LongTextTTSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * TTS 控制器（模块化重构版）
 * 
 * @author Kiro
 * @since 2026-08-12
 */
@Slf4j
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TTSController {

    private final EdgeTTSCoreService coreService;
    private final LongTextTTSService longTextService;

    /**
     * 生成语音（短文本，<5000字符）
     * 
     * POST /api/tts/generate
     * 
     * @param request 请求参数
     * @return 音频文件（MP3）
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateSpeech(@Validated @RequestBody TTSRequest request) {
        long startTime = System.currentTimeMillis();
        
        log.info("🎤 [TTS] 收到请求: text='{}...', voice={}, rate={}, pitch={}", 
                request.getText().substring(0, Math.min(30, request.getText().length())),
                request.getVoice(), request.getRate(), request.getPitch());

        try {
            // 生成语音
            byte[] audioData = coreService.generateSpeech(
                    request.getText(),
                    request.getVoice(),
                    request.getRate(),
                    request.getPitch()
            );

            // 返回音频流
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioData.length);
            headers.set("Content-Disposition", "inline; filename=speech.mp3");
            
            long duration = System.currentTimeMillis() - startTime;

            log.info("✅ [TTS] 生成成功: {} bytes, 耗时 {} ms", audioData.length, duration);

            return new ResponseEntity<>(audioData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("❌ [TTS] 生成失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 生成语音（长文本，支持 >5000字符）
     * 
     * POST /api/tts/long-text
     * 
     * @param request 请求参数
     * @return 音频文件（MP3）
     */
    @PostMapping("/long-text")
    public ResponseEntity<byte[]> generateLongTextSpeech(@Validated @RequestBody LongTextRequest request) {
        long startTime = System.currentTimeMillis();
        
        // 防止 maxSegmentLength 为 null
        Integer maxSegmentLength = request.getMaxSegmentLength();
        if (maxSegmentLength == null) {
            maxSegmentLength = 500;
            log.warn("⚠️ [长文本 TTS] maxSegmentLength 为 null，使用默认值 500");
        }
        
        log.info("🎤 [长文本 TTS] 收到请求: 文本长度={} 字符, voice={}, rate={}, pitch={}, maxSegmentLength={}", 
                request.getText().length(), request.getVoice(), request.getRate(), 
                request.getPitch(), maxSegmentLength);

        try {
            // 生成语音
            byte[] audioData = longTextService.generateLongTextSpeech(
                    request.getText(),
                    request.getVoice(),
                    request.getRate(),
                    request.getPitch(),
                    maxSegmentLength
            );

            // 返回音频流
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioData.length);
            headers.set("Content-Disposition", "inline; filename=long-speech.mp3");
            
            long duration = System.currentTimeMillis() - startTime;

            log.info("✅ [长文本 TTS] 生成成功: {} bytes, 耗时 {} ms", audioData.length, duration);

            return new ResponseEntity<>(audioData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("❌ [长文本 TTS] 生成失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 健康检查
     * 
     * GET /api/tts/health
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();

        try {
            boolean installed = coreService.checkInstallation();
            String version = coreService.getVersion();

            result.put("status", installed ? "ok" : "error");
            result.put("message", installed ? "edge-tts 已安装" : "edge-tts 未安装");
            result.put("installed", installed);
            result.put("version", version);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            result.put("installed", false);

            return ResponseEntity.ok(result);
        }
    }

    /**
     * 获取支持的音色列表
     * 
     * GET /api/tts/voices
     * 
     * @return 音色列表
     */
    @GetMapping("/voices")
    public ResponseEntity<Map<String, Object>> getVoices() {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> voices = coreService.getAvailableVoices();
            result.put("success", true);
            result.put("data", voices);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());

            return ResponseEntity.ok(result);
        }
    }
}
