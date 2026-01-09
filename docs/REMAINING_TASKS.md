# 남은 작업 체크리스트

## ✅ 현재 상태

**MCP Server (REST Wrapper) 구현 완료!**

구현된 것:
- ✅ MCP 프로토콜 모델 (McpRequest, McpResponse, Tool 등)
- ✅ McpServerWithPortalWrapper (REST Wrapper 로직)
- ✅ McpStdioServer (stdio 기반 서버)
- ✅ PortalRestClient (포털 API 호출 클라이언트)
- ✅ 표준 MCP 프로토콜 준수

남은 작업
- ✅ 1. 도구 목록 실제 포털 API로 교체
- ✅ 2. PortalRestClient에 실제 API 정보 등록
- ✅ 3. JAR 생성 및 Cursor 연결

구조는 완성되었고, 실제 포털 API 정보만 연결하면 됩니다!
---

## 📋 남은 작업

### 1. 도구 목록 실제 포털 API로 교체 ✅

**위치**: `src/main/java/com/example/mcpserver/server/McpServerWithPortalWrapper.java`

**현재**: 예시 도구 3개 (직원 조회, 휴가 신청, 공지사항)
```java
// 라인 54-115: initializePortalTools()
```

**해야 할 일:**
- 실제 포털 API 엔드포인트 확인
- 각 API를 MCP 도구로 등록
- Tool description과 inputSchema 작성

**예시:**
```java
// 실제 포털 API에 맞게 수정
Tool actualTool = new Tool(
    "get_vacation_balance",  // 실제 도구 이름
    "직원의 휴가 잔여일수를 조회합니다. 포털 REST API: GET /api/portal/vacations/balance/{employeeId}",
    actualSchema  // 실제 파라미터 스키마
);
registerTool(actualTool, new PortalApiMapping(
    "/vacations/balance/{employeeId}",  // 실제 엔드포인트
    HttpMethod.GET
));
```

---

### 2. PortalRestClient에 실제 API 정보 확인 ✅

**위치**: `src/main/java/com/example/mcpserver/portal/PortalRestClient.java`

**현재 상태:**
- ✅ `callPortalApi()` 메서드로 범용 호출 가능
- ✅ 포털 API URL 설정 가능 (환경 변수 또는 기본값)

**해야 할 일:**
- 실제 포털 API 기본 URL 확인
- 인증 토큰 추가 (필요시)
- 에러 처리 개선 (필요시)

**설정 방법:**
```properties
# application.properties
portal.api.base-url=http://portal.example.com/api/portal
```

또는 환경 변수:
```bash
export PORTAL_API_BASE_URL=http://portal.example.com/api/portal
```

**인증 추가 (필요시):**
```java
// PortalRestClient.java 수정
headers.set("Authorization", "Bearer " + getAuthToken());
```

---

### 3. JAR 파일 생성 및 Cursor 연결 ✅

**빌드:**
```bash
mvn clean package
```

**JAR 파일 위치:**
```
target/mcp-server-sample-0.0.1-SNAPSHOT.jar
```

**Cursor 설정:**
`C:\Users\User\.cursor\mcp.json`:
```json
{
  "mcpServers": {
    "portal-mcp": {
      "command": "java",
      "args": [
        "-jar",
        "C:/Users/User/Documents/projects/mcp-server-sample/target/mcp-server-sample-0.0.1-SNAPSHOT.jar",
        "--mcp-stdio"
      ],
      "env": {
        "PORTAL_API_BASE_URL": "http://localhost:8081/api/portal"
      }
    }
  }
}
```

**Cursor 재시작 후 확인**

---

## ✅ 추가 고려사항 (선택)

### 1. 인증/인가
- 포털 API 인증 토큰 관리
- 사용자별 권한 제어

### 2. 에러 처리
- 포털 API 오류 처리
- 재시도 로직

### 3. 로깅
- 요청/응답 로깅
- 디버깅용 로그

### 4. 테스트
- 실제 포털 API와 통신 테스트
- 도구 호출 테스트

---

## 🎯 작업 순서

1. **실제 포털 API 엔드포인트 확인**
   - 어떤 API가 있는지
   - 파라미터 형식
   - 응답 형식

2. **도구 등록** (`McpServerWithPortalWrapper.initializePortalTools()`)
   - 실제 API를 MCP 도구로 매핑
   - description과 inputSchema 작성

3. **PortalRestClient 설정**
   - 포털 API URL 설정
   - 인증 추가 (필요시)

4. **빌드 및 테스트**
   - `mvn clean package`
   - 로컬에서 테스트

5. **Cursor 연결**
   - mcp.json 설정
   - Cursor 재시작
   - 도구 목록 확인

---


