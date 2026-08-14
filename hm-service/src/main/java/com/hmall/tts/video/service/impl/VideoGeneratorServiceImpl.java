package com.hmall.tts.video.service.impl;

import com.hmall.tts.video.dto.*;
import com.hmall.tts.video.service.VideoGeneratorService;
import com.hmall.tts.video.subtitle.ASSSubtitleGenerator;
import com.hmall.tts.video.util.FFmpegUtil;
import com.hmall.tts.volcengine.dto.DialogSegment;
import com.hmall.tts.volcengine.dto.DocumentTTSResult;
import com.hmall.tts.volcengine.dto.VoiceConfig;
import com.hmall.tts.volcengine.service.DocumentTTSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 视频生成服务实现类
 */
@Slf4j
@Service
public class VideoGeneratorServiceImpl implements VideoGeneratorService {
    
    @Autowired
    private DocumentTTSService documentTTSService;
    
    @Autowired
    private ASSSubtitleGenerator assSubtitleGenerator;
    
    @Autowired
    private FFmpegUtil ffmpegUtil;
    
    @Value("${tts.output.dir:./tts}")
    private String outputDir;
    
    @Override
    public VideoGenerateResponse generateVideoFromDocument(MultipartFile file, VideoGenerateRequest request) throws Exception {
        log.info("开始生成视频：文件名={}", file.getOriginalFilename());
        
        String taskId = UUID.randomUUID().toString();
        
        try {
            // ✅ 修复：构建音色配置
            VoiceConfig voiceConfig = VoiceConfig.builder()
                    .boldVoice(request.getBoldVoice())
                    .normalVoice(request.getNormalVoice())
                    .format(request.getAudioFormat())
                    .sampleRate(request.getSampleRate())
                    .build();
            
            // ✅ 修复：调用generateDocumentSpeech一次性获取音频和字幕
            log.info("[{}] 步骤1：调用TTS生成音频和字幕（一次性获取，确保100%同步）", taskId);
            DocumentTTSResult ttsResult = documentTTSService.generateDocumentSpeech(file, voiceConfig);
            
            if (!Boolean.TRUE.equals(ttsResult.getSuccess())) {
                log.error("[{}] TTS生成失败：{}", taskId, ttsResult.getMessage());
                return VideoGenerateResponse.builder()
                        .success(false)
                        .message("TTS生成失败：" + ttsResult.getMessage())
                        .taskId(taskId)
                        .build();
            }
            
            // ✅ 修复：从TTS结果中获取音频URL
            String audioUrl = ttsResult.getAudioUrl();  // 例如：/tts/documents/xxx.mp3
            log.info("[{}] TTS生成成功，音频URL：{}", taskId, audioUrl);
            
            // ✅ 修复：复制音频文件到temp目录（使用新的taskId）
            log.info("[{}] 步骤2：复制音频文件到temp目录", taskId);
            String audioFileName = taskId + "." + request.getAudioFormat();
            Path audioPath = copyAudioToTemp(audioUrl, audioFileName);
            log.info("[{}] 音频文件已保存：{}", taskId, audioPath);
            
            // ✅ 修复：使用TTS返回的DialogSegments（100%正确，来自实际音频）
            log.info("[{}] 步骤3：使用TTS返回的DialogSegments（100%同步）", taskId);
            List<DialogSegment> dialogSegments = ttsResult.getSegments();
            
            if (dialogSegments == null || dialogSegments.isEmpty()) {
                log.error("[{}] TTS返回的DialogSegments为空", taskId);
                return VideoGenerateResponse.builder()
                        .success(false)
                        .message("TTS返回的字幕数据为空")
                        .taskId(taskId)
                        .build();
            }
            
            // 转换为字幕片段
            List<SubtitleSegment> subtitleSegments = convertToSubtitleSegments(dialogSegments);
            log.info("[{}] 对话片段数量：{}（来自实际TTS音频）", taskId, subtitleSegments.size());
            
            // 步骤4：生成ASS字幕文件
            log.info("[{}] 步骤4：生成ASS字幕文件", taskId);
            VideoConfig videoConfig = request.getVideoConfig();
            if (videoConfig == null) {
                videoConfig = VideoConfig.builder().build(); // 使用默认配置
            }
            
            SubtitleConfig subtitleConfig = request.getSubtitleConfig();
            if (subtitleConfig == null) {
                subtitleConfig = SubtitleConfig.builder().build(); // 使用默认配置
            }
            
            String assContent = assSubtitleGenerator.generateASS(
                    subtitleSegments,
                    subtitleConfig,
                    videoConfig.getWidth(),
                    videoConfig.getHeight()
            );
            
            // 保存ASS字幕文件
            String assFileName = taskId + ".ass";
            Path assPath = saveToFile(assContent.getBytes("UTF-8"), assFileName);
            log.info("[{}] ASS字幕文件已保存：{}", taskId, assPath);
            
            // 步骤5：调用FFmpeg生成视频
            log.info("[{}] 步骤5：调用FFmpeg生成视频", taskId);
            String videoFileName = taskId + ".mp4";
            Path videoPath = Paths.get(outputDir, "videos", videoFileName);
            Files.createDirectories(videoPath.getParent());
            
            ffmpegUtil.generateVideo(
                    audioPath.toString(),
                    assPath.toString(),
                    videoPath.toString(),
                    videoConfig
            );
            
            // 步骤6：构建响应
            log.info("[{}] 视频生成成功", taskId);
            File videoFile = videoPath.toFile();
            
            // ✅ 使用TTS返回的总时长（精确值，不是估算值）
            double duration = ttsResult.getTotalDuration() != null ? 
                            ttsResult.getTotalDuration() : 
                            calculateTotalDuration(subtitleSegments);
            
            log.info("[{}] 视频生成完成，总时长：{}秒，字幕片段数：{}", taskId, duration, subtitleSegments.size());
            
            return VideoGenerateResponse.builder()
                    .success(true)
                    .message("视频生成成功")
                    .taskId(taskId)
                    .videoUrl("/tts/videos/" + videoFileName)
                    .duration(duration)
                    .videoSize(videoFile.length())
                    .subtitles(subtitleSegments)
                    .build();
                    
        } catch (Exception e) {
            log.error("[{}] 视频生成失败", taskId, e);
            return VideoGenerateResponse.builder()
                    .success(false)
                    .message("视频生成失败：" + e.getMessage())
                    .taskId(taskId)
                    .build();
        }
    }
    
