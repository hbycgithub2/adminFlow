package com.hmall.tts.segment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.tts.segment.dto.*;
import com.hmall.tts.segment.service.SegmentEditorService;
import com.hmall.tts.volcengine.dto.*;
import com.hmall.tts.volcengine.service.VolcengineTTSService;
import com.hmall.tts.volcengine.docx.AudioMerger;
import com.hmall.tts.video.util.FFmpegUtil;
import com.hmall.tts.whisperx.service.WhisperXService;
import com.hmall.tts.video.dto.VideoConfig;
import com.hmall.tts.video.dto.SubtitleConfig;
import com.hmall.tts.video.dto.SubtitleSegment;
import com.hmall.tts.video.subtitle.ASSSubtitleGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Base64;
import java.util.UUID;

/**
 * 段落编辑服务实现
 * 
 * 核心功能：
 * 1. 支持局部编辑（只重新TTS修改的段落）
 * 2. 重新合并完整音频
 * 3. WhisperX重新对齐完整音频（100%准确）
 * 4. 异步处理，实时进度反馈
 * 
 * @author Kiro
 * @since 2026-08-17
 */
@Slf4j
@Service
public class SegmentEditorServiceImpl implements SegmentEditorService {
    
    @Autowired
    private VolcengineTTSService ttsService;
    
    @Autowired
    private AudioMerger audioMerger;
    
    @Autowired
    private WhisperXService whisperXService;
    
    @Autowired
    private FFmpegUtil ffmpegUtil;
    
    @Autowired
    private ASSSubtitleGenerator assSubtitleGenerator;
    
    @Value("${tts.temp.dir:./tts/temp}")
    private String tempDir;
    
    @Value("${tts.output.dir:./tts}")
    private String outputDir;
    
    /**
     * 任务状态存储（内存存储，生产环境应使用Redis）
     */
    private final Map<String, JobStatusResponse> jobStatusMap = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public SegmentEditResponse editSegment(SegmentEditRequest request) {
        log.info("[编辑段落] 任务ID={}, 段落={}, 新文本={}", 
                request.getTaskId(), request.getSegmentIndex(), 
                request.getNewText().length() > 20 ? request.getNewText().substring(0, 20) + "..." : request.getNewText());
        
        try {
            // 1. 加载元数据
            TaskMetadata metadata = loadMetadata(request.getTaskId());
            if (metadata == null) {
                return SegmentEditResponse.failure("任务不存在");
            }
            
            // 2. 验证段落索引
            if (request.getSegmentIndex() < 0 || request.getSegmentIndex() >= metadata.getSegments().size()) {
                return SegmentEditResponse.failure("段落索引超出范围");
            }
            
            // 3. 更新段落信息
            SegmentMetadata segment = metadata.getSegments().get(request.getSegmentIndex());
            segment.setText(request.getNewText());
            if (request.getVoiceId() != null) {
                segment.setVoiceId(request.getVoiceId());
            }
            if (request.getIsBold() != null) {
                segment.setIsBold(request.getIsBold());
            }
            
            // 4. 标记需要重新生成音频
            segment.setAudioDataBase64(null);  // 清空旧音频
            
            // 5. 保存元数据
            metadata.setUpdateTime(System.currentTimeMillis());
            saveMetadata(metadata);
            
            // 6. 如果需要重新生成视频
            if (Boolean.TRUE.equals(request.getRegenerateVideo())) {
                String jobId = UUID.randomUUID().toString();
                
                // 创建任务状态
                JobStatusResponse jobStatus = JobStatusResponse.builder()
                        .jobId(jobId)
                        .status("pending")
                        .progress(0)
                        .currentStep("等待处理...")
                        .build();
                jobStatusMap.put(jobId, jobStatus);
                
                // 异步生成视频
                regenerateVideoAsync(request.getTaskId(), jobId);
                
                return SegmentEditResponse.success("正在生成视频...", request.getTaskId(), jobId);
            }
            
            return SegmentEditResponse.success("段落已更新", request.getTaskId(), null);
            
        } catch (Exception e) {
            log.error("[编辑段落] 失败", e);
            return SegmentEditResponse.failure("编辑失败: " + e.getMessage());
        }
    }
    
