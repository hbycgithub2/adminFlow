package com.hmall.tts.subtitle.parser;

import com.hmall.tts.subtitle.dto.SubtitleSegment;
import com.hmall.tts.subtitle.dto.SubtitleStyle;
import com.hmall.tts.video.animation.AnimationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ASS字幕文件解析器
 * 
 * <p>从ASS文件中读取字幕数据，转换为SubtitleSegment列表</p>
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Component
@Slf4j
public class ASSParser {
    
    /**
     * Dialogue行格式: Dialogue: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text
     */
    private static final Pattern DIALOGUE_PATTERN = Pattern.compile(
            "^Dialogue:\\s*(\\d+),(\\d+:\\d+:\\d+\\.\\d+),(\\d+:\\d+:\\d+\\.\\d+),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),(.*)$"
    );
    
    /**
     * ASS动画标签格式: {\\tag...}
     */
    private static final Pattern ANIMATION_TAG_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    
    /**
     * 解析ASS文件
     * 
     * @param assFilePath ASS文件路径
     * @return 字幕片段列表
     * @throws IOException 文件读取异常
     */
    public List<SubtitleSegment> parse(String assFilePath) throws IOException {
        log.info("开始解析ASS文件: {}", assFilePath);
        
        List<SubtitleSegment> segments = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(assFilePath));
        
        int id = 1;
        boolean inEventsSection = false;
        
        for (String line : lines) {
            line = line.trim();
            
            // 检测[Events]段
            if (line.equals("[Events]")) {
                inEventsSection = true;
                log.debug("进入[Events]段");
                continue;
            }
            
            // 检测下一段（退出Events）
            if (inEventsSection && line.startsWith("[") && !line.equals("[Events]")) {
                inEventsSection = false;
                log.debug("退出[Events]段");
                break;
            }
            
            // 解析Dialogue行
            if (inEventsSection && line.startsWith("Dialogue:")) {
                try {
                    SubtitleSegment segment = parseDialogueLine(line);
                    segment.setId(id++);
                    segments.add(segment);
                    log.debug("解析字幕: id={}, text={}, start={}, duration={}", 
                            segment.getId(), segment.getText(), segment.getStartTime(), segment.getDuration());
                } catch (Exception e) {
                    log.warn("解析Dialogue行失败: {}, 错误: {}", line, e.getMessage());
                }
            }
        }
        
        log.info("ASS文件解析完成，共{}条字幕", segments.size());
        return segments;
    }
    
    /**
     * 解析Dialogue行
     * 
     * <p>
     * Dialogue格式示例:<br>
     * Dialogue: 0,0:00:00.00,0:00:04.50,Default,,0,0,0,,{\fad(300,300)}你好，我是云舟
     * </p>
     * 
     * @param line Dialogue行文本
     * @return 字幕片段
     */
    private SubtitleSegment parseDialogueLine(String line) {
        Matcher matcher = DIALOGUE_PATTERN.matcher(line);
        
        if (!matcher.matches()) {
            throw new IllegalArgumentException("无效的Dialogue格式: " + line);
        }
        
        // 提取字段
        String startTimeStr = matcher.group(2);  // 0:00:00.00
        String endTimeStr = matcher.group(3);    // 0:00:04.50
        String text = matcher.group(10);         // {\fad(300,300)}你好，我是云舟
        
        // 解析时间
        Double startTime = parseTime(startTimeStr);
        Double endTime = parseTime(endTimeStr);
        Double duration = endTime - startTime;
        
        // 提取动画类型
        String animationType = extractAnimationType(text);
        
        // 移除动画标签，保留纯文本
        text = text.replaceAll("\\{[^}]+\\}", "").trim();
        
        // 构建样式（默认样式）
        SubtitleStyle style = SubtitleStyle.builder()
                .fontName("Microsoft YaHei")
                .fontSize(64)
                .fontColor("#FFFFFF")
                .borderColor("#000000")
                .borderWidth(3)
                .position(2)
                .animationType(animationType)
                .build();
        
        return SubtitleSegment.builder()
                .text(text)
                .startTime(startTime)
                .duration(duration)
                .style(style)
                .build();
    }
    
    /**
     * 解析ASS时间字符串
     * 
     * @param timeStr ASS时间格式（0:00:01.23）
     * @return 秒数
     */
    private Double parseTime(String timeStr) {
        // 格式: 0:00:01.23
        String[] parts = timeStr.split(":");
        
        if (parts.length != 3) {
            throw new IllegalArgumentException("无效的时间格式: " + timeStr);
        }
        
        double hours = Double.parseDouble(parts[0]);
        double minutes = Double.parseDouble(parts[1]);
        double seconds = Double.parseDouble(parts[2]);
        
        return hours * 3600 + minutes * 60 + seconds;
    }
    
    /**
     * 提取动画类型
     * 
     * <p>从文本中提取ASS动画标签，识别对应的动画类型</p>
     * 
     * @param text 包含动画标签的文本
     * @return 动画类型代码
     */
    private String extractAnimationType(String text) {
        Matcher matcher = ANIMATION_TAG_PATTERN.matcher(text);
        
        if (!matcher.find()) {
            return "none";  // 无动画标签
        }
        
        String tag = matcher.group(1);  // 提取{}内的内容
        
        // 匹配已知的动画类型
        for (AnimationType type : AnimationType.values()) {
            String assTag = type.getAssTag();
            if (!assTag.isEmpty() && tag.contains(assTag)) {
                return type.getCode();
            }
        }
        
        // 特殊判断
        if (tag.contains("\\fad")) {
            return "fade";
        } else if (tag.contains("\\move") && tag.contains("1280")) {
            return "slide_up";
        } else if (tag.contains("\\move") && tag.contains(",0,")) {
            return "slide_down";
        } else if (tag.contains("\\move") && tag.contains("0,920")) {
            return "slide_left";
        } else if (tag.contains("\\move") && tag.contains("1920,920")) {
            return "slide_right";
        } else if (tag.contains("\\fscx") && tag.contains("\\fscy")) {
            return "zoom_in";
        } else if (tag.contains("820")) {
            return "bounce";
        }
        
        return "fade";  // 默认渐入渐出
    }
}
