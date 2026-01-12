package com.example.mcpserver.util;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * UTF-8 인코딩을 보장하는 로그 출력 유틸리티
 */
public class LogUtil {
    
    private static final PrintWriter ERR_WRITER = new PrintWriter(
        new OutputStreamWriter(System.err, StandardCharsets.UTF_8), true);
    
    private static final PrintWriter OUT_WRITER = new PrintWriter(
        new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    
    /**
     * UTF-8로 인코딩된 에러 메시지를 출력합니다.
     * 
     * @param message 출력할 메시지
     */
    public static void errPrintln(String message) {
        synchronized (ERR_WRITER) {
            ERR_WRITER.println(message);
            // autoFlush=true이므로 flush() 불필요
        }
    }
    
    /**
     * UTF-8로 인코딩된 에러 메시지를 출력합니다.
     * 
     * @param format 포맷 문자열
     * @param args 인자
     */
    public static void errPrintf(String format, Object... args) {
        ERR_WRITER.printf(format, args);
        ERR_WRITER.flush();
    }
    
    /**
     * UTF-8로 인코딩된 정보 메시지를 출력합니다.
     * 
     * @param message 출력할 메시지
     */
    public static void infoPrintln(String message) {
        OUT_WRITER.println(message);
    }
    
    /**
     * UTF-8로 인코딩된 정보 메시지를 출력합니다.
     * 
     * @param format 포맷 문자열
     * @param args 인자
     */
    public static void infoPrintf(String format, Object... args) {
        OUT_WRITER.printf(format, args);
        OUT_WRITER.flush();
    }
    
    /**
     * UTF-8로 인코딩된 디버그 메시지를 출력합니다.
     * 
     * @param message 출력할 메시지
     */
    public static void debugPrintln(String message) {
        synchronized (OUT_WRITER) {
            OUT_WRITER.println(message);
            // autoFlush=true이므로 flush() 불필요
        }
    }
    
    /**
     * UTF-8로 인코딩된 디버그 메시지를 출력합니다.
     * 
     * @param format 포맷 문자열
     * @param args 인자
     */
    public static void debugPrintf(String format, Object... args) {
        OUT_WRITER.printf(format, args);
        OUT_WRITER.flush();
    }
}