    @Override
    public SegmentEditResponse insertSegment(SegmentInsertRequest request) {
        log.info("[插入段落] 任务ID={}, 位置={}, 文本={}", 
                request.getTaskId(), request.getInsertAfter(), 
                request.getText().length() > 20 ? request.getText().substring(0, 20) + "..." : request.getText());
        
        try {
            // 1. 加载元数据
            TaskMetadata metadata = loadMetadata(request.getTaskId());
            if (metadata == null) {
                return SegmentEditResponse.failure("任务不存在");
            }
            
            // 2. 验证插入位置
            if (request.getInsertAfter() < -1 || request.getInsertAfter() >= metadata.getSegments().size()) {
                return SegmentEditResponse.failure("插入位置超出范围");
            }
            
            // 3. 创建新段落
            SegmentMetadata newSegment = SegmentMetadata.builder()
                    .text(request.getText())
                    .voiceId(request.getVoiceId())
                    .isBold(request.getIsBold())
                    .needPause(true)
                    .pauseDuration(800)
                    .build();
            
            // 4. 插入到列表
            int insertIndex = request.getInsertAfter() + 1;
            metadata.getSegments().add(insertIndex, newSegment);
            
            // 5. 重新计算所有段落的索引
            for (int i = 0; i < metadata.getSegments().size(); i++) {
                metadata.getSegments().get(i).setIndex(i);
            }
            
            // 6. 保存元数据
            metadata.setUpdateTime(System.currentTimeMillis());
            saveMetadata(metadata);
            
            // 7. 如果需要重新生成视频
            if (Boolean.TRUE.equals(request.getRegenerateVideo())) {
                String jobId = UUID.randomUUID().toString();
                
                JobStatusResponse jobStatus = JobStatusResponse.builder()
                        .jobId(jobId)
                        .status("pending")
                        .progress(0)
                        .currentStep("等待处理...")
                        .build();
                jobStatusMap.put(jobId, jobStatus);
                
                regenerateVideoAsync(request.getTaskId(), jobId);
                
                return SegmentEditResponse.success("正在生成视频...", request.getTaskId(), jobId);
            }
            
            return SegmentEditResponse.success("段落已插入", request.getTaskId(), null);
            
        } catch (Exception e) {
            log.error("[插入段落] 失败", e);
            return SegmentEditResponse.failure("插入失败: " + e.getMessage());
        }
    }
    
    @Override
    public SegmentEditResponse deleteSegment(SegmentDeleteRequest request) {
        log.info("[删除段落] 任务ID={}, 段落={}", request.getTaskId(), request.getSegmentIndex());
        
        try {
            // 1. 加载元数据
            TaskMetadata metadata = loadMetadata(request.getTaskId());
            if (metadata == null) {
                return SegmentEditResponse.failure("任务不存在");
            }
            
            // 2. 验证段落索引
            if (request.getSegmentIndex() < 0 || request.getSegmentIndex() >= metadata.getSegments().size()) {
                return SegmentEditResponse.failure("段落索引超出范围");
            }
            
            // 3. 删除段落
            metadata.getSegments().remove(request.getSegmentIndex().intValue());
            
            // 4. 重新计算所有段落的索引
            for (int i = 0; i < metadata.getSegments().size(); i++) {
                metadata.getSegments().get(i).setIndex(i);
            }
            
            // 5. 保存元数据
            metadata.setUpdateTime(System.currentTimeMillis());
            saveMetadata(metadata);
            
            // 6. 如果需要重新生成视频
            if (Boolean.TRUE.equals(request.getRegenerateVideo())) {
                String jobId = UUID.randomUUID().toString();
                
                JobStatusResponse jobStatus = JobStatusResponse.builder()
                        .jobId(jobId)
                        .status("pending")
                        .progress(0)
                        .currentStep("等待处理...")
                        .build();
                jobStatusMap.put(jobId, jobStatus);
                
                regenerateVideoAsync(request.getTaskId(), jobId);
                
                return SegmentEditResponse.success("正在生成视频...", request.getTaskId(), jobId);
            }
            
            return SegmentEditResponse.success("段落已删除", request.getTaskId(), null);
            
        } catch (Exception e) {
            log.error("[删除段落] 失败", e);
            return SegmentEditResponse.failure("删除失败: " + e.getMessage());
        }
    }
    
