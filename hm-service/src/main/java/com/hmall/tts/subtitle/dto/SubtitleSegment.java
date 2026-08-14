package com.hmall.tts.subtitle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 字幕片段数据模型
 * 
 * <p>表示一句字幕，包含文本、时间和样式信息</p>
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleSegment implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 字幕序号
     * <p>从1开始的顺序编号</p>
     */
    private Integer id;
    
    /**
     * 字幕文本内容
     * <p>最大长度: 100字符</p>
     */
    private String text;
    
    /**
     * 开始时间（秒）
     * <p>精度: 0.01秒</p>
     */
    private Double startTime;
    
    /**
     * 持续时间（秒）
     * <p>精度: 0.01秒</p>
     */
    private Double duration;
    
    /**
     * 字幕样式
     * <p>如果为null，使用默认样式</p>
     */
    private SubtitleStyle style;
    
    /**
     * 获取结束时间
     * @return 结束时间（秒）
     */
    public Double getEndTime() {
        if (startTime == null || duration == null) {
            return 0.0;
        }
        return startTime + duration;
    }
}
