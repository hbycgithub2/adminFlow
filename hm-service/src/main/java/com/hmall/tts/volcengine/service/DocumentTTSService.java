package com.hmall.tts.volcengine.service;

import com.hmall.tts.volcengine.dto.DialogSegment;
import com.hmall.tts.volcengine.dto.DocumentTTSResult;
import com.hmall.tts.volcengine.dto.VoiceConfig;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
     * 生成文档对话语音（支持跳过WhisperX对齐）⭐
     * 
     * 使用场景：
     * 1. Manual模式生成音频：skipAlignment=true，使用智能算法（快速）
     * 2. Auto模式生成视频：skipAlignment=false，使用WhisperX对齐（精确）
     * 
     * @param file Word文档文件
     * @param voiceConfig 音色配置
     * @param skipAlignment 是否跳过WhisperX对齐（true=跳过，使用智能算法；false=使用WhisperX）
     * @return 生成结果
     */
    DocumentTTSResult generateDocumentSpeech(MultipartFile file, VoiceConfig voiceConfig, boolean skipAlignment);
    
    /**
     * 生成文档对话语音（返回字节数组）
     * 
     * @param file Word文档文件
     * @param voiceConfig 音色配置
     * @return 音频字节数组
     * @throws Exception 生成失败时抛出异常
     */
    byte[] generateDocumentSpeechBytes(MultipartFile file, VoiceConfig voiceConfig) throws Exception;
    
    /**
     * 生成文档对话语音（简化参数版本）
     * 
     * @param file Word文档文件
     * @param boldVoice 加粗文本音色
     * @param normalVoice 非加粗文本音色
     * @param format 音频格式
     * @param sampleRate 采样率
     * @return 音频字节数组
     * @throws Exception 生成失败时抛出异常
     */
    byte[] generateSpeechFromDocument(MultipartFile file, String boldVoice, String normalVoice, 
                                     String format, Integer sampleRate) throws Exception;
    
    /**
     * 获取对话片段信息（用于生成字幕）
     * 
     * @param file Word文档文件
     * @param boldVoice 加粗文本音色
     * @param normalVoice 非加粗文本音色
     * @return 对话片段列表
     * @throws Exception 解析失败时抛出异常
     */
    List<DialogSegment> getDialogSegments(MultipartFile file, String boldVoice, String normalVoice) throws Exception;
}
