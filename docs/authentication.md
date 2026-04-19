# 인증/인가 구조

## 개요

MediPrice는 **세션을 사용하지 않고 JWT로 인증을 완전 통일**한다.
회원과 비회원 모두 JWT를 발급받으며, 토큰 종류에 따라 기능 제한이 달라진다.

## 인증 흐름

### 회원

```
1. POST /api/auth/login (이메일 + 비밀번호)
    ↓
2. 서버: 자격 증명 검증 → JWT 발급
    ↓
3. 응답: Set-Cookie로 JWT 저장 (HttpOnly, Secure)
    ↓
4. 이후 모든 요청: 쿠키에 JWT가 자동 포함
    ↓
5. JwtAuthFilter: 쿠키에서 JWT 추출 → 검증 → SecurityContext에 인증 정보 세팅
```

### 비회원

```
1. 첫 접속 시 JWT 없음 감지
    ↓
2. GET /api/auth/token/guest → Guest JWT 발급
    ↓
3. 응답: Set-Cookie로 Guest JWT 저장 (HttpOnly)
    ↓
4. 이후 검색 요청마다 서버에서 검색 횟수 체크
    ↓
5. 3회 초과 → 403 + "로그인이 필요합니다" 응답
```

## JWT 구조

### 토큰 페이로드

```json
// 회원 토큰
{
  "sub": "member_123",
  "role": "MEMBER",
  "iat": 1700000000,
  "exp": 1700086400
}

// 비회원 토큰
{
  "sub": "guest_uuid",
  "role": "GUEST",
  "iat": 1700000000,
  "exp": 1700086400
}
```

### 토큰 저장

| 항목 | 값 |
|------|-----|
| 저장 방식 | HttpOnly 쿠키 |
| 쿠키 이름 | `accessToken` |
| HttpOnly | `true` (JS에서 접근 불가 → XSS 방지) |
| Secure | 배포 환경에서 `true` |
| 만료 | `JWT_EXPIRATION` 환경변수 (기본 24시간) |

## 필터 체인

```
요청 진입
    ↓
JwtAuthFilter (OncePerRequestFilter)
    ├── 쿠키에서 accessToken 추출
    ├── JWT 유효성 검증 (서명, 만료)
    ├── 유효하면 → SecurityContext에 Authentication 세팅
    └── 무효하면 → SecurityContext 비움 (인증 없는 상태로 통과)
    ↓
Spring Security FilterChain
    ├── /api/auth/** → permitAll (인증 불필요)
    ├── /api/** → 인증 필요 (JWT 필수)
    └── /** → permitAll (JSP 페이지)
    ↓
Controller / RestController
```

## 비회원 검색 횟수 제한

### 저장소

```java
// 서버 메모리 — ConcurrentHashMap 사용
// key: guestId (JWT의 sub 클레임)
// value: 검색 횟수 (AtomicInteger)
ConcurrentHashMap<String, AtomicInteger> guestSearchCount;
```

### 동작

| 검색 횟수 | 동작 |
|-----------|------|
| 1~3회 | 정상 검색 결과 반환 |
| 4회 이상 | `GuestSearchLimitExceededException` → 로그인 유도 |

### 초기화 시점
- 서버 재시작 시 초기화 (프로토타입 수준, 별도 영속화 없음)

## 관련 클래스 (구현 예정)

| 패키지 | 클래스 | 역할 |
|--------|--------|------|
| `filter/` | `JwtAuthFilter` | 쿠키에서 JWT 추출 → SecurityContext 세팅 |
| `util/` | `JwtUtil` | JWT 생성, 검증, 클레임 추출 |
| `service/` | `AuthService` | 로그인, 회원가입, Guest 토큰 발급 |
| `api/` | `AuthApiController` | `/api/auth/**` 엔드포인트 |
| `controller/` | `AuthController` | 로그인/회원가입 JSP 페이지 렌더링 |
| `entity/` | `Member` | 회원 엔티티 (BaseEntity 상속) |

## 관련 API

| 메서드 | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/auth/login` | 로그인 → JWT 발급 | 불필요 |
| POST | `/api/auth/logout` | 로그아웃 → 쿠키 삭제 | 불필요 |
| POST | `/api/auth/register` | 회원가입 | 불필요 |
| GET | `/api/auth/token/guest` | 비회원 임시 토큰 발급 | 불필요 |

## SecurityConfig 현재 상태

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                auth.anyRequest().permitAll());
        return http.build();
    }
}
```

현재는 모든 요청 permitAll 상태. JWT 구현 시 다음이 추가된다:
- `JwtAuthFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 등록
- `/api/**` 경로에 인증 요구 설정
- `/api/auth/**`는 permitAll 유지
