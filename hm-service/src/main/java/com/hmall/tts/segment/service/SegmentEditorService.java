package com.hmall.tts.segment.service;

import com.hmall.tts.segment.dto.*;

/**
 * 段落编辑服务接口
 * 
 * 支持局部编辑视频中的某个段落，包括：
 * - 编辑文本和配音
 * - 插入新段落
 * - 删除段落
 * 
 * @author Kiro
 * @since 2026-08-17
 */
public interface SegmentEditorService {
    
    /**
     * 编辑某个段落
     * 
     * @param request 编辑请求
     * @return 编辑响应（包含异步任务ID）
     */
    SegmentEditResponse editSegment(SegmentEditRequest request);
    
    /**
     * 插入新段落
     * 
     * @param request 插入请求
     * @return 编辑响应（包含异步任务ID）
     */
    SegmentEditResponse insertSegment(SegmentInsertRequest request);
    
    /**
     * 删除某个段落
     * 
     * @param request 删除请求
     * @return 编辑响应（包含异步任务ID）
     */
    SegmentEditResponse deleteSegment(SegmentDeleteRequest request);
    
    /**
     * 查询异步任务状态
     * 
     * @param jobId 任务ID
     * @return 任务状态
     */
    JobStatusResponse getJobStatus(String jobId);
}
