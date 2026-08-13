package com.hmall.tts.volcengine.controller;

import com.hmall.tts.volcengine.dto.TTSRequest;
import com.hmall.tts.volcengine.dto.TTSResponse;
import com.hmall.tts.volcengine.dto.VoiceInfo;
import com.hmall.tts.volcengine.service.VolcengineTTSService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 火山引擎 TTS 控制器
 * 
 * @author Kiro
 * @since 2026-08-13
 */
@Slf4j
@RestController
@RequestMapping("/api/volcengine/tts")
@RequiredArgsConstructor
@Api(tags = "火山引擎TTS接口")
public class VolcengineTTSController {
    
    private final VolcengineTTSService ttsService;
    
    /**
     * 生成语音（返回音频文件URL）
     */
    @PostMapping("/generate")
    @ApiOperation("生成语音（返回文件URL）")
    public ResponseEntity<TTSResponse> generateSpeech(@Valid @RequestBody TTSRequest request) {
        log.info("收到TTS请求: {}", request.getText().substring(0, Math.min(50, request.getText().length())));
        TTSResponse response = ttsService.generateSpeech(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 生成语音（返回Base64编码的音频数据）
     */
    @PostMapping("/generate-base64")
    @ApiOperation("生成语音（返回Base64）")
    public ResponseEntity<TTSResponse> generateSpeechBase64(@Valid @RequestBody TTSRequest request) {
        log.info("收到TTS Base64请求: {}", request.getText().substring(0, Math.min(50, request.getText().length())));
        TTSResponse response = ttsService.generateSpeechBase64(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 生成语音（直接返回音频流）
     */
    @PostMapping("/generate-stream")
    @ApiOperation("生成语音（返回音频流）")
    public ResponseEntity<byte[]> generateSpeechStream(@Valid @RequestBody TTSRequest request) {
        try {
            log.info("收到TTS流式请求: {}", request.getText().substring(0, Math.min(50, request.getText().length())));
            
            byte[] audioData = ttsService.generateSpeechBytes(request);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioData.length);
            headers.set("Content-Disposition", "inline; filename=\"speech.mp3\"");
            
            return new ResponseEntity<>(audioData, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("生成语音流失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取支持的音色列表
     */
    @GetMapping("/voices")
    @ApiOperation("获取音色列表")
    public ResponseEntity<List<VoiceInfo>> getVoiceList() {
        List<VoiceInfo> voices = ttsService.getVoiceList();
        return ResponseEntity.ok(voices);
    }
    
    /**
     * 获取指定音色信息
     */
    @GetMapping("/voices/{voiceId}")
    @ApiOperation("获取指定音色信息")
    public ResponseEntity<VoiceInfo> getVoiceInfo(@PathVariable String voiceId) {
        VoiceInfo voice = ttsService.getVoiceInfo(voiceId);
        if (voice == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(voice);
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    @ApiOperation("健康检查")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean healthy = ttsService.healthCheck();
            result.put("status", healthy ? "ok" : "error");
            result.put("service", "volcengine-tts");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("service", "volcengine-tts");
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 快速测试接口
     */
    @GetMapping("/test")
    @ApiOperation("快速测试")
    public ResponseEntity<TTSResponse> test(@RequestParam(defaultValue = "你好，这是火山引擎TTS测试") String text) {
        TTSRequest request = TTSRequest.builder()
                .text(text)
                .build();
        
        TTSResponse response = ttsService.generateSpeech(request);
        return ResponseEntity.ok(response);
    }
}
