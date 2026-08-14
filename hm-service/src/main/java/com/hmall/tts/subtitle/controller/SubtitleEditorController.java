package com.hmall.tts.subtitle.controller;

import com.hmall.tts.subtitle.dto.SubtitleEditData;
import com.hmall.tts.subtitle.dto.SubtitleUpdateRequest;
import com.hmall.tts.subtitle.dto.SubtitleUpdateResponse;
import com.hmall.tts.subtitle.service.SubtitleEditorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 字幕编辑器REST API控制器
 * 
 * @author Kiro
 * @since 2026-08-14
 */
@RestController
@RequestMapping("/api/subtitle-editor")
@Slf4j
public class SubtitleEditorController {
    
    @Autowired
    private SubtitleEditorService subtitleEditorService;
    
    /**
     * 加载字幕数据
     * 
     * <p>GET /api/subtitle-editor/load?taskId=xxx</p>
     * 
     * @param taskId 任务ID
     * @return 字幕编辑数据
     */
    @GetMapping("/load")
    public SubtitleEditData loadSubtitles(@RequestParam String taskId) {
        log.info("接收加载字幕请求: taskId={}", taskId);
        
        try {
            SubtitleEditData data = subtitleEditorService.loadSubtitles(taskId);
            log.info("加载字幕成功: taskId={}, subtitles={}", taskId, data.getSubtitles().size());
            return data;
        } catch (Exception e) {
            log.error("加载字幕失败: taskId={}", taskId, e);
            throw new RuntimeException("加载字幕失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新字幕数据
     * 
     * <p>POST /api/subtitle-editor/update</p>
     * 
     * @param request 更新请求
     * @return 更新响应
     */
    @PostMapping("/update")
    public SubtitleUpdateResponse updateSubtitles(@RequestBody SubtitleUpdateRequest request) {
        log.info("接收更新字幕请求: taskId={}, subtitles={}, regenerateVideo={}", 
                request.getTaskId(), request.getSubtitles().size(), request.getRegenerateVideo());
        
        try {
            SubtitleUpdateResponse response = subtitleEditorService.updateSubtitles(request);
            log.info("更新字幕响应: taskId={}, success={}, message={}", 
                    request.getTaskId(), response.getSuccess(), response.getMessage());
            return response;
        } catch (Exception e) {
            log.error("更新字幕失败: taskId={}", request.getTaskId(), e);
            return SubtitleUpdateResponse.failure("更新字幕失败: " + e.getMessage());
        }
    }
    
    /**
     * 重新生成视频
     * 
     * <p>POST /api/subtitle-editor/regenerate?taskId=xxx</p>
     * 
     * @param taskId 任务ID
     * @return 重新生成响应
     */
    @PostMapping("/regenerate")
    public SubtitleUpdateResponse regenerateVideo(@RequestParam String taskId) {
        log.info("接收重新生成视频请求: taskId={}", taskId);
        
        try {
            SubtitleUpdateResponse response = subtitleEditorService.regenerateVideo(taskId);
            log.info("重新生成视频响应: taskId={}, success={}, message={}", 
                    taskId, response.getSuccess(), response.getMessage());
            return response;
        } catch (Exception e) {
            log.error("重新生成视频失败: taskId={}", taskId, e);
            return SubtitleUpdateResponse.failure("重新生成视频失败: " + e.getMessage());
        }
    }
}
