package com.example.mcpserver.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * 도구 호출 요청 파라미터 모델
 * 
 * 클라이언트가 특정 도구를 호출할 때 전달하는 파라미터입니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCallRequest {
    
    /**
     * 호출할 도구의 이름
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * 도구에 전달할 인자들
     * 도구의 inputSchema에 정의된 형식에 맞춰야 합니다.
     */
    @JsonProperty("arguments")
    private Map<String, Object> arguments;

    // 기본 생성자
    public ToolCallRequest() {
    }

    // 생성자
    public ToolCallRequest(String name, Map<String, Object> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    // Getter와 Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}

