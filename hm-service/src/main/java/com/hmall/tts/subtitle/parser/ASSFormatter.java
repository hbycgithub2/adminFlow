package com.hmall.tts.subtitle.parser;

import com.hmall.tts.subtitle.dto.SubtitleSegment;
import com.hmall.tts.subtitle.dto.SubtitleStyle;
import com.hmall.tts.video.animation.AnimationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ASS字幕文件格式化器（Pretty Printer）
 * 
 * <p>将SubtitleSegment列表格式化为标准ASS文件内容</p>
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Component
@Slf4j
public class ASSFormatter {
    
    /**
     * 格式化字幕片段列表为ASS文件内容
     * 
     * @param subtitles 字幕片段列表
     * @param videoWidth 视频宽度（默认1920）
     * @param videoHeight 视频高度（默认1080）
     * @return ASS文件内容
     */
    public String format(List<SubtitleSegment> subtitles, int videoWidth, int videoHeight) {
        log.info("开始格式化ASS字幕，共{}条", subtitles.size());
        
        StringBuilder sb = new StringBuilder();
        
        // 1. 生成头部
        sb.append(generateHeader(videoWidth, videoHeight));
        sb.append("\n");
        
        // 2. 生成样式定义（使用第一条字幕的样式作为默认样式）
        SubtitleStyle defaultStyle = subtitles.isEmpty() ? 
                SubtitleStyle.builder().build() : 
                subtitles.get(0).getStyle();
        sb.append(generateStyles(defaultStyle));
        sb.append("\n");
        
        // 3. 生成Events段
        sb.append("[Events]\n");
        sb.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
        
        // 4. 生成每条Dialogue
        for (SubtitleSegment segment : subtitles) {
            sb.append(generateDialogueLine(segment, "Default"));
            sb.append("\n");
        }
        
        log.info("ASS字幕格式化完成");
        return sb.toString();
    }
    
    /**
     * 生成ASS文件头部
     * 
     * @param videoWidth 视频宽度
     * @param videoHeight 视频高度
     * @return 头部内容
     */
    private String generateHeader(int videoWidth, int videoHeight) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("[Script Info]\n");
        sb.append("Title: 字幕编辑器生成\n");
        sb.append("ScriptType: v4.00+\n");
        sb.append("WrapStyle: 0\n");
        sb.append("PlayResX: ").append(videoWidth).append("\n");
        sb.append("PlayResY: ").append(videoHeight).append("\n");
        sb.append("ScaledBorderAndShadow: yes\n");
        
        return sb.toString();
    }
    
    /**
     * 生成样式定义
     * 
     * @param style 字幕样式
     * @return 样式内容
     */
    private String generateStyles(SubtitleStyle style) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("[V4+ Styles]\n");
        sb.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
        
        // 生成Default样式行
        String fontColorASS = convertColorToASS(style.getFontColor());
        String borderColorASS = convertColorToASS(style.getBorderColor());
        
        sb.append(String.format("Style: Default,%s,%d,%s,&H000088EF,%s,&H80000000,0,0,0,0,100,100,0,0,1,%d,2,%d,10,10,10,1\n",
                style.getFontName(),
                style.getFontSize(),
                fontColorASS,
                borderColorASS,
                style.getBorderWidth(),
                style.getPosition()
        ));
        
        return sb.toString();
    }
    
    /**
     * 生成Dialogue行
     * 
     * @param segment 字幕片段
     * @param styleName 样式名称
     * @return Dialogue行
     */
    private String generateDialogueLine(SubtitleSegment segment, String styleName) {
        String startTime = formatTime(segment.getStartTime());
        String endTime = formatTime(segment.getStartTime() + segment.getDuration());
        
        // 获取动画标签
        String animationTag = getAnimationTag(segment.getStyle().getAnimationType());
        
        // 转义文本
        String text = escapeText(segment.getText());
        
        // 添加动画标签（必须用{}包裹）
        String fullText = animationTag.isEmpty() ? text : "{" + animationTag + "}" + text;
        
        // 格式：Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        return String.format("Dialogue: 0,%s,%s,%s,,0,0,0,,%s",
                startTime, endTime, styleName, fullText);
    }
    
    /**
     * 格式化时间为ASS格式
     * 
     * @param seconds 秒数
     * @return ASS时间字符串（0:00:01.23）
     */
    private String formatTime(Double seconds) {
        if (seconds == null) {
            seconds = 0.0;
        }
        
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        double secs = seconds % 60;
        
        return String.format("%d:%02d:%05.2f", hours, minutes, secs);
    }
    
    /**
     * 获取动画标签
     * 
     * @param animationType 动画类型代码
     * @return ASS动画标签
     */
    private String getAnimationTag(String animationType) {
        if (animationType == null || animationType.isEmpty()) {
            return "";
        }
        
        AnimationType type = AnimationType.fromCode(animationType);
        return type.getAssTag();
    }
    
    /**
     * 转义ASS特殊字符
     * 
     * @param text 原始文本
     * @return 转义后的文本
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
    
    /**
     * 将HEX颜色转换为ASS格式
     * 
     * <p>
     * HEX格式：#RRGGBB<br>
     * ASS格式：&HBBGGRR&
     * </p>
     * 
     * @param hexColor HEX颜色（#FFFFFF）
     * @return ASS颜色（&H00FFFFFF&）
     */
    private String convertColorToASS(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#")) {
            return "&H00FFFFFF&"; // 默认白色
        }
        
        String hex = hexColor.substring(1); // 移除 #
        if (hex.length() != 6) {
            return "&H00FFFFFF&";
        }
        
        // HEX: #RRGGBB → ASS: &H00BBGGRR&
        String rr = hex.substring(0, 2);
        String gg = hex.substring(2, 4);
        String bb = hex.substring(4, 6);
        
        return "&H00" + bb + gg + rr + "&";
    }
}
