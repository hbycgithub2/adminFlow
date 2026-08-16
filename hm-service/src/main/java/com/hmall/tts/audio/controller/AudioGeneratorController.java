package com.hmall.tts.audio.controller;

import com.hmall.tts.audio.dto.AudioGenerateRequest;
import com.hmall.tts.audio.dto.AudioGenerateResponse;
import com.hmall.tts.audio.service.AudioGeneratorService;
import com.hmall.tts.volcengine.dto.VoiceConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 音频生成控制器
 * 
 * 提供音频生成接口（接口2：仅生成MP3）
 * 
 * @author Kiro
 * @since 2026-08-16
 */
@Slf4j
@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
@Api(tags = "音频生成")
public class AudioGeneratorController {
    
    private final AudioGeneratorService audioGeneratorService;
    
    /**
     * 从Word文档生成音频（接口2）⭐
     * 
     * 用途：
     * 1. 用户可以单独生成音频，下载MP3
     * 2. 支持灵活模式：生成音频 → 编辑 → 上传生成视频
     * 
     * @param file Word文档文件
     * @param boldVoice 粗体文本音色
     * @param normalVoice 普通文本音色
     * @param audioFormat 音频格式
     * @param sampleRate 采样率
     * @return 音频生成响应（包含音频URL、字幕数据）
     */
    @PostMapping("/generate")
    @ApiOperation("从Word文档生成音频")
    public ResponseEntity<AudioGenerateResponse> generateAudio(
            @ApiParam("Word文档文件") @RequestParam("file") MultipartFile file,
            @ApiParam("粗体文本音色") @RequestParam(value = "boldVoice", defaultValue = "zh_male_m191_uranus_bigtts") String boldVoice,
            @ApiParam("普通文本音色") @RequestParam(value = "normalVoice", defaultValue = "zh_female_vv_uranus_bigtts") String normalVoice,
            @ApiParam("音频格式") @RequestParam(value = "audioFormat", defaultValue = "mp3") String audioFormat,
            @ApiParam("采样率") @RequestParam(value = "sampleRate", defaultValue = "24000") Integer sampleRate
    ) {
        log.info("收到音频生成请求，文件名：{}，音色：{}|{}", file.getOriginalFilename(), boldVoice, normalVoice);
        
        try {
            // 构建音色配置
            VoiceConfig voiceConfig = VoiceConfig.builder()
                    .boldVoice(boldVoice)
                    .normalVoice(normalVoice)
                    .format(audioFormat)
                    .sampleRate(sampleRate)
                    .build();
            
            // 生成音频
            AudioGenerateResponse response = audioGeneratorService.generateAudioFromDocument(file, voiceConfig);
            
            if (Boolean.TRUE.equals(response.getSuccess())) {
                log.info("音频生成成功，任务ID：{}，音频URL：{}", response.getTaskId(), response.getAudioUrl());
                return ResponseEntity.ok(response);
            } else {
                log.error("音频生成失败：{}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("音频生成异常", e);
            return ResponseEntity.internalServerError()
                    .body(AudioGenerateResponse.fail("音频生成异常：" + e.getMessage()));
        }
    }
    
    /**
     * 从Word文档生成音频（接受JSON请求体）
     * 
     * @param file Word文档文件
     * @param request 音频生成请求
     * @return 音频生成响应
     */
    @PostMapping("/generate-with-config")
    @ApiOperation("从Word文档生成音频（接受配置对象）")
    public ResponseEntity<AudioGenerateResponse> generateAudioWithConfig(
            @ApiParam("Word文档文件") @RequestParam("file") MultipartFile file,
            @ApiParam("音频生成配置") @RequestPart(value = "config", required = false) AudioGenerateRequest request
    ) {
        log.info("收到音频生成请求（配置模式），文件名：{}", file.getOriginalFilename());
        
        try {
            // 如果没有提供配置，使用默认配置
            if (request == null) {
                request = AudioGenerateRequest.builder().build();
            }
            
            // 转换为VoiceConfig
            VoiceConfig voiceConfig = request.toVoiceConfig();
            
            // 生成音频
            AudioGenerateResponse response = audioGeneratorService.generateAudioFromDocument(file, voiceConfig);
            
            if (Boolean.TRUE.equals(response.getSuccess())) {
                log.info("音频生成成功，任务ID：{}，音频URL：{}", response.getTaskId(), response.getAudioUrl());
                return ResponseEntity.ok(response);
            } else {
                log.error("音频生成失败：{}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("音频生成异常", e);
            return ResponseEntity.internalServerError()
                    .body(AudioGenerateResponse.fail("音频生成异常：" + e.getMessage()));
        }
    }
}
