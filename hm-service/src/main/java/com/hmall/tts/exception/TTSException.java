package com.hmall.tts.exception;

/**
 * TTS 异常
 * 
 * @author Kiro
 * @since 2026-08-12
 */
public class TTSException extends RuntimeException {
    
    private TTSErrorCode errorCode;
    
    public TTSException(TTSErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public TTSException(TTSErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public TTSException(TTSErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
    
    public TTSException(TTSErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public TTSErrorCode getErrorCode() {
        return errorCode;
    }
}
