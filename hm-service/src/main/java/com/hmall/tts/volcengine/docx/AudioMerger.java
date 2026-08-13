package com.hmall.tts.volcengine.docx;

import com.hmall.tts.volcengine.dto.AudioSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * 音频合并器
 * 将多个音频片段合并为一个完整的音频文件
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AudioMerger {
    
    private final SmartPauseCalculator pauseCalculator;
    
    /**
     * 合并音频片段
     * 
     * @param audioSegments 音频片段列表
     * @param sampleRate 采样率
     * @return 合并后的音频字节数组
     * @throws Exception 合并失败时抛出异常
     */
    public byte[] merge(List<AudioSegment> audioSegments, int sampleRate) throws Exception {
        log.info("开始合并音频片段，片段数: {}", audioSegments.size());
        
        if (audioSegments.isEmpty()) {
            throw new Exception("音频片段列表为空");
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            for (int i = 0; i < audioSegments.size(); i++) {
                AudioSegment segment = audioSegments.get(i);
                
                // 写入音频数据
                outputStream.write(segment.getAudioData());
                log.debug("写入音频片段 {}/{}, 大小: {} 字节", 
                        i + 1, audioSegments.size(), segment.getAudioData().length);
                
                // 如果需要停顿，添加静音
                if (segment.getNeedPause() != null && segment.getNeedPause()) {
                    int pauseDuration = segment.getPauseDuration();
                    if (pauseDuration > 0) {
                        byte[] silence = pauseCalculator.generateSilence(pauseDuration, sampleRate);
                        outputStream.write(silence);
                        log.debug("添加停顿: {}ms", pauseDuration);
                    }
                }
            }
            
            byte[] mergedAudio = outputStream.toByteArray();
            log.info("音频合并完成，总大小: {} KB", mergedAudio.length / 1024.0);
            
            return mergedAudio;
            
        } catch (Exception e) {
            log.error("音频合并失败: {}", e.getMessage(), e);
            throw new Exception("音频合并失败: " + e.getMessage(), e);
        } finally {
            outputStream.close();
        }
    }
    
    /**
     * 简单合并（无停顿）
     * 
     * @param audioList 音频字节数组列表
     * @return 合并后的音频字节数组
     * @throws Exception 合并失败时抛出异常
     */
    public byte[] mergeSimple(List<byte[]> audioList) throws Exception {
        log.info("开始简单合并音频，片段数: {}", audioList.size());
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            for (byte[] audio : audioList) {
                outputStream.write(audio);
            }
            
            byte[] mergedAudio = outputStream.toByteArray();
            log.info("简单合并完成，总大小: {} KB", mergedAudio.length / 1024.0);
            
            return mergedAudio;
            
        } catch (Exception e) {
            log.error("简单合并失败: {}", e.getMessage(), e);
            throw new Exception("简单合并失败: " + e.getMessage(), e);
        } finally {
            outputStream.close();
        }
    }
}
