package com.hmall.tts.whisperx.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hmall.tts.whisperx.dto.CharTimestamp;
import com.hmall.tts.whisperx.exception.WhisperXException;
import com.hmall.tts.whisperx.service.WhisperXService;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * WhisperX强制对齐服务实现类
 * 
 * @author Kiro
 * @since 2026-08-15
 */
@Slf4j
@Service
public class WhisperXServiceImpl implements WhisperXService {
    
    @Value("${whisperx.python.command:auto}")
    private String pythonCommand;
    
    @Value("${whisperx.script.path:D:/code/adminFlow/scripts/whisperx_align.py}")
    private String scriptPath;
    
    @Value("${whisperx.temp.dir:D:/code/adminFlow/temp/whisperx}")
    private String tempDir;
    
    @Value("${whisperx.timeout.seconds:120}")
    private int timeoutSeconds;
    
    /**
     * 实际使用的Python命令（自动检测或配置指定）
     */
    private String actualPythonCommand = null;
    
    @Override
    public List<CharTimestamp> align(byte[] audioData, String originalText) throws Exception {
        long startTime = System.currentTimeMillis();
        Path audioPath = null;  // ✅ 提前声明，确保finally能访问
        
        try {
            // ✅ 首次调用时自动检测Python 3.13
            if (actualPythonCommand == null) {
                actualPythonCommand = detectPython313();
                log.info("[WhisperX] 自动检测Python命令：{}", actualPythonCommand);
            }
            
            log.info("[WhisperX] 开始强制对齐，音频大小：{} KB，文本长度：{}", 
                    audioData.length / 1024.0, originalText.length());
            log.debug("[WhisperX] 原文：{}", originalText.length() > 100 ? 
                     originalText.substring(0, 100) + "..." : originalText);
            
            // 1. 保存音频到临时文件
            audioPath = saveAudioToTemp(audioData);
            log.debug("[WhisperX] 音频已保存到：{}", audioPath);
            
            // ✅ 获取Python命令数组（支持 "py -3.13"）
            String[] pythonCmd = getPythonCommandArray();
            
            // ✅ 构建完整命令（Python命令 + 脚本路径 + 参数）
            List<String> commandList = new ArrayList<>();
            commandList.addAll(Arrays.asList(pythonCmd));  // 添加Python命令（可能是多个部分）
            commandList.add(scriptPath);                   // 添加脚本路径
            commandList.add(audioPath.toString());         // 添加音频路径
            commandList.add(originalText);                 // 添加原文
            
            // 2. 调用Python脚本（传递音频路径和原文）
            ProcessBuilder pb = new ProcessBuilder(commandList);
            pb.redirectErrorStream(false);  // 分开处理stdout和stderr
            
            log.debug("[WhisperX] 执行命令：{} {} {} \"{}\"", 
                     String.join(" ", pythonCmd), scriptPath, audioPath, 
                     originalText.length() > 50 ? originalText.substring(0, 50) + "..." : originalText);
            
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
                        log.debug("[WhisperX日志] {}", line);
                    }
                } catch (Exception e) {
                    log.warn("[WhisperX] 读取stderr失败", e);
                }
            });
            stderrThread.start();
            
            // 在后台线程读取stdout
            StringBuilder output = new StringBuilder();
            Thread stdoutThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = stdoutReader.readLine()) != null) {
                        output.append(line);
                    }
                } catch (Exception e) {
                    log.warn("[WhisperX] 读取stdout失败", e);
                }
            });
            stdoutThread.start();
            
            // 等待进程结束（添加超时机制）
            boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!finished) {
                // 超时，强制终止进程
                process.destroyForcibly();
                log.error("[WhisperX] 对齐超时（{}秒），强制终止", timeoutSeconds);
                throw new WhisperXException("WhisperX对齐超时（" + timeoutSeconds + "秒）");
            }
            
            // 等待两个读取线程结束
            stdoutThread.join(3000);
            stderrThread.join(3000);
            
            int exitCode = process.exitValue();
            
            log.debug("[WhisperX] 进程退出码：{}", exitCode);
            
            // 检查脚本级错误（exitCode != 0）
            if (exitCode != 0) {
                String errorMsg = String.format("WhisperX脚本异常，退出码：%d", exitCode);
                log.error("[WhisperX] {}", errorMsg);
                throw new WhisperXException(errorMsg);
            }
            
            // 3. 解析JSON结果
            String jsonStr = output.toString().trim();
            if (jsonStr.isEmpty()) {
                log.warn("[WhisperX] 返回空结果");
                throw new WhisperXException("WhisperX返回空结果");
            }
            
            log.debug("[WhisperX] JSON结果长度：{} 字节", jsonStr.length());
            
            JSONObject json;
            try {
                json = JSON.parseObject(jsonStr);
            } catch (Exception e) {
                log.error("[WhisperX] JSON解析失败，原始输出前500字符：{}", 
                         jsonStr.length() > 500 ? jsonStr.substring(0, 500) : jsonStr);
                throw new WhisperXException("JSON解析失败：" + e.getMessage());
            }
            
            // 检查业务结果
            Boolean success = json.getBoolean("success");
            if (success == null || !success) {
                String error = json.getString("error");
                String errorDetail = json.getString("error_detail");
                log.warn("[WhisperX] 对齐失败（业务层）：{}", error);
                if (errorDetail != null) {
                    log.debug("[WhisperX] 错误详情：{}", errorDetail);
                }
                throw new WhisperXException("WhisperX对齐失败：" + error);
            }
            
            // 提取字符级时间戳
            JSONArray chars = json.getJSONArray("chars");
            List<CharTimestamp> timestamps = new ArrayList<>();
            
            if (chars != null) {
                for (int i = 0; i < chars.size(); i++) {
                    JSONObject charObj = chars.getJSONObject(i);
                    timestamps.add(new CharTimestamp(
                        charObj.getString("char"),
                        charObj.getDouble("start"),
                        charObj.getDouble("end")
                    ));
                }
            }
            
            // 获取对齐信息
            String alignedText = json.getString("aligned_text");
            String accuracy = json.getString("accuracy");
            Double duration = json.getDouble("duration");
            Double audioStartOffset = json.getDouble("audio_start_offset");  // ✅ Day 8新增
            
            // ✅ Day 8日志：输出音频偏移量（用于诊断）
            if (audioStartOffset != null && audioStartOffset > 0.01) {
                log.info("[WhisperX] 音频偏移量：{}秒（已自动归零）", String.format("%.3f", audioStartOffset));
            }
            
            // ✅ 准确率阈值检查（低于80%认为对齐失败）
            if (accuracy != null && accuracy.endsWith("%")) {
                try {
                    double accuracyValue = Double.parseDouble(accuracy.replace("%", ""));
                    if (accuracyValue < 80.0) {
                        log.error("[WhisperX] ❌ 对齐准确率过低：{}（阈值：80%），可能原文与音频不匹配", accuracy);
                        log.debug("[WhisperX] 原文：{}", originalText.trim());
                        log.debug("[WhisperX] 对齐：{}", alignedText);
                        throw new WhisperXException(String.format(
                            "WhisperX对齐准确率过低（%s < 80%%），原文与音频可能不匹配", accuracy
                        ));
                    }
                } catch (NumberFormatException e) {
                    log.warn("[WhisperX] 无法解析准确率：{}", accuracy);
                }
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[WhisperX] ✅ 对齐完成，字符数：{}，准确率：{}，音频时长：{}秒，耗时：{} ms", 
                    timestamps.size(), accuracy, String.format("%.2f", duration), elapsedTime);
            
            // 对齐验证：检查对齐文字是否与原文匹配
            if (alignedText != null && !alignedText.equals(originalText.trim())) {
                log.warn("[WhisperX] ⚠️ 对齐文字与原文不完全匹配");
                log.debug("[WhisperX] 原文：{}", originalText.trim());
                log.debug("[WhisperX] 对齐：{}", alignedText);
            }
            
            return timestamps;
            
        } catch (WhisperXException e) {
            throw e;
        } catch (Exception e) {
            log.error("[WhisperX] 对齐异常", e);
            throw new WhisperXException("WhisperX对齐异常：" + e.getMessage(), e);
        } finally {
            // ✅ 确保临时文件被清理（无论成功还是失败）
            if (audioPath != null) {
                try {
                    Files.deleteIfExists(audioPath);
                    log.debug("[WhisperX] ✅ 临时文件已清理：{}", audioPath);
                } catch (Exception e) {
                    log.warn("[WhisperX] ⚠️ 清理临时文件失败：{}", audioPath, e);
                }
            }
        }
    }
    
    @Override
    public List<List<CharTimestamp>> alignBatch(List<byte[]> audioDataList, List<String> originalTextList) throws Exception {
        if (audioDataList.size() != originalTextList.size()) {
            throw new WhisperXException("音频列表和原文列表长度不一致");
        }
        
        List<List<CharTimestamp>> results = new ArrayList<>();
        
        for (int i = 0; i < audioDataList.size(); i++) {
            try {
                List<CharTimestamp> timestamps = align(audioDataList.get(i), originalTextList.get(i));
                results.add(timestamps);
            } catch (Exception e) {
                log.error("[WhisperX] 批量对齐中的第{}个音频失败", i + 1, e);
                results.add(new ArrayList<>());  // 添加空列表
            }
        }
        
        return results;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // ✅ 首次调用时自动检测Python 3.13
            if (actualPythonCommand == null) {
                actualPythonCommand = detectPython313();
            }
            
            // ✅ 获取Python命令数组（支持 "py -3.13"）
            String[] pythonCmd = getPythonCommandArray();
            
            // ✅ 构建完整命令
            List<String> commandList = new ArrayList<>();
            commandList.addAll(Arrays.asList(pythonCmd));
            commandList.add("--version");
            
            // 检查Python是否可用
            ProcessBuilder pb = new ProcessBuilder(commandList);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.warn("[WhisperX] Python不可用");
                return false;
            }
            
            // 检查脚本是否存在
            Path scriptFilePath = Paths.get(scriptPath);
            if (!Files.exists(scriptFilePath)) {
                log.warn("[WhisperX] 脚本文件不存在：{}", scriptPath);
                return false;
            }
            
            log.info("[WhisperX] 服务可用（Python: {}）", String.join(" ", pythonCmd));
            return true;
            
        } catch (Exception e) {
            log.error("[WhisperX] 检查服务可用性失败", e);
            return false;
        }
    }
    
    /**
     * ✅ 获取Python命令（支持带参数的命令，如 "py -3.13"）
     * 
     * @return Python命令数组（例如：["py", "-3.13"] 或 ["python"]）
     */
    private String[] getPythonCommandArray() {
        // 首次调用时检测
        if (actualPythonCommand == null) {
            try {
                actualPythonCommand = detectPython313();
            } catch (WhisperXException e) {
                log.error("[WhisperX] 无法检测Python命令", e);
                throw new RuntimeException("无法检测Python 3.13命令", e);
            }
        }
        
        // 如果命令包含空格，拆分为数组（支持 "py -3.13"）
        if (actualPythonCommand.contains(" ")) {
            String[] parts = actualPythonCommand.split("\\s+");
            log.debug("[WhisperX] Python命令拆分：{}", Arrays.toString(parts));
            return parts;
        }
        
        // 单个命令（如 "python" 或 "python313"）
        return new String[] { actualPythonCommand };
    }
    
    /**
     * ✅ 自动检测Python 3.13命令
     * 
     * 检测顺序：
     * 1. 配置文件指定的命令（如果不是"auto"）
     * 2. python313 或 python313.exe
     * 3. C:\Python313\python.exe
     * 4. %LOCALAPPDATA%\Programs\Python\Python313\python.exe
     * 5. py -3.13（Windows Python Launcher）
     * 6. python（回退，但会记录警告）
     * 
     * @return Python 3.13命令
     * @throws WhisperXException 如果找不到Python 3.13
     */
    private String detectPython313() throws WhisperXException {
        // 1. 如果配置文件指定了具体命令（不是"auto"），直接使用
        if (pythonCommand != null && !"auto".equalsIgnoreCase(pythonCommand)) {
            log.info("[WhisperX] 使用配置的Python命令：{}", pythonCommand);
            return pythonCommand;
        }
        
        // 2. 尝试python313命令
        if (testPythonCommand("python313")) {
            log.info("[WhisperX] ✅ 检测到python313命令");
            return "python313";
        }
        
        if (testPythonCommand("python313.exe")) {
            log.info("[WhisperX] ✅ 检测到python313.exe命令");
            return "python313.exe";
        }
        
        // 3. 尝试常见安装路径
        String[] commonPaths = {
            "C:\\Python313\\python.exe",
            System.getenv("LOCALAPPDATA") + "\\Programs\\Python\\Python313\\python.exe",
            System.getenv("PROGRAMFILES") + "\\Python313\\python.exe"
        };
        
        for (String path : commonPaths) {
            if (path != null && new java.io.File(path).exists()) {
                log.info("[WhisperX] ✅ 检测到Python 3.13：{}", path);
                return path;
            }
        }
        
        // 4. 尝试Windows Python Launcher
        if (testPythonCommand("py", "-3.13", "--version")) {
            log.info("[WhisperX] ✅ 检测到py -3.13");
            return "py -3.13";  // ✅ 支持返回带参数的命令
        }
        
        // 5. 回退到python（但记录警告）
        if (testPythonCommand("python")) {
            log.warn("[WhisperX] ⚠️  未找到Python 3.13，回退到python命令");
            log.warn("[WhisperX] ⚠️  如果是Python 3.14，WhisperX将无法工作");
            log.warn("[WhisperX] ⚠️  请运行 setup_python311_whisperx.bat 安装Python 3.13");
            return "python";
        }
        
        // 6. 全部失败
        throw new WhisperXException(
            "未找到Python 3.13！\n" +
            "请运行以下命令安装：\n" +
            "  D:\\code\\adminFlow\\scripts\\setup_python311_whisperx.bat\n" +
            "或手动安装Python 3.13到 C:\\Python313\\"
        );
    }
    
    /**
     * 测试Python命令是否可用
     */
    private boolean testPythonCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
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
