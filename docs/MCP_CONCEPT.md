# MCP (Model Context Protocol) 챗봇 데모

이 프로젝트는 MCP(Model Context Protocol)를 사용한 챗봇 시스템의 데모입니다. 
사내 포털 등에 챗봇을 적용하기 위한 기반 구조를 제공합니다.

## 📚 MCP란?

**MCP (Model Context Protocol)**
 - AI 애플리케이션이 외부 데이터 소스 및 도구와 안전하게 상호작용할 수 있도록 하는 프로토콜입니다.
 - AI 모델(LLM)이 외부 도구(웹 검색, 데이터베이스, 앱 등)와 통신하며 데이터를 주고받을 수 있도록 표준화된 규약(프로토콜)을 제공하는 서버.
 - 개발자는 LLM(ChatGPT, Claude 등)마다 따로 서버를 개발할 필요 없고, LLM 제공자도 고객에 맞게 작업할 필요x.

**MCP 정의(조건)**
- JSON-RPC 2.0
- 고정된 method 명세 (initialize, tools/list, call 등)
- 서버가 capabilities 제공
- 클라이언트가 호출
- AI 필요 x -> AI가 도구를 쓸 수 있게 하려고 만들어진 프로토콜 목적이다!!

### 핵심 개념
1. **MCP Host**
  - Client들을 보유/관리
  - Client ↔ Server 연결 세션 유지
  - 입출력 라우팅
  - *실제 제품에서 [MCP Client + LLM] 통합 → MCP Host란 이름으로 동작

2. **클라이언트 (Client)**
   - **MCP Server에 JSON-RPC 요청 보내는 엔진**
   - 자연어 요청을 분석하여 서버 도구 호출
   - 응답 결과를 자연어로 변환하여 제공
   - AI 모델(LLM 에이전트)실제 실행 or 규칙 기반 에이전트도 가능

3. **서버 (Server)**
   - 클라이언트의 요청을 처리하고 결과를 반환
   - JSON-RPC 2.0 사용
   - 일반적으로 AI 모델(LLM)이 활용 할 수 있는 기능/데이터를 제공
      - 도구(Tools), 리소스(Resources), 프롬프트(Prompts)
      - 예: 날씨 API, 데이터베이스, 파일 시스템 등
  - 특수한 경우 JSON-RPC <-> REST 중계 역할만 할 수도 있다.

4. **도구 (Tools)**
   - 실행 가능한 함수들
   - 각 도구는 이름, 설명, 입력 스키마를 가짐
   - 예: `get_weather`, `search_database`, `calculate` 등

5. **프로토콜**
   - JSON-RPC 2.0 기반
   - 요청-응답 패턴
   - 표준화된 메시지 형식

## 🛠️ 아키텍처

### 시나리오
✅1. 데모 - 외부 MPC 클라이언트 (Cursor)
```
[Cursor UI]
        ↓ JSON-RPC
[Cursor (MCP Host + LLM Client)]
        ↓ JSON-RPC
[MCP Server (REST Wrapper)] **이걸 만들어서 커서에게 제공**
        ↓ REST
[포털 REST API]
```

✅2. 포탈 - 자체 MPC 클라이언트 시나리오
```
[포털 Web UI]
        ↓
[백엔드(LLM Client)]
        ↓
[MCP Server (REST Wrapper)] JSON-RPC 2.0 -> REST
        ↓
[포털 REST API]
```
*만약 REST API 서버가 별도로 존재하지 않으면 MCP Server가 직접 DB와 연결되어 데이터 제공.

