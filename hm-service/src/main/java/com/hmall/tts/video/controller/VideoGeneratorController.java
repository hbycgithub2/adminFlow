package com.hmall.tts.video.controller;

import com.hmall.tts.video.animation.AnimationType;
import com.hmall.tts.video.dto.*;
import com.hmall.tts.video.service.VideoGeneratorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 视频生成控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/video-generator")
@Api(tags = "视频生成")
public class VideoGeneratorController {
    
    @Autowired
    private VideoGeneratorService videoGeneratorService;
    
    /**
     * 生成视频（从Word文档）
     */
    @PostMapping("/generate")
    @ApiOperation("从Word文档生成带字幕的视频")
    public ResponseEntity<VideoGenerateResponse> generateVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "boldVoice", defaultValue = "zh_male_m191_uranus_bigtts") String boldVoice,
            @RequestParam(value = "normalVoice", defaultValue = "zh_female_vv_uranus_bigtts") String normalVoice,
            @RequestParam(value = "audioFormat", defaultValue = "mp3") String audioFormat,
            @RequestParam(value = "sampleRate", defaultValue = "24000") Integer sampleRate,
            // 视频配置
            @RequestParam(value = "videoWidth", defaultValue = "1920") Integer videoWidth,
            @RequestParam(value = "videoHeight", defaultValue = "1080") Integer videoHeight,
            @RequestParam(value = "videoFps", defaultValue = "30") Integer videoFps,
            @RequestParam(value = "backgroundColor", defaultValue = "#FFFFFF") String backgroundColor,
            @RequestParam(value = "videoBitrate", defaultValue = "2000") Integer videoBitrate,
            // 字幕配置
            @RequestParam(value = "fontName", defaultValue = "Arial") String fontName,
            @RequestParam(value = "fontSize", defaultValue = "48") Integer fontSize,
            @RequestParam(value = "fontColor", defaultValue = "#FFFFFF") String fontColor,
            @RequestParam(value = "borderColor", defaultValue = "#000000") String borderColor,
            @RequestParam(value = "borderWidth", defaultValue = "2") Integer borderWidth,
            @RequestParam(value = "shadowDistance", defaultValue = "2") Integer shadowDistance,
            @RequestParam(value = "subtitlePosition", defaultValue = "2") Integer subtitlePosition,
            @RequestParam(value = "animationType", defaultValue = "fade") String animationType
    ) {
        try {
            log.info("收到视频生成请求：文件名={}, 动画类型={}", file.getOriginalFilename(), animationType);
            
            // 构建视频配置
            VideoConfig videoConfig = VideoConfig.builder()
                    .width(videoWidth)
                    .height(videoHeight)
                    .fps(videoFps)
                    .backgroundColor(backgroundColor)
                    .bitrate(videoBitrate)
                    .build();
            
            // 构建字幕配置
            SubtitleConfig subtitleConfig = SubtitleConfig.builder()
                    .fontName(fontName)
                    .fontSize(fontSize)
                    .fontColor(fontColor)
                    .borderColor(borderColor)
                    .borderWidth(borderWidth)
                    .shadowDistance(shadowDistance)
                    .position(subtitlePosition)
                    .animationType(AnimationType.fromCode(animationType))
                    .build();
            
            // 构建请求
            VideoGenerateRequest request = VideoGenerateRequest.builder()
                    .boldVoice(boldVoice)
                    .normalVoice(normalVoice)
                    .audioFormat(audioFormat)
                    .sampleRate(sampleRate)
                    .videoConfig(videoConfig)
                    .subtitleConfig(subtitleConfig)
                    .build();
            
            // 生成视频
            VideoGenerateResponse response = videoGeneratorService.generateVideoFromDocument(file, request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("视频生成失败", e);
            return ResponseEntity.ok(
                    VideoGenerateResponse.builder()
                            .success(false)
                            .message("视频生成失败：" + e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * 获取可用的动画类型列表
     */
    @GetMapping("/animation-types")
    @ApiOperation("获取可用的动画类型列表")
    public ResponseEntity<List<Map<String, String>>> getAnimationTypes() {
        List<Map<String, String>> animationTypes = Arrays.stream(AnimationType.values())
                .map(type -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("code", type.getCode());
                    map.put("description", type.getDescription());
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(animationTypes);
    }
    
    /**
     * 获取可用的音色列表
     */
    @GetMapping("/voices")
    @ApiOperation("获取可用的音色列表")
    public ResponseEntity<Map<String, List<Map<String, String>>>> getVoices() {
        Map<String, List<Map<String, String>>> voices = new HashMap<>();
        
        // 男声
        List<Map<String, String>> maleVoices = Arrays.asList(
                createVoiceMap("zh_male_m191_uranus_bigtts", "云舟（沉稳男声）"),
                createVoiceMap("zh_male_taocheng_uranus_bigtts", "小天（阳光男声）")
        );
        
        // 女声
        List<Map<String, String>> femaleVoices = Arrays.asList(
                createVoiceMap("zh_female_vv_uranus_bigtts", "薇薇（温柔女声）"),
                createVoiceMap("zh_female_xiaohe_uranus_bigtts", "小何（甜美女声）")
        );
        
        voices.put("male", maleVoices);
        voices.put("female", femaleVoices);
        
        return ResponseEntity.ok(voices);
    }
    
    private Map<String, String> createVoiceMap(String code, String name) {
        Map<String, String> map = new HashMap<>();
        map.put("code", code);
        map.put("name", name);
        return map;
    }
    
    /**
     * 从音频文件生成视频（接口3）⭐
     * 
     * 支持三种模式：
     * 1. 有原始字幕 + 原始MP3 → 直接使用字幕
     * 2. 有原始字幕 + 编辑后MP3 → 重新对齐字幕
     * 3. 无字幕 + 自定义MP3 → 识别生成字幕
     * 
     * @param audioFile 音频文件（MP3、WAV等）
     * @param subtitlesJson 字幕数据JSON（可选）
     * @param originalText 原始文本（可选，用于重新对齐）
     * @param forceReAlign 是否强制重新对齐
     * @param videoWidth 视频宽度
     * @param videoHeight 视频高度
     * @param videoFps 视频帧率
     * @param backgroundColor 背景颜色
     * @param videoBitrate 视频比特率
     * @param fontName 字体名称
     * @param fontSize 字体大小
     * @param fontColor 字体颜色
     * @param borderColor 边框颜色
     * @param borderWidth 边框宽度
     * @param shadowDistance 阴影距离
     * @param subtitlePosition 字幕位置
     * @param animationType 动画类型
     * @return 视频生成响应
     */
    @PostMapping("/generate-from-audio")
    @ApiOperation("从音频文件生成视频")
    public ResponseEntity<VideoGenerateResponse> generateVideoFromAudio(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "subtitles", required = false) String subtitlesJson,
            @RequestParam(value = "originalText", required = false) String originalText,
            @RequestParam(value = "forceReAlign", defaultValue = "false") Boolean forceReAlign,
            // 视频配置
            @RequestParam(value = "videoWidth", defaultValue = "1920") Integer videoWidth,
            @RequestParam(value = "videoHeight", defaultValue = "1080") Integer videoHeight,
            @RequestParam(value = "videoFps", defaultValue = "30") Integer videoFps,
            @RequestParam(value = "backgroundColor", defaultValue = "#FFFFFF") String backgroundColor,
            @RequestParam(value = "videoBitrate", defaultValue = "2000") Integer videoBitrate,
            // 字幕配置
            @RequestParam(value = "fontName", defaultValue = "Arial") String fontName,
            @RequestParam(value = "fontSize", defaultValue = "48") Integer fontSize,
            @RequestParam(value = "fontColor", defaultValue = "#FFFFFF") String fontColor,
            @RequestParam(value = "borderColor", defaultValue = "#000000") String borderColor,
            @RequestParam(value = "borderWidth", defaultValue = "2") Integer borderWidth,
            @RequestParam(value = "shadowDistance", defaultValue = "2") Integer shadowDistance,
            @RequestParam(value = "subtitlePosition", defaultValue = "2") Integer subtitlePosition,
            @RequestParam(value = "animationType", defaultValue = "fade") String animationType
    ) {
        try {
            log.info("收到从音频生成视频请求：文件名={}, 有字幕={}, 有原文={}, 强制重对齐={}", 
                    audioFile.getOriginalFilename(), 
                    subtitlesJson != null && !subtitlesJson.isEmpty(),
                    originalText != null && !originalText.isEmpty(),
                    forceReAlign);
            
            // 构建视频配置
            VideoConfig videoConfig = VideoConfig.builder()
                    .width(videoWidth)
                    .height(videoHeight)
                    .fps(videoFps)
                    .backgroundColor(backgroundColor)
                    .bitrate(videoBitrate)
                    .build();
            
            // 构建字幕配置
            SubtitleConfig subtitleConfig = SubtitleConfig.builder()
                    .fontName(fontName)
                    .fontSize(fontSize)
                    .fontColor(fontColor)
                    .borderColor(borderColor)
                    .borderWidth(borderWidth)
                    .shadowDistance(shadowDistance)
                    .position(subtitlePosition)
                    .animationType(AnimationType.fromCode(animationType))
                    .build();
            
            // 解析字幕JSON
            List<com.hmall.tts.volcengine.dto.DialogSegment> subtitles = null;
            if (subtitlesJson != null && !subtitlesJson.isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    subtitles = objectMapper.readValue(
                            subtitlesJson, 
                            objectMapper.getTypeFactory().constructCollectionType(
                                    List.class, 
                                    com.hmall.tts.volcengine.dto.DialogSegment.class
                            )
                    );
                    log.info("解析字幕数据成功，共{}个片段", subtitles.size());
                } catch (Exception e) {
                    log.warn("解析字幕JSON失败，将忽略字幕数据：{}", e.getMessage());
                }
            }
            
            // 构建请求
            VideoFromAudioRequest request = VideoFromAudioRequest.builder()
                    .subtitles(subtitles)
                    .originalText(originalText)
                    .videoConfig(videoConfig)
                    .subtitleConfig(subtitleConfig)
                    .forceReAlign(forceReAlign)
                    .build();
            
            // 生成视频
            VideoGenerateResponse response = videoGeneratorService.generateVideoFromAudio(audioFile, request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("从音频生成视频失败", e);
            return ResponseEntity.ok(
                    VideoGenerateResponse.builder()
                            .success(false)
                            .message("视频生成失败：" + e.getMessage())
                            .build()
            );
        }
    }
}
