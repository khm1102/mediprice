# 인증/인가 구조

## 현재 상태 (P1 시점)

**비회원/회원 모든 인증 미구현 — 모든 `/api/**` 비인증 통과.**

P0.5에서 SecurityFilterChain을 두 개로 분리하고 EntryPoint/AccessDeniedHandler까지 셋업했지만, JwtAuthFilter/JwtUtil/AuthApiController/Member 엔티티 등 인증 기능 자체는 미구현. 비회원 검색 횟수 제한도 미구현(`GuestSearchLimitExceededException` ErrorCode만 정의된 상태).

> 단계 표기: ✅ 적용 완료 / ⏸ P2 보류 (비회원 별도 미계획)

## 현재 SecurityConfig 구조 (✅ P0.5 적용)

두 개의 `SecurityFilterChain`이 등록되어 있다.

### apiSecurityFilterChain — `/api/**` 전용 (`@Order(1)`)

- `securityMatcher("/api/**")` — REST API만 매칭
- 인증 실패 → `apiAuthenticationEntryPoint`가 **JSON 응답** (`ApiResponse.error`)
- 권한 부족 → `apiAccessDeniedHandler`가 **JSON 응답**
- CSRF 비활성, stateless, CORS 활성
- 화이트리스트: 모든 `/api/**` permitAll (현재 인증 미적용 상태)

### pageSecurityFilterChain — 페이지 흐름 (`@Order(2)`, default 매칭)

- 모든 요청 매칭 (api 체인 다음 평가)
- 현재는 `anyRequest().permitAll()` — JSP 페이지 흐름이 모두 열려 있음
- P2 로그인 도입 시 `/auth/**` 외엔 `authenticated()` + formLogin redirect 추가

> Order가 중요한 이유: page 체인이 먼저 평가되면 모든 요청을 흡수해 api 체인 분리가 무의미해짐.

## SecurityConfig 보조 빈 (✅ 셋업 완료, 현재 동작 안 함)

| 빈 | 역할 | 현 상태 |
|---|---|---|
| `corsConfigurationSource` | CORS 단일 정의 (`@Primary`). `allowedOriginPatterns`/`allowCredentials(true)`/명시 헤더 화이트리스트 | 활성 |
| `apiAuthenticationEntryPoint` | 401 + `ApiResponse.error(ErrorCode)` JSON. request attribute의 ErrorCode 우선 사용 | 등록됨 (호출 X — permitAll이라 인증 분기 안 탐) |
| `apiAccessDeniedHandler` | 403 + `ApiResponse.error(ACCESS_DENIED)` JSON | 등록됨 |
| `AuthAttributeNames.ERROR_CODE` | JwtAuthFilter ↔ EntryPoint 간 ErrorCode 전달 키 (상수) | 정의만 |

## P2 인증 흐름 (계획)

회원 인증을 도입하면 다음 패턴을 따른다.

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

### JWT 토큰 페이로드 (계획)

```json
{
  "sub": "member_123",
  "role": "MEMBER",
  "iat": 1700000000,
  "exp": 1700086400
}
```

### 토큰 저장 (계획)

| 항목 | 값 |
|---|---|
| 저장 방식 | HttpOnly 쿠키 |
| 쿠키 이름 | `ACCESS_TOKEN` |
| HttpOnly | `true` (JS 접근 불가 → XSS 방지) |
| Secure | 운영 환경에서 `true` |
| SameSite | `Strict` 권장 (CSRF 방지) |
| 만료 | `JWT_EXPIRATION` 환경변수 (기본 24시간) |

### 시크릿 정책

- `JWT_SECRET` 환경변수 필수 — `application.yml`에 fallback 없음 → 미지정 시 부팅 실패 (P0.5에서 적용됨)
- 32바이트(256bit) 이상 랜덤 문자열 — `JwtUtil` 작성 시 길이 검증 (P2)
- 생성 예: `openssl rand -base64 48`

## P2 도입 시 필터 체인 (계획)

```
요청 진입
    ↓
TraceIdFilter (✅ 적용 — 모든 로그에 traceId 부여)
    ↓
CharacterEncodingFilter (UTF-8)
    ↓
springSecurityFilterChain
    ├── apiSecurityFilterChain
    │       ↓
    │   JwtAuthFilter (⏸ P2 — addFilterBefore)
    │       ├── 쿠키 ACCESS_TOKEN 추출
    │       ├── JwtUtil.parseToken() 호출
    │       ├── 유효 → UsernamePasswordAuthenticationToken 세팅
    │       └── 만료/변조 → request.setAttribute(AuthAttributeNames.ERROR_CODE, ...)
    │       ↓
    │   AuthenticationEntryPoint (인증 실패 시 JSON)
    │
    └── pageSecurityFilterChain
            ↓
        formLogin redirect 추가 (⏸ P2)
```

## 미구현 클래스 (P2 시 작성 예정)

| 패키지 | 클래스 | 단계 |
|---|---|---|
| `global/filter/` | `JwtAuthFilter` (쿠키 → SecurityContext) | ⏸ P2 |
| `util/` | `JwtUtil` (JWT 생성/검증/길이 검증) | ⏸ P2 |
| `service/` | `AuthService` (login/register/logout) | ⏸ P2 |
| `controller/` | `AuthApiController` (`/api/auth/login`, `/register`, `/logout`) | ⏸ P2 |
| `controller/` | `AuthController` (login/register JSP 렌더링) | ⏸ P2 |
| `entity/` | `Member` (`BaseEntity` 상속, Long id PK) | ⏸ P2 |

## 비회원 / 검색 횟수 제한

**미계획**. P1 초기엔 Guest JWT + 검색 3회 제한 안이 있었으나 폐기. `GuestSearchLimitExceededException`(ErrorCode `G001`) 클래스만 잔존하며 실제 throw하는 코드는 없음. 정책상 P2에서도 재도입 계획 없음.

## 관련 API (현재 vs 계획)

| 메서드 | URL | 설명 | 단계 |
|---|---|---|---|
| POST | `/api/auth/login` | 로그인 → JWT 발급 | ⏸ P2 |
| POST | `/api/auth/logout` | 로그아웃 → 쿠키 삭제 | ⏸ P2 |
| POST | `/api/auth/register` | 회원가입 | ⏸ P2 |
