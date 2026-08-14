package com.hmall.tts.video.util;

/**
 * 时间格式转换工具类
 */
public class TimeUtil {
    
    /**
     * 将秒数转换为ASS时间格式（H:MM:SS.CS）
     * 
     * @param seconds 秒数
     * @return ASS时间格式字符串，例如：0:00:03.50
     */
    public static String formatTimeForASS(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int centiseconds = (int) ((seconds * 100) % 100);
        
        return String.format("%d:%02d:%02d.%02d", hours, minutes, secs, centiseconds);
    }
    
    /**
     * 将秒数转换为SRT时间格式（HH:MM:SS,mmm）
     * 
     * @param seconds 秒数
     * @return SRT时间格式字符串，例如：00:00:03,500
     */
    public static String formatTimeForSRT(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int milliseconds = (int) ((seconds * 1000) % 1000);
        
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, milliseconds);
    }
    
    /**
     * 将毫秒转换为秒
     */
    public static double millisecondsToSeconds(long milliseconds) {
        return milliseconds / 1000.0;
    }
    
    /**
     * 将秒转换为毫秒
     */
    public static long secondsToMilliseconds(double seconds) {
        return (long) (seconds * 1000);
    }
}
