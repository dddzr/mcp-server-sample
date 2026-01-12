package com.example.mcpserver.server;

import com.example.mcpserver.protocol.*;
import com.example.mcpserver.portal.PortalRestClient;
import com.example.mcpserver.util.LogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

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
        // UTF-8 인코딩으로 stdin 읽기
        this.reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        // UTF-8 인코딩으로 stdout 쓰기
        this.writer = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    }
    
    /**
     * MCP 서버 인스턴스를 생성합니다.
     * 포털 REST API를 래핑하는 서버를 생성합니다.
     */
    private McpServerWithPortalWrapper createMcpServer() {
        ObjectMapper mapper = new ObjectMapper();
        RestTemplate restTemplate = createRestTemplateWithSslBypass();
        PortalRestClient portalClient = new PortalRestClient(restTemplate);
        return new McpServerWithPortalWrapper(mapper, portalClient);
    }
    
    /**
     * SSL 인증서 검증을 우회하는 RestTemplate을 생성합니다.
     * localhost의 자체 서명 인증서를 허용하기 위해 사용합니다.
     */
    private RestTemplate createRestTemplateWithSslBypass() {
        try {
            // 모든 인증서를 신뢰하는 TrustManager 생성
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, (chain, authType) -> true) // 모든 인증서 신뢰
                .build();
            
            // TrustManager 설정
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE // 모든 호스트명 허용
            );
            
            CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                    PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslSocketFactory)
                        .build()
                )
                .evictExpiredConnections()
                .build();
            
                   HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
                   RestTemplate restTemplate = new RestTemplate(factory);
                   
                   // UTF-8 인코딩을 보장하기 위한 MessageConverter 설정
                   ObjectMapper mapper = new ObjectMapper();
                   mapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
                   
                   MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(mapper);
                   jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
                   
                   StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
                   
                   restTemplate.setMessageConverters(Arrays.asList(
                        stringConverter,
                        jsonConverter
                    ));
                
                   
                   return restTemplate;
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            // SSL 설정 실패 시 기본 RestTemplate 반환
            LogUtil.errPrintln("SSL 설정 실패, 기본 RestTemplate 사용: " + e.getMessage());
            e.printStackTrace();
            return new RestTemplate();
        }
    }
    
    /**
     * 서버를 시작합니다.
     * 표준 입력에서 메시지를 읽고 처리합니다.
     */
    public void start() {
        LogUtil.infoPrintln("MCP 서버 시작 (stdio 모드)");
        
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
                    
                    // Notification(id가 null)인 경우 응답을 보내지 않음
                    if (response != null && request.getId() != null) {
                        // 응답 전송 (표준 출력)
                        String responseJson = objectMapper.writeValueAsString(response);
                        writer.println(responseJson);
                        writer.flush();
                    }
                    
                } catch (Exception e) {
                    // 오류 발생 시 오류 응답 전송
                    LogUtil.errPrintln("오류 발생: " + e.getMessage());
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
            LogUtil.errPrintln("서버 종료: " + e.getMessage());
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

