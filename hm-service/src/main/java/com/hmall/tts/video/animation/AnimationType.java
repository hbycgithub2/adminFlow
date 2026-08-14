package com.hmall.tts.video.animation;

import lombok.Getter;

/**
 * 字幕动画类型
 */
@Getter
public enum AnimationType {
    
    /**
     * 无动画
     */
    NONE("none", "无动画", ""),
    
    /**
     * 渐入渐出
     */
    FADE("fade", "渐入渐出", "\\fad(300,300)"),
    
    /**
     * 从下飞入
     */
    SLIDE_UP("slide_up", "从下飞入", "\\move(960,1280,960,920,0,500)"),
    
    /**
     * 从上飞入
     */
    SLIDE_DOWN("slide_down", "从上飞入", "\\move(960,0,960,920,0,500)"),
    
    /**
     * 从左飞入
     */
    SLIDE_LEFT("slide_left", "从左飞入", "\\move(0,920,960,920,0,500)"),
    
    /**
     * 从右飞入
     */
    SLIDE_RIGHT("slide_right", "从右飞入", "\\move(1920,920,960,920,0,500)"),
    
    /**
     * 缩放进入
     */
    ZOOM_IN("zoom_in", "缩放进入", "\\t(0,500,\\fscx120\\fscy120)\\t(500,1000,\\fscx100\\fscy100)"),
    
    /**
     * 弹跳效果
     */
    BOUNCE("bounce", "弹跳效果", "\\move(960,920,960,820,0,300)\\t(300,600,\\move(960,820,960,920))");
    
    /**
     * 动画代码
     */
    private final String code;
    
    /**
     * 动画描述
     */
    private final String description;
    
    /**
     * ASS动画标签
     */
    private final String assTag;
    
    AnimationType(String code, String description, String assTag) {
        this.code = code;
        this.description = description;
        this.assTag = assTag;
    }
    
    /**
     * 根据代码获取动画类型
     */
    public static AnimationType fromCode(String code) {
        for (AnimationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return FADE; // 默认渐入渐出
    }
}
