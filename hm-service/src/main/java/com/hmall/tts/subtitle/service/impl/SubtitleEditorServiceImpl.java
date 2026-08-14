package com.hmall.tts.subtitle.service.impl;

import com.hmall.tts.subtitle.dto.*;
import com.hmall.tts.subtitle.parser.ASSFormatter;
import com.hmall.tts.subtitle.parser.ASSParser;
import com.hmall.tts.subtitle.service.SubtitleEditorService;
import com.hmall.tts.video.util.FFmpegUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 字幕编辑服务实现
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Service
@Slf4j
public class SubtitleEditorServiceImpl implements SubtitleEditorService {
    
    @Autowired
    private ASSParser assParser;
    
    @Autowired
    private ASSFormatter assFormatter;
    
    @Autowired
    private FFmpegUtil ffmpegUtil;
    
    @Value("${tts.temp.dir:./tts/temp}")
    private String tempDir;
    
    @Value("${tts.output.dir:./tts/videos}")
    private String outputDir;
    
    /**
     * 确保目录存在
     */
    private void ensureDirectoriesExist() {
        try {
            Files.createDirectories(Paths.get(tempDir));
            Files.createDirectories(Paths.get(outputDir));
            log.debug("目录检查完成: temp={}, output={}", tempDir, outputDir);
        } catch (IOException e) {
            log.error("创建目录失败", e);
        }
    }
    
    /**
     * 加载字幕数据
     */
    @Override
    public SubtitleEditData loadSubtitles(String taskId) {
        log.info("[{}] 开始加载字幕数据", taskId);
        
        // 确保目录存在
        ensureDirectoriesExist();
        
        try {
            // 1. 构建ASS文件路径
            String assFilePath = tempDir + File.separator + taskId + ".ass";
            
            // 2. 检查文件是否存在
            if (!Files.exists(Paths.get(assFilePath))) {
                log.error("[{}] ASS文件不存在: {}", taskId, assFilePath);
                throw new RuntimeException("字幕文件不存在");
            }
            
            // 3. 解析ASS文件
            List<SubtitleSegment> subtitles = assParser.parse(assFilePath);
            
            // 4. 计算总时长
            Double totalDuration = subtitles.isEmpty() ? 0.0 : 
                    subtitles.stream()
                            .mapToDouble(SubtitleSegment::getEndTime)
                            .max()
                            .orElse(0.0);
            
            // 5. 构建视频URL
            String videoUrl = "/tts/videos/" + taskId + ".mp4";
            
            log.info("[{}] 字幕数据加载成功，共{}条字幕，总时长{}秒", taskId, subtitles.size(), totalDuration);
            
            return SubtitleEditData.builder()
                    .taskId(taskId)
                    .subtitles(subtitles)
                    .totalDuration(totalDuration)
                    .videoUrl(videoUrl)
                    .build();
            
        } catch (IOException e) {
            log.error("[{}] 加载字幕数据失败", taskId, e);
            throw new RuntimeException("加载字幕数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新字幕数据
     */
    @Override
    public SubtitleUpdateResponse updateSubtitles(SubtitleUpdateRequest request) {
        String taskId = request.getTaskId();
        log.info("[{}] 开始更新字幕数据，共{}条字幕", taskId, request.getSubtitles().size());
        
        try {
            // 1. 验证字幕数据
            List<Integer> overlaps = validateTimeOverlap(request.getSubtitles());
            if (!overlaps.isEmpty()) {
                log.warn("[{}] 检测到时间重叠的字幕: {}", taskId, overlaps);
                return SubtitleUpdateResponse.failure("字幕时间重叠，请检查字幕" + overlaps);
            }
            
            // 2. 格式化为ASS内容
            String assContent = assFormatter.format(request.getSubtitles(), 1920, 1080);
            
            // 3. 保存ASS文件
            String assFilePath = tempDir + File.separator + taskId + ".ass";
            Files.write(Paths.get(assFilePath), assContent.getBytes("UTF-8"));
            log.info("[{}] ASS文件保存成功: {}", taskId, assFilePath);
            
            // 4. 如果需要重新生成视频
            if (Boolean.TRUE.equals(request.getRegenerateVideo())) {
                log.info("[{}] 开始重新生成视频", taskId);
                return regenerateVideo(taskId);
            }
            
            log.info("[{}] 字幕更新成功", taskId);
            return SubtitleUpdateResponse.success("字幕更新成功");
            
        } catch (Exception e) {
            log.error("[{}] 更新字幕失败", taskId, e);
            return SubtitleUpdateResponse.failure("更新字幕失败: " + e.getMessage());
        }
    }
    
    /**
     * 重新生成视频
     */
    @Override
    public SubtitleUpdateResponse regenerateVideo(String taskId) {
        log.info("[{}] 开始重新生成视频", taskId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 检查音频文件
            String audioPath = tempDir + File.separator + taskId + ".mp3";
            if (!Files.exists(Paths.get(audioPath))) {
                log.error("[{}] 音频文件不存在: {}", taskId, audioPath);
                return SubtitleUpdateResponse.failure("音频文件不存在");
            }
            
            // 2. 检查ASS文件
            String assPath = tempDir + File.separator + taskId + ".ass";
            if (!Files.exists(Paths.get(assPath))) {
                log.error("[{}] 字幕文件不存在: {}", taskId, assPath);
                return SubtitleUpdateResponse.failure("字幕文件不存在");
            }
            
            // 3. 生成视频
            String outputPath = outputDir + File.separator + taskId + ".mp4";
            
            // 创建输出目录
            Files.createDirectories(Paths.get(outputDir));
            
            // 调用FFmpeg生成视频（使用原音频+新字幕）
            boolean success = ffmpegUtil.generateVideoFromAudioAndASS(
                    audioPath, 
                    assPath, 
                    outputPath, 
                    1920, 
                    1080, 
                    "#FFFFFF"
            );
            
            if (!success) {
                log.error("[{}] 视频生成失败", taskId);
                return SubtitleUpdateResponse.failure("视频生成失败");
            }
            
            double duration = (System.currentTimeMillis() - startTime) / 1000.0;
            String videoUrl = "/tts/videos/" + taskId + ".mp4";
            
            log.info("[{}] 视频重新生成成功，耗时{}秒，URL: {}", taskId, duration, videoUrl);
            
            SubtitleUpdateResponse response = SubtitleUpdateResponse.success("视频重新生成成功", videoUrl);
            response.setDuration(duration);
            return response;
            
        } catch (Exception e) {
            log.error("[{}] 重新生成视频失败", taskId, e);
            return SubtitleUpdateResponse.failure("重新生成视频失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证字幕时间重叠
     * 
     * @param subtitles 字幕列表
     * @return 重叠的字幕ID列表
     */
    private List<Integer> validateTimeOverlap(List<SubtitleSegment> subtitles) {
        List<Integer> overlaps = new ArrayList<>();
        
        for (int i = 0; i < subtitles.size() - 1; i++) {
            SubtitleSegment current = subtitles.get(i);
            SubtitleSegment next = subtitles.get(i + 1);
            
            // 检查当前字幕的结束时间是否大于下一条字幕的开始时间
            if (current.getEndTime() > next.getStartTime()) {
                overlaps.add(current.getId());
                overlaps.add(next.getId());
            }
        }
        
        return overlaps;
    }
}
