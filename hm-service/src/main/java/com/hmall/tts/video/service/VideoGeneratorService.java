package com.hmall.tts.video.service;

import com.hmall.tts.video.dto.VideoGenerateRequest;
import com.hmall.tts.video.dto.VideoGenerateResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频生成服务接口
 */
public interface VideoGeneratorService {
    
    /**
     * 从Word文档生成视频
     * 
     * @param file Word文档文件
     * @param request 视频生成请求
     * @return 视频生成响应
     * @throws Exception 生成失败时抛出异常
     */
    VideoGenerateResponse generateVideoFromDocument(MultipartFile file, VideoGenerateRequest request) throws Exception;
}
