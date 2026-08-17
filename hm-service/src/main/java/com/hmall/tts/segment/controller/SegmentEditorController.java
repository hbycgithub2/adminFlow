package com.hmall.tts.segment.controller;

import com.hmall.tts.segment.dto.*;
import com.hmall.tts.segment.service.SegmentEditorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 段落编辑控制器
 * 
 * 提供局部编辑视频段落的REST API
 * 
 * @author Kiro
 * @since 2026-08-17
 */
@Slf4j
@RestController
@RequestMapping("/api/segment-editor")
@Api(tags = "段落编辑")
public class SegmentEditorController {
    
    @Autowired
    private SegmentEditorService segmentEditorService;
    
    /**
     * 编辑某个段落
     * 
     * @param request 编辑请求
     * @return 编辑响应
     */
    @PostMapping("/edit")
    @ApiOperation("编辑某个段落的文字和配音")
    public ResponseEntity<SegmentEditResponse> editSegment(@RequestBody SegmentEditRequest request) {
        log.info("[API] 编辑段落请求：taskId={}, segmentIndex={}", 
                request.getTaskId(), request.getSegmentIndex());
        
        try {
            SegmentEditResponse response = segmentEditorService.editSegment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[API] 编辑段落失败", e);
            return ResponseEntity.ok(SegmentEditResponse.failure("编辑失败: " + e.getMessage()));
        }
    }
    
    /**
     * 插入新段落
     * 
     * @param request 插入请求
     * @return 编辑响应
     */
    @PostMapping("/insert")
    @ApiOperation("插入新段落")
    public ResponseEntity<SegmentEditResponse> insertSegment(@RequestBody SegmentInsertRequest request) {
        log.info("[API] 插入段落请求：taskId={}, insertAfter={}", 
                request.getTaskId(), request.getInsertAfter());
        
        try {
            SegmentEditResponse response = segmentEditorService.insertSegment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[API] 插入段落失败", e);
            return ResponseEntity.ok(SegmentEditResponse.failure("插入失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除某个段落
     * 
     * @param request 删除请求
     * @return 编辑响应
     */
    @PostMapping("/delete")
    @ApiOperation("删除某个段落")
    public ResponseEntity<SegmentEditResponse> deleteSegment(@RequestBody SegmentDeleteRequest request) {
        log.info("[API] 删除段落请求：taskId={}, segmentIndex={}", 
                request.getTaskId(), request.getSegmentIndex());
        
        try {
            SegmentEditResponse response = segmentEditorService.deleteSegment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[API] 删除段落失败", e);
            return ResponseEntity.ok(SegmentEditResponse.failure("删除失败: " + e.getMessage()));
        }
    }
    
    /**
     * 查询异步任务状态
     * 
     * @param jobId 任务ID
     * @return 任务状态
     */
    @GetMapping("/status")
    @ApiOperation("查询视频生成进度")
    public ResponseEntity<JobStatusResponse> getJobStatus(@RequestParam String jobId) {
        log.debug("[API] 查询任务状态：jobId={}", jobId);
        
        try {
            JobStatusResponse response = segmentEditorService.getJobStatus(jobId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[API] 查询任务状态失败", e);
            return ResponseEntity.ok(JobStatusResponse.builder()
                    .jobId(jobId)
                    .status("error")
                    .errorMessage(e.getMessage())
                    .build());
        }
    }
}
