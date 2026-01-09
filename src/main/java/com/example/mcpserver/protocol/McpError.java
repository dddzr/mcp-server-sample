package com.example.mcpserver.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP 오류 정보 모델
 * 
 * 요청 처리 중 발생한 오류를 나타냅니다.
 * JSON-RPC 2.0 표준 오류 코드를 사용합니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpError {
    
    /**
     * 오류 코드
     * -32700: Parse error (잘못된 JSON)
     * -32600: Invalid Request (잘못된 요청 형식)
     * -32601: Method not found (존재하지 않는 메서드)
     * -32602: Invalid params (잘못된 파라미터)
     * -32603: Internal error (서버 내부 오류)
     * -32000 ~ -32099: Server error (서버 정의 오류)
     */
    @JsonProperty("code")
    private int code;
    
    /**
     * 오류 메시지
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * 추가 오류 정보 (선택적)
     */
    @JsonProperty("data")
    private Object data;

    // 기본 생성자
    public McpError() {
    }

    // 생성자
    public McpError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public McpError(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // Getter와 Setter
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    // 표준 오류 코드 상수
    public static class ErrorCode {
        public static final int PARSE_ERROR = -32700;
        public static final int INVALID_REQUEST = -32600;
        public static final int METHOD_NOT_FOUND = -32601;
        public static final int INVALID_PARAMS = -32602;
        public static final int INTERNAL_ERROR = -32603;
    }
}

