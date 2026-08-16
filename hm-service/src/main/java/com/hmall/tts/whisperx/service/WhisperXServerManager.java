package com.hmall.tts.whisperx.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * WhisperX HTTP服务管理器
 * 
 * 功能：
 * 1. Spring Boot启动时自动启动WhisperX HTTP服务
 * 2. Spring Boot关闭时自动停止WhisperX HTTP服务
 * 3. 健康检查和自动重启
 * 
 * 配置：
 * - whisperx.server.enabled: 是否启用自动启动（默认true）
 * - whisperx.server.auto-start: 是否自动启动（默认true）
 * - whisperx.server.python-path: Python路径（默认：py -3.13）
 * - whisperx.server.script-path: 脚本路径
 * - whisperx.server.host: 服务地址（默认：localhost）
 * - whisperx.server.port: 服务端口（默认：5000）
 * 
 * @author Kiro
 * @since 2026-08-16
 */
@Slf4j
public class WhisperXServerManager {
    
    public WhisperXServerManager() {
        // 构造函数日志 - 验证Bean是否被创建
        System.out.println("========================================");
        System.out.println("[WhisperX Server] ✅ WhisperXServerManager Bean 已创建");
        System.out.println("========================================");
    }
    
    @Value("${whisperx.server.enabled:true}")
    private boolean enabled;
    
    @Value("${whisperx.server.auto-start:true}")
    private boolean autoStart;
    
    @Value("${whisperx.server.python-path:py -3.13}")
    private String pythonPath;
    
    @Value("${whisperx.server.script-path:D:/code/adminFlow/scripts/whisperx_server.py}")
    private String scriptPath;
    
    @Value("${whisperx.server.host:localhost}")
    private String host;
    
    @Value("${whisperx.server.port:5000}")
    private int port;
    
    @Value("${whisperx.server.startup-timeout:60}")
    private int startupTimeout;
    
    private Process serverProcess;
    private Thread logReaderThread;
    private volatile boolean serverStarted = false;
    
    /**
     * Spring Boot启动时自动执行
     */
    @PostConstruct
    public void init() {
        System.out.println("========================================");
        System.out.println("[WhisperX Server] @PostConstruct 方法被调用");
        System.out.println("========================================");
        
        if (!enabled) {
            log.info("[WhisperX Server] ⚠️ 服务已禁用（whisperx.server.enabled=false）");
            System.out.println("[WhisperX Server] ⚠️ 服务已禁用（whisperx.server.enabled=false）");
            return;
        }
        
        if (!autoStart) {
            log.info("[WhisperX Server] ⚠️ 自动启动已禁用（whisperx.server.auto-start=false）");
            System.out.println("[WhisperX Server] ⚠️ 自动启动已禁用（whisperx.server.auto-start=false）");
            return;
        }
        
        log.info("========================================");
        log.info("[WhisperX Server] 🚀 开始启动 HTTP 服务");
        log.info("========================================");
        log.info("[WhisperX Server] Python路径: {}", pythonPath);
        log.info("[WhisperX Server] 脚本路径: {}", scriptPath);
        log.info("[WhisperX Server] 服务地址: http://{}:{}", host, port);
        log.info("========================================");
        
        System.out.println("========================================");
        System.out.println("[WhisperX Server] 🚀 开始启动 HTTP 服务");
        System.out.println("[WhisperX Server] Python路径: " + pythonPath);
        System.out.println("[WhisperX Server] 脚本路径: " + scriptPath);
        System.out.println("[WhisperX Server] 服务地址: http://" + host + ":" + port);
        System.out.println("========================================");
        
        try {
            startServer();
        } catch (Exception e) {
            log.error("[WhisperX Server] ❌ 启动失败：{}", e.getMessage(), e);
            log.warn("[WhisperX Server] ⚠️ 将回退到Python脚本模式");
        }
    }
    
