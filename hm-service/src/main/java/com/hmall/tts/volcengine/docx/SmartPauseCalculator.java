package com.hmall.tts.volcengine.docx;

import com.hmall.tts.volcengine.dto.MergedSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 智能停顿计算器
 * 根据文本内容和音色切换，计算自然的停顿时长
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@Slf4j
@Component
public class SmartPauseCalculator {
    
    /**
     * 基础停顿时长（毫秒）
     */
    private static final int BASE_PAUSE = 300;
    
    /**
     * 音色切换停顿时长（毫秒）
     */
    private static final int SPEAKER_CHANGE_PAUSE = 800;
    
    /**
     * 问句额外停顿（毫秒）
     */
    private static final int QUESTION_PAUSE = 200;
    
    /**
     * 感叹句额外停顿（毫秒）
     */
    private static final int EXCLAMATION_PAUSE = 100;
    
    /**
     * 计算两个片段之间的停顿时长
     * 
     * @param current 当前片段
     * @param next 下一个片段
     * @return 停顿时长（毫秒）
     */
    public int calculatePause(MergedSegment current, MergedSegment next) {
        // 如果是最后一个片段，不添加停顿
        if (next == null) {
            log.debug("最后一个片段，不添加停顿");
            return 0;
        }
        
        int pause = BASE_PAUSE;
        
        String currentText = current.getText().trim();
        String currentSpeaker = current.getSpeaker();
        String nextSpeaker = next.getSpeaker();
        
        // 规则1：音色切换增加停顿（模拟对话中的思考时间）
        if (!currentSpeaker.equals(nextSpeaker)) {
            pause = SPEAKER_CHANGE_PAUSE;
            log.debug("音色切换，停顿时长: {}ms", pause);
        }
        
        // 规则2：问句增加停顿（等待对方回答）
        if (currentText.endsWith("？") || currentText.endsWith("?")) {
            pause += QUESTION_PAUSE;
            log.debug("问句结尾，增加停顿: {}ms，总停顿: {}ms", QUESTION_PAUSE, pause);
        }
        
        // 规则3：感叹句增加停顿（强调情绪）
        if (currentText.endsWith("！") || currentText.endsWith("!")) {
            pause += EXCLAMATION_PAUSE;
            log.debug("感叹句结尾，增加停顿: {}ms，总停顿: {}ms", EXCLAMATION_PAUSE, pause);
        }
        
        // 规则4：句号/逗号的自然停顿
        if (currentText.endsWith("。") || currentText.endsWith(".")) {
            // 句号已经有基础停顿，不额外增加
            log.debug("句号结尾，使用基础停顿: {}ms", pause);
        } else if (currentText.endsWith("，") || currentText.endsWith(",")) {
            // 逗号停顿更短
            pause = Math.min(pause, 200);
            log.debug("逗号结尾，缩短停顿: {}ms", pause);
        }
        
        return pause;
    }
    
    /**
     * 生成静音音频数据（暂时跳过，避免格式不匹配问题）
     * 
     * @param durationMs 静音时长（毫秒）
     * @param sampleRate 采样率
     * @return 静音音频字节数组
     */
    public byte[] generateSilence(int durationMs, int sampleRate) {
        log.debug("停顿时长: {}ms（暂时跳过静音生成，让音频自然衔接）", durationMs);
        
        // 暂时返回空数组，不添加静音
        // 原因：TTS返回MP3格式，直接拼接PCM静音会导致音频损坏
        // 解决方案：
        // 1. 使用预先生成的MP3静音片段
        // 2. 使用FFmpeg转换
        // 3. 让前端播放器控制停顿（当前采用）
        
        return new byte[0];
    }
}
