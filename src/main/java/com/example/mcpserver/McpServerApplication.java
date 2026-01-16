package com.example.mcpserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * MCP Server Spring Boot 애플리케이션
 * 
 * 실행 모드:
 * 1. stdio 모드: jar파일을 MCP Client에 등록 or --mcp-stdio 인자로 실행 (McpStdioServer.main 사용)
 * 2. HTTP/SSE 모드: 이 클래스로 실행 (Spring Boot 웹 서버 시작)
 */
@SpringBootApplication
public class McpServerApplication extends SpringBootServletInitializer {
    
    public static void main(String[] args) {
        // stdio 모드 체크
        boolean isStdioMode = false;
        for (String arg : args) {
            if ("--mcp-stdio".equals(arg)) {
                isStdioMode = true;
                break;
            }
        }
        
        if (isStdioMode) {
            // stdio 모드: Spring Boot 없이 직접 실행
            com.example.mcpserver.server.McpStdioServer.main(args);
        } else {
            // HTTP/SSE 모드: Spring Boot 웹 서버 시작
            SpringApplication app = new SpringApplication(McpServerApplication.class);
            app.setWebApplicationType(org.springframework.boot.WebApplicationType.SERVLET);
            app.run(args);
        }
    }
}
