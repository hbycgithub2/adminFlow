package com.hmall.tts.whisper.exception;

/**
 * Whisper异常
 * 
 * @author Kiro
 * @since 2026-08-14
 */
public class WhisperException extends RuntimeException {
    
    public WhisperException(String message) {
        super(message);
    }
    
    public WhisperException(String message, Throwable cause) {
        super(message, cause);
    }
}
