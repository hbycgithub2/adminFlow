package com.hmall.tts.volcengine.service.impl;

import com.hmall.tts.volcengine.client.VolcengineClient;
import com.hmall.tts.volcengine.config.VolcengineConfig;
import com.hmall.tts.volcengine.dto.TTSRequest;
import com.hmall.tts.volcengine.dto.TTSResponse;
import com.hmall.tts.volcengine.dto.VoiceInfo;
import com.hmall.tts.volcengine.service.VolcengineTTSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 火山引擎 TTS 服务实现类
 * 
 * @author Kiro
 * @since 2026-08-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VolcengineTTSServiceImpl implements VolcengineTTSService {
    
    private final VolcengineClient client;
    private final VolcengineConfig config;
    
    @Override
    public TTSResponse generateSpeech(TTSRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始生成语音，文本长度: {}", request.getText().length());
            
            // 1. 构建请求体
            String payload = buildPayload(request);
            
            // 2. 获取音色（用于选择Resource ID）
            String speaker = request.getSpeaker() != null ? 
                    request.getSpeaker() : config.getDefaultSpeaker();
            
            // 3. 发送请求获取音频数据
            byte[] audioData = client.sendTTSRequest(payload, speaker);
            
            // 4. 保存音频文件
            String fileName = UUID.randomUUID().toString() + "." + getFormat(request);
            Path outputDir = Paths.get(config.getOutputDir());
            Files.createDirectories(outputDir);
            
            Path audioFile = outputDir.resolve(fileName);
            Files.write(audioFile, audioData);
            
            long generateTime = System.currentTimeMillis() - startTime;
            
            log.info("语音生成成功，文件: {}, 大小: {} KB, 耗时: {} ms", 
                    fileName, audioData.length / 1024.0, generateTime);
            
            // 4. 构建响应
            return TTSResponse.builder()
                    .success(true)
                    .message("语音生成成功")
                    .audioPath(audioFile.toString())
                    .audioUrl("/tts/" + fileName)
                    .audioSize((long) audioData.length)
                    .generateTime(generateTime)
                    .build();
            
        } catch (Exception e) {
            log.error("语音生成失败: {}", e.getMessage(), e);
            return TTSResponse.fail("语音生成失败: " + e.getMessage(), -1);
        }
    }
    
    @Override
    public TTSResponse generateSpeechBase64(TTSRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始生成语音（Base64），文本长度: {}", request.getText().length());
            
            // 1. 构建请求体
            String payload = buildPayload(request);
            
            // 2. 获取音色（用于选择Resource ID）
            String speaker = request.getSpeaker() != null ? 
                    request.getSpeaker() : config.getDefaultSpeaker();
            
            // 3. 发送请求获取音频数据
            byte[] audioData = client.sendTTSRequest(payload, speaker);
            
            // 4. Base64编码
            String audioBase64 = Base64.getEncoder().encodeToString(audioData);
            
            long generateTime = System.currentTimeMillis() - startTime;
            
            log.info("语音生成成功（Base64），大小: {} KB, 耗时: {} ms", 
                    audioData.length / 1024.0, generateTime);
            
            // 4. 构建响应
            return TTSResponse.builder()
                    .success(true)
                    .message("语音生成成功")
                    .audioData(audioBase64)
                    .audioSize((long) audioData.length)
                    .generateTime(generateTime)
                    .build();
            
        } catch (Exception e) {
            log.error("语音生成失败: {}", e.getMessage(), e);
            return TTSResponse.fail("语音生成失败: " + e.getMessage(), -1);
        }
    }
    
    @Override
    public byte[] generateSpeechBytes(TTSRequest request) throws Exception {
        log.info("开始生成语音（字节数组），文本长度: {}", request.getText().length());
        
        // 1. 构建请求体
        String payload = buildPayload(request);
        
        // 2. 获取音色（用于选择Resource ID）
        String speaker = request.getSpeaker() != null ? 
                request.getSpeaker() : config.getDefaultSpeaker();
        
        // 3. 发送请求获取音频数据
        return client.sendTTSRequest(payload, speaker);
    }
    
    @Override
    public List<VoiceInfo> getVoiceList() {
        List<VoiceInfo> voices = new ArrayList<>();
        
        // 中文音色
        voices.add(VoiceInfo.builder()
                .voiceId("zh_female_vv_uranus_bigtts")
                .voiceName("晓晓")
                .description("温柔女声，适合讲故事、客服")
                .gender("female")
                .language("zh-CN")
                .style("gentle")
                .recommended(true)
                .build());
        
        voices.add(VoiceInfo.builder()
                .voiceId("zh_male_vv_uranus_bigtts")
                .voiceName("云扬")
                .description("沉稳男声，适合新闻播报、商务")
                .gender("male")
                .language("zh-CN")
                .style("calm")
                .recommended(true)
                .build());
        
        voices.add(VoiceInfo.builder()
                .voiceId("zh_female_calm_uranus_bigtts")
                .voiceName("晓静")
                .description("平静女声，适合教育、解说")
                .gender("female")
                .language("zh-CN")
                .style("calm")
                .recommended(false)
                .build());
        
        voices.add(VoiceInfo.builder()
                .voiceId("zh_male_calm_uranus_bigtts")
                .voiceName("云舒")
                .description("平静男声，适合知识讲解")
                .gender("male")
                .language("zh-CN")
                .style("calm")
                .recommended(false)
                .build());
        
        // 英文音色
        voices.add(VoiceInfo.builder()
                .voiceId("en_female_vv_uranus_bigtts")
                .voiceName("Emma")
                .description("温柔女声（英文）")
                .gender("female")
                .language("en-US")
                .style("gentle")
                .recommended(false)
                .build());
        
        voices.add(VoiceInfo.builder()
                .voiceId("en_male_vv_uranus_bigtts")
                .voiceName("Tom")
                .description("沉稳男声（英文）")
                .gender("male")
                .language("en-US")
                .style("calm")
                .recommended(false)
                .build());
        
        return voices;
    }
    
    @Override
    public VoiceInfo getVoiceInfo(String voiceId) {
        return getVoiceList().stream()
                .filter(voice -> voice.getVoiceId().equals(voiceId))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public boolean healthCheck() {
        return client.checkHealth();
    }
    
    /**
     * 构建请求 payload
     */
    private String buildPayload(TTSRequest request) {
        String speaker = request.getSpeaker() != null ? 
                request.getSpeaker() : config.getDefaultSpeaker();
        String format = getFormat(request);
        int sampleRate = request.getSampleRate() != null ? 
                request.getSampleRate() : config.getDefaultSampleRate();
        
        StringBuilder payload = new StringBuilder();
        payload.append("{");
        payload.append("\"req_params\":{");
        payload.append("\"text\":\"").append(escapeJson(request.getText())).append("\",");
        payload.append("\"speaker\":\"").append(speaker).append("\",");
        payload.append("\"audio_params\":{");
        payload.append("\"format\":\"").append(format).append("\",");
        payload.append("\"sample_rate\":").append(sampleRate);
        
        // 可选参数
        if (request.getSpeed() != null) {
            payload.append(",\"speed\":").append(request.getSpeed());
        }
        if (request.getVolume() != null) {
            payload.append(",\"volume\":").append(request.getVolume());
        }
        if (request.getPitch() != null) {
            payload.append(",\"pitch\":").append(request.getPitch());
        }
        
        payload.append("}");
        payload.append("}");
        payload.append("}");
        
        return payload.toString();
    }
    
    /**
     * 获取音频格式
     */
    private String getFormat(TTSRequest request) {
        return request.getFormat() != null ? 
                request.getFormat() : config.getDefaultFormat();
    }
    
    /**
     * 转义 JSON 字符串
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
