# 인증/인가 구조

## 개요

MediPrice는 **세션을 사용하지 않고 JWT로 인증을 완전 통일**한다.
회원과 비회원 모두 JWT를 발급받으며, 토큰 종류에 따라 기능 제한이 달라진다.

> 단계 표기: ✅ 적용 완료 / ⏳ P1 예정 / ⏸ P2 보류

## 현재 SecurityConfig 구조 (✅ P0.5 적용)

두 개의 `SecurityFilterChain`으로 분리되어 등록된다.

### apiSecurityFilterChain — `/api/**` 전용 (`@Order(1)`)

- `securityMatcher("/api/**")` — REST API만 매칭
- 인증 실패 → `apiAuthenticationEntryPoint`가 **JSON 응답** (`ApiResponse.error`)
- 권한 부족 → `apiAccessDeniedHandler`가 **JSON 응답**
- CSRF 비활성, stateless, CORS 활성
- 화이트리스트: `/api/auth/token/guest`, `/api/hospitals/**`, `/api/health` → permitAll
- 그 외 `/api/**` → `authenticated`

### pageSecurityFilterChain — 페이지 흐름 (`@Order(2)`, default 매칭)

- 모든 요청 매칭 (api 체인 다음 평가)
- 현재는 `anyRequest().permitAll()` — JSP 페이지 흐름이 모두 열려 있음
- P2 로그인 도입 시 `/auth/**` 외엔 `authenticated()` + formLogin redirect 추가

> Order가 중요한 이유: page 체인이 먼저 평가되면 모든 요청을 흡수해 api 체인 분리가 무의미해짐.

## 인증 흐름

### 비회원 (⏳ P1)

```
1. 첫 접속 시 JWT 없음 감지 (브라우저 측 JS 또는 API 첫 호출)
    ↓
2. GET /api/auth/token/guest → Guest JWT 발급
    ↓
3. 응답: Set-Cookie로 Guest JWT 저장 (HttpOnly, SameSite=Strict)
    ↓
4. 이후 검색 요청마다 GuestSearchCounter가 횟수 체크
    ↓
5. 3회 초과 → 429 + ErrorCode G001 ("로그인이 필요합니다")
```

### 회원 (⏸ P2)

```
1. POST /api/auth/login (이메일 + 비밀번호)
    ↓
2. 서버: BCrypt로 자격 증명 검증 → JWT 발급
    ↓
3. 응답: Set-Cookie로 JWT 저장 (HttpOnly, Secure, SameSite=Strict)
    ↓
4. 이후 모든 요청: 쿠키에 JWT가 자동 포함
    ↓
5. JwtAuthFilter: 쿠키에서 JWT 추출 → 검증 → SecurityContext에 인증 정보 세팅
```

## JWT 구조

### 토큰 페이로드

```json
// 비회원 토큰 (P1)
{
  "sub": "guest_uuid",
  "role": "GUEST",
  "iat": 1700000000,
  "exp": 1700086400
}

// 회원 토큰 (P2)
{
  "sub": "member_123",
  "role": "MEMBER",
  "iat": 1700000000,
  "exp": 1700086400
}
```

### 토큰 저장

| 항목 | 값 |
|------|-----|
| 저장 방식 | HttpOnly 쿠키 |
| 쿠키 이름 | `ACCESS_TOKEN` |
| HttpOnly | `true` (JS 접근 불가 → XSS 방지) |
| Secure | 운영 환경에서 `true` |
| SameSite | `Strict` 권장 (CSRF 방지) |
| 만료 | `JWT_EXPIRATION` 환경변수 (기본 24시간) |

### 시크릿 정책

- `JWT_SECRET` 환경변수 필수 — `application.yml`에 fallback 없음 → 미지정 시 부팅 실패
- 32바이트(256bit) 이상 랜덤 문자열 — `JwtUtil` 작성 시 길이 검증 (P1)
- 생성 예: `openssl rand -base64 48`

## 필터 체인 (P1 완성 형태)

