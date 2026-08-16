package com.hmall.config;

import com.hmall.tts.whisperx.service.WhisperXServerManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WhisperX 配置类（强制加载 WhisperXServerManager）
 * 
 * @author Kiro
 * @since 2026-08-17
 */
@Slf4j
@Configuration
public class WhisperXConfig {
    
    public WhisperXConfig() {
        System.out.println("========================================");
        System.out.println("[WhisperX Config] ✅ WhisperXConfig 配置类已加载");
        System.out.println("========================================");
        log.info("[WhisperX Config] ✅ WhisperXConfig 配置类已加载");
    }
    
    @Bean
    public WhisperXServerManager whisperXServerManager() {
        System.out.println("========================================");
        System.out.println("[WhisperX Config] ✅ 正在创建 WhisperXServerManager Bean");
        System.out.println("========================================");
        log.info("[WhisperX Config] ✅ 正在创建 WhisperXServerManager Bean");
        return new WhisperXServerManager();
    }
}
