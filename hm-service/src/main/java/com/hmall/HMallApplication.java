package com.hmall;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@MapperScan("com.hmall.mapper")
@SpringBootApplication
@org.springframework.scheduling.annotation.EnableAsync  // ⭐ 启用异步支持（用于局部编辑功能）
public class HMallApplication {
    
    private static Process whisperxProcess = null;
    
    public static void main(String[] args) {
        // 1. 启动 WhisperX 服务
        startWhisperXService();
        
        // 2. 启动 Spring Boot
        ConfigurableApplicationContext context = SpringApplication.run(HMallApplication.class, args);
        
        // 3. 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("========================================");
            log.info("[WhisperX] 正在关闭服务...");
            log.info("========================================");
            stopWhisperXService();
        }));
    }
    
    private static void startWhisperXService() {
        System.out.println("========================================");
        System.out.println("[WhisperX] 🚀 正在启动 HTTP 服务...");
        System.out.println("========================================");
        
        try {
            // 1. 检查端口是否已被占用
            if (isPortInUse(5000)) {
                System.out.println("[WhisperX] ⚠️ 端口 5000 已被占用，假设服务已在运行");
                return;
            }
            
            // 2. 构建启动命令
            List<String> command = new ArrayList<>();
            command.add("py");
            command.add("-3.13");
            command.add("D:/code/adminFlow/scripts/whisperx_server.py");
            
            System.out.println("[WhisperX] 启动命令: " + String.join(" ", command));
            
            // 3. 启动进程
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            whisperxProcess = pb.start();
            
            // 4. 后台读取日志
            Thread logThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(whisperxProcess.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[WhisperX Server] " + line);
                    }
                } catch (Exception e) {
                    // ignore
                }
            });
            logThread.setDaemon(true);
            logThread.setName("WhisperX-Log");
            logThread.start();
            
            // 5. 等待服务启动（180秒超时）
            System.out.println("[WhisperX] ⏳ 等待服务启动（最多180秒）...");
            long startTime = System.currentTimeMillis();
            long timeout = 180 * 1000L;
            
            while (System.currentTimeMillis() - startTime < timeout) {
                if (isPortInUse(5000)) {
                    System.out.println("========================================");
                    System.out.println("[WhisperX] ✅ HTTP 服务启动成功！");
                    System.out.println("[WhisperX] 服务地址: http://localhost:5000");
                    System.out.println("========================================");
                    return;
                }
                Thread.sleep(1000);
            }
            
            System.out.println("[WhisperX] ❌ 启动超时，将回退到按需启动模式");
            
        } catch (Exception e) {
            System.out.println("[WhisperX] ❌ 启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void stopWhisperXService() {
        if (whisperxProcess != null && whisperxProcess.isAlive()) {
            try {
                whisperxProcess.destroy();
                boolean exited = whisperxProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                if (!exited) {
                    whisperxProcess.destroyForcibly();
                }
                System.out.println("[WhisperX] ✅ 服务已停止");
            } catch (Exception e) {
                System.out.println("[WhisperX] ❌ 停止服务失败: " + e.getMessage());
            }
        }
    }
    
    private static boolean isPortInUse(int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}