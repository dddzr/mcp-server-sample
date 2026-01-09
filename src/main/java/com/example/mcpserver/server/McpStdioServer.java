package com.example.mcpserver.server;

import com.example.mcpserver.protocol.*;
import com.example.mcpserver.portal.PortalRestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * 표준 MCP 서버 (stdio 기반)
 * 
 * 표준 입력(stdin)에서 JSON-RPC 2.0 메시지를 읽고,
 * 표준 출력(stdout)으로 응답을 보냅니다.
 * 
 * Cursor와 같은 MCP 호스트에서 사용할 수 있습니다.
 * 
 * 실행 방법:
 * java -jar mcp-server-sample.jar --mcp-stdio
 * 
 * 또는 Cursor mcp.json:
 * {
 *   "mcpServers": {
 *     "mcp-server-sample": {
 *       "command": "java",
 *       "args": ["-jar", "path/to/mcp-server-sample.jar", "--mcp-stdio"]
 *     }
 *   }
 * }
 */
public class McpStdioServer {
    
    private final McpServerWithPortalWrapper mcpServer;
    private final ObjectMapper objectMapper;
    private final BufferedReader reader;
    private final PrintWriter writer;
    
    public McpStdioServer() {
        this.objectMapper = new ObjectMapper();
        this.mcpServer = createMcpServer();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.writer = new PrintWriter(System.out, true);
    }
    
    /**
     * MCP 서버 인스턴스를 생성합니다.
     * 포털 REST API를 래핑하는 서버를 생성합니다.
     */
    private McpServerWithPortalWrapper createMcpServer() {
        ObjectMapper mapper = new ObjectMapper();
        RestTemplate restTemplate = new RestTemplate();
        PortalRestClient portalClient = new PortalRestClient(restTemplate);
        return new McpServerWithPortalWrapper(mapper, portalClient);
    }
    
    /**
     * 서버를 시작합니다.
     * 표준 입력에서 메시지를 읽고 처리합니다.
     */
    public void start() {
        System.err.println("MCP 서버 시작 (stdio 모드)");
        
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    // JSON-RPC 요청 파싱
                    JsonNode requestNode = objectMapper.readTree(line);
                    
                    // 요청 생성
                    McpRequest request = new McpRequest();
                    request.setId(requestNode.has("id") ? requestNode.get("id").asText() : null);
                    request.setMethod(requestNode.has("method") ? requestNode.get("method").asText() : null);
                    request.setParams(requestNode.has("params") ? requestNode.get("params") : null);
                    
                    // 요청 처리
                    McpResponse response = mcpServer.handleRequest(request);
                    
                    // 응답 전송 (표준 출력)
                    String responseJson = objectMapper.writeValueAsString(response);
                    writer.println(responseJson);
                    writer.flush();
                    
                } catch (Exception e) {
                    // 오류 발생 시 오류 응답 전송
                    System.err.println("오류 발생: " + e.getMessage());
                    e.printStackTrace();
                    
                    McpResponse errorResponse = new McpResponse(
                        null,
                        new McpError(
                            McpError.ErrorCode.INTERNAL_ERROR,
                            "Internal error: " + e.getMessage()
                        )
                    );
                    
                    String errorJson = objectMapper.writeValueAsString(errorResponse);
                    writer.println(errorJson);
                    writer.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("서버 종료: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 메인 메서드
     * stdio 모드로 실행됩니다.
     */
    public static void main(String[] args) {
        // stdio 모드로 실행
        McpStdioServer server = new McpStdioServer();
        server.start();
    }
}

