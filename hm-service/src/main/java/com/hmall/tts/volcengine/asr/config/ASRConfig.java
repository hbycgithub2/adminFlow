package com.hmall.tts.volcengine.asr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ASR配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "volcengine.asr")
public class ASRConfig {
    
    /**
     * ASR AppID
     */
    private String appId;
    
    /**
     * ASR Access Token
     */
    private String accessToken;
    
    /**
     * ASR API地址
     */
    private String apiUrl = "https://openspeech.bytedance.com/api/v1/asr";
    
    /**
     * 是否启用ASR（默认禁用，降级到智能估算）
     */
    private Boolean enabled = false;
    
    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectTimeout = 5000;
    
    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout = 30000;
}