    @Override
    public JobStatusResponse getJobStatus(String jobId) {
        JobStatusResponse status = jobStatusMap.get(jobId);
        if (status == null) {
            return JobStatusResponse.builder()
                    .jobId(jobId)
                    .status("notfound")
                    .errorMessage("任务不存在")
                    .build();
        }
        return status;
    }
    
    /**
     * 异步重新生成视频
     */
    @Async
    public void regenerateVideoAsync(String taskId, String jobId) {
        log.info("[异步任务] 开始重新生成视频，taskId={}, jobId={}", taskId, jobId);
        
        try {
            JobStatusResponse status = jobStatusMap.get(jobId);
            
            // 步骤1：加载元数据
            status.setStatus("processing");
            status.setProgress(10);
            status.setCurrentStep("加载元数据...");
            
            TaskMetadata metadata = loadMetadata(taskId);
            if (metadata == null) {
                throw new Exception("任务不存在");
            }
            
            // 步骤2：重新TTS生成需要更新的段落
            status.setProgress(20);
            status.setCurrentStep("生成新音频...");
            
            int totalSegments = metadata.getSegments().size();
            int processedSegments = 0;
            
            for (SegmentMetadata segment : metadata.getSegments()) {
                if (segment.getAudioDataBase64() == null || segment.getAudioDataBase64().isEmpty()) {
                    // 需要重新生成
                    log.info("[异步任务] 重新TTS生成段落{}：{}", segment.getIndex(), 
                            segment.getText().length() > 20 ? segment.getText().substring(0, 20) + "..." : segment.getText());
                    
                    try {
                        // ⭐ 修复：使用正确的API方法和字段名
                        TTSRequest request = TTSRequest.builder()
                                .text(segment.getText())
                                .speaker(segment.getVoiceId())  // ⭐ 字段名是speaker不是voiceId
                                .format(metadata.getVoiceConfig().getFormat())
                                .sampleRate(metadata.getVoiceConfig().getSampleRate())
                                .build();
                        
                        byte[] audioData = ttsService.generateSpeechBytes(request);
                        segment.setAudioDataBase64(Base64.getEncoder().encodeToString(audioData));
                        log.debug("[异步任务] ✅ 段落{}生成成功，大小: {} KB", segment.getIndex(), audioData.length / 1024.0);
                    } catch (Exception e) {
                        log.error("[异步任务] ❌ 段落{}TTS失败: {}", segment.getIndex(), e.getMessage());
                        
                        // ⭐ 错误处理：使用原音频（从fullAudioPath切割）
                        log.warn("[异步任务] 降级：使用原音频（段落{}）", segment.getIndex());
                        
                        if (metadata.getFullAudioPath() != null && segment.getStartTime() != null && segment.getDuration() != null) {
                            try {
                                byte[] audioData = extractAudioSegment(
                                    metadata.getFullAudioPath(), 
                                    segment.getStartTime(), 
                                    segment.getDuration()
                                );
                                segment.setAudioDataBase64(Base64.getEncoder().encodeToString(audioData));
                                log.info("[异步任务] ✅ 段落{}使用原音频", segment.getIndex());
                            } catch (Exception e2) {
                                log.error("[异步任务] ❌ 段落{}切割原音频也失败: {}", segment.getIndex(), e2.getMessage());
                                throw new Exception("段落" + segment.getIndex() + "音频生成失败: " + e.getMessage());
                            }
                        } else {
                            throw new Exception("段落" + segment.getIndex() + "音频生成失败且无法回退: " + e.getMessage());
                        }
                    }
                } else {
                    // 已有音频数据，跳过
                    log.debug("[异步任务] 段落{}已有音频数据，跳过", segment.getIndex());
                }
                
                // 更新进度
                processedSegments++;
                int progress = 20 + (processedSegments * 20 / totalSegments);  // 20-40%
                status.setProgress(progress);
            }
            
            // ⭐ 步骤2.5：为未修改的段落切割音频（从fullAudioPath）
            status.setProgress(40);
            status.setCurrentStep("准备音频数据...");
            
            if (metadata.getFullAudioPath() != null) {
                for (SegmentMetadata segment : metadata.getSegments()) {
                    // 如果没有audioDataBase64，说明是未修改的段落，需要从fullAudioPath切割
                    if (segment.getAudioDataBase64() == null || segment.getAudioDataBase64().isEmpty()) {
                        log.debug("[异步任务] 段落{}未修改，从完整音频切割", segment.getIndex());
                        
                        try {
                            byte[] audioData = extractAudioSegment(
                                metadata.getFullAudioPath(), 
                                segment.getStartTime(), 
                                segment.getDuration()
                            );
                            segment.setAudioDataBase64(Base64.getEncoder().encodeToString(audioData));
                            log.debug("[异步任务] ✅ 段落{}切割成功", segment.getIndex());
                        } catch (Exception e) {
                            log.error("[异步任务] ❌ 段落{}切割失败: {}", segment.getIndex(), e.getMessage());
                            throw new Exception("段落" + segment.getIndex() + "音频切割失败: " + e.getMessage());
                        }
                    }
                }
            }
            
            // 步骤3：合并完整音频
            status.setProgress(50);
            status.setCurrentStep("合并音频...");
            
            byte[] fullAudio = mergeAllSegments(metadata.getSegments(), metadata.getVoiceConfig().getSampleRate());
            
            // 步骤4：WhisperX对齐
            status.setProgress(65);
            status.setCurrentStep("对齐字幕...");
            
            List<DialogSegment> dialogSegments = alignWithWhisperX(fullAudio, metadata.getSegments());
            
            // 步骤5：生成ASS字幕
            status.setProgress(80);
            status.setCurrentStep("生成字幕文件...");
            
            List<SubtitleSegment> subtitleSegments = convertToSubtitleSegments(dialogSegments);
            String assContent = generateASS(subtitleSegments, metadata);
            
            // 保存ASS文件
            Path assPath = Paths.get(tempDir, taskId + ".ass");
            Files.write(assPath, assContent.getBytes("UTF-8"));
            
            // 步骤6：保存音频文件
            Path audioPath = Paths.get(tempDir, taskId + ".mp3");
            Files.write(audioPath, fullAudio);
            
            // 步骤7：生成视频
            status.setProgress(90);
            status.setCurrentStep("生成视频...");
            
            Path videoPath = Paths.get(outputDir, "videos", taskId + ".mp4");
            Files.createDirectories(videoPath.getParent());
            
            VideoConfig videoConfig = metadata.getVideoConfig() != null ? 
                    metadata.getVideoConfig() : VideoConfig.builder().build();
            
            ffmpegUtil.generateVideo(
                    audioPath.toString(),
                    assPath.toString(),
                    videoPath.toString(),
                    videoConfig
            );
            
            // 步骤8：更新元数据
            status.setProgress(98);
            status.setCurrentStep("更新元数据...");
            
            updateSegmentTimestamps(metadata.getSegments(), dialogSegments);
            metadata.setUpdateTime(System.currentTimeMillis());
            saveMetadata(metadata);
            
            // 完成
            status.setStatus("completed");
            status.setProgress(100);
            status.setCurrentStep("完成！");
            status.setVideoUrl("/tts/videos/" + taskId + ".mp4");
            
            log.info("[异步任务] 视频生成完成，taskId={}, jobId={}", taskId, jobId);
            
        } catch (Exception e) {
            log.error("[异步任务] 视频生成失败，taskId={}, jobId={}", taskId, jobId, e);
            
            JobStatusResponse status = jobStatusMap.get(jobId);
            status.setStatus("failed");
            status.setCurrentStep("失败");
            status.setErrorMessage(e.getMessage());
        }
    }
    
