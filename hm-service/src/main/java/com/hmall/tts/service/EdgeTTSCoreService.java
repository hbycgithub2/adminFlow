package com.hmall.tts.service;

import com.hmall.tts.config.EdgeTTSProperties;
import com.hmall.tts.exception.TTSErrorCode;
import com.hmall.tts.exception.TTSException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Edge TTS 核心服务（底层调用）
 * 
 * @author Kiro
 * @since 2026-08-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EdgeTTSCoreService {

    private final EdgeTTSProperties properties;

    /**
     * 生成语音（核心方法）
     * 
     * @param text 文本内容
     * @param voice 音色
     * @param rate 语速
     * @param pitch 音调
     * @return 音频数据（MP3）
     */
    public byte[] generateSpeech(String text, String voice, String rate, String pitch) {
        // 验证参数
        if (text == null || text.trim().isEmpty()) {
            throw new TTSException(TTSErrorCode.EMPTY_TEXT);
        }

        // 创建临时目录
        Path tempDirPath = Paths.get(properties.getTempDir());
        try {
            if (!Files.exists(tempDirPath)) {
                Files.createDirectories(tempDirPath);
            }
        } catch (IOException e) {
            throw new TTSException(TTSErrorCode.FILE_IO_ERROR, "创建临时目录失败", e);
        }

        // 生成临时文件名
        String tempFileName = String.format("tts_%s_%s.mp3", 
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8));
        Path tempFilePath = tempDirPath.resolve(tempFileName);

        try {
            // 构建命令
            List<String> command = buildCommand(voice, rate, pitch, text, tempFilePath);
            
            log.debug("🎤 [Edge TTS Core] 执行命令: {}", String.join(" ", command));

            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();

            // 读取输出（避免进程阻塞）
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待进程完成
            boolean finished = process.waitFor(properties.getTimeout(), TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new TTSException(TTSErrorCode.TIMEOUT);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("❌ [Edge TTS Core] 执行失败: exitCode={}, output={}", exitCode, output);
                throw new TTSException(TTSErrorCode.EXECUTION_FAILED, "Edge TTS 执行失败: " + output.toString());
            }

            // 读取生成的音频文件
            if (!Files.exists(tempFilePath)) {
                throw new TTSException(TTSErrorCode.EXECUTION_FAILED, "音频文件未生成");
            }

            byte[] audioData = Files.readAllBytes(tempFilePath);
            
            log.debug("✅ [Edge TTS Core] 生成成功: {} bytes", audioData.length);

            return audioData;

        } catch (TTSException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [Edge TTS Core] 未知错误: {}", e.getMessage(), e);
            throw new TTSException(TTSErrorCode.UNKNOWN_ERROR, e.getMessage(), e);
        } finally {
            // 清理临时文件
            try {
                if (Files.exists(tempFilePath)) {
                    Files.delete(tempFilePath);
                }
            } catch (IOException e) {
                log.warn("⚠️ [Edge TTS Core] 清理临时文件失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 检查 edge-tts 是否安装
     * 
     * @return true 已安装，false 未安装
     */
    public boolean checkInstallation() {
        try {
            String[] cmdParts = properties.getCommand().split("\\s+");
            List<String> command = new ArrayList<>(Arrays.asList(cmdParts));
            command.add("--version");
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {
            log.error("❌ [Edge TTS Core] 检查安装失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 edge-tts 版本
     * 
     * @return 版本号
     */
    public String getVersion() {
        try {
            String[] cmdParts = properties.getCommand().split("\\s+");
            List<String> command = new ArrayList<>(Arrays.asList(cmdParts));
            command.add("--version");
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            process.waitFor(5, TimeUnit.SECONDS);

            return output.toString().trim();

        } catch (Exception e) {
            return "未知版本";
        }
    }

    /**
     * 获取支持的音色列表
     * 
     * @return 音色列表
     */
    public Map<String, Object> getAvailableVoices() {
        Map<String, Object> result = new HashMap<>();

        // 中文音色（普通话）
        List<Map<String, String>> zhCN = new ArrayList<>();
        zhCN.add(createVoice("zh-CN-XiaoxiaoNeural", "晓晓", "女", "温柔", "新闻、小说"));
        zhCN.add(createVoice("zh-CN-XiaoyiNeural", "晓伊", "女", "活泼", "动画、小说"));
        zhCN.add(createVoice("zh-CN-YunjianNeural", "云健", "男", "激情", "体育、小说"));
        zhCN.add(createVoice("zh-CN-YunxiNeural", "云希", "男", "活泼阳光", "小说"));
        zhCN.add(createVoice("zh-CN-YunxiaNeural", "云霞", "男", "可爱", "动画、小说"));
        zhCN.add(createVoice("zh-CN-YunyangNeural", "云扬", "男", "专业可靠", "新闻"));
        zhCN.add(createVoice("zh-CN-liaoning-XiaobeiNeural", "晓北", "女", "幽默", "方言（东北）"));
        zhCN.add(createVoice("zh-CN-shaanxi-XiaoniNeural", "晓妮", "女", "明亮", "方言（陕西）"));

        // 粤语音色
        List<Map<String, String>> zhHK = new ArrayList<>();
        zhHK.add(createVoice("zh-HK-HiuGaaiNeural", "曉佳", "女", "友好积极", "通用"));
        zhHK.add(createVoice("zh-HK-HiuMaanNeural", "曉曼", "女", "友好积极", "通用"));
        zhHK.add(createVoice("zh-HK-WanLungNeural", "雲龍", "男", "友好积极", "通用"));

        // 台湾国语音色
        List<Map<String, String>> zhTW = new ArrayList<>();
        zhTW.add(createVoice("zh-TW-HsiaoChenNeural", "曉臻", "女", "友好积极", "通用"));
        zhTW.add(createVoice("zh-TW-YunJheNeural", "雲哲", "男", "友好积极", "通用"));
        zhTW.add(createVoice("zh-TW-HsiaoYuNeural", "曉雨", "女", "友好积极", "通用"));

        // 英文音色（推荐）
        List<Map<String, String>> enUS = new ArrayList<>();
        enUS.add(createVoice("en-US-JennyNeural", "Jenny", "女", "温柔", "通用"));
        enUS.add(createVoice("en-US-GuyNeural", "Guy", "男", "活泼", "通用"));
        enUS.add(createVoice("en-US-AriaNeural", "Aria", "女", "专业", "新闻、小说"));
        enUS.add(createVoice("en-US-ChristopherNeural", "Christopher", "男", "可靠", "新闻、小说"));
        enUS.add(createVoice("en-US-EricNeural", "Eric", "男", "理性", "新闻、小说"));

        result.put("zh-CN", zhCN);
        result.put("zh-HK", zhHK);
        result.put("zh-TW", zhTW);
        result.put("en-US", enUS);

        return result;
    }

    /**
     * 构建命令
     */
    private List<String> buildCommand(String voice, String rate, String pitch, String text, Path outputPath) {
        List<String> command = new ArrayList<>();
        
        // 处理命令（支持 "py -m edge_tts" 这种多参数格式）
        String[] cmdParts = properties.getCommand().split("\\s+");
        for (String part : cmdParts) {
            command.add(part);
        }
        
        command.add("--voice");
        command.add(voice);
        command.add("--rate");
        command.add(rate);
        command.add("--pitch");
        command.add(pitch);
        command.add("--text");
        command.add(text);
        command.add("--write-media");
        command.add(outputPath.toString());
        
        return command;
    }

    /**
     * 创建音色信息
     */
    private Map<String, String> createVoice(String code, String name, String gender, 
                                           String characteristic, String scene) {
        Map<String, String> voice = new HashMap<>();
        voice.put("code", code);
        voice.put("name", name);
        voice.put("gender", gender);
        voice.put("characteristic", characteristic);
        voice.put("scene", scene);
        return voice;
    }
}
