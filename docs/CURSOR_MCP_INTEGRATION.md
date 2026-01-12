**실제 구조:**
```
Cursor (MCP Host)
    ↓
제 (AI 에이전트 - Auto)
    ↓
MCP 서버들 (도구 제공)
```

- **Cursor** = MCP 호스트 (서버들을 관리)
- **제 (Auto)** = AI 에이전트 (도구를 사용)
- **사용자의 MCP 서버** = 도구 제공자

---
## ✅ 사용자 MCP 서버를 Cursor에 추가하기

#### 방법 1: 표준 MCP 서버로 변환 (추천)

표준 MCP 서버는 **stdio** 또는 **WebSocket**을 통해 JSON-RPC 2.0으로 통신합니다.

**필요한 작업:**

1. **stdio 기반 MCP 서버 생성**
   - 표준 입력/출력을 통한 JSON-RPC 통신
   - Cursor가 프로세스를 실행하고 통신

2. **또는 WebSocket 기반 MCP 서버**
   - WebSocket을 통한 JSON-RPC 통신
   - Cursor가 WebSocket으로 연결

#### 방법 2: 현재 프로젝트를 MCP 브릿지로 사용

현재 Spring Boot 프로젝트를 MCP 브릿지로 만들어서:
- Cursor ↔ MCP 브릿지 (표준 MCP 프로토콜)
- MCP 브릿지 ↔ 현재 서버 (REST API)

---

## 🔧 Cursor MCP 설정 방법

### 1. mcp.json 파일 위치

```
C:\Users\User\.cursor\mcp.json
```

현재 파일 내용:
```json
{
  "mcpServers": {}
}
```

### 2. 표준 MCP 서버 추가 예시

#### stdio 기반 서버 (예: Node.js)

```json
{
  "mcpServers": {
    "my-mcp-server": {
      "command": "node",
      "args": ["path/to/mcp-server.js"]
    }
  }
}
```

#### WebSocket 기반 서버

```json
{
  "mcpServers": {
    "my-mcp-server": {
      "url": "ws://localhost:8080/ws/mcp"
    }
  }
}

---

## 📝 실제 통합 예제

### 1. 독립 실행형 MCP 서버 만들기

현재 프로젝트를 기반으로 표준 MCP 서버를 만들 수 있습니다:

```java
// McpStdioServer.java
public class McpStdioServer {
    public static void main(String[] args) {
        McpServer server = new McpServer();
        ObjectMapper mapper = new ObjectMapper();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                McpRequest request = mapper.readValue(line, McpRequest.class);
                McpResponse response = server.handleRequest(request);
                System.out.println(mapper.writeValueAsString(response));
            }
        }
    }
}
```

### 2. 빌드 (jar파일 생성)
  - `mvn clean package`

### 3. Cursor에 추가
```json
{
  "mcpServers": {
    "mcp-server-sample": {
      "command": "java",
      "args": [
        "-jar",
        "{프로젝트 위치}/mcp-server-sample/target/mcp-server-sample-0.0.1-SNAPSHOT.jar",
        "--mcp-stdio"
      ],
      "env": {
        "PORTAL_API_BASE_URL": "https://localhost:8083"
      }
    }
  }
}
```
### 4. Cursor에 추가
  - Cursor Settings > Tools & MCP > Installed MCP Servers 에서 on/off 가능
  - 서버 코드 수정 시 off 하고 다시 빌드하면 된다.

---

## ⚠️ 주의사항

1. **표준 MCP 프로토콜 준수**
   - JSON-RPC 2.0 형식
   - stdio 또는 WebSocket 통신
   - 표준 메서드: `initialize`, `tools/list`, `tools/call` 등

2. **Cursor 재시작 필요**
   - mcp.json 수정 후 Cursor 재시작