    /**
     * 复制音频文件到temp目录
     * 
     * @param audioUrl 音频URL（例如：/tts/documents/xxx.mp3）
     * @param audioFileName 新的音频文件名（例如：taskId.mp3）
     * @return 复制后的音频文件路径
     */
    private Path copyAudioToTemp(String audioUrl, String audioFileName) throws Exception {
        // audioUrl例如：/tts/documents/xxx.mp3
        // 实际路径：./tts/documents/xxx.mp3
        
        String sourcePathStr = outputDir + audioUrl.replace("/tts", "");
        Path sourcePath = Paths.get(sourcePathStr);
        
        if (!Files.exists(sourcePath)) {
            log.error("音频文件不存在：{}", sourcePath);
            throw new Exception("音频文件不存在：" + sourcePath);
        }
        
        // 复制到temp目录
        Path targetDir = Paths.get(outputDir, "temp");
        Files.createDirectories(targetDir);
        
        Path targetPath = targetDir.resolve(audioFileName);
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("音频文件复制成功：{} -> {}", sourcePath, targetPath);
        
        return targetPath;
    }
    
    /**
     * 将对话片段转换为字幕片段
     */
    private List<SubtitleSegment> convertToSubtitleSegments(List<DialogSegment> dialogSegments) {
        List<SubtitleSegment> subtitleSegments = new ArrayList<>();
        
        for (DialogSegment dialogSegment : dialogSegments) {
            SubtitleSegment subtitleSegment = SubtitleSegment.builder()
                    .text(dialogSegment.getText())
                    .startTime(dialogSegment.getStartTime())
                    .duration(dialogSegment.getDuration())
                    .isBold(dialogSegment.getIsBold())
                    .speaker(dialogSegment.getVoiceId()) // DialogSegment的字段是voiceId不是speaker
                    .build();
            
            subtitleSegments.add(subtitleSegment);
        }
        
        return subtitleSegments;
    }
    
    /**
     * 保存数据到文件
     */
    private Path saveToFile(byte[] data, String fileName) throws Exception {
        Path directory = Paths.get(outputDir, "temp");
        Files.createDirectories(directory);
        
        Path filePath = directory.resolve(fileName);
        Files.write(filePath, data);
        
        return filePath;
    }
    
    /**
     * 计算总时长
     */
    private double calculateTotalDuration(List<SubtitleSegment> segments) {
        if (segments.isEmpty()) {
            return 0.0;
        }
        
        SubtitleSegment lastSegment = segments.get(segments.size() - 1);
        return lastSegment.getStartTime() + lastSegment.getDuration();
    }
}
