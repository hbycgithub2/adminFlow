package com.hmall.tts.volcengine.service;

import com.hmall.tts.volcengine.dto.TTSRequest;
import com.hmall.tts.volcengine.dto.TTSResponse;
import com.hmall.tts.volcengine.dto.VoiceInfo;

import java.util.List;

/**
 * 火山引擎 TTS 服务接口
 * 
 * @author Kiro
 * @since 2026-08-13
 */
public interface VolcengineTTSService {
    
    /**
     * 生成语音（返回音频文件路径）
     * 
     * @param request TTS请求参数
     * @return TTS响应结果
     */
    TTSResponse generateSpeech(TTSRequest request);
    
    /**
     * 生成语音（返回Base64编码的音频数据）
     * 
     * @param request TTS请求参数
     * @return TTS响应结果
     */
    TTSResponse generateSpeechBase64(TTSRequest request);
    
    /**
     * 生成语音（返回音频字节数组）
     * 
     * @param request TTS请求参数
     * @return 音频字节数组
     */
    byte[] generateSpeechBytes(TTSRequest request) throws Exception;
    
    /**
     * 获取支持的音色列表
     * 
     * @return 音色列表
     */
    List<VoiceInfo> getVoiceList();
    
    /**
     * 获取指定音色信息
     * 
     * @param voiceId 音色ID
     * @return 音色信息
     */
    VoiceInfo getVoiceInfo(String voiceId);
    
    /**
     * 健康检查
     * 
     * @return 是否健康
     */
    boolean healthCheck();
}