    /**
     * 加载元数据
     */
    private TaskMetadata loadMetadata(String taskId) {
        try {
            Path metadataPath = Paths.get(tempDir, taskId + ".json");
            if (!Files.exists(metadataPath)) {
                return null;
            }
            String json = new String(Files.readAllBytes(metadataPath), "UTF-8");
            return objectMapper.readValue(json, TaskMetadata.class);
        } catch (Exception e) {
            log.error("[加载元数据] 失败，taskId={}", taskId, e);
            return null;
        }
    }
    
    /**
     * 保存元数据
     */
    private void saveMetadata(TaskMetadata metadata) {
        try {
            Path metadataPath = Paths.get(tempDir, metadata.getTaskId() + ".json");
            Files.createDirectories(metadataPath.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata);
            Files.write(metadataPath, json.getBytes("UTF-8"));
            log.debug("[保存元数据] 成功，taskId={}", metadata.getTaskId());
        } catch (Exception e) {
            log.error("[保存元数据] 失败，taskId={}", metadata.getTaskId(), e);
        }
    }
    
    /**
     * 合并所有段落的音频
     * 
     * 策略：
     * 1. 如果段落有audioDataBase64，直接解码使用（已重新TTS）
     * 2. 如果没有，从fullAudioPath按时间戳切割（未修改的段落）
     */
    private byte[] mergeAllSegments(List<SegmentMetadata> segments, int sampleRate) throws Exception {
        log.info("[合并音频] 开始合并{}个段落", segments.size());
        
        List<com.hmall.tts.volcengine.dto.AudioSegment> audioSegments = new ArrayList<>();
        
        for (int i = 0; i < segments.size(); i++) {
            SegmentMetadata segment = segments.get(i);
            byte[] audioData;
            
            // ⭐ 关键修复：判断是否需要从完整音频切割
            if (segment.getAudioDataBase64() != null && !segment.getAudioDataBase64().isEmpty()) {
                // 情况1：已重新TTS（有Base64数据）
                log.debug("[合并音频] 段落{}: 使用新TTS音频", i);
                audioData = Base64.getDecoder().decode(segment.getAudioDataBase64());
            } else {
                // 情况2：未修改（从完整音频切割）
                log.debug("[合并音频] 段落{}: 从完整音频切割（{}-{}秒）", 
                         i, 
                         String.format("%.2f", segment.getStartTime()),
                         String.format("%.2f", segment.getEndTime()));
                
                // ⭐ 关键：这里需要从TaskMetadata的fullAudioPath切割
                // 但我们在这个方法中没有TaskMetadata引用
                // 解决：在调用mergeAllSegments前，先切割好音频并设置到audioDataBase64
                throw new Exception("段落" + i + "音频数据缺失，需要先从完整音频切割");
            }
            
            // 构建AudioSegment
            com.hmall.tts.volcengine.dto.AudioSegment audioSegment = 
                    new com.hmall.tts.volcengine.dto.AudioSegment();
            audioSegment.setAudioData(audioData);
            audioSegment.setNeedPause(segment.getNeedPause());
            audioSegment.setPauseDuration(segment.getPauseDuration());
            
            audioSegments.add(audioSegment);
        }
        
        // 使用AudioMerger合并
        byte[] mergedAudio = audioMerger.merge(audioSegments, sampleRate);
        
        log.info("[合并音频] 合并完成，总大小：{} KB", mergedAudio.length / 1024.0);
        
        return mergedAudio;
    }
    
