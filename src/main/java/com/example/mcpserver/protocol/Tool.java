package com.example.mcpserver.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * MCP 도구(Tool) 모델
 * 
 * 도구는 클라이언트가 호출할 수 있는 함수를 나타냅니다.
 * 예: 날씨 조회, 데이터베이스 쿼리, 파일 읽기 등
 * 
 * 챗봇에서 사용자가 "날씨 알려줘"라고 하면,
 * 클라이언트는 이 도구를 호출하여 날씨 정보를 가져올 수 있습니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tool {
    
    /**
     * 도구의 고유 이름
     * 예: "get_weather", "search_database" 등
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * 도구에 대한 설명
     * AI가 이 도구를 언제 사용해야 하는지 판단하는 데 사용됩니다.
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * 도구의 입력 파라미터 스키마
     * JSON Schema 형식으로 정의됩니다.
     * 예: {"type": "object", "properties": {"city": {"type": "string"}}}
     */
    @JsonProperty("inputSchema")
    private Map<String, Object> inputSchema;

    // 기본 생성자
    public Tool() {
    }

    // 생성자
    public Tool(String name, String description, Map<String, Object> inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    // Getter와 Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(Map<String, Object> inputSchema) {
        this.inputSchema = inputSchema;
    }
}

