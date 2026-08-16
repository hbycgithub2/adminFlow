package com.hmall.tts.whisperx.exception;

/**
 * WhisperX异常
 * 
 * @author Kiro
 * @since 2026-08-15
 */
public class WhisperXException extends Exception {
    
    public WhisperXException(String message) {
        super(message);
    }
    
    public WhisperXException(String message, Throwable cause) {
        super(message, cause);
    }
}
