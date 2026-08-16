package com.hmall.tts.whisperx.service;

import com.hmall.tts.whisperx.dto.CharTimestamp;

import java.util.List;

/**
 * WhisperX强制对齐服务接口
 * 
 * 核心功能：
 * 1. 将音频与原文精确对齐
 * 2. 返回字符级时间戳
 * 3. 准确率：98-99%
 * 
 * @author Kiro
 * @since 2026-08-15
 */
public interface WhisperXService {
    
    /**
     * 强制对齐音频和文字
     * 
     * 核心原理：
     * 1. Whisper快速识别语言和分段
     * 2. Wav2Vec2在音频中找到每个字的精确时间点
     * 3. 输出：原文 + 精确时间戳（不使用Whisper识别的文字）
     * 
     * @param audioData 音频数据（MP3/WAV等格式）
     * @param originalText 原始文本（100%准确的文字）
     * @return 字符级时间戳列表
     * @throws Exception 对齐失败时抛出异常
     */
    List<CharTimestamp> align(byte[] audioData, String originalText) throws Exception;
    
    /**
     * 批量对齐
     * 
     * @param audioDataList 音频数据列表
     * @param originalTextList 原文列表
     * @return 字符级时间戳列表的列表
     * @throws Exception 对齐失败时抛出异常
     */
    List<List<CharTimestamp>> alignBatch(List<byte[]> audioDataList, List<String> originalTextList) throws Exception;
    
    /**
     * 检查WhisperX服务是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
}
