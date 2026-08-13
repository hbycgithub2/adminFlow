package com.hmall.tts.volcengine.asr.exception;

/**
 * ASR识别异常
 */
public class ASRException extends RuntimeException {
    
    public ASRException(String message) {
        super(message);
    }
    
    public ASRException(String message, Throwable cause) {
        super(message, cause);
    }
}
