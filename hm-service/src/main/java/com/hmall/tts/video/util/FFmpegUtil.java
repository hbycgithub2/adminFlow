package com.hmall.tts.video.util;

import com.hmall.tts.video.dto.VideoConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * FFmpeg工具类
 */
@Slf4j
@Component
public class FFmpegUtil {
    
    /**
     * FFmpeg可执行文件路径
     * 使用绝对路径避免PATH环境变量问题
     */
    private static final String FFMPEG_PATH = "D:\\ai\\codex\\ffmpeg-9.0.1-essentials_build\\bin\\ffmpeg.exe";
    
    /**
     * 生成视频
     * 
     * @param audioPath 音频文件路径
     * @param assPath ASS字幕文件路径
     * @param outputPath 输出视频文件路径
     * @param config 视频配置
     * @throws Exception 生成失败时抛出异常
     */
    public void generateVideo(String audioPath, String assPath, String outputPath, VideoConfig config) throws Exception {
        log.info("开始生成视频：audio={}, ass={}, output={}", audioPath, assPath, outputPath);
        
        // 构建FFmpeg命令
        List<String> command = buildFFmpegCommand(audioPath, assPath, outputPath, config);
        
        log.info("FFmpeg命令：{}", String.join(" ", command));
        
        // 执行命令
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // 读取输出日志
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            log.debug("FFmpeg输出：{}", line);
        }
        
        // 等待完成
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg执行失败，退出码：" + exitCode);
        }
        
        // 检查输出文件是否生成
        File outputFile = new File(outputPath);
        if (!outputFile.exists()) {
            throw new RuntimeException("视频文件生成失败：" + outputPath);
        }
        
        log.info("视频生成成功：{}，大小：{} bytes", outputPath, outputFile.length());
    }
    
    /**
     * 构建FFmpeg命令
     */
    private List<String> buildFFmpegCommand(String audioPath, String assPath, String outputPath, VideoConfig config) {
        List<String> command = new ArrayList<>();
        
        command.add(FFMPEG_PATH);
        
        // 输入音频
        command.add("-i");
        command.add(audioPath);
        
        // 生成背景视频（纯色或图片）
        if (config.getBackgroundImagePath() != null && !config.getBackgroundImagePath().isEmpty()) {
            // 使用背景图片
            command.add("-loop");
            command.add("1");
            command.add("-i");
            command.add(config.getBackgroundImagePath());
        } else {
            // 使用纯色背景
            command.add("-f");
            command.add("lavfi");
            command.add("-i");
            command.add(String.format("color=c=%s:s=%dx%d",
                    config.getBackgroundColorFFmpeg(),
                    config.getWidth(),
                    config.getHeight()
            ));
        }
        
        // 添加ASS字幕滤镜
        command.add("-vf");
        command.add(String.format("ass=%s", assPath.replace("\\", "/")));
        
        // 视频编码参数
        command.add("-c:v");
        command.add(config.getCodec());
        
        command.add("-b:v");
        command.add(config.getBitrate() + "k");
        
        command.add("-r");
        command.add(String.valueOf(config.getFps()));
        
        // 音频编码参数
        command.add("-c:a");
        command.add(config.getAudioCodec());
        
        // 视频长度与音频一致
        command.add("-shortest");
        
        // 覆盖输出文件
        command.add("-y");
        
        // 输出文件
        command.add(outputPath);
        
        return command;
    }
    
    /**
     * 检查FFmpeg是否可用
     */
    public boolean checkFFmpegAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(FFMPEG_PATH, "-version");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.error("FFmpeg不可用", e);
            return false;
        }
    }
    
    /**
     * 从音频和ASS字幕生成视频（简化版，用于字幕编辑后重新生成）
     * 
     * @param audioPath 音频文件路径
     * @param assPath ASS字幕文件路径
     * @param outputPath 输出视频文件路径
     * @param width 视频宽度
     * @param height 视频高度
     * @param backgroundColor 背景颜色（#RRGGBB）
     * @return 是否成功
     */
    public boolean generateVideoFromAudioAndASS(String audioPath, String assPath, String outputPath, 
                                                 int width, int height, String backgroundColor) {
        log.info("开始生成视频：audio={}, ass={}, output={}", audioPath, assPath, outputPath);
        
        try {
            List<String> command = new ArrayList<>();
            
            command.add(FFMPEG_PATH);
            
            // 输入音频
            command.add("-i");
            command.add(audioPath);
            
            // 纯色背景
            command.add("-f");
            command.add("lavfi");
            command.add("-i");
            command.add(String.format("color=c=%s:s=%dx%d", backgroundColor, width, height));
            
            // 添加ASS字幕
            command.add("-vf");
            command.add(String.format("ass=%s", assPath.replace("\\", "/")));
            
            // 视频编码
            command.add("-c:v");
            command.add("libx264");
            command.add("-b:v");
            command.add("2000k");
            command.add("-r");
            command.add("30");
            
            // 音频编码
            command.add("-c:a");
            command.add("aac");
            
            // 视频长度与音频一致
            command.add("-shortest");
            
            // 覆盖输出文件
            command.add("-y");
            
            // 输出文件
            command.add(outputPath);
            
            log.info("FFmpeg命令：{}", String.join(" ", command));
            
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // 读取输出日志
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg输出：{}", line);
            }
            
            // 等待完成
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.error("FFmpeg执行失败，退出码：{}", exitCode);
                return false;
            }
            
            // 检查输出文件
            File outputFile = new File(outputPath);
            if (!outputFile.exists()) {
                log.error("视频文件生成失败：{}", outputPath);
                return false;
            }
            
            log.info("视频生成成功：{}，大小：{} bytes", outputPath, outputFile.length());
            return true;
            
        } catch (Exception e) {
            log.error("生成视频异常", e);
            return false;
        }
    }
    
    /**
     * 获取音频时长（秒）
     */
    public double getAudioDuration(String audioPath) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("ffprobe");
        command.add("-v");
        command.add("error");
        command.add("-show_entries");
        command.add("format=duration");
        command.add("-of");
        command.add("default=noprint_wrappers=1:nokey=1");
        command.add(audioPath);
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String durationStr = reader.readLine();
        
        process.waitFor();
        
        return Double.parseDouble(durationStr);
    }
}