```
요청 진입
    ↓
TraceIdFilter (이미 적용 — 모든 로그에 traceId 부여)
    ↓
CharacterEncodingFilter (UTF-8)
    ↓
springSecurityFilterChain
    ├── apiSecurityFilterChain
    │       ↓
    │   JwtAuthFilter (P1 추가 예정 — addFilterBefore)
    │       ├── 쿠키 ACCESS_TOKEN 추출
    │       ├── JwtUtil.parseToken() 호출
    │       ├── 유효 → UsernamePasswordAuthenticationToken 세팅
    │       └── 만료/변조 → request.setAttribute(AuthAttributeNames.ERROR_CODE, ...)
    │       ↓
    │   AuthenticationEntryPoint (인증 실패 시 JSON)
    │
    └── pageSecurityFilterChain
            ↓
        (P2에서 formLogin redirect 추가)
```

> P1 시점에는 JwtAuthFilter가 토큰 없는 경우도 통과시키는 **옵셔널 인증** — `/api/hospitals/**`는 비회원도 접근 가능해야 하기 때문. 횟수 제한은 `GuestSearchCounter`가 별도로 강제.

## 비회원 검색 횟수 제한 (⏳ P1)

### 저장소

```java
// 서버 메모리 — 단일 인스턴스 가정
ConcurrentHashMap<String, AtomicInteger> guestSearchCount;
// key: guestId (JWT의 sub 클레임)
// value: 검색 횟수 (AtomicInteger)
```

### 동작

| 검색 횟수 | 동작 |
|-----------|------|
| 1~3회 | 정상 검색 결과 반환 |
| 4회 이상 | `GuestSearchLimitExceededException` → 429 + ErrorCode G001 |

### 한계

- **단일 인스턴스 운영 가정** — 다중 인스턴스 배포 시 인스턴스마다 카운터가 따로라 정책 우회 가능
- 서버 재시작 시 초기화 (별도 영속화 없음)
- TTL 미적용 (메모리 누수 방지를 위해 ScheduledExecutorService로 주기 소거 검토)

## SecurityConfig 보조 빈

| 빈 | 역할 |
|---|---|
| `corsConfigurationSource` | CORS 단일 정의 (@Primary). `allowedOriginPatterns`/`allowCredentials(true)`/명시 헤더 화이트리스트 |
| `apiAuthenticationEntryPoint` | 401 + `ApiResponse.error(ErrorCode)` JSON. request attribute의 ErrorCode 우선 사용 |
| `apiAccessDeniedHandler` | 403 + `ApiResponse.error(ACCESS_DENIED)` JSON |
| `AuthAttributeNames.ERROR_CODE` | JwtAuthFilter ↔ EntryPoint 간 ErrorCode 전달 키 (상수) |

## 관련 클래스

| 패키지 | 클래스 | 상태 |
|--------|--------|------|
| `config/` | `SecurityConfig` (apiChain + pageChain + CORS + EntryPoint) | ✅ |
| `filter/` | `AuthAttributeNames` (request attribute 키 상수) | ✅ |
| `filter/` | `JwtAuthFilter` (쿠키 → SecurityContext) | ⏳ P1 |
| `util/` | `JwtUtil` (JWT 생성/검증/길이 검증) | ⏳ P1 |
| `service/` | `GuestSearchCounter` (검색 횟수 강제) | ⏳ P1 |
| `service/` | `AuthService` (login/register/logout) | ⏸ P2 |
| `api/` | `AuthApiController` (`/api/auth/token/guest`) | ⏳ P1 (login/register는 P2) |
| `controller/` | `AuthController` (login/register JSP) | ⏸ P2 |
| `entity/` | `Member` | ⏸ P2 |

## 관련 API

| 메서드 | URL | 설명 | 단계 |
|--------|-----|------|------|
| GET | `/api/auth/token/guest` | 비회원 임시 토큰 발급 | ⏳ P1 |
| POST | `/api/auth/login` | 로그인 → JWT 발급 | ⏸ P2 |
| POST | `/api/auth/logout` | 로그아웃 → 쿠키 삭제 | ⏸ P2 |
| POST | `/api/auth/register` | 회원가입 | ⏸ P2 |
