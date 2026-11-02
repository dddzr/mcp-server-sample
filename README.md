# MCP Server (REST Wrapper)

- 이 프로젝트는 MCP(Model Context Protocol)를 사용한 챗봇 시스템의  MCP Server 데모입니다.
사내 포털 등에 챗봇을 적용하기 위한 기반 구조를 제공합니다.
- 포털 시스템이 DB와 연결되어 있고, 포털 REST API를 MCP 프로토콜로 래핑하는 서버입니다.

## TODO
Spring Ai 사용하여 tool 관리 가독성 좋고 편리하게 (예: @Tool 어노테이션 사용)
## TODO: 구조 개선 (portal_mcp 이전 경험 반영)

### 1. MCP 프로토콜 처리를 공식 SDK로 교체
- [ ] `protocol/`(McpRequest·McpResponse·McpError·Tool·ToolCallRequest) → SDK `McpSchema` 객체로 교체 후 삭제
- [ ] `McpSseController`(HTTP+SSE, MCP 사양에서 deprecated) → SDK Streamable HTTP 전송으로 교체
      - 세션 불필요하면 `HttpServletStatelessServerTransport`를 `ServletRegistrationBean`으로 `/mcp`에 등록
- [ ] `McpStdioServer` → SDK `StdioServerTransportProvider`로 교체 (stdio 지원은 유지)

### 2. Tool 분리
- [ ] `McpServerWithPortalWrapper`의 Tool들을 1 Tool = 1 클래스로 분리 (확인 필요: 현재 Tool 수·구성)
  - 공통 인터페이스 `PortalTool { name, description, inputSchema, execute(context, args) }`
  - 구현체를 `@Component`로 등록 → `List<PortalTool>` 주입받아 SDK `SyncToolSpecification`으로 일괄 등록
  - 인자 꺼내기·결과 컬럼 고르기 등 공통 처리는 `ToolSupport`로
- [ ] Tool 실행 정보를 `ToolContext`(토큰이 묶인 API 클라이언트, 요청 `_meta` 값)로 묶어 넘김

### 3. 포탈 API 연동
- [ ] `PortalRestClient` → 인터페이스(`PortalApi`) + 구현 분리. 테스트에서 가짜 구현으로 Tool 단위 테스트
- [ ] 사용자 인증: 요청 `Authorization` 헤더를 transport `contextExtractor`로 읽어 Tool 실행까지 전달
- [ ] 아래 「4. 토큰 처리」 결정에 맞춰 포탈 API 호출 시 인증 방식 정리

### 4. 권한·토큰 처리
- [ ] `tools/list`를 사용자 권한으로 필터 (`addToolFilter`). 권한 없어 빠진 Tool은 안내용 resource로 제공 (선택)
- [ ] `tools/call` 실행 직전 권한 재확인 (목록 필터 결과에 기대지 않음)
- [ ] 토큰 처리 방식 결정 — 클라이언트 토큰을 포탈 API에 그대로 전달(passthrough)하는 것은 MCP 보안 권고상 비권장
  - 권장: MCP Server가 토큰 `aud`(=이 서버) 검증 → 포탈 API용 토큰은 별도 발급(token exchange) 또는 서버 자격증명 + 사용자 ID 전달
  - 데모 단순화로 passthrough 유지 시 README에 한계로 명시

### 5. 기타
- [ ] `RestTemplateConfig` → 필요 없으면 제거 (JDK `HttpClient` 또는 `RestClient`)
- [ ] `util/LogUtil` → SLF4J 직접 사용, 토큰 원문 로그 금지, 요청 ID로 클라이언트·서버·포탈 로그 연결
- [ ] 결과 건수 제한 (LLM에 너무 많은 행을 넘기지 않게, 예: 50건 + `truncated`)


## 📚 아키텍처

```
[Cursor / MCP Client]
    ↓ JSON-RPC 2.0
[MCP Server (REST Wrapper)] ← 이 프로젝트
    ↓ REST
[포털 REST API] ← 별도 프로젝트 (portal-api)
```

## 📁 프로젝트 구조

```
mcp-server-sample/
src/main/java/com/example/mcpserver/
├── McpServerApplication.java             # Spring Boot 애플리케이션 (SSE)
├── config/                           # Spring Bean 설정
│   └── RestTemplateConfig.java           # RestTemplate, ObjectMapper Bean 등록 (SSE)
├── controller/                       # HTTP Controller
│   └── McpSseController.java             # SSE 엔드포인트 제공 (SSE)
├── server/                           # MCP Server 핵심 기능
│   ├── McpStdioServer.java               # stdio 모드 서버 (stdio)
│   └── McpServerWithPortalWrapper.java   # REST Wrapper
├── protocol/                         # MCP 프로토콜 모델 계층
│   ├── McpRequest.java                   # 요청 메시지
│   ├── McpResponse.java                  # 응답 메시지
│   ├── McpError.java                     # 오류 정보
│   ├── Tool.java                         # 도구 모델
│   └── ToolCallRequest.java              # 도구 호출 요청
├── portal/                           # 외부 REST API Adapter
│   └── PortalRestClient.java             # 포털 REST API 클라이언트
└── util/                             # 유틸리티
    └── LogUtil.java                      # 로깅 유틸리티

portal-api/ (기존에 DB 연결되어 있던 별도 프로젝트 )
└── src/main/java/com/example/portal/
    └── controller/
        └── PortalController.java          # 실제 포털 API
```

