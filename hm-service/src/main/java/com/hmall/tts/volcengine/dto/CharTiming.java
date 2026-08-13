package com.hmall.tts.volcengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个字符的时间信息
 * 用于实现逐字高亮效果（类似KTV卡拉OK）
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharTiming {
    
    /**
     * 单个字符
     */
    private String character;
    
    /**
     * 字符开始时间（秒）
     */
    private Double startTime;
    
    /**
     * 字符持续时间（秒）
     */
    private Double duration;
}
