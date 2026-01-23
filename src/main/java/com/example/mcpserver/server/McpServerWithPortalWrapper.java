package com.example.mcpserver.server;

import com.example.mcpserver.protocol.*;
import com.example.mcpserver.portal.PortalRestClient;
import com.example.mcpserver.util.LogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 서버 (REST Wrapper 버전)
 * 
 * 포털 REST API를 MCP 도구로 래핑하는 서버입니다.
 * 
 * 아키텍처:
 * [Cursor] → JSON-RPC → [이 서버 (REST Wrapper)] → REST → [포털 REST API]
 * 
 * 역할:
 * 1. MCP 프로토콜 (JSON-RPC 2.0) 요청을 받음
 * 2. 포털 REST API를 호출
 * 3. 결과를 MCP 프로토콜 형식으로 변환하여 반환
 * 
 * 이 서버는 비즈니스 로직을 가지지 않고,
 * 단순히 포털 REST API를 MCP 도구로 변환하는 래퍼 역할만 합니다.
 */
@Service
public class McpServerWithPortalWrapper {
    
    private final ObjectMapper objectMapper;
    private final PortalRestClient portalRestClient;
    
    // 등록된 도구들을 저장하는 맵
    // 각 도구는 포털 REST API 엔드포인트에 매핑됩니다.
    private final Map<String, Tool> tools;
    private final Map<String, PortalApiMapping> apiMappings;