## 🚀 실행 방법 - Studio 모드

### 1. 포털 REST API 서버 실행 (별도 프로젝트)

### 2. MCP Server 빌드

```bash
mvn clean package
```
- 안되면
```
wmic process where "CommandLine like '%mcp-server-sample%' and Name='java.exe'" get ProcessId,CommandLine
CommandLine                                                                                                                                                           ProcessId
java -jar C:/{my-path}/mcp-server-sample-0.0.1-SNAPSHOT.jar --mcp-stdio                                         23092
"C:\Program Files\Java\jdk-17\bin\java.exe" -jar C:/{my-path}/mcp-server-sample-0.0.1-SNAPSHOT.jar --mcp-stdio  22320
java -jar C:/{my-path}/mcp-server-sample-0.0.1-SNAPSHOT.jar --mcp-stdio                                         23304
"C:\Program Files\Java\jdk-17\bin\java.exe" -jar C:/{my-path}/mcp-server-sample-0.0.1-SNAPSHOT.jar --mcp-stdio  23492

taskkill /PID 23092 /PID 22320 /PID 23304 /PID 23492 /F
```

### 3. MCP Server 실행

```bash
java -jar target/mcp-server-sample-0.0.1-SNAPSHOT.jar --mcp-stdio
```

### 4. Cursor에 추가 or MCP Client에 추가

#### ✅ Cursor: `C:\Users\User\.cursor\mcp.json`
- Cursor Settings > Tools & MCP > Installed MCP Servers 에서 on/off 가능
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
        "PORTAL_API_BASE_URL": "http://localhost:8083/api/portal"
      }
    }
  }
}
```
#### ✅ MCP Client: `application.yml`
```json
mcp:
  servers:
    mcp-server-sample:
      command: java
      args:
        - -jar
        - C:\Users\User\Documents\projects\mcp-server-sample\target\mcp-server-sample-0.0.1-SNAPSHOT.jar
        - --mcp-stdio
      cwd: C:\Users\User\Documents\projects\personal\mcp-server-sample
    #   env:
    #     PORTAL_API_BASE_URL: http://localhost:8083/api/portal
```


## 🌐 실행 방법 - HTTP/SSE 모드

```bash
# Maven으로 직접 실행 (Spring boot)
mvn spring-boot:run

# 또는 --mcp-stdio 인자 없이 실행하면 HTTP/SSE 모드로 실행됩니다
java -jar target/mcp-server-sample-0.0.1-SNAPSHOT.jar
```

서버가 `http://localhost:8080`에서 시작됩니다.

**SSE 엔드포인트:**
- `GET /mcp/events?clientId={id}` - SSE 연결 생성
- `POST /mcp/request?clientId={id}` - MCP 요청 전송 (응답은 SSE로 전송)
- `GET /mcp/clients/count` - 연결된 클라이언트 수 조회


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
export PORTAL_API_BASE_URL=http://localhost:8083/api/portal
```

또는 `application.properties`:
```properties
portal.api.base-url=http://localhost:8083/api/portal
```

## 📝 테스트

### 1. 포털 API 테스트

```bash
# 직원 정보 조회
curl http://localhost:8083/api/portal/employees/12345

# 공지사항 조회
curl http://localhost:8083/api/portal/notices
```

### 2. MCP Server 테스트

#### stdio 모드 테스트

```bash
# 도구 목록 조회
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | \
  java -jar target/mcp-server-sample-0.0.1-SNAPSHOT.jar --mcp-stdio
```

#### SSE 모드 테스트

**1. SSE 연결 생성 (브라우저 또는 curl)**

```bash
# SSE 연결
curl -N http://localhost:8080/mcp/events?clientId=test-client
```

**2. MCP 요청 전송 (다른 터미널에서)**

```bash
# 도구 목록 조회
curl -X POST "http://localhost:8080/mcp/request?clientId=test-client" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tools/list",
    "params": {}
  }'
```

**3. JavaScript 클라이언트 예시**

```javascript
// SSE 연결
const eventSource = new EventSource('http://localhost:8080/mcp/events?clientId=my-client');

eventSource.addEventListener('connected', (event) => {
    console.log('연결됨:', JSON.parse(event.data));
});

eventSource.addEventListener('response', (event) => {
    console.log('응답:', JSON.parse(event.data));
});

eventSource.addEventListener('error', (event) => {
    console.log('오류:', JSON.parse(event.data));
});

// MCP 요청 전송
fetch('http://localhost:8080/mcp/request?clientId=my-client', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        jsonrpc: '2.0',
        id: '1',
        method: 'tools/call',
        params: {
          "tool": "도구_이름",
          "arguments": {
              "boardId": 1
          }
        }
    })
});
```

## 🎯 다음 단계

1. **포털 API 추가**: `portal-api` 프로젝트에 실제 API 추가
2. **도구 매핑**: `server.McpServerWithPortalWrapper.initializePortalTools()`에 새 도구 추가
3. **인증 추가**: 실제 환경에서는 인증 토큰 등 추가
