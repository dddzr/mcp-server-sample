package com.example.mcpserver.portal;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

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
    
    public PortalRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        // 포털 API URL (환경 변수 우선, 없으면 기본값)
        String envUrl = System.getenv("PORTAL_API_BASE_URL");
        this.portalBaseUrl = envUrl != null ? envUrl : "http://localhost:8081/api/portal";
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
        String url = portalBaseUrl + endpoint;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        // 실제 환경에서는 인증 토큰 등을 추가
        // headers.set("Authorization", "Bearer " + token);
        
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                method,
                entity,
                Object.class
            );
            
            return response.getBody();
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

