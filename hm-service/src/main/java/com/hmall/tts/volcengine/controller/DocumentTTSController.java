package com.hmall.tts.volcengine.controller;

import com.hmall.tts.volcengine.dto.DocumentTTSResult;
import com.hmall.tts.volcengine.dto.VoiceConfig;
import com.hmall.tts.volcengine.service.DocumentTTSService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档TTS控制器
 * 提供Word文档对话语音生成功能
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Slf4j
@RestController
@RequestMapping("/api/document-tts")
@RequiredArgsConstructor
@Api(tags = "文档TTS接口")
public class DocumentTTSController {
    
    private final DocumentTTSService documentTTSService;
    
    /**
     * 生成文档对话语音（返回文件信息）
     */
    @PostMapping("/generate")
    @ApiOperation("生成文档对话语音")
    public ResponseEntity<DocumentTTSResult> generateDocumentSpeech(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "boldVoice", defaultValue = "zh_male_m191_uranus_bigtts") String boldVoice,
            @RequestParam(value = "normalVoice", defaultValue = "zh_female_vv_uranus_bigtts") String normalVoice,
            @RequestParam(value = "format", defaultValue = "mp3") String format,
            @RequestParam(value = "sampleRate", defaultValue = "24000") Integer sampleRate,
            // 多音色模式参数
            @RequestParam(value = "multiVoiceMode", required = false) String multiVoiceMode,
            @RequestParam(value = "blueVoice", required = false) String blueVoice,
            @RequestParam(value = "redVoice", required = false) String redVoice,
            @RequestParam(value = "greenVoice", required = false) String greenVoice,
            @RequestParam(value = "purpleVoice", required = false) String purpleVoice,
            // 字幕对齐参数（新增）
            @RequestParam(value = "alignSubtitles", defaultValue = "true") Boolean alignSubtitles
    ) {
        boolean isMultiVoice = "true".equals(multiVoiceMode);
        
        if (isMultiVoice) {
            log.info("收到文档TTS请求: 文件={}, 多音色模式=启用, 蓝={}, 红={}, 绿={}, 紫={}, 对齐字幕={}", 
                    file.getOriginalFilename(), blueVoice, redVoice, greenVoice, purpleVoice, alignSubtitles);
            // ⭐ 诊断日志：详细打印每个音色的完整ID
            log.info("🎵 音色详情:");
            log.info("  蓝色音色ID: {}", blueVoice);
            log.info("  红色音色ID: {} ← 重点检查", redVoice);
            log.info("  绿色音色ID: {}", greenVoice);
            log.info("  紫色音色ID: {}", purpleVoice);
        } else {
            log.info("收到文档TTS请求: 文件={}, 加粗音色={}, 非加粗音色={}, 对齐字幕={}", 
                    file.getOriginalFilename(), boldVoice, normalVoice, alignSubtitles);
        }
        
        try {
            VoiceConfig voiceConfig = VoiceConfig.builder()
                    .boldVoice(boldVoice)
                    .normalVoice(normalVoice)
                    .format(format)
                    .sampleRate(sampleRate)
                    .multiVoiceMode(isMultiVoice)
                    .blueVoice(blueVoice)
                    .redVoice(redVoice)
                    .greenVoice(greenVoice)
                    .purpleVoice(purpleVoice)
                    .alignSubtitles(alignSubtitles)  // 新增：字幕对齐开关
                    .build();
            
            DocumentTTSResult result = documentTTSService.generateDocumentSpeech(file, voiceConfig);
            
            if (result.getSuccess()) {
                log.info("文档TTS生成成功: 任务ID={}, URL={}", result.getTaskId(), result.getAudioUrl());
                return ResponseEntity.ok(result);
            } else {
                log.error("文档TTS生成失败: {}", result.getMessage());
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("文档TTS生成异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(DocumentTTSResult.fail("文档TTS生成异常: " + e.getMessage()));
        }
    }
    
    /**
     * 生成文档对话语音（返回音频流）
     */
    @PostMapping("/generate-stream")
    @ApiOperation("生成文档对话语音（流式）")
    public ResponseEntity<byte[]> generateDocumentSpeechStream(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "boldVoice", defaultValue = "zh_male_m191_uranus_bigtts") String boldVoice,
            @RequestParam(value = "normalVoice", defaultValue = "zh_female_vv_uranus_bigtts") String normalVoice,
            @RequestParam(value = "format", defaultValue = "mp3") String format,
            @RequestParam(value = "sampleRate", defaultValue = "24000") Integer sampleRate,
            // 多音色模式参数
            @RequestParam(value = "multiVoiceMode", required = false) String multiVoiceMode,
            @RequestParam(value = "blueVoice", required = false) String blueVoice,
            @RequestParam(value = "redVoice", required = false) String redVoice,
            @RequestParam(value = "greenVoice", required = false) String greenVoice,
            @RequestParam(value = "purpleVoice", required = false) String purpleVoice,
            // 字幕对齐参数（新增）
            @RequestParam(value = "alignSubtitles", defaultValue = "true") Boolean alignSubtitles
    ) {
        boolean isMultiVoice = "true".equals(multiVoiceMode);
        
        if (isMultiVoice) {
            log.info("收到文档TTS流式请求: 文件={}, 多音色模式=启用", file.getOriginalFilename());
        } else {
            log.info("收到文档TTS流式请求: 文件={}, 加粗音色={}, 非加粗音色={}", 
                    file.getOriginalFilename(), boldVoice, normalVoice);
        }
        
        try {
            VoiceConfig voiceConfig = VoiceConfig.builder()
                    .boldVoice(boldVoice)
                    .normalVoice(normalVoice)
                    .format(format)
                    .sampleRate(sampleRate)
                    .multiVoiceMode(isMultiVoice)
                    .blueVoice(blueVoice)
                    .redVoice(redVoice)
                    .greenVoice(greenVoice)
                    .purpleVoice(purpleVoice)
                    .alignSubtitles(alignSubtitles)  // 新增：字幕对齐开关
                    .build();
            
            byte[] audioData = documentTTSService.generateDocumentSpeechBytes(file, voiceConfig);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentDispositionFormData("attachment", 
                    file.getOriginalFilename().replace(".docx", ".mp3"));
            
            log.info("文档TTS流式生成成功，大小: {} KB", audioData.length / 1024.0);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(audioData);
            
        } catch (Exception e) {
            log.error("文档TTS流式生成失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 快速测试接口
     */
    @PostMapping("/test")
    @ApiOperation("快速测试文档TTS")
    public ResponseEntity<DocumentTTSResult> testDocumentTTS(
            @RequestParam("file") MultipartFile file
    ) {
        log.info("收到文档TTS测试请求: 文件={}", file.getOriginalFilename());
        
        // 使用默认配置
        VoiceConfig voiceConfig = VoiceConfig.builder()
                .boldVoice("zh_male_m191_uranus_bigtts")  // 云舟（男声）
                .normalVoice("zh_female_vv_uranus_bigtts") // 薇薇（女声）
                .format("mp3")
                .sampleRate(24000)
                .build();
        
        DocumentTTSResult result = documentTTSService.generateDocumentSpeech(file, voiceConfig);
        
        return ResponseEntity.ok(result);
    }
}
