package com.hmall.tts.volcengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 火山引擎 TTS 配置类
 * 
 * @author Kiro
 * @since 2026-08-13
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "volcengine.tts")
public class VolcengineConfig {

    /**
     * API Key（火山引擎控制台获取）
     */
    private String apiKey = "a83eef4b-bde3-4cbf-ac5f-0a35a17b31ad";

    /**
     * Resource ID（TTS服务资源ID）
     */
    private String resourceId = "seed-tts-2.0";

    /**
     * 大模型 Resource ID（用于男声等特定音色）
     */
    private String resourceIdBigModel = "volcano_tts";

    /**
     * 是否自动选择 Resource ID（根据音色自动选择）
     */
    private boolean autoSelectResourceId = true;

    /**
     * API URL（火山引擎 TTS 接口地址）
     */
    private String url = "https://openspeech.bytedance.com/api/v3/tts/unidirectional";

    /**
     * 连接超时时间（秒）
     */
    private int connectTimeout = 30;

    /**
     * 请求超时时间（分钟）
     */
    private int requestTimeout = 5;

    /**
     * 默认音色
     */
    private String defaultSpeaker = "zh_female_vv_uranus_bigtts";

    /**
     * 默认音频格式
     */
    private String defaultFormat = "mp3";

    /**
     * 默认采样率
     */
    private int defaultSampleRate = 24000;
    
    /**
     * 音频文件输出目录
     */
    private String outputDir = "tts";

    /**
     * 根据音色选择合适的 Resource ID
     *
     * @param speaker 音色ID
     * @return Resource ID
     */
    public String getResourceIdForSpeaker(String speaker) {
        if (!autoSelectResourceId) {
            return resourceId;
        }

        // 如果是男声或某些特定音色，使用大模型 Resource ID
        if (speaker != null && (
                speaker.contains("male") ||  // 男声
                        speaker.contains("calm")     // 平静系列
        )) {
            return resourceIdBigModel;
        }

        // 默认使用 seed-tts-2.0
        return resourceId;
    }
}
