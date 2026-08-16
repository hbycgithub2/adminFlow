package com.hmall.tts.whisper.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hmall.tts.whisper.dto.WordTimestamp;
import com.hmall.tts.whisper.exception.WhisperException;
import com.hmall.tts.whisper.service.WhisperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Whisper语音识别服务实现类
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Slf4j
@Service
public class WhisperServiceImpl implements WhisperService {
    
    @Value("${whisper.python.command:py}")
    private String pythonCommand;
    
    @Value("${whisper.script.path:D:/code/adminFlow/scripts/whisper_transcribe.py}")
    private String scriptPath;
    
    @Value("${whisper.temp.dir:D:/code/adminFlow/temp/whisper}")
    private String tempDir;
    
    @Value("${whisper.timeout.seconds:60}")
    private int timeoutSeconds;
    
    @Override
    public List<WordTimestamp> transcribe(byte[] audioData) throws Exception {
        return transcribeWithPrompt(audioData, null);
    }
    
    @Override
    public List<WordTimestamp> transcribeWithPrompt(byte[] audioData, String promptText) throws Exception {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("[Whisper] 开始识别音频，大小：{} KB{}", 
                    audioData.length / 1024.0,
                    promptText != null ? "，使用提示文本" : "");
            
            // 1. 保存音频到临时文件
            Path audioPath = saveAudioToTemp(audioData);
            log.debug("[Whisper] 音频已保存到：{}", audioPath);
            
            // 2. 调用Python脚本
            // ✅ 修复：使用数组方式构建命令，避免引号问题
            ProcessBuilder pb;
            if (promptText != null && !promptText.isEmpty()) {
                // 带提示文本
                log.debug("[Whisper] 执行命令：{} {} {} \"{}\"", 
                         pythonCommand, scriptPath, audioPath, promptText);
                pb = new ProcessBuilder(
                    pythonCommand,
                    scriptPath,
                    audioPath.toString(),
                    promptText  // ✅ 传递原文作为提示
                );
            } else {
                // 不带提示文本
                log.debug("[Whisper] 执行命令：{} {} {}", 
                         pythonCommand, scriptPath, audioPath);
                pb = new ProcessBuilder(
                    pythonCommand,
                    scriptPath,
                    audioPath.toString()
                );
            }
            pb.redirectErrorStream(false);  // 分开处理stdout和stderr
            
            Process process = pb.start();
            
            // 读取标准输出（JSON结果）
            BufferedReader stdoutReader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );
            
