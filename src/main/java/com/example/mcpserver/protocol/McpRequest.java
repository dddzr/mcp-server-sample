package com.example.mcpserver.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP (Model Context Protocol) 요청 메시지 모델
 * 
 * MCP는 JSON-RPC 2.0 기반의 프로토콜로, 클라이언트와 서버 간의 통신을 정의합니다.
 * 이 클래스는 클라이언트가 서버에 보내는 요청 메시지를 나타냅니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpRequest {
    
    /**
     * JSON-RPC 프로토콜 버전 (항상 "2.0")
     */
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    /**
     * 요청 ID - 클라이언트가 생성한 고유 식별자
     * 서버는 응답 시 동일한 ID를 반환하여 요청-응답을 매칭합니다.
     */
    @JsonProperty("id")
    private String id;
    
    /**
     * 호출할 메서드 이름
     * 예: "tools/list", "tools/call", "resources/read" 등
     */
    @JsonProperty("method")
    private String method;
    
    /**
     * 메서드에 전달할 파라미터
     * 메서드에 따라 다른 구조를 가질 수 있습니다.
     */
    @JsonProperty("params")
    private Object params;

    // 기본 생성자
    public McpRequest() {
    }

    // 생성자
    public McpRequest(String id, String method, Object params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    // Getter와 Setter
    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }
}