### 도구 선택 흐름 (AI활용 방법)
#### ✅1. 데모 - 외부 MPC 클라이언트 (Cursor)
```
1. Cursor → tools/list 요청
   ↓
2. MCP Server → 도구 목록 반환
   [
     {
       "name": "get_employee_info",
       "description": "직원 정보를 조회합니다...",
       "inputSchema": {...}
     }
   ]
   ↓
3. Cursor LLM → 사용자 메시지 분석
   "홍길동 정보 알려줘"
   ↓
4. LLM → description과 매칭하여 도구 선택
   "get_employee_info" 선택
   ↓
5. Cursor → tools/call 요청
   {
     "name": "get_employee_info",
     "arguments": {"employeeId": "홍길동"}
   }
   ↓
6. MCP Server → 포털 API 호출
```
#### ✅2. 포탈 - 자체 MPC 클라이언트 시나리오 (Spring AI)
```
1. 사용자 → 포털 Web UI
   "홍길동 정보 알려줘"
   ↓
2. 백엔드(Spring Boot + Spring AI) → 의도 분석
   - Spring AI를 통해 LLM 호출
   - 사용자 메시지 분석
   - 도구 목록 조회 (MCP Server에 tools/list 요청)
   - LLM이 도구 선택 및 파라미터 추출
   ↓
3. 백엔드 내부 처리 (Spring AI 활용)
   - LLM 응답 파싱
   - 도구: "get_employee_info"
   - 파라미터: {"employeeId": "홍길동"}
   ↓
4. 백엔드 → MCP Server에 tools/call 요청
   {
     "name": "get_employee_info",
     "arguments": {"employeeId": "홍길동"}
   }
   ↓
5. MCP Server → 포털 REST API 호출
   GET /api/portal/employees/홍길동
   ↓
6. 포털 REST API → 결과 반환
   {"id": "홍길동", "name": "홍길동", "department": "개발팀"}
   ↓
7. MCP Server → 결과를 MCP 형식으로 변환
   ↓
8. 백엔드 → Spring AI로 결과를 자연어로 변환
   "홍길동님은 개발팀 소속입니다."
   ↓
9. 사용자 ← 포털 Web UI에 표시
```

## 🛠️ 구현 기술
### 도구 선택 방법
**도구 선택 기준:**
- ✅ **Tool.description**: LLM이 이 설명을 보고 도구 선택
- ✅ **Tool.inputSchema**: 파라미터 형식 확인
- ✅ **사용자 메시지**: LLM이 자연어로 분석

**예시:**
```java
Tool tool = new Tool(
    "get_employee_info",
    "직원 정보를 조회합니다. 포털 REST API: GET /api/portal/employees/{id}",
    inputSchema
);
```
```json
{
  "tools": [
    {
      "name": "getUser",
      "description": "Get user info by id",
      "input_schema": {...}
    }
  ]
}
```

### Spring AI
**Spring AI**
- LLM과 통신하기 위한 **라이브러리/도구**
- 실제 LLM (Ollama, OpenAI, Gemini 등)과 통신

**Spring AI의 역할:**
- ✅ 사용자 메시지를 LLM에 전달
- ✅ LLM 응답을 받아서 처리
- ✅ 도구 선택, 파라미터 추출, 결과 변환 등

**LLM의 역할 (편의상 Spring AI 역할이라고 설명되기도 한다)**
1. **의도 분석**: 사용자 메시지 → 도구 선택
   - "홍길동 정보 알려줘" → `get_employee_info` 도구 선택
   
2. **파라미터 추출**: 자연어 → 구조화된 파라미터
   - "홍길동 정보 알려줘" → `{employeeId: "홍길동"}`
   
3. **결과를 자연어로 변환**: 도구 결과 → 사용자 친화적 응답
   - `{name: "홍길동", dept: "개발팀"}` → "홍길동님은 개발팀 소속입니다."

**Spring AI가 필요한 경우:**
- 자체 클라이언트를 만들 때 (Cursor 없이)
- MCP Server가 직접 DB 연결할 때
- 자연어를 도구 호출로 변환할 때

**Cursor와의 차이:**
- **Cursor**: 내장 LLM이 자동으로 처리 (Spring AI 불필요)
- **자체 클라이언트**: Spring Boot + Spring AI로 LLM 통신 구현 필요

### 🔑 권한 통제 방식
**Tool 등록 단계에서 통제(AI레벨 통제)**
- 권한별로 다른 Tool목록 제공
- LLM이 민감 API를 알지도 못함 -> 가장 안전

**API 실행 단계에서 통제(백엔드레벨 통제)**
- AI가 이상하게 호출해도 백엔드가 차단
- 기존 API 그대로 이용

### 🔒 보안 고려사항

실제 프로덕션 환경에서는 다음을 고려해야 합니다:

1. **인증/인가**: JWT 토큰, OAuth 등
2. **입력 검증**: 사용자 입력의 유효성 검사
3. **Rate Limiting**: API 호출 제한
4. **로깅**: 모든 요청/응답 로깅
5. **에러 처리**: 민감한 정보 노출 방지
6. **CORS 설정**: 특정 도메인만 허용
