package com.hmall.tts.video.subtitle;

import com.hmall.tts.video.dto.SubtitleConfig;
import com.hmall.tts.video.dto.SubtitleSegment;
import com.hmall.tts.video.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ASS字幕生成器
 */
@Slf4j
@Component
public class ASSSubtitleGenerator {
    
    /**
     * 生成ASS字幕内容
     * 
     * @param segments 字幕片段列表
     * @param config 字幕配置
     * @param videoWidth 视频宽度
     * @param videoHeight 视频高度
     * @return ASS字幕内容
     */
    public String generateASS(List<SubtitleSegment> segments, SubtitleConfig config, int videoWidth, int videoHeight) {
        StringBuilder ass = new StringBuilder();
        
        // 1. 脚本信息
        ass.append("[Script Info]\n");
        ass.append("Title: 对话字幕视频\n");
        ass.append("ScriptType: v4.00+\n");
        ass.append("WrapStyle: 0\n");
        ass.append("PlayResX: ").append(videoWidth).append("\n");
        ass.append("PlayResY: ").append(videoHeight).append("\n");
        ass.append("ScaledBorderAndShadow: yes\n");
        ass.append("\n");
        
        // 2. 样式定义
        ass.append("[V4+ Styles]\n");
        ass.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
        
        String styleLine = generateStyleLine(config);
        ass.append(styleLine).append("\n");
        ass.append("\n");
        
        // 3. 字幕事件
        ass.append("[Events]\n");
        ass.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
        
        for (SubtitleSegment segment : segments) {
            String dialogue = generateDialogueLine(segment, config);
            ass.append(dialogue).append("\n");
        }
        
        log.info("ASS字幕生成完成，共{}个片段", segments.size());
        
        return ass.toString();
    }
    
    /**
     * 生成样式行
     */
    private String generateStyleLine(SubtitleConfig config) {
        // 格式：Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, ...
        return String.format("Style: Default,%s,%d,%s,&H000088EF,%s,&H80000000,%d,%d,0,0,100,100,0,0,1,%d,%d,%d,10,10,10,1",
                config.getFontName(),
                config.getFontSize(),
                config.getFontColorASS(),
                config.getBorderColorASS(),
                config.getBold() ? -1 : 0,
                config.getItalic() ? -1 : 0,
                config.getBorderWidth(),
                config.getShadowDistance(),
                config.getPosition()
        );
    }
    
    /**
     * 生成对话行
     */
    private String generateDialogueLine(SubtitleSegment segment, SubtitleConfig config) {
        String startTime = TimeUtil.formatTimeForASS(segment.getStartTime());
        String endTime = TimeUtil.formatTimeForASS(segment.getEndTime());
        
        // 获取动画标签（必须用{}包裹）
        String animationTag = config.getAnimationType().getAssTag();
        if (animationTag != null && !animationTag.isEmpty()) {
            animationTag = "{" + animationTag + "}";
        } else {
            animationTag = "";
        }
        
        // 转义特殊字符
        String text = escapeText(segment.getText());
        
        // 格式：Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        return String.format("Dialogue: 0,%s,%s,Default,,0,0,0,,%s%s",
                startTime,
                endTime,
                animationTag,
                text
        );
    }
    
    /**
     * 转义特殊字符
     */
    private String escapeText(String text) {
        if (text == null) {
            return "";
        }
        
        // ASS格式中需要转义的字符
        return text.replace("\\", "\\\\")
                   .replace("{", "\\{")
                   .replace("}", "\\}")
                   .replace("\n", "\\N");
    }
}