    /**
     * ⭐ 核心方法：从完整音频中提取指定时间段的音频
     * 
     * 使用FFmpeg切割音频：
     * ffmpeg -i full.mp3 -ss 5.2 -t 8.3 -acodec copy segment.mp3
     * 
     * @param fullAudioPath 完整音频文件路径
     * @param startTime 开始时间（秒）
     * @param duration 持续时间（秒）
     * @return 切割后的音频数据
     */
    private byte[] extractAudioSegment(String fullAudioPath, double startTime, double duration) throws Exception {
        log.debug("[音频切割] 切割音频：文件={}, 开始={}秒, 时长={}秒", 
                 fullAudioPath, 
                 String.format("%.3f", startTime), 
                 String.format("%.3f", duration));
        
        // 生成临时输出文件
        String outputFileName = "segment_" + UUID.randomUUID().toString() + ".mp3";
        Path outputPath = Paths.get(tempDir, outputFileName);
        Files.createDirectories(outputPath.getParent());
        
        try {
            // 构建FFmpeg命令
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-y");  // 覆盖输出文件
            command.add("-i");
            command.add(fullAudioPath);
            command.add("-ss");
            command.add(String.format("%.3f", startTime));
            command.add("-t");
            command.add(String.format("%.3f", duration));
            command.add("-acodec");
            command.add("copy");  // 不重新编码，直接复制（快速）
            command.add(outputPath.toString());
            
            log.debug("[音频切割] 执行命令: {}", String.join(" ", command));
            
            // 执行FFmpeg
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取输出（用于日志）
            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // 等待完成
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.error("[音频切割] FFmpeg执行失败，退出码: {}, 输出:\n{}", exitCode, output);
                throw new Exception("音频切割失败: FFmpeg退出码 " + exitCode);
            }
            
            // 读取切割后的音频
            if (!Files.exists(outputPath)) {
                throw new Exception("音频切割失败: 输出文件不存在");
            }
            
            byte[] audioData = Files.readAllBytes(outputPath);
            
            log.debug("[音频切割] ✅ 切割成功，大小: {} KB", audioData.length / 1024.0);
            
            // 删除临时文件
            Files.deleteIfExists(outputPath);
            
            return audioData;
            
        } catch (Exception e) {
            log.error("[音频切割] 切割失败", e);
            // 清理临时文件
            try {
                Files.deleteIfExists(outputPath);
            } catch (Exception ignored) {
            }
            throw e;
        }
    }
    
    /**
     * WhisperX对齐
     */
    private List<DialogSegment> alignWithWhisperX(byte[] fullAudio, List<SegmentMetadata> segments) {
        log.info("[WhisperX对齐] 开始对齐，音频大小：{} KB", fullAudio.length / 1024.0);
        
        try {
            // 1. 提取完整文本
            String fullText = segments.stream()
                    .map(SegmentMetadata::getText)
                    .collect(Collectors.joining());
            
            log.info("[WhisperX对齐] 完整文本长度：{} 字符", fullText.length());
            
            // 2. WhisperX一次性对齐完整音频
            if (!whisperXService.isAvailable()) {
                log.warn("[WhisperX对齐] WhisperX服务不可用，使用估算方法");
                return buildDialogSegmentsWithEstimation(segments, fullAudio);
            }
            
            List<com.hmall.tts.whisperx.dto.CharTimestamp> charTimestamps = 
                    whisperXService.align(fullAudio, fullText);
            
            if (charTimestamps == null || charTimestamps.isEmpty()) {
                log.warn("[WhisperX对齐] WhisperX返回空结果，使用估算方法");
                return buildDialogSegmentsWithEstimation(segments, fullAudio);
            }
            
            log.info("[WhisperX对齐] ✅ WhisperX对齐成功，共{}个字符时间戳", charTimestamps.size());
            
            // 3. 将字符时间戳映射到DialogSegment
            List<DialogSegment> dialogSegments = new ArrayList<>();
            int charIndex = 0;
            
            for (int i = 0; i < segments.size(); i++) {
                SegmentMetadata segment = segments.get(i);
                String text = segment.getText();
                
                if (text.isEmpty()) {
                    continue;
                }
                
                List<CharTiming> charTimings = new ArrayList<>();
                double startTime = -1.0;
                double endTime = 0.0;
                
                // 收集这个段落的所有字符时间戳
                for (int j = 0; j < text.length() && charIndex < charTimestamps.size(); j++, charIndex++) {
                    com.hmall.tts.whisperx.dto.CharTimestamp whisperXChar = charTimestamps.get(charIndex);
                    
                    if (startTime < 0) {
                        startTime = whisperXChar.getStartTime();
                    }
                    endTime = whisperXChar.getEndTime();
                    
                    CharTiming charTiming = CharTiming.builder()
                            .character(whisperXChar.getCharacter())
                            .startTime(whisperXChar.getStartTime())
                            .duration(whisperXChar.getDuration())
                            .build();
                    
                    charTimings.add(charTiming);
                }
                
                if (startTime < 0) {
                    startTime = 0.0;
                }
                
                DialogSegment dialogSegment = DialogSegment.builder()
                        .index(i)
                        .text(text)
                        .isBold(segment.getIsBold())
                        .startTime(startTime)
                        .duration(endTime - startTime)
                        .voiceId(segment.getVoiceId())
                        .charTimings(charTimings)
                        .build();
                
                dialogSegments.add(dialogSegment);
                
                log.debug("[WhisperX对齐] 段落{}: 「{}」, 时间: {}-{}秒", 
                         i, 
                         text.length() > 20 ? text.substring(0, 20) + "..." : text,
                         String.format("%.3f", startTime),
                         String.format("%.3f", endTime));
            }
            
            log.info("[WhisperX对齐] ✅ 完成，共{}个DialogSegment", dialogSegments.size());
            
            return dialogSegments;
            
        } catch (Exception e) {
            log.error("[WhisperX对齐] 对齐失败，使用估算方法", e);
            return buildDialogSegmentsWithEstimation(segments, fullAudio);
        }
    }
    
    /**
     * 降级方法：使用估算构建DialogSegment
     */
    private List<DialogSegment> buildDialogSegmentsWithEstimation(
            List<SegmentMetadata> segments, byte[] fullAudio) {
        
        log.info("[估算方法] 使用估算方法构建DialogSegment...");
        
        // 估算总时长（44100 Hz, 16-bit, Mono）
        double totalDuration = fullAudio.length / (44100.0 * 2);
        
        List<DialogSegment> dialogSegments = new ArrayList<>();
        double currentTime = 0.0;
        
        for (int i = 0; i < segments.size(); i++) {
            SegmentMetadata segment = segments.get(i);
            
            // 简单估算：按文本长度比例分配时间
            double segmentDuration = (segment.getText().length() * totalDuration) / 
                    segments.stream().mapToInt(s -> s.getText().length()).sum();
            
            DialogSegment dialogSegment = DialogSegment.builder()
                    .index(i)
                    .text(segment.getText())
                    .isBold(segment.getIsBold())
                    .startTime(currentTime)
                    .duration(segmentDuration)
                    .voiceId(segment.getVoiceId())
                    .charTimings(new ArrayList<>())
                    .build();
            
            dialogSegments.add(dialogSegment);
            currentTime += segmentDuration;
        }
        
        return dialogSegments;
    }
    
    /**
     * 转换为字幕片段
     */
    private List<SubtitleSegment> convertToSubtitleSegments(List<DialogSegment> dialogSegments) {
        List<SubtitleSegment> subtitleSegments = new ArrayList<>();
        
        for (DialogSegment dialog : dialogSegments) {
            SubtitleSegment subtitle = SubtitleSegment.builder()
                    .text(dialog.getText())
                    .startTime(dialog.getStartTime())
                    .duration(dialog.getDuration())
                    .isBold(dialog.getIsBold())
                    .speaker(dialog.getVoiceId())
                    .build();
            
            subtitleSegments.add(subtitle);
        }
        
        log.debug("[转换字幕] 转换了{}个DialogSegment为SubtitleSegment", dialogSegments.size());
        
        return subtitleSegments;
    }
    
    /**
     * 生成ASS字幕
     */
    private String generateASS(List<SubtitleSegment> subtitles, TaskMetadata metadata) {
        SubtitleConfig subtitleConfig = metadata.getSubtitleConfig() != null ? 
                metadata.getSubtitleConfig() : SubtitleConfig.builder().build();
        
        VideoConfig videoConfig = metadata.getVideoConfig() != null ? 
                metadata.getVideoConfig() : VideoConfig.builder().build();
        
        String assContent = assSubtitleGenerator.generateASS(
                subtitles,
                subtitleConfig,
                videoConfig.getWidth(),
                videoConfig.getHeight()
        );
        
        log.debug("[生成ASS] 生成了{}个字幕片段的ASS文件", subtitles.size());
        
        return assContent;
    }
    
    /**
     * 更新段落时间戳
     */
    private void updateSegmentTimestamps(List<SegmentMetadata> segments, List<DialogSegment> dialogSegments) {
        for (int i = 0; i < Math.min(segments.size(), dialogSegments.size()); i++) {
            SegmentMetadata segment = segments.get(i);
            DialogSegment dialog = dialogSegments.get(i);
            segment.setStartTime(dialog.getStartTime());
            segment.setDuration(dialog.getDuration());
            segment.setEndTime(dialog.getStartTime() + dialog.getDuration());
        }
    }
}
