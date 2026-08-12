package com.hmall.tts.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Edge TTS 配置属性
 * 
 * @author Kiro
 * @since 2026-08-12
 */
@Data
@Component
@ConfigurationProperties(prefix = "edge-tts")
public class EdgeTTSProperties {
    
    /**
     * edge-tts 命令路径
     */
    private String command = "py -m edge_tts";
    
    /**
     * 超时时间（秒）
     */
    private long timeout = 30;
    
    /**
     * 临时文件目录
     */
    private String tempDir = "temp";
    
    /**
     * 长文本分段最大长度（字符）
     */
    private int maxSegmentLength = 500;
    
    /**
     * 是否启用缓存
     */
    private boolean cacheEnabled = false;
    
    /**
     * 缓存过期时间（秒）
     */
    private long cacheExpire = 3600;
}
