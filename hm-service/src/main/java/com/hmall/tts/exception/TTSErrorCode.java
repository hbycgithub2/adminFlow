package com.hmall.tts.exception;

/**
 * TTS 错误码
 * 
 * @author Kiro
 * @since 2026-08-12
 */
public enum TTSErrorCode {
    
    /**
     * TTS 未安装
     */
    NOT_INSTALLED("TTS_001", "Edge TTS 未安装，请运行 install-edge-tts.bat"),
    
    /**
     * 参数错误
     */
    INVALID_PARAMETER("TTS_002", "参数错误"),
    
    /**
     * 文本为空
     */
    EMPTY_TEXT("TTS_003", "文本内容不能为空"),
    
    /**
     * 文本过长
     */
    TEXT_TOO_LONG("TTS_004", "文本内容过长"),
    
    /**
     * 音色不存在
     */
    VOICE_NOT_FOUND("TTS_005", "音色不存在"),
    
    /**
     * 执行超时
     */
    TIMEOUT("TTS_006", "TTS 执行超时"),
    
    /**
     * 执行失败
     */
    EXECUTION_FAILED("TTS_007", "TTS 执行失败"),
    
    /**
     * 文件读写失败
     */
    FILE_IO_ERROR("TTS_008", "文件读写失败"),
    
    /**
     * 音频合并失败
     */
    MERGE_FAILED("TTS_009", "音频合并失败"),
    
    /**
     * 未知错误
     */
    UNKNOWN_ERROR("TTS_999", "未知错误");
    
    private final String code;
    private final String message;
    
    TTSErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