    public McpServerWithPortalWrapper(ObjectMapper objectMapper, PortalRestClient portalRestClient) {
        this.objectMapper = objectMapper;
        this.portalRestClient = portalRestClient;
        this.tools = new ConcurrentHashMap<>();
        this.apiMappings = new ConcurrentHashMap<>();
        
        // 포털 API를 MCP 도구로 매핑
        try {
            LogUtil.infoPrintln("MCP 서버 초기화 시작...");
            initializePortalTools();
            LogUtil.infoPrintln("MCP 서버 초기화 완료. 등록된 도구 수: " + tools.size());
        } catch (Exception e) {
            LogUtil.errPrintln("MCP 서버 초기화 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 포털 REST API를 MCP 도구로 매핑합니다.
     * 
     * 실제 포털 API 엔드포인트를 MCP 도구로 변환합니다.
     */
    private void initializePortalTools() {
        // 예시 1: 직원 정보 조회 도구
        Map<String, Object> employeeSchema = new HashMap<>();
        employeeSchema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> idProp = new HashMap<>();
        idProp.put("type", "string");
        idProp.put("description", "직원 ID");
        properties.put("employeeId", idProp);
        employeeSchema.put("properties", properties);
        employeeSchema.put("required", Arrays.asList("employeeId"));
        
        Tool employeeTool = new Tool(
            "get_employee_info",
            "직원 정보를 조회합니다. 포털 REST API: GET /api/portal/employees/{id}",
            employeeSchema
        );
        registerTool(employeeTool, new PortalApiMapping(
            "/employees/{employeeId}",
            HttpMethod.GET
        ));
        
        // 예시 2: 휴가 신청 도구
        Map<String, Object> vacationSchema = new HashMap<>();
        vacationSchema.put("type", "object");
        Map<String, Object> vacProperties = new HashMap<>();
        Map<String, Object> startDateProp = new HashMap<>();
        startDateProp.put("type", "string");
        startDateProp.put("description", "시작일 (YYYY-MM-DD)");
        vacProperties.put("startDate", startDateProp);
        Map<String, Object> endDateProp = new HashMap<>();
        endDateProp.put("type", "string");
        endDateProp.put("description", "종료일 (YYYY-MM-DD)");
        vacProperties.put("endDate", endDateProp);
        vacationSchema.put("properties", vacProperties);
        vacationSchema.put("required", Arrays.asList("startDate", "endDate"));
        
        Tool vacationTool = new Tool(
            "request_vacation",
            "휴가를 신청합니다. 포털 REST API: POST /api/portal/vacations",
            vacationSchema
        );
        registerTool(vacationTool, new PortalApiMapping(
            "/vacations",
            HttpMethod.POST
        ));
        
        // 3. 포털 메뉴 정보 조회 도구
        // POST https://localhost:8083/selectMenuInfo.json
        Map<String, Object> menuInfoSchema = new HashMap<>();
        menuInfoSchema.put("type", "object");
        Map<String, Object> menuInfoProperties = new HashMap<>();

        // input_data 객체 스키마
        Map<String, Object> menuInputDataSchema = new HashMap<>();
        menuInputDataSchema.put("type", "object");
        Map<String, Object> menuInputDataProps = new HashMap<>();
        menuInputDataProps.put("menu_name", Map.of(
            "type", "string",
            "description", "조회할 메뉴의 이름입니다. 사용자 요청에서 메뉴 이름을 추출하여 전달합니다. 예: 사용자가 '공지사항 메뉴 정보 줘'라고 요청하면 '공지사항'을 전달합니다."
        ));
        menuInputDataSchema.put("properties", menuInputDataProps);
        menuInputDataSchema.put("required", Arrays.asList("menu_name"));

        // access_token 파라미터 추가 (선택적)
        menuInfoProperties.put("access_token", Map.of(
            "type", "string",
            "description", "포털 접근 토큰(Bearer). 필요 시 전달합니다."
        ));

        // 최상위 menuInfo 도구 스키마 구성
        menuInfoProperties.put("input_data", menuInputDataSchema);
        menuInfoSchema.put("properties", menuInfoProperties);
        menuInfoSchema.put("required", Arrays.asList("input_data"));

        Tool menuInfoTool = new Tool(
            "get_menu_info",
            "메뉴 정보를 조회합니다. 포털 REST API: POST /selectMenuInfo.json",
            menuInfoSchema
        );
        registerTool(menuInfoTool, new PortalApiMapping(
            "/selectMenuInfo.json",
            HttpMethod.POST
        ));
        
            }

    /**
     * 도구를 등록합니다.
     * 
     * @param tool MCP 도구 정의
     * @param apiMapping 포털 REST API 매핑 정보
     */
    public void registerTool(Tool tool, PortalApiMapping apiMapping) {
        tools.put(tool.getName(), tool);
        apiMappings.put(tool.getName(), apiMapping);
    }

    /**
     * MCP 요청을 처리합니다.
     * 
     * @param request MCP 요청
     * @return MCP 응답
     */
    public McpResponse handleRequest(McpRequest request) {
        try {
            String method = request.getMethod();
            String id = request.getId();
            
            switch (method) {
                case "tools/list":
                    return handleToolsList(id);
                    
                case "tools/call":
                    return handleToolCall(id, request.getParams());
                    
                case "initialize":
                    return handleInitialize(id, request.getParams());
                    
                case "notifications/initialized":
                    // Notification은 응답이 필요 없음 (id가 null)
                    return null;
                    
                default:
                    // Notification인 경우 (id가 null) 응답을 반환하지 않음
                    if (id == null) {
                        return null;
                    }
                    return new McpResponse(id, new McpError(
                        McpError.ErrorCode.METHOD_NOT_FOUND,
                        "Method not found: " + method
                    ));
            }
        } catch (Exception e) {
            return new McpResponse(
                request.getId(),
                new McpError(
                    McpError.ErrorCode.INTERNAL_ERROR,
                    "Internal server error: " + e.getMessage()
                )
            );
        }
    }

    /**
     * 도구 목록을 반환합니다.
     */
    private McpResponse handleToolsList(String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("tools", new ArrayList<>(tools.values()));
        return new McpResponse(id, result);
    }

    /**
     * 도구 호출을 처리합니다.
     * 포털 REST API를 호출합니다.
     */
    @SuppressWarnings("unchecked")
    private McpResponse handleToolCall(String id, Object params) {
        try {
            Map<String, Object> paramsMap = objectMapper.convertValue(params, Map.class);
            String toolName = (String) paramsMap.get("name");
            Map<String, Object> arguments = (Map<String, Object>) paramsMap.get("arguments");
            
            // 도구가 존재하는지 확인
            if (!tools.containsKey(toolName)) {
                return new McpResponse(id, new McpError(
                    McpError.ErrorCode.INVALID_PARAMS,
                    "Tool not found: " + toolName
                ));
            }
            
            // 포털 API 매핑 정보 가져오기
            PortalApiMapping mapping = apiMappings.get(toolName);
            if (mapping == null) {
                return new McpResponse(id, new McpError(
                    McpError.ErrorCode.INTERNAL_ERROR,
                    "API mapping not found for tool: " + toolName
                ));
            }
            
            // 액세스 토큰 추출
            String access_token = null;
            if (arguments != null) {
                if (arguments.containsKey("access_token")) {
                    access_token = (String) arguments.get("access_token");
                    LogUtil.debugPrintln("[DEBUG] 액세스 토큰 추출 성공 (access_token): " + access_token);
                }
                
                if (access_token == null) {
                    LogUtil.debugPrintln("[DEBUG] 액세스 토큰을 찾을 수 없습니다.");
                }
            } else {
                LogUtil.debugPrintln("[DEBUG] arguments가 null입니다.");
            }
            
            // 액세스 토큰 전달 여부 저장 (응답 처리 시 사용)
            final boolean hasAccessToken = access_token != null && !access_token.trim().isEmpty();
            
            // POST 요청 본문 구성 (.json으로 끝나는 엔드포인트는 input_data 구조로 전송)
            Object requestBody = null;
            if (mapping.getMethod() == HttpMethod.POST || mapping.getMethod() == HttpMethod.PUT) {
                    if (arguments != null) {
                        requestBody = new HashMap<>(arguments);
                    }
                }
            }
            
            // 포털 REST API 호출
            String endpoint = buildEndpoint(mapping.getEndpoint(), arguments);

            Object apiResult = portalRestClient.callPortalApi(
                endpoint,
                mapping.getMethod(),
                requestBody,
                access_token
            );
            
            // 응답 원본 로그 출력 (디버깅 용)
            // try {
            //     if (apiResult instanceof String) {
            //         LogUtil.debugPrintln("[DEBUG] API 응답 (String): " + apiResult);
            //     } else if (apiResult instanceof Map) {
            //         String responseJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiResult);
            //         LogUtil.debugPrintln("[DEBUG] API 응답 (Map):\n" + responseJson);
            //     } else {
            //         LogUtil.debugPrintln("[DEBUG] API 응답 (기타 타입): " + apiResult);
            //     }
            // } catch (Exception e) {
            //     LogUtil.debugPrintln("[DEBUG] API 응답 로그 출력 실패: " + e.getMessage());
            // }
            
            // TODO: 오류 응답 처리
            
            // 결과를 MCP 형식으로 변환
            Map<String, Object> responseResult = new HashMap<>();
            responseResult.put("content", Arrays.asList(
                Map.of("type", "text", "text", objectMapper.writeValueAsString(apiResult))
            ));
            return new McpResponse(id, responseResult);
            
        } catch (Exception e) {
            // 에러 상세 정보 포함
            String errorMessage = "Error calling portal API: " + e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " (원인: " + e.getCause().getMessage() + ")";
            }
            return new McpResponse(id, new McpError(
                McpError.ErrorCode.INTERNAL_ERROR,
                errorMessage
            ));
        }
    }

    /**
     * 엔드포인트에 파라미터를 치환합니다.
     * 예: "/employees/{employeeId}" + {employeeId: "123"} → "/employees/123"
     */
    private String buildEndpoint(String endpoint, Map<String, Object> arguments) {
        String result = endpoint;
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue().toString());
        }
        return result;
    }

    /**
     * 초기화 요청을 처리합니다.
     */
    private McpResponse handleInitialize(String id, Object params) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", Map.of(
            "tools", Map.of("listChanged", true)
        ));
        result.put("serverInfo", Map.of(
            "name", "MCP Portal REST Wrapper",
            "version", "1.0.0"
        ));
        return new McpResponse(id, result);
    }
    

    /**
     * 포털 REST API 매핑 정보
     */
    public static class PortalApiMapping {
        private final String endpoint;
        private final HttpMethod method;
        
        public PortalApiMapping(String endpoint, HttpMethod method) {
            this.endpoint = endpoint;
            this.method = method;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public HttpMethod getMethod() {
            return method;
        }
    }
}

