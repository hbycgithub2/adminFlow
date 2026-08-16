package com.hmall.tts.volcengine.controller;

import com.hmall.tts.volcengine.config.VolcengineConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * TTS资源访问控制器
 * 提供音频文件下载功能
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Slf4j
@RestController
@RequestMapping("/tts")
@RequiredArgsConstructor
public class TtsResourceController {
    
    private final VolcengineConfig config;
    
    /**
     * 下载文档TTS生成的音频文件
     */
    @GetMapping("/documents/{fileName}")
    public ResponseEntity<Resource> downloadDocumentAudio(@PathVariable String fileName) {
        try {
            log.info("请求下载音频文件: {}", fileName);
            
            // 从配置读取路径（统一使用DocumentTTSService的保存路径）
            Path filePath = Paths.get(config.getOutputDir(), "documents", fileName);
            
            log.info("音频文件路径: {}", filePath.toAbsolutePath());
            
            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                log.error("音频文件不存在: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            // 检查文件大小
            long fileSize = Files.size(filePath);
            log.info("音频文件大小: {} bytes ({} KB)", fileSize, fileSize / 1024.0);
            
            // 创建资源
            Resource resource = new FileSystemResource(filePath.toFile());
            
            // 判断文件格式
            String contentType = "audio/mpeg"; // 默认MP3
            if (fileName.endsWith(".wav")) {
                contentType = "audio/wav";
            } else if (fileName.endsWith(".ogg")) {
                contentType = "audio/ogg";
            }
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(fileSize);
            headers.setCacheControl("no-cache");
            
            log.info("✅ 返回音频文件: {}, 类型: {}, 大小: {} bytes", fileName, contentType, fileSize);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
            
        } catch (Exception e) {
            log.error("❌ 下载音频文件失败: {}, 错误: {}", fileName, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
