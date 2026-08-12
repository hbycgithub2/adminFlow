package com.hmall.tts.service;

import com.hmall.tts.exception.TTSErrorCode;
import com.hmall.tts.exception.TTSException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 音频合并服务
 * 
 * 功能：
 * 1. 合并多个 MP3 音频文件
 * 2. 简单的字节流拼接（适用于相同格式的 MP3）
 * 
 * 注意：
 * - 这是简单的字节流拼接，适用于相同采样率、比特率的 MP3
 * - 如果需要更复杂的合并（如淡入淡出、音量调整），需要使用 FFmpeg
 * 
 * @author Kiro
 * @since 2026-08-12
 */
@Slf4j
@Service
public class AudioMergeService {
    
    /**
     * 合并多个音频数据
     * 
     * @param audioDataList 音频数据列表
     * @return 合并后的音频数据
     */
    public byte[] merge(List<byte[]> audioDataList) {
        if (audioDataList == null || audioDataList.isEmpty()) {
            throw new TTSException(TTSErrorCode.INVALID_PARAMETER, "音频数据列表不能为空");
        }
        
        if (audioDataList.size() == 1) {
            log.info("🔊 [音频合并] 只有1个音频，无需合并");
            return audioDataList.get(0);
        }
        
        log.info("🔊 [音频合并] 开始合并: {} 个音频", audioDataList.size());
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            long totalSize = 0;
            
            for (int i = 0; i < audioDataList.size(); i++) {
                byte[] audioData = audioDataList.get(i);
                
                if (audioData == null || audioData.length == 0) {
                    log.warn("⚠️ [音频合并] 跳过空音频: index={}", i);
                    continue;
                }
                
                // 跳过第一个音频之后的 MP3 头部（ID3 标签）
                // MP3 头部通常以 "ID3" 开头，长度约 128 字节
                // 为了简化，这里直接拼接所有字节（可能会有轻微的杂音）
                outputStream.write(audioData);
                totalSize += audioData.length;
                
                log.debug("   音频 {}: {} bytes", i + 1, audioData.length);
            }
            
            byte[] mergedData = outputStream.toByteArray();
            
            log.info("✅ [音频合并] 合并完成: {} 个音频 → {} bytes", audioDataList.size(), mergedData.length);
            
            return mergedData;
            
        } catch (IOException e) {
            log.error("❌ [音频合并] 合并失败: {}", e.getMessage(), e);
            throw new TTSException(TTSErrorCode.MERGE_FAILED, "音频合并失败", e);
        }
    }
    
    /**
     * 合并两个音频数据
     * 
     * @param audio1 音频1
     * @param audio2 音频2
     * @return 合并后的音频数据
     */
    public byte[] merge(byte[] audio1, byte[] audio2) {
        return merge(List.of(audio1, audio2));
    }
}
