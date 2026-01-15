# MCP Server (REST Wrapper)

포털 REST API를 MCP 프로토콜로 래핑하는 서버입니다.

## 📚 아키텍처

```
[Cursor]
    ↓ JSON-RPC 2.0
[MCP Server (REST Wrapper)] ← 이 프로젝트
    ↓ REST
[포털 REST API] ← 별도 프로젝트 (portal-api)
```

## 🏗️ 프로젝트 구조

```
mcp-server-sample/ (이 프로젝트)
src/main/java/com/example/mcpserver/
├── config/                 # Spring/Client 설정
│   └── RestTemplateConfig.java
├── server/                 # MCP 핵심 기능 (서버)
│   ├── McpStdioServer.java             # stdio 서버
│   └── McpServerWithPortalWrapper.java # REST Wrapper
├── protocol/               # MCP 프로토콜 모델 계층
│   ├── McpRequest.java      # 요청 메시지
│   ├── McpResponse.java     # 응답 메시지
│   ├── McpError.java        # 오류 정보
│   ├── Tool.java            # 도구 모델
│   └── ToolCallRequest.java # 도구 호출 요청
└── portal/                 # 외부 REST API Adapter
    └── PortalRestClient.java

portal-api/ (별도 프로젝트)
└── src/main/java/com/example/portal/
    └── controller/
        └── PortalController.java            # 실제 포털 API
```

## 🚀 실행 방법

### 1. 포털 REST API 서버 실행 (별도 프로젝트)

### 2. MCP Server 빌드

```bash
mvn clean package
```

### 3. MCP Server 실행 (stdio 모드)

```bash
java -jar target/mcp-server-sample-0.0.1-SNAPSHOT.jar --mcp-stdio
```

### 4. Cursor에 추가

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

## 🔧 제공되는 도구

포털 REST API가 MCP 도구로 변환됩니다:

1. **get_employee_info**: 직원 정보 조회
   - 포털 API: `GET /api/portal/employees/{employeeId}`

2. **request_vacation**: 휴가 신청
   - 포털 API: `POST /api/portal/vacations`

3. **get_notices**: 공지사항 조회
   - 포털 API: `GET /api/portal/notices`

## ⚙️ 설정

### 포털 API URL 설정

환경 변수:
```bash
export PORTAL_API_BASE_URL=http://localhost:8081/api/portal
```

또는 `application.properties`:
```properties
portal.api.base-url=http://localhost:8081/api/portal
```

## 📝 테스트

### 1. 포털 API 테스트

```bash
# 직원 정보 조회
curl http://localhost:8081/api/portal/employees/12345

# 공지사항 조회
curl http://localhost:8081/api/portal/notices
```

### 2. MCP Server 테스트

```bash
# 도구 목록 조회
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | \
  java -jar target/mcp-server-sample-0.0.1-SNAPSHOT.jar --mcp-stdio
```

## 🎯 다음 단계

1. **포털 API 추가**: `portal-api` 프로젝트에 실제 API 추가
2. **도구 매핑**: `server.McpServerWithPortalWrapper.initializePortalTools()`에 새 도구 추가
3. **인증 추가**: 실제 환경에서는 인증 토큰 등 추가
