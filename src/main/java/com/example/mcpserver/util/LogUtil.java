package com.example.mcpserver.util;

// import java.io.OutputStreamWriter;
// import java.io.PrintWriter;
// import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 로그 출력 유틸리티
 * 
 * stdio 모드에서는 System.out(JSON 응답)과 System.err(로그)를 분리해야 하므로,
 * slf4j를 사용하되 System.err로 출력되도록 설정합니다. (logback.xml 설정)
 */
public class LogUtil {
    
    // private static final PrintWriter ERR_WRITER = new PrintWriter(
    //     new OutputStreamWriter(System.err, StandardCharsets.UTF_8), true);
    
    // // [stdio 모드] System.out 대신 System.err 사용 -> JSON 응답과 섞이지 않도록.
    // private static final PrintWriter OUT_WRITER = new PrintWriter(
    //     new OutputStreamWriter(System.err, StandardCharsets.UTF_8), true);
    private static final Logger logger = LoggerFactory.getLogger(LogUtil.class);
    
    /**
     * 에러 메시지를 출력합니다.
     * 
     * @param message 출력할 메시지
     */
    public static void errPrintln(String message) {
        // ERR_WRITER.println(message);
        logger.error(message);
    }
    
    /**
     * 에러 메시지를 출력합니다.
     * 
     * @param format 포맷 문자열
     * @param args 인자
     */
    public static void errPrintf(String format, Object... args) {
        // ERR_WRITER.printf(format, args);
        logger.error(format, args);
    }
    
    /**
     * 정보 메시지를 출력합니다.
     * 
     * @param message 출력할 메시지
     */
    public static void infoPrintln(String message) {
        // OUT_WRITER.println(message);
        logger.info(message);
    }
    
    /**
     * 정보 메시지를 출력합니다.
     * 
     * @param format 포맷 문자열
     * @param args 인자
     */
    public static void infoPrintf(String format, Object... args) {
        // OUT_WRITER.printf(format, args);
        logger.info(format, args);
    }
    
    /**
     * 디버그 메시지를 출력합니다.
     * 
     * @param message 출력할 메시지
     */
    public static void debugPrintln(String message) {
        logger.debug(message);
    }
    
    /**
     * 디버그 메시지를 출력합니다.
     * 
     * @param format 포맷 문자열
     * @param args 인자
     */
    public static void debugPrintf(String format, Object... args) {
        logger.debug(format, args);
    }
}
