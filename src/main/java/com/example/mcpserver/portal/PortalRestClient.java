package com.example.mcpserver.portal;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mcpserver.util.LogUtil;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 포털 REST API 클라이언트
 * 
 * 실제 포털의 REST API를 호출하는 클라이언트입니다.
 * 
 * 이 클래스는 실제 포털 REST API와 통신합니다.
 * MCP 서버는 이 클라이언트를 통해 포털 API를 호출합니다.
 * 
 * 예시:
 * - GET /api/portal/employees/{id} - 직원 정보 조회
 * - POST /api/portal/vacations - 휴가 신청
 * - GET /api/portal/notices - 공지사항 조회
 */
@Service
public class PortalRestClient {
    
    private final RestTemplate restTemplate;
    private final String portalBaseUrl;
    private final ObjectMapper objectMapper;
    
    public PortalRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        // UTF-8 인코딩 보장을 위한 설정
        this.objectMapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
        // 포털 API URL (환경 변수 우선, 없으면 기본값 - McpStdioServer는 Spring Boot 컨텍스트를 시작하지 않고 직접 main 메서드로 실행)
        String envUrl = System.getenv("PORTAL_API_BASE_URL");
        this.portalBaseUrl = envUrl != null ? envUrl : "https://localhost:8083";
    }
    
    /**
     * 포털 REST API를 호출합니다.
     * 
     * @param endpoint API 엔드포인트 (예: "/employees/123")
     * @param method HTTP 메서드 (GET, POST, PUT, DELETE)
     * @param requestBody 요청 본문 (POST/PUT 시 사용)
     * @return API 응답
     */
    public Object callPortalApi(String endpoint, HttpMethod method, Object requestBody) {
        return callPortalApi(endpoint, method, requestBody, null);
    }
    
    /**
     * 포털 REST API를 호출합니다. (access_token 포함)
     * 
     * @param endpoint API 엔드포인트 (예: "/employees/123")
     * @param method HTTP 메서드 (GET, POST, PUT, DELETE)
     * @param requestBody 요청 본문 (POST/PUT 시 사용)
     * @param access_token 접근 토큰(Bearer), 선택적
     * @return API 응답
     */
    public Object callPortalApi(String endpoint, HttpMethod method, Object requestBody, String access_token) {
        String url = portalBaseUrl + "/mcp" + endpoint;
        
        HttpHeaders headers = new HttpHeaders();
        
        // JSP 페이지 요청인 경우 Content-Type을 다르게 설정
        if (endpoint.contains(".do") || endpoint.contains(".jsp")) {
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        } else {
            // JSON 요청인 경우 Content-Type과 Accept 모두 설정 (UTF-8 인코딩 명시)
            if (method == HttpMethod.POST || method == HttpMethod.PUT) {
                headers.set("Content-Type", "application/json;charset=UTF-8");
            }
            headers.set("Accept", "application/json;charset=UTF-8");
        }
        
        // access_token이 있으면 Authorization 헤더에 추가
        LogUtil.debugPrintln("[DEBUG] 전달받은 access_token: " + access_token);
        if (access_token != null && !access_token.trim().isEmpty()) {
            headers.setBearerAuth(access_token);
            LogUtil.debugPrintln("[DEBUG] Authorization 헤더 값 설정(Bearer)");
        } else {
            LogUtil.debugPrintln("[DEBUG] access_token이 없습니다.");
        }
        
        // 모든 헤더 로그 출력
        LogUtil.debugPrintln("[DEBUG] 요청 URL: " + url);
        LogUtil.debugPrintln("[DEBUG] 요청 메서드: " + method);
        LogUtil.debugPrintln("[DEBUG] 요청 헤더: " + headers);
        
        // 요청 본문 설정 및 로그 출력
        HttpEntity<?> entity;
        if (requestBody != null && (method == HttpMethod.POST || method == HttpMethod.PUT)) {
            // Content-Type을 MediaType으로 명시적으로 설정 (UTF-8 charset 포함)
            headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
            entity = new HttpEntity<>(requestBody, headers);
            
            // 요청 본문 로그 출력
            try {
                String requestBodyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
                LogUtil.debugPrintln("[DEBUG] 요청 본문:\n" + requestBodyJson);
            } catch (Exception e) {
                LogUtil.debugPrintln("[DEBUG] 요청 본문 로그 출력 실패: " + e.getMessage());
                LogUtil.debugPrintln("[DEBUG] 요청 본문 (toString): " + requestBody);
            }
        } else {
            entity = new HttpEntity<>(requestBody, headers);
            if (requestBody != null) {
                LogUtil.debugPrintln("[DEBUG] 요청 본문: " + requestBody);
            } else {
                LogUtil.debugPrintln("[DEBUG] 요청 본문: 없음");
            }
        }
        
        try {
            // 먼저 String으로 응답 받기 (Content-Type 확인을 위해)
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                method,
                entity,
                String.class
            );
            
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return null;
            }
            
            // Content-Type 헤더로 응답 타입 판단
            MediaType contentTypeHeader = response.getHeaders().getContentType();
            String contentType = contentTypeHeader != null ? contentTypeHeader.toString() : "";
            LogUtil.debugPrintln("[DEBUG] 응답 Content-Type: " + contentType);
            
            if (contentType.contains("application/json")) {
                // JSON 응답인 경우 파싱
                try {
                    // JSONP 형식 응답 처리 (예: ")]}', {...}" 또는 ")]}', {...}")
                    String jsonBody = responseBody;
                    if (responseBody.startsWith(")]}',")) {
                        // JSONP 접두사 제거
                        jsonBody = responseBody.substring(5).trim();
                    } else if (responseBody.startsWith(")]}'")) {
                        // 다른 JSONP 형식
                        jsonBody = responseBody.substring(4).trim();
                    }
                    
                    // JSON 문자열을 Map으로 파싱
                    Map<String, Object> parsedResponse = objectMapper.readValue(jsonBody, Map.class);
                    return parsedResponse;
                } catch (Exception e) {
                    throw new RuntimeException("포털 API 응답 파싱 실패: " + e.getMessage(), e);
                }
            } else if (contentType.contains("text/html")) {
                // HTML 응답인 경우 String 그대로 반환
                return responseBody;
            } else {
                // 기타 타입도 String으로 반환
                return responseBody;
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // HTTP 4xx 에러인 경우 상세 정보 포함
            throw new RuntimeException("포털 API 호출 실패 (HTTP " + e.getStatusCode() + "): " + e.getMessage() + 
                (e.getResponseBodyAsString() != null ? " 응답: " + e.getResponseBodyAsString() : ""), e);
        } catch (Exception e) {
            throw new RuntimeException("포털 API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 직원 정보 조회 (예시)
     */
    public Object getEmployeeInfo(String employeeId) {
        return callPortalApi("/employees/" + employeeId, HttpMethod.GET, null);
    }
    
    /**
     * 휴가 신청 (예시)
     */
    public Object requestVacation(Map<String, Object> vacationRequest) {
        return callPortalApi("/vacations", HttpMethod.POST, vacationRequest);
    }
    
    /**
     * 공지사항 조회 (예시)
     */
    public Object getNotices() {
        return callPortalApi("/notices", HttpMethod.GET, null);
    }
}

