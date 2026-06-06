# 인증/인가 구조

## 현재 상태 (2026-06-06)

MediPrice는 Spring Security를 stateless로 구성하고, JWT를 `mp_token` 쿠키에 저장해 인증한다.

- 공개 API: `/api/hospitals/**`, `/api/items`, `/api/health`, `/api/auth/token/guest`, `/api/auth/logout`
- 운영/디버그 API: `/api/internal/**`는 SecurityConfig에서는 `permitAll`이지만 `BatchAdminGuard`가 `batch.admin-enabled`와 `X-Batch-Admin-Secret`을 검사한다.
- 회원 API: `/api/favorites/**`, `/api/auth/me`
- 페이지: 현재 `pageSecurityFilterChain`에서 전체 `permitAll`
- 로그인: Google OAuth2 → 약관 동의 → JWT 쿠키 발급
- 검색 API는 공개 접근 가능하다.

주의: `/api/internal/**`는 컨트롤러 단 가드가 1차 보호를 담당한다. 운영에서는 Spring Security `ADMIN` role 또는 인프라 IP 제한을 이중 방어로 추가하는 것이 좋다.

## SecurityConfig 구조

두 개의 `SecurityFilterChain`이 등록되어 있다.

### apiSecurityFilterChain — `/api/**` 전용 (`@Order(1)`)

- `securityMatcher("/api/**")`
- CSRF 비활성, stateless, CORS 활성
- `JwtAuthFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 등록
- 인증 실패는 `apiAuthenticationEntryPoint`가 JSON `ApiResponse.error`로 응답
- 권한 부족은 `apiAccessDeniedHandler`가 JSON `ApiResponse.error`로 응답

### pageSecurityFilterChain — 페이지 흐름 (`@Order(2)`)

- API 체인 다음에 평가되는 기본 체인
- 현재 모든 JSP 페이지 요청을 `permitAll`
- 페이지 접근 제어가 필요해지면 `/auth/**`, `/legal/**`, 정적 리소스를 제외하고 인증 정책을 추가한다.

## JWT 흐름

### 회원 로그인

```
GET /auth/oauth2/authorize/google
    ↓
oauth2_state HttpOnly 쿠키 발급 후 Google 인증 페이지로 redirect
    ↓
GET /auth/oauth2/callback?code=...&state=...
    ↓
GoogleOAuthService.exchangeCodeForUserInfo()
    ↓
기존 회원이면 JWT 발급, 신규 회원이면 /auth/consent로 이동
    ↓
POST /auth/consent
    ↓
Member 생성 또는 재활성화 후 mp_token HttpOnly 쿠키 저장
```

### 게스트 토큰

`GET /api/auth/token/guest`는 `role=GUEST` JWT를 발급한다.

### 토큰 페이로드

회원:

```json
{
  "sub": "1",
  "email": "user@example.com",
  "role": "MEMBER",
  "name": "홍길동",
  "iat": 1700000000,
  "exp": 1700086400
}
```

게스트:

```json
{
  "sub": "uuid",
  "role": "GUEST",
  "iat": 1700000000,
  "exp": 1700086400
}
```

## 쿠키 정책

| 쿠키 | 용도 | 현재 상태 |
|---|---|---|
| `mp_token` | 회원/게스트 JWT | `Path=/`, `Max-Age`, `HttpOnly=true`, `Secure=COOKIE_SECURE`, `SameSite=COOKIE_SAME_SITE` |
| `oauth2_state` | OAuth state 검증 | HttpOnly, 5분 만료 |
| `consent_key` | 신규 회원 약관 동의 임시 키 | HttpOnly, `/auth/consent`, 10분 만료 |

`mp_token`은 `MpTokenCookieFactory`가 `ResponseCookie`로 발급한다. 로그인 상태 표시는 JS가 쿠키를 직접 디코딩하지 않고 `/api/auth/me` 응답을 `authReady`에서 캐싱한다. 운영 HTTPS에서는 `COOKIE_SECURE=true`를 권장한다.

## 회원 도메인

- `Member`: Google OAuth 계정, role, 약관 동의 시각, soft delete 지원
- `AuthService`: OAuth 로그인 처리, 신규 회원 등록, 탈퇴 처리
- `Favorite`: 회원별 병원 즐겨찾기, soft delete/restore
- `FavoriteApiController`: `/api/favorites/**`

게스트 JWT는 `ROLE_GUEST` 권한으로 SecurityContext에 들어갈 수 있다. 회원 전용 API(`/api/favorites/**`, `/api/auth/me`)는 `ROLE_MEMBER`만 허용한다.

## 관련 API

| 메서드 | URL | 설명 | 접근 |
|---|---|---|---|
| GET | `/auth/oauth2/authorize/google` | Google OAuth 시작 | 공개 |
| GET | `/auth/oauth2/callback` | Google OAuth 콜백 | 공개 |
| GET | `/auth/consent` | 신규 회원 약관 동의 페이지 | 공개 |
| POST | `/auth/consent` | 약관 동의 후 회원 등록 | 공개 |
| POST | `/api/auth/logout` | JWT 쿠키 삭제 | 공개 |
| GET | `/api/auth/token/guest` | 게스트 JWT 발급 | 공개 |
| GET | `/api/auth/me` | 현재 회원 정보 | 회원 |
| DELETE | `/api/auth/me` | 회원 탈퇴 | 회원 |
| GET | `/api/favorites` | 즐겨찾기 목록 | 회원 |
| POST | `/api/favorites` | 즐겨찾기 추가 | 회원 |
| DELETE | `/api/favorites/{ykiho}` | 즐겨찾기 삭제 | 회원 |
| GET | `/api/favorites/{ykiho}/status` | 즐겨찾기 여부 | 회원 |
