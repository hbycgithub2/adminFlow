package com.hmall.tts.whisperx.controller;

import com.hmall.tts.whisperx.service.impl.WhisperXServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * WhisperX服务管理接口
 * 
 * @author Kiro
 * @since 2026-08-17
 */
@Slf4j
@RestController
@RequestMapping("/api/whisperx")
@RequiredArgsConstructor
public class WhisperXManagementController {
    
    private final WhisperXServiceImpl whisperxService;
    
    /**
     * 查看服务状态
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return whisperxService.getServerStatus();
    }
    
    /**
     * 手动停止服务（已禁用，服务由 HMallApplication 主类管理）
     */
    @PostMapping("/stop")
    public Map<String, Object> stopServer() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "手动停止已禁用，服务由 HMallApplication 主类管理，请停止 Spring Boot 应用");
        return result;
    }
    
    /**
     * 手动启动服务（已禁用，服务由 HMallApplication 主类管理）
     */
    @PostMapping("/start")
    public Map<String, Object> startServer() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "手动启动已禁用，服务已在 Spring Boot 启动时自动启动");
        return result;
    }
}
