package com.hmall.tts.subtitle.service;

import com.hmall.tts.subtitle.dto.SubtitleEditData;
import com.hmall.tts.subtitle.dto.SubtitleUpdateRequest;
import com.hmall.tts.subtitle.dto.SubtitleUpdateResponse;

/**
 * 字幕编辑服务接口
 * 
 * @author Kiro
 * @since 2026-08-14
 */
public interface SubtitleEditorService {
    
    /**
     * 加载字幕数据
     * 
     * @param taskId 任务ID
     * @return 字幕编辑数据
     */
    SubtitleEditData loadSubtitles(String taskId);
    
    /**
     * 更新字幕数据
     * 
     * @param request 更新请求
     * @return 更新响应
     */
    SubtitleUpdateResponse updateSubtitles(SubtitleUpdateRequest request);
    
    /**
     * 重新生成视频
     * 
     * @param taskId 任务ID
     * @return 更新响应
     */
    SubtitleUpdateResponse regenerateVideo(String taskId);
}
