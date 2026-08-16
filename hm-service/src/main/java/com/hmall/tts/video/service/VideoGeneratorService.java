package com.hmall.tts.video.service;

import com.hmall.tts.video.dto.VideoFromAudioRequest;
import com.hmall.tts.video.dto.VideoGenerateRequest;
import com.hmall.tts.video.dto.VideoGenerateResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频生成服务接口
 * 
 * @author Kiro
 * @since 2026-08-16
 */
public interface VideoGeneratorService {
    
    /**
     * 从Word文档生成视频（接口1：一键生成）
     * 
     * @param file Word文档文件
     * @param request 视频生成请求
     * @return 视频生成响应（增强：包含audioUrl）
     * @throws Exception 生成失败时抛出异常
     */
    VideoGenerateResponse generateVideoFromDocument(MultipartFile file, VideoGenerateRequest request) throws Exception;
    
    /**
     * 从音频文件生成视频（接口3：上传MP3生成视频）⭐
     * 
     * 支持三种模式：
     * 1. 有原始字幕 + 原始MP3 → 直接使用字幕
     * 2. 有原始字幕 + 编辑后MP3 → 重新对齐字幕
     * 3. 无字幕 + 自定义MP3 → 识别生成字幕
     * 
     * @param audioFile 音频文件（MP3、WAV等）
     * @param request 请求参数（字幕数据、原始文本、配置等）
     * @return 视频生成响应
     * @throws Exception 生成失败时抛出异常
     */
    VideoGenerateResponse generateVideoFromAudio(MultipartFile audioFile, VideoFromAudioRequest request) throws Exception;
}
