package com.hmall.tts.volcengine.service;

import com.hmall.tts.volcengine.dto.DocumentTTSResult;
import com.hmall.tts.volcengine.dto.VoiceConfig;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档TTS服务接口
 * 
 * @author Kiro
 * @since 2026-08-14
 */
public interface DocumentTTSService {
    
    /**
     * 生成文档对话语音
     * 
     * @param file Word文档文件
     * @param voiceConfig 音色配置
     * @return 生成结果
     */
    DocumentTTSResult generateDocumentSpeech(MultipartFile file, VoiceConfig voiceConfig);
    
    /**
     * 生成文档对话语音（返回字节数组）
     * 
     * @param file Word文档文件
     * @param voiceConfig 音色配置
     * @return 音频字节数组
     * @throws Exception 生成失败时抛出异常
     */
    byte[] generateDocumentSpeechBytes(MultipartFile file, VoiceConfig voiceConfig) throws Exception;
}