    /**
     * Spring Boot关闭时自动执行
     */
    @PreDestroy
    public void destroy() {
        if (serverProcess != null && serverProcess.isAlive()) {
            log.info("[WhisperX Server] 🛑 正在停止 HTTP 服务...");
            
            try {
                serverProcess.destroy();
                
                // 等待进程结束（最多5秒）
                boolean exited = serverProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                
                if (!exited) {
                    log.warn("[WhisperX Server] ⚠️ 进程未正常退出，强制终止");
                    serverProcess.destroyForcibly();
                }
                
                log.info("[WhisperX Server] ✅ HTTP 服务已停止");
                
            } catch (Exception e) {
                log.error("[WhisperX Server] ❌ 停止服务失败：{}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 启动WhisperX HTTP服务
     */
    private void startServer() throws Exception {
        // 1. 检查脚本文件是否存在
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            throw new Exception("脚本文件不存在：" + scriptPath);
        }
        
        // 2. 检查端口是否被占用
        if (isPortInUse(port)) {
            log.warn("[WhisperX Server] ⚠️ 端口 {} 已被占用，假设服务已在运行", port);
            serverStarted = true;
            return;
        }
        
        // 3. 构建启动命令
        List<String> command = new ArrayList<>();
        
        // 解析Python路径（可能包含参数，如 "py -3.13"）
        String[] pythonParts = pythonPath.split("\\s+");
        for (String part : pythonParts) {
            command.add(part);
        }
        
        command.add(scriptPath);
        
        log.info("[WhisperX Server] 执行命令: {}", String.join(" ", command));
        
        // 4. 启动进程
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(scriptFile.getParentFile());
        processBuilder.redirectErrorStream(true);
        
        serverProcess = processBuilder.start();
        
        // 5. 启动日志读取线程
        startLogReader();
        
        // 6. 等待服务启动（检查健康接口）
        boolean started = waitForServerStartup(startupTimeout);
        
        if (started) {
            serverStarted = true;
            log.info("========================================");
            log.info("[WhisperX Server] ✅ HTTP 服务启动成功！");
            log.info("[WhisperX Server] 健康检查: http://{}:{}/health", host, port);
            log.info("[WhisperX Server] 对齐接口: POST http://{}:{}/align", host, port);
            log.info("[WhisperX Server] 批量接口: POST http://{}:{}/align_batch", host, port);
            log.info("========================================");
        } else {
            throw new Exception("服务启动超时（" + startupTimeout + "秒）");
        }
    }
    
    /**
     * 启动日志读取线程
     */
    private void startLogReader() {
        logReaderThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(serverProcess.getInputStream(), Charset.forName("UTF-8")))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    // 转发Python服务的日志到Java日志
                    log.info("[WhisperX Server] {}", line);
                }
                
            } catch (Exception e) {
                if (serverProcess != null && serverProcess.isAlive()) {
                    log.error("[WhisperX Server] 日志读取异常：{}", e.getMessage());
                }
            }
        }, "WhisperX-Log-Reader");
        
        logReaderThread.setDaemon(true);
        logReaderThread.start();
    }
    
    /**
     * 等待服务启动
     */
    private boolean waitForServerStartup(int timeoutSeconds) {
        String healthUrl = String.format("http://%s:%d/health", host, port);
        
        log.info("[WhisperX Server] 等待服务启动（最多{}秒）...", timeoutSeconds);
        log.info("[WhisperX Server] 健康检查URL: {}", healthUrl);
        
        long startTime = System.currentTimeMillis();
        long timeout = timeoutSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                // 检查进程是否还活着
                if (!serverProcess.isAlive()) {
                    log.error("[WhisperX Server] ❌ 进程已退出，退出码：{}", serverProcess.exitValue());
                    return false;
                }
                
                // 尝试访问健康检查接口
                java.net.URL url = new java.net.URL(healthUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode == 200) {
                    // 读取响应
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                        String response = br.lines().collect(java.util.stream.Collectors.joining());
                        log.info("[WhisperX Server] ✅ 健康检查通过: {}", response);
                    }
                    return true;
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                // 忽略异常，继续等待
            }
            
            try {
                Thread.sleep(1000);  // 每秒检查一次
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        log.error("[WhisperX Server] ❌ 启动超时（{}秒）", timeoutSeconds);
        return false;
    }
    
    /**
     * 检查端口是否被占用
     */
    private boolean isPortInUse(int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取服务是否已启动
     */
    public boolean isServerStarted() {
        return serverStarted;
    }
    
    /**
     * 手动重启服务
     */
    public void restartServer() throws Exception {
        log.info("[WhisperX Server] 🔄 手动重启服务...");
        
        // 停止现有服务
        destroy();
        
        // 等待1秒
        Thread.sleep(1000);
        
        // 重新启动
        startServer();
    }
}
