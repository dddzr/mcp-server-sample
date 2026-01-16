package com.example.mcpserver.controller;

import com.example.mcpserver.protocol.*;
import com.example.mcpserver.server.McpServerWithPortalWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Server-Sent Events (SSE) Controller
 * 
 * SSE를 통해 클라이언트와 MCP 프로토콜로 통신합니다.
 * 
 * 통신 방식:
 * - 클라이언트 → POST /mcp/request (JSON-RPC 요청)
 * - 서버 → SSE 스트림으로 응답 전송
 */
@RestController
@RequestMapping("/mcp")
public class McpSseController {
    
    private static final Logger logger = LoggerFactory.getLogger(McpSseController.class);
    
    @Autowired
    private McpServerWithPortalWrapper mcpServer;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 클라이언트별 SSE Emitter 저장
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    /**
     * SSE 연결을 생성합니다.
     * 
     * @param clientId 클라이언트 ID (선택적)
     * @return SSE Emitter
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam(required = false) String clientId) {
        final String finalClientId = (clientId == null || clientId.isEmpty()) 
            ? "client-" + System.currentTimeMillis() 
            : clientId;
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 무제한 타임아웃
        emitters.put(finalClientId, emitter);
        
        // 연결 종료 시 정리
        emitter.onCompletion(() -> {
            emitters.remove(finalClientId);
            logger.info("SSE 연결 종료: {}", finalClientId);
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(finalClientId);
            logger.info("SSE 연결 타임아웃: {}", finalClientId);
        });
        
        emitter.onError((ex) -> {
            emitters.remove(finalClientId);
            logger.error("SSE 연결 오류: {}", finalClientId, ex);
        });
        
        logger.info("SSE 연결 생성: {}", finalClientId);
        
        // 초기 연결 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("{\"clientId\":\"" + finalClientId + "\",\"status\":\"connected\"}"));
        } catch (IOException e) {
            logger.error("초기 메시지 전송 실패", e);
        }
        
        return emitter;
    }
    
    /**
     * MCP 요청을 처리하고 SSE로 응답을 전송합니다.
     * 
     * @param request MCP JSON-RPC 요청
     * @param clientId 클라이언트 ID (선택적)
     * @return 응답 상태
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> handleRequest(
            @RequestBody McpRequest request,
            @RequestParam(required = false) String clientId) {
        
        final String finalClientId = (clientId == null || clientId.isEmpty()) 
            ? "client-" + System.currentTimeMillis() 
            : clientId;
        
        SseEmitter emitter = emitters.get(finalClientId);
        if (emitter == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "SSE 연결이 없습니다. 먼저 /mcp/events로 연결하세요."));
        }
        
        try {
            // MCP 요청 처리
            McpResponse response = mcpServer.handleRequest(request);
            
            // SSE로 응답 전송
            if (response != null && request.getId() != null) {
                String responseJson = objectMapper.writeValueAsString(response);
                emitter.send(SseEmitter.event()
                    .name("response")
                    .id(request.getId())
                    .data(responseJson));
                
                logger.debug("응답 전송: {}", responseJson);
            }
            
            return ResponseEntity.ok(Map.of("status", "processed", "clientId", finalClientId));
            
        } catch (Exception e) {
            logger.error("요청 처리 실패", e);
            
            try {
                McpResponse errorResponse = new McpResponse(
                    request.getId(),
                    new McpError(
                        McpError.ErrorCode.INTERNAL_ERROR,
                        "Internal error: " + e.getMessage()
                    )
                );
                String errorJson = objectMapper.writeValueAsString(errorResponse);
                emitter.send(SseEmitter.event()
                    .name("error")
                    .id(request.getId())
                    .data(errorJson));
            } catch (IOException ioException) {
                logger.error("오류 응답 전송 실패", ioException);
            }
            
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 연결된 클라이언트 수 조회
     */
    @GetMapping("/clients/count")
    public ResponseEntity<Map<String, Object>> getClientCount() {
        return ResponseEntity.ok(Map.of(
            "count", emitters.size(),
            "clients", emitters.keySet()
        ));
    }
}
