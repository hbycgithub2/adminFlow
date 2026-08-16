package com.hmall.tts.whisper.service;

import com.hmall.tts.whisper.dto.WordTimestamp;

import java.util.List;

/**
 * Whisper语音识别服务接口
 * 
 * @author Kiro
 * @since 2026-08-14
 */
public interface WhisperService {
    
    /**
     * 识别音频，返回逐字时间戳
     * 
     * @param audioData 音频数据（MP3格式）
     * @return 逐字时间戳列表
     * @throws Exception 识别失败时抛出异常
     */
    List<WordTimestamp> transcribe(byte[] audioData) throws Exception;
    
    /**
     * 识别音频，返回逐字时间戳（带提示文本）
     * 
     * @param audioData 音频数据（MP3格式）
     * @param promptText 提示文本（原文），帮助Whisper更准确识别
     * @return 逐字时间戳列表
     * @throws Exception 识别失败时抛出异常
     */
    List<WordTimestamp> transcribeWithPrompt(byte[] audioData, String promptText) throws Exception;
    
    /**
     * 批量识别音频
     * 
     * @param audioDataList 音频数据列表
     * @return 逐字时间戳列表的列表
     * @throws Exception 识别失败时抛出异常
     */
    List<List<WordTimestamp>> transcribeBatch(List<byte[]> audioDataList) throws Exception;
    
    /**
     * 检查Whisper服务是否可用
     * 
     * @return true表示可用，false表示不可用
     */
    boolean isAvailable();
}
