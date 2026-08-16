package com.hmall.tts.audio.service;

import com.hmall.tts.audio.dto.AudioGenerateResponse;
import com.hmall.tts.volcengine.dto.VoiceConfig;
import org.springframework.web.multipart.MultipartFile;

/**
 * 音频生成服务接口
 * 
 * 职责：专门负责音频生成，与视频解耦
 * 
 * @author Kiro
 * @since 2026-08-16
 */
public interface AudioGeneratorService {
    
    /**
     * 从Word文档生成音频
     * 
     * @param file Word文档文件
     * @param voiceConfig 音色配置
     * @return 音频生成响应（包含音频URL、字幕数据）
     * @throws Exception 生成失败时抛出异常
     */
    AudioGenerateResponse generateAudioFromDocument(MultipartFile file, VoiceConfig voiceConfig) throws Exception;
}