            // 读取标准错误（日志信息）
            BufferedReader stderrReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)
            );
            
            // 在后台线程读取stderr
            Thread stderrThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = stderrReader.readLine()) != null) {
                        log.debug("[Whisper日志] {}", line);
                    }
                } catch (Exception e) {
                    log.warn("[Whisper] 读取stderr失败", e);
                }
            });
            stderrThread.start();
            
            // ✅ 修复：在后台线程读取stdout，避免阻塞
            StringBuilder output = new StringBuilder();
            Thread stdoutThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = stdoutReader.readLine()) != null) {
                        output.append(line);
                    }
                } catch (Exception e) {
                    log.warn("[Whisper] 读取stdout失败", e);
                }
            });
            stdoutThread.start();
            
            // ✅ 修复：等待进程结束和两个读取线程完成（添加超时机制）
            boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!finished) {
                // 超时，强制终止进程
                process.destroyForcibly();
                log.error("[Whisper] 识别超时（{}秒），强制终止", timeoutSeconds);
                throw new WhisperException("Whisper识别超时（" + timeoutSeconds + "秒）");
            }
            
            // 等待两个读取线程结束
            stdoutThread.join(3000);
            stderrThread.join(3000);
            
            int exitCode = process.exitValue();
            
            log.debug("[Whisper] 进程退出码：{}", exitCode);
            
            // ✅ Bug #6修复：只检查脚本级错误（exitCode != 0）
            // 业务逻辑失败（success=false）通过JSON判断，不抛异常
            if (exitCode != 0) {
                String errorMsg = String.format("Whisper脚本异常，退出码：%d", exitCode);
                log.error("[Whisper] {}", errorMsg);
                throw new WhisperException(errorMsg);
            }
            
            // 3. 解析JSON结果
            String jsonStr = output.toString().trim();
            if (jsonStr.isEmpty()) {
                log.warn("[Whisper] 返回空结果");
                throw new WhisperException("Whisper返回空结果");
            }
            
            log.debug("[Whisper] JSON结果：{}", jsonStr);
            
            JSONObject json;
            try {
                json = JSON.parseObject(jsonStr);
            } catch (Exception e) {
                log.error("[Whisper] JSON解析失败，原始输出：{}", jsonStr);
                throw new WhisperException("JSON解析失败：" + e.getMessage());
            }
            
            // ✅ 关键改进：业务失败也是正常的，记录日志但抛异常让上层降级处理
            Boolean success = json.getBoolean("success");
            if (success == null || !success) {
                String error = json.getString("error");
                log.warn("[Whisper] 识别失败（业务层）：{}", error);
                throw new WhisperException("Whisper识别失败：" + error);
            }
            
            // 提取逐字时间戳
            JSONArray words = json.getJSONArray("words");
            List<WordTimestamp> timestamps = new ArrayList<>();
            
            if (words != null) {
                for (int i = 0; i < words.size(); i++) {
                    JSONObject word = words.getJSONObject(i);
                    timestamps.add(new WordTimestamp(
                        word.getString("text"),
                        word.getDouble("start"),
                        word.getDouble("end")
                    ));
                }
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[Whisper] 识别完成，字数：{}，耗时：{} ms（完全免费）", 
                    timestamps.size(), elapsedTime);
            
            // 4. 清理临时文件
            try {
                Files.deleteIfExists(audioPath);
            } catch (Exception e) {
                log.warn("[Whisper] 清理临时文件失败：{}", audioPath, e);
            }
            
            return timestamps;
            
        } catch (WhisperException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Whisper] 识别异常", e);
            throw new WhisperException("Whisper识别异常：" + e.getMessage(), e);
        }
    }
    
    @Override
    public List<List<WordTimestamp>> transcribeBatch(List<byte[]> audioDataList) throws Exception {
        List<List<WordTimestamp>> results = new ArrayList<>();
        
        for (byte[] audioData : audioDataList) {
            try {
                List<WordTimestamp> timestamps = transcribe(audioData);
                results.add(timestamps);
            } catch (Exception e) {
                log.error("[Whisper] 批量识别中的某个音频失败", e);
                results.add(new ArrayList<>());  // 添加空列表
            }
        }
        
        return results;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // 检查Python是否可用
            ProcessBuilder pb = new ProcessBuilder(pythonCommand, "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.warn("[Whisper] Python不可用");
                return false;
            }
            
            // 检查脚本是否存在
            Path scriptFilePath = Paths.get(scriptPath);
            if (!Files.exists(scriptFilePath)) {
                log.warn("[Whisper] 脚本文件不存在：{}", scriptPath);
                return false;
            }
            
            log.info("[Whisper] 服务可用");
            return true;
            
        } catch (Exception e) {
            log.error("[Whisper] 检查服务可用性失败", e);
            return false;
        }
    }
    
    /**
     * 保存音频到临时文件
     */
    private Path saveAudioToTemp(byte[] audioData) throws Exception {
        // 创建临时目录
        Path tempDirPath = Paths.get(tempDir);
        Files.createDirectories(tempDirPath);
        
        // 生成临时文件名
        String fileName = UUID.randomUUID().toString() + ".mp3";
        Path audioPath = tempDirPath.resolve(fileName);
        
        // 写入音频数据
        Files.write(audioPath, audioData);
        
        return audioPath;
    }
}
