package com.hmall.tts.volcengine.client;

import com.hmall.tts.volcengine.config.VolcengineConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 火山引擎 HTTP 客户端
 * 负责与火山引擎 TTS API 进行通信
 * 
 * @author Kiro
 * @since 2026-08-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VolcengineClient {
    
    private final VolcengineConfig config;
    
    private static final Pattern CODE_PATTERN = Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");
    private static final Pattern DATA_PATTERN = Pattern.compile("\"data\"\\s*:\\s*\"([^\"]*)\"");
    
    // ✅ 复用HttpClient，避免连接池耗尽
    private volatile HttpClient sharedHttpClient;
    
    /**
     * 创建一个信任所有证书的SSLContext
     * 注意：生产环境中应该使用正确的证书验证
     */
    private SSLContext createTrustAllSSLContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext;
        } catch (Exception e) {
            log.error("创建SSLContext失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取或创建共享的HttpClient实例（双检锁单例）
     */
    private HttpClient getOrCreateHttpClient() {
        if (sharedHttpClient == null) {
            synchronized (this) {
                if (sharedHttpClient == null) {
                    SSLContext sslContext = createTrustAllSSLContext();
                    HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(config.getConnectTimeout()))
                            .version(HttpClient.Version.HTTP_1_1)
                            .followRedirects(HttpClient.Redirect.NORMAL);
                    
                    if (sslContext != null) {
                        clientBuilder.sslContext(sslContext);
                        log.info("✅ 已创建共享HttpClient实例");
                    }
                    
                    sharedHttpClient = clientBuilder.build();
                }
            }
        }
        return sharedHttpClient;
    }
    
    /**
     * 发送 TTS 请求并获取音频数据（带重试机制）
     * 
     * @param payload JSON 格式的请求体
     * @param speaker 音色ID（用于选择Resource ID）
     * @return 音频数据（字节数组）
     * @throws Exception 请求失败时抛出异常
     */
    public byte[] sendTTSRequest(String payload, String speaker) throws Exception {
        int maxRetries = 2;  // 最多重试2次
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                if (attempt > 1) {
                    log.warn("第{}次重试...", attempt - 1);
                    Thread.sleep(1000 * attempt);  // 递增等待：1s, 2s, 3s
                }
                return sendTTSRequestInternal(payload, speaker);
            } catch (Exception e) {
                lastException = e;
                if (attempt <= maxRetries) {
                    log.warn("第{}次请求失败: {}，将重试", attempt, e.getMessage());
                } else {
                    log.error("所有重试失败（共{}次），最终失败: {}", maxRetries + 1, e.getMessage());
                }
            }
        }
        throw lastException;
    }
    
    /**
     * 发送 TTS 请求的内部实现
     */
    private byte[] sendTTSRequestInternal(String payload, String speaker) throws Exception {
        log.info("发送TTS请求，payload长度: {}, 音色: {}", payload.length(), speaker);
        log.info("API URL: {}", config.getUrl());
        log.info("API Key 长度: {}", config.getApiKey().length());
        
        // 根据音色选择 Resource ID
        String resourceId = config.getResourceIdForSpeaker(speaker);
        log.info("选择的 Resource ID: {}", resourceId);
        
        try {
            // ✅ 使用共享的HttpClient实例
            HttpClient client = getOrCreateHttpClient();
        
        // 构建 HTTP 请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getUrl()))
                .timeout(Duration.ofMinutes(config.getRequestTimeout()))
                .header("X-Api-Key", config.getApiKey())
                .header("X-Api-Resource-Id", resourceId)  // 使用动态选择的 Resource ID
                .header("Content-Type", "application/json")
                .header("User-Agent", "Java-HttpClient/11")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        
        log.info("请求头: X-Api-Key={}, X-Api-Resource-Id={}", 
                config.getApiKey().substring(0, 8) + "...", resourceId);
        
        // 发送请求并处理响应
        ByteArrayOutputStream audioData = new ByteArrayOutputStream();
        
        try {
            log.info("开始发送HTTP请求...");
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            
            log.info("收到响应，状态码: {}", response.statusCode());
            
            // 检查HTTP状态码
            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("HTTP错误: 状态码={}, 响应体={}", response.statusCode(), errorBody);
                throw new Exception("HTTP错误: " + response.statusCode() + ", " + errorBody);
            }
            
            // 逐行读取流式响应
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                
                String line;
                int lineCount = 0;
                
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    
                    lineCount++;
                    log.debug("接收到第{}行数据: {}", lineCount, line);
                    
                    // 提取返回码和数据
                    int code = extractCode(line);
                    String data = extractData(line);
                    
                    // code = 0 表示正常数据块
                    if (code == 0 && data != null && !data.isEmpty()) {
                        byte[] chunkAudio = Base64.getDecoder().decode(data);
                        audioData.write(chunkAudio);
                        log.debug("写入音频数据块，大小: {} 字节", chunkAudio.length);
                    }
                    
                    // code = 20000000 表示传输完成
                    if (code == 20000000) {
                        log.info("音频传输完成，共接收{}行数据", lineCount);
                        break;
                    }
                    
                    // code > 0 表示发生错误
                    if (code > 0) {
                        log.error("火山引擎返回错误: {}", line);
                        throw new Exception("火山引擎API错误: " + line);
                    }
                }
            }
            
            if (audioData.size() == 0) {
                throw new Exception("未接收到音频数据");
            }
            
            log.info("音频数据接收完成，总大小: {} KB", audioData.size() / 1024.0);
            return audioData.toByteArray();
            
        } catch (javax.net.ssl.SSLHandshakeException e) {
            log.error("SSL握手失败: {}", e.getMessage());
            log.error("可能原因:");
            log.error("1. API Key无效或过期");
            log.error("2. 网络代理或防火墙拦截");
            log.error("3. JDK的SSL配置问题");
            log.error("4. 火山引擎API服务器SSL证书问题");
            throw new Exception("SSL握手失败，请检查网络连接和API Key配置: " + e.getMessage(), e);
        } catch (java.net.ConnectException e) {
            log.error("连接失败: {}", e.getMessage());
            throw new Exception("无法连接到火山引擎API，请检查网络连接: " + e.getMessage(), e);
        } catch (java.net.SocketTimeoutException e) {
            log.error("连接超时: {}", e.getMessage());
            throw new Exception("连接超时，请检查网络状况: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("TTS请求失败: {}", e.getMessage(), e);
            throw new Exception("TTS请求失败: " + e.getMessage(), e);
        }
    } catch (Exception e) {
        log.error("sendTTSRequest外层异常: {}", e.getMessage(), e);
        throw e;
    }
}
    
    /**
     * 从 JSON 行中提取返回码
     */
    private int extractCode(String jsonLine) {
        Matcher matcher = CODE_PATTERN.matcher(jsonLine);
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }
    
    /**
     * 从 JSON 行中提取音频数据（Base64编码）
     */
    private String extractData(String jsonLine) {
        Matcher matcher = DATA_PATTERN.matcher(jsonLine);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }
    
    /**
     * 检查服务是否可用
     */
    public boolean checkHealth() {
        try {
            String testPayload = buildSimplePayload("测试", config.getDefaultSpeaker());
            byte[] audio = sendTTSRequest(testPayload, config.getDefaultSpeaker());
            return audio != null && audio.length > 0;
        } catch (Exception e) {
            log.error("健康检查失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 构建简单的测试 payload
     */
    private String buildSimplePayload(String text, String speaker) {
        return String.format(
            "{\"req_params\":{\"text\":\"%s\",\"speaker\":\"%s\",\"audio_params\":{\"format\":\"%s\",\"sample_rate\":%d}}}",
            text,
            speaker != null ? speaker : config.getDefaultSpeaker(),
            config.getDefaultFormat(),
            config.getDefaultSampleRate()
        );
    }
}
