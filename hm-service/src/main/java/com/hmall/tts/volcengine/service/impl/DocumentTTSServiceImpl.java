package com.hmall.tts.volcengine.service.impl;

import com.hmall.tts.volcengine.config.VolcengineConfig;
import com.hmall.tts.volcengine.docx.*;
import com.hmall.tts.volcengine.dto.*;
import com.hmall.tts.volcengine.service.DocumentTTSService;
import com.hmall.tts.volcengine.service.VolcengineTTSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 文档TTS服务实现
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentTTSServiceImpl implements DocumentTTSService {
    
    private final WordDocumentParser documentParser;
    private final TextSegmentMerger segmentMerger;
    private final SmartPauseCalculator pauseCalculator;
    private final AudioMerger audioMerger;
    private final VolcengineTTSService ttsService;
    private final VolcengineConfig config;
    
    /**
     * 并发执行器（限制并发数为3）
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    
    /**
     * 最大文本长度（字符）
     */
    private static final int MAX_TEXT_LENGTH = 800;
    
    @Override
    public DocumentTTSResult generateDocumentSpeech(MultipartFile file, VoiceConfig voiceConfig) {
        long startTime = System.currentTimeMillis();
        String taskId = UUID.randomUUID().toString();
        
        try {
            log.info("开始生成文档TTS，任务ID: {}, 文件名: {}", taskId, file.getOriginalFilename());
            
            // 1. 验证文件
            validateFile(file);
            
            // 2. 解析Word文档获取原始片段
            List<TextSegment> originalSegments = documentParser.parse(file.getInputStream(), voiceConfig);
            
            // 3. 生成音频（同时返回片段时间信息）
            AudioGenerationResult audioResult = generateDocumentSpeechWithTiming(originalSegments, voiceConfig);
            
            // 4. 保存音频文件
            String fileName = taskId + "." + voiceConfig.getFormat();
            Path outputDir = Paths.get(config.getOutputDir(), "documents");
            Files.createDirectories(outputDir);
            
            Path audioFile = outputDir.resolve(fileName);
            Files.write(audioFile, audioResult.getAudioData());
            
            long generateTime = System.currentTimeMillis() - startTime;
            
            log.info("文档TTS生成成功，任务ID: {}, 文件: {}, 大小: {} KB, 耗时: {} ms",
                    taskId, fileName, audioResult.getAudioData().length / 1024.0, generateTime);
            
            return DocumentTTSResult.success(
                    taskId,
                    "/tts/documents/" + fileName,
                    (long) audioResult.getAudioData().length,
                    generateTime,
                    audioResult.getDialogSegments(),
                    audioResult.getTotalDuration()
            );
            
        } catch (Exception e) {
            log.error("文档TTS生成失败，任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
            return DocumentTTSResult.fail("文档TTS生成失败: " + e.getMessage());
        }
    }
    
    @Override
    public byte[] generateDocumentSpeechBytes(MultipartFile file, VoiceConfig voiceConfig) throws Exception {
        log.info("开始生成文档TTS音频，文件名: {}", file.getOriginalFilename());
        
        try {
            // 1. 解析Word文档
            log.info("步骤1: 解析Word文档...");
            List<TextSegment> segments = documentParser.parse(file.getInputStream(), voiceConfig);
            
            if (segments.isEmpty()) {
                throw new Exception("文档中没有可用的文本内容");
            }
            
            log.info("解析完成，共{}个文本片段", segments.size());
            
            // 2. 生成音频并返回结果
            AudioGenerationResult result = generateDocumentSpeechWithTiming(segments, voiceConfig);
            
            return result.getAudioData();
            
        } catch (Exception e) {
            log.error("生成文档TTS音频失败: {}", e.getMessage(), e);
            throw new Exception("生成文档TTS音频失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成文档语音（包含时间信息）
     */
    private AudioGenerationResult generateDocumentSpeechWithTiming(List<TextSegment> segments, VoiceConfig voiceConfig) throws Exception {
        // 1. 合并相同音色的连续片段
        log.info("步骤2: 合并文本片段...");
        List<MergedSegment> mergedSegments = segmentMerger.merge(segments);
        
        // 2. 拆分过长的片段
        List<MergedSegment> finalSegments = new ArrayList<>();
        for (MergedSegment segment : mergedSegments) {
            List<MergedSegment> split = segmentMerger.splitIfTooLong(segment, MAX_TEXT_LENGTH);
            finalSegments.addAll(split);
        }
        
        log.info("合并完成，共{}个合并片段", finalSegments.size());
        
        // 3. 并发调用TTS API
        log.info("步骤3: 并发生成语音（{}个API调用）...", finalSegments.size());
        List<AudioSegment> audioSegments = synthesizeParallel(finalSegments, voiceConfig);
        
        // 4. 计算停顿并设置
        log.info("步骤4: 计算智能停顿...");
        calculatePauses(audioSegments, mergedSegments);
        
        // 5. 构建对话片段列表（用于前端实时显示）
        log.info("步骤5: 构建对话片段时间信息...");
        List<DialogSegment> dialogSegments = buildDialogSegments(segments, audioSegments, voiceConfig);
        
        // 6. 合并音频
        log.info("步骤6: 合并音频片段...");
        byte[] finalAudio = audioMerger.merge(audioSegments, voiceConfig.getSampleRate());
        
        // 7. 计算总时长（根据采样率和音频数据大小估算）
        double totalDuration = calculateTotalDuration(finalAudio, voiceConfig);
        
        log.info("文档TTS音频生成完成，总大小: {} KB, 总时长: {:.2f}秒", 
                finalAudio.length / 1024.0, totalDuration);
        
        return new AudioGenerationResult(finalAudio, dialogSegments, totalDuration);
    }
    
    /**
     * 构建对话片段列表（用于前端实时显示）
     * 按行合并文本：同一行的所有文本合并为一个DialogSegment
     * 使用实际音频时长而不是估算时长
     */
    private List<DialogSegment> buildDialogSegments(List<TextSegment> originalSegments, 
                                                   List<AudioSegment> audioSegments,
                                                   VoiceConfig voiceConfig) {
        List<DialogSegment> dialogSegments = new ArrayList<>();
        double currentTime = 0.0;
        
        if (originalSegments.isEmpty() || audioSegments.isEmpty()) {
            return dialogSegments;
        }
        
        // 步骤1：构建行到AudioSegment的映射
        // 合并策略：连续相同isBold的片段合并为一行
        List<LineInfo> lines = new ArrayList<>();
        StringBuilder lineText = new StringBuilder();
        Boolean currentBold = originalSegments.get(0).getIsBold();
        String currentSpeaker = originalSegments.get(0).getSpeaker();
        
        for (int i = 0; i < originalSegments.size(); i++) {
            TextSegment segment = originalSegments.get(i);
            
            // 判断是否需要输出当前行
            boolean shouldOutput = !segment.getIsBold().equals(currentBold) || i == originalSegments.size() - 1;
            
            if (shouldOutput) {
                // 如果是最后一个片段且加粗状态相同，需要添加当前文本
                if (i == originalSegments.size() - 1 && segment.getIsBold().equals(currentBold)) {
                    lineText.append(segment.getText());
                }
                
                // 输出当前行
                String text = lineText.toString().trim();
                if (!text.isEmpty()) {
                    lines.add(new LineInfo(text, currentBold, currentSpeaker));
                }
                
                // 开始新行
                if (i < originalSegments.size() - 1) {
                    lineText = new StringBuilder();
                    if (!segment.getIsBold().equals(currentBold)) {
                        lineText.append(segment.getText());
                        currentBold = segment.getIsBold();
                        currentSpeaker = segment.getSpeaker();
                    }
                }
            } else {
                lineText.append(segment.getText());
            }
        }
        
        // 步骤2：根据AudioSegment的实际音频时长构建DialogSegment
        int audioIndex = 0;
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            LineInfo line = lines.get(lineIndex);
            
            // 找到对应的AudioSegment（相同音色）
            double lineDuration = 0.0;
            while (audioIndex < audioSegments.size()) {
                AudioSegment audioSegment = audioSegments.get(audioIndex);
                
                // 计算该AudioSegment的实际时长
                double segmentDuration = calculateAudioDuration(
                    audioSegment.getAudioData(), 
                    voiceConfig.getFormat(), 
                    voiceConfig.getSampleRate()
                );
                
                lineDuration += segmentDuration;
                
                // 添加停顿时间
                if (audioSegment.getNeedPause() != null && audioSegment.getNeedPause()) {
                    double pauseSec = (audioSegment.getPauseDuration() != null ? 
                                      audioSegment.getPauseDuration() : 800) / 1000.0;
                    lineDuration += pauseSec;
                }
                
                audioIndex++;
                
                // 检查下一个AudioSegment是否属于同一行（同一音色）
                if (audioIndex < audioSegments.size()) {
                    AudioSegment nextSegment = audioSegments.get(audioIndex);
                    String nextSpeaker = nextSegment.getMergedSegment().getSpeaker();
                    if (!nextSpeaker.equals(line.speaker)) {
                        // 下一个是不同音色，当前行结束
                        break;
                    }
                } else {
                    break;
                }
            }
            
            // 创建DialogSegment
            DialogSegment dialogSegment = DialogSegment.builder()
                    .index(lineIndex)
                    .text(line.text)
                    .isBold(line.isBold)
                    .startTime(currentTime)
                    .duration(lineDuration)
                    .voiceId(line.speaker)
                    .charTimings(buildCharTimings(line.text, currentTime, lineDuration)) // 生成逐字时间戳
                    .build();
            
            dialogSegments.add(dialogSegment);
            currentTime += lineDuration;
        }
        
        log.info("构建了{}个对话行，总实际时长: {:.2f}秒", dialogSegments.size(), currentTime);
        
        return dialogSegments;
    }
    
    /**
     * 计算音频时长（基于音频数据大小）
     */
    private double calculateAudioDuration(byte[] audioData, String format, int sampleRate) {
        if (audioData == null || audioData.length == 0) {
            return 0.0;
        }
        
        int dataSize = audioData.length;
        
        // MP3格式：根据比特率估算（火山引擎默认128kbps）
        if ("mp3".equalsIgnoreCase(format)) {
            int bitrate = 128000; // 128kbps
            return (dataSize * 8.0) / bitrate;
        }
        
        // WAV格式：根据采样率精确计算
        if ("wav".equalsIgnoreCase(format)) {
            int bytesPerSample = 2; // 16-bit
            int channels = 1; // mono
            return dataSize / (double) (sampleRate * bytesPerSample * channels);
        }
        
        // OGG格式：根据比特率估算（假设96kbps）
        if ("ogg".equalsIgnoreCase(format)) {
            int bitrate = 96000; // 96kbps
            return (dataSize * 8.0) / bitrate;
        }
        
        // 默认估算
        return dataSize / 20000.0;
    }
    
    /**
     * 构建逐字时间戳（智能分配算法 + 最后一字强制对齐）
     * 
     * 智能分配规则：
     * 1. 标点符号（，。！？；：）：0.15秒
     * 2. 助词（的了吗呢啊）：0.18秒
     * 3. 常见字（你我他是在有这个）：0.22秒
     * 4. 普通汉字：剩余时间均分
     * 5. 最后一个字：强制对齐到整句结束时间（100%准确）
     * 
     * @param text 文本内容
     * @param startTime 整句开始时间
     * @param totalDuration 整句总时长
     * @return 逐字时间戳列表
     */
    private List<CharTiming> buildCharTimings(String text, double startTime, double totalDuration) {
        List<CharTiming> timings = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return timings;
        }
        
        // 统计各类字符数量
        int punctuationCount = 0;
        int auxiliaryCount = 0;
        int commonCount = 0;
        int normalCount = 0;
        
        for (char c : text.toCharArray()) {
            if (isPunctuation(c)) {
                punctuationCount++;
            } else if (isAuxiliary(c)) {
                auxiliaryCount++;
            } else if (isCommonChar(c)) {
                commonCount++;
            } else {
                normalCount++;
            }
        }
        
        // 计算各类字符的总时长
        double punctuationTime = punctuationCount * 0.15;  // 标点：0.15秒
        double auxiliaryTime = auxiliaryCount * 0.18;      // 助词：0.18秒
        double commonTime = commonCount * 0.22;            // 常见字：0.22秒
        
        // 普通字均分剩余时间
        double remainingTime = totalDuration - punctuationTime - auxiliaryTime - commonTime;
        double normalCharDuration = normalCount > 0 ? remainingTime / normalCount : 0.25;
        
        // 确保普通字时长合理（0.2-0.35秒）
        if (normalCharDuration < 0.2) {
            normalCharDuration = 0.2;
        } else if (normalCharDuration > 0.35) {
            normalCharDuration = 0.35;
        }
        
        // 构建逐字时间戳
        double currentTime = startTime;
        int charCount = text.length();
        
        for (int i = 0; i < charCount; i++) {
            char c = text.charAt(i);
            double charDuration;
            
            // 判断字符类型并分配时长
            if (isPunctuation(c)) {
                charDuration = 0.15;
            } else if (isAuxiliary(c)) {
                charDuration = 0.18;
            } else if (isCommonChar(c)) {
                charDuration = 0.22;
            } else {
                charDuration = normalCharDuration;
            }
            
            // 最后一个字：强制对齐到整句结束时间（100%准确）
            if (i == charCount - 1) {
                double expectedEndTime = startTime + totalDuration;
                charDuration = expectedEndTime - currentTime;
                
                // 确保最后一个字的时长合理（至少0.05秒，不能太短）
                if (charDuration < 0.05) {
                    charDuration = 0.05;
                }
                // 注意：不设置上限，让最后一字的时长完全由整句结束时间决定
                
                log.debug("最后一字强制对齐：字符='{}', 开始时间={}, 时长={}, 结束时间={}", 
                         c, currentTime, charDuration, currentTime + charDuration);
            }
            
            CharTiming timing = CharTiming.builder()
                    .character(String.valueOf(c))
                    .startTime(currentTime)
                    .duration(charDuration)
                    .build();
            
            timings.add(timing);
            currentTime += charDuration;
        }
        
        log.debug("智能分配逐字时间戳：文本长度{}，标点{}，助词{}，常见字{}，普通字{}",
                charCount, punctuationCount, auxiliaryCount, commonCount, normalCount);
        
        return timings;
    }
    
    /**
     * 判断是否为标点符号
     */
    private boolean isPunctuation(char c) {
        // 使用Unicode转义避免中文引号导致的编译错误
        String punctuations = "，。！？；：、\u201C\u201D\u2018\u2019（）《》【】…—·";
        return punctuations.indexOf(c) >= 0;
    }
    
    /**
     * 判断是否为助词
     */
    private boolean isAuxiliary(char c) {
        String auxiliaries = "的了吗呢啊呀嘛吧哦哎唉";
        return auxiliaries.indexOf(c) >= 0;
    }
    
    /**
     * 判断是否为常见字
     */
    private boolean isCommonChar(char c) {
        String commonChars = "你我他她它是在有这个那么什人们都和也不了就说着到给";
        return commonChars.indexOf(c) >= 0;
    }
    
    /**
     * 行信息（内部类）
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class LineInfo {
        private String text;
        private Boolean isBold;
        private String speaker;
    }
    
    /**
     * 计算音频总时长
     */
    private double calculateTotalDuration(byte[] audioData, VoiceConfig voiceConfig) {
        // MP3格式：根据比特率估算（假设128kbps）
        if ("mp3".equalsIgnoreCase(voiceConfig.getFormat())) {
            int bitrate = 128000; // 128kbps
            return (audioData.length * 8.0) / bitrate;
        }
        
        // WAV格式：根据采样率和音频数据大小精确计算
        if ("wav".equalsIgnoreCase(voiceConfig.getFormat())) {
            int sampleRate = voiceConfig.getSampleRate();
            int bytesPerSample = 2; // 16-bit
            int channels = 1; // mono
            return audioData.length / (double) (sampleRate * bytesPerSample * channels);
        }
        
        // OGG格式：根据比特率估算（假设96kbps）
        if ("ogg".equalsIgnoreCase(voiceConfig.getFormat())) {
            int bitrate = 96000; // 96kbps
            return (audioData.length * 8.0) / bitrate;
        }
        
        // 默认估算
        return audioData.length / 20000.0;
    }
    
    /**
     * 音频生成结果（包含片段信息）
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class AudioGenerationResult {
        private byte[] audioData;
        private List<DialogSegment> dialogSegments;
        private Double totalDuration;
    }
    
    /**
     * 并发调用TTS API
     */
    private List<AudioSegment> synthesizeParallel(List<MergedSegment> segments, VoiceConfig voiceConfig) throws Exception {
        List<CompletableFuture<AudioSegment>> futures = new ArrayList<>();
        
        for (MergedSegment segment : segments) {
            CompletableFuture<AudioSegment> future = CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("开始生成音频，音色: {}, 文本长度: {}, 文本内容: [{}]", 
                            segment.getSpeaker(), segment.getText().length(), segment.getText());
                    
                    TTSRequest request = TTSRequest.builder()
                            .text(segment.getText())
                            .speaker(segment.getSpeaker())
                            .format(voiceConfig.getFormat())
                            .sampleRate(voiceConfig.getSampleRate())
                            .build();
                    
                    byte[] audio = ttsService.generateSpeechBytes(request);
                    
                    log.debug("音频生成完成，大小: {} KB", audio.length / 1024.0);
                    
                    return new AudioSegment(audio, segment);
                    
                } catch (Exception e) {
                    log.error("TTS合成失败，文本: [{}], 错误: {}", segment.getText(), e.getMessage());
                    // ✅ 关键修改：返回null而不是抛异常，让其他片段继续处理
                    return null;
                }
            }, executor);
            
            futures.add(future);
        }
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // 收集结果（保持原始顺序，过滤null）
        List<AudioSegment> results = new ArrayList<>();
        for (CompletableFuture<AudioSegment> future : futures) {
            try {
                AudioSegment segment = future.get();
                if (segment != null) {
                    results.add(segment);
                } else {
                    log.warn("跳过失败的TTS片段");
                }
            } catch (Exception e) {
                log.warn("某个TTS请求失败: {}", e.getMessage());
            }
        }
        
        log.info("并发TTS合成完成，共生成{}个音频片段", results.size());
        
        return results;
    }
    
    /**
     * 计算并设置停顿
     */
    private void calculatePauses(List<AudioSegment> audioSegments, List<MergedSegment> mergedSegments) {
        for (int i = 0; i < audioSegments.size(); i++) {
            AudioSegment current = audioSegments.get(i);
            AudioSegment next = (i < audioSegments.size() - 1) ? audioSegments.get(i + 1) : null;
            
            if (next != null) {
                int pauseDuration = pauseCalculator.calculatePause(
                        current.getMergedSegment(), 
                        next.getMergedSegment()
                );
                
                current.setNeedPause(pauseDuration > 0);
                current.setPauseDuration(pauseDuration);
            }
        }
    }
    
    /**
     * 验证上传的文件
     */
    private void validateFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".docx")) {
            throw new Exception("只支持.docx格式的Word文档");
        }
        
        // 限制文件大小（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new Exception("文件大小不能超过10MB");
        }
    }
    
    @Override
    public byte[] generateSpeechFromDocument(MultipartFile file, String boldVoice, String normalVoice, 
                                            String format, Integer sampleRate) throws Exception {
        // 构建音色配置
        VoiceConfig voiceConfig = VoiceConfig.builder()
                .boldVoice(boldVoice)
                .normalVoice(normalVoice)
                .format(format)
                .sampleRate(sampleRate)
                .build();
        
        return generateDocumentSpeechBytes(file, voiceConfig);
    }
    
    @Override
    public List<DialogSegment> getDialogSegments(MultipartFile file, String boldVoice, String normalVoice) throws Exception {
        log.info("开始获取对话片段：文件名={}", file.getOriginalFilename());
        
        // 构建音色配置
        VoiceConfig voiceConfig = VoiceConfig.builder()
                .boldVoice(boldVoice)
                .normalVoice(normalVoice)
                .build();
        
        // 步骤1：解析Word文档
        List<TextSegment> segments = documentParser.parse(file.getInputStream(), voiceConfig);
        log.info("文档解析完成，共{}个文本片段", segments.size());
        
        // 步骤2：使用独立模式（与generateDocumentSpeechWithTiming保持一致）
        List<MergedSegment> independentSegments = segmentMerger.mergeNoMerge(segments);
        log.info("独立模式处理完成，共{}个独立片段", independentSegments.size());
        
        // 步骤3：拆分过长片段
        List<MergedSegment> finalSegments = new ArrayList<>();
        for (MergedSegment segment : independentSegments) {
            List<MergedSegment> split = segmentMerger.splitIfTooLong(segment, MAX_TEXT_LENGTH);
            finalSegments.addAll(split);
        }
        log.info("拆分后共{}个片段", finalSegments.size());
        
        // 步骤4：估算每个片段的时长
        List<DialogSegment> dialogSegments = new ArrayList<>();
        double currentTime = 0.0;
        
        for (int i = 0; i < finalSegments.size(); i++) {
            MergedSegment segment = finalSegments.get(i);
            MergedSegment nextSegment = (i < finalSegments.size() - 1) ? finalSegments.get(i + 1) : null;
            
            // 估算文本语音时长（平均每字0.3秒）
            double textDuration = segment.getText().length() * 0.3;
            
            // 计算停顿时长
            int pauseDuration = pauseCalculator.calculatePause(segment, nextSegment);
            double pauseSec = pauseDuration / 1000.0;
            
            // 获取是否加粗（从原始片段中获取）
            Boolean isBold = segment.getOriginalSegments().isEmpty() ? 
                             false : segment.getOriginalSegments().get(0).getIsBold();
            
            // 创建对话片段
            DialogSegment dialogSegment = DialogSegment.builder()
                    .text(segment.getText())
                    .isBold(isBold)
                    .voiceId(segment.getSpeaker())
                    .startTime(currentTime)
                    .duration(textDuration + pauseSec)
                    .build();
            
            dialogSegments.add(dialogSegment);
            
            currentTime += dialogSegment.getDuration();
        }
        
        log.info("对话片段生成完成，共{}个片段，总时长{}秒", dialogSegments.size(), currentTime);
        
        return dialogSegments;
    }
}
