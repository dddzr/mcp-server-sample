package com.example.mcpserver.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP (Model Context Protocol) 응답 메시지 모델
 * 
 * 서버가 클라이언트의 요청에 대해 반환하는 응답을 나타냅니다.
 * 성공 시 result 필드에 데이터를 포함하고,
 * 실패 시 error 필드에 오류 정보를 포함합니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {
    
    /**
     * JSON-RPC 프로토콜 버전 (항상 "2.0")
     */
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    /**
     * 요청 ID - 원본 요청의 ID와 동일한 값
     */
    @JsonProperty("id")
    private String id;
    
    /**
     * 성공 시 결과 데이터
     * 메서드에 따라 다른 구조를 가질 수 있습니다.
     */
    @JsonProperty("result")
    private Object result;
    
    /**
     * 실패 시 오류 정보
     * 오류가 발생한 경우에만 포함됩니다.
     */
    @JsonProperty("error")
    private McpError error;

    // 기본 생성자
    public McpResponse() {
    }

    // 성공 응답 생성자
    public McpResponse(String id, Object result) {
        this.id = id;
        this.result = result;
    }

    // 오류 응답 생성자
    public McpResponse(String id, McpError error) {
        this.id = id;
        this.error = error;
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

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public McpError getError() {
        return error;
    }

    public void setError(McpError error) {
        this.error = error;
    }
}

