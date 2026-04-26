# 예외 처리 구조

## 개요

MediPrice는 **ErrorCode 기반 예외 계층 + 글로벌 핸들러** 패턴을 사용한다.
모든 예외는 `ErrorCode` enum에 정의된 코드를 기반으로 관리되며, 프론트엔드는 에러 코드(`code`)로 분기한다.

```
Service에서 예외 발생
    ↓
throw new HospitalNotFoundException()   ← ErrorCode.HOSPITAL_NOT_FOUND 자동 매핑
    ↓
GlobalExceptionHandler가 catch
    ↓
ApiResponse.error(ErrorCode) → {"success": false, "error": {"code": "H001", "message": "..."}}
```

---

## ErrorCode 체계

**파일:** `global/exception/ErrorCode.java`

에러 코드는 **카테고리 접두사 + 3자리 번호**로 구성한다.

| 접두사 | 카테고리 | 번호 범위 | 설명 |
|--------|----------|-----------|------|
| **C** | Common | C001–C099 | HTTP 표준 에러, 입력값 검증 등 프레임워크 수준 |
| **A** | Auth | A001–A099 | 인증/인가 실패 |
| **G** | Guest | G001–G099 | 비회원 제한 관련 |
| **H** | Hospital | H001–H099 | 병원/가격 도메인 |
| **M** | Member | M001–M099 | 회원 도메인 |

### 코드 선별 기준

- **공통(C):** Spring이 기본 발생시키는 HTTP 에러(404, 405)와 입력값 검증을 코드화
- **인증(A):** JWT 라이프사이클(만료, 위변조)과 로그인 실패를 구분하기 위해 분리
- **비즈니스(G, H, M):** 도메인별로 접두사를 달리해서, 같은 404라도 "뭐가 없는지" 코드만으로 식별 가능

### 현재 등록된 코드

```
C001  서버 내부 오류가 발생했습니다.                              500
C002  입력값이 올바르지 않습니다.                                 400
C003  요청한 리소스를 찾을 수 없습니다.                           404
C004  지원하지 않는 HTTP 메서드입니다.                            405

A001  이메일 또는 비밀번호가 올바르지 않습니다.                    401
A002  토큰이 만료되었습니다.                                     401
A003  유효하지 않은 토큰입니다.                                  401
A004  접근 권한이 없습니다.                                      403
A005  인증이 필요합니다.                                        401   ← P0.5에서 추가

G001  비회원 검색 횟수를 초과했습니다. 로그인 후 이용해주세요.       429

H001  병원 정보를 찾을 수 없습니다.                              404
H002  가격 정보를 찾을 수 없습니다.                              404

M001  회원 정보를 찾을 수 없습니다.                              404
M002  이미 가입된 이메일입니다.                                  409
```

> `A005 UNAUTHORIZED`는 인증 자체가 누락된 경우(쿠키 없음/익명) 사용. 토큰 만료(A002)/변조(A003)와 구분.

### 새 코드 추가 시 규칙

1. 해당 카테고리의 마지막 번호 다음 번호를 사용한다.
2. `ErrorCode` enum에 추가하고, 대응하는 leaf 예외 클래스를 해당 서브패키지에 생성한다.
3. 메시지는 **사용자에게 보여줄 수 있는 한국어**로 작성한다.

---

## 예외 계층 구조

```
MediPriceException (abstract, ErrorCode 보유)
├── business/BusinessException
│   ├── HospitalNotFoundException     → H001
│   ├── PriceNotFoundException        → H002
│   ├── MemberNotFoundException       → M001
│   ├── DuplicateEmailException       → M002
│   └── GuestSearchLimitExceededException → G001
└── auth/AuthenticationException
    ├── LoginFailedException             → A001
    ├── TokenExpiredException            → A002
    ├── TokenInvalidException            → A003
    └── AuthorizationDeniedException     → A004
```

> Spring Security의 `org.springframework.security.access.AccessDeniedException`과 이름이 겹치지 않도록 우리 도메인 예외는 `AuthorizationDeniedException`으로 명명. Security 단계 인가 실패는 `SecurityConfig.apiAccessDeniedHandler`가 직접 처리하고, 본 예외는 `GlobalExceptionHandler.handleMediPriceException`이 처리한다.

### 패키지 구조

```
global/exception/
├── ErrorCode.java              ← 에러 코드 enum
├── MediPriceException.java     ← abstract 베이스 (ErrorCode 필드)
├── GlobalExceptionHandler.java ← @RestControllerAdvice
├── auth/                       ← LoginFailed, TokenExpired/Invalid, AuthorizationDenied, AuthenticationException
└── business/                   ← BusinessException, HospitalNotFound, PriceNotFound, MemberNotFound, DuplicateEmail, GuestSearchLimitExceeded
```

### 설계 원칙

- **MediPriceException:** abstract. 직접 인스턴스화 금지. 반드시 하위 클래스를 통해 사용.
- **중간 클래스 (BusinessException, AuthenticationException):** catch 블록에서 카테고리 단위로 잡을 수 있게 하는 용도.
- **leaf 클래스:** 기본 생성자에서 해당 ErrorCode를 부모에 전달. 1~2줄로 유지.
- **ErrorCode와 1:1 대응:** 각 leaf 예외는 하나의 ErrorCode에 매핑.

### 사용 예시

```java
// Service에서 사용
public Hospital lookupHospital(Long id) {
    return hospitalRepository.findById(id)
            .orElseThrow(HospitalNotFoundException::new);
}

// 카테고리 단위 catch가 필요할 때
try {
    hospitalService.lookupHospital(id);
} catch (BusinessException e) {
    // 모든 비즈니스 예외를 한번에 처리
}
```

---

## GlobalExceptionHandler

**파일:** `global/exception/GlobalExceptionHandler.java` (단위 테스트: `test/.../global/exception/GlobalExceptionHandlerTest`)

### 핸들러 매핑

| 핸들러 | 대상 예외 | 로그 레벨 | HTTP 상태 |
|--------|-----------|-----------|-----------|
| `handleMediPriceException` | `MediPriceException` 하위 전체 | 4xx → WARN, 5xx → ERROR (자동 격상) | ErrorCode 정의값 |
| `handleValidation` | `MethodArgumentNotValidException` | WARN | 400 (필드 오류 메시지 포함) |
| `handleBind` | `BindException` | WARN | 400 |
| `handleMissingParam` | `MissingServletRequestParameterException` | WARN | 400 |
| `handleTypeMismatch` | `MethodArgumentTypeMismatchException` | WARN | 400 (예: `lat=abc`) |
| `handleNotReadable` | `HttpMessageNotReadableException` | WARN | 400 (요청 본문 파싱 실패) |
| `handleNotFound` | `NoResourceFoundException`, `NoHandlerFoundException` | WARN | 404 |
| `handleMethodNotAllowed` | `HttpRequestMethodNotSupportedException` | WARN | 405 |
| `handleRuntime` | `RuntimeException` (위 핸들러에 안 잡힌 것) | ERROR + stacktrace | 500 |

> Spring Security의 `AccessDeniedException`은 GlobalExceptionHandler에 등록하지 않음 — `SecurityConfig.apiAccessDeniedHandler`가 필터 체인 단계에서 직접 처리한다.

### 로그 포맷 통일

모든 핸들러는 다음 단일 포맷을 따른다:

```
[{ErrorCode}] {category}: {detail}
```

내부 헬퍼 `logWarn`/`logError`로 통일:

```java
private void logWarn(ErrorCode code, String category, Object detail) {
    log.warn("[{}] {}: {}", code.getCode(), category, detail);
}

private void logError(ErrorCode code, String category, Object detail, Throwable cause) {
    log.error("[{}] {}: {}", code.getCode(), category, detail, cause);
}
```

예시 로그:
```
[H001] Domain error: 병원 정보를 찾을 수 없습니다.
[C002] Validation failed: lat: must not be null
[C002] Type mismatch: lat (Double) 변환 실패: abc
[C003] No handler: No endpoint GET /unknown.
[C001] Unhandled runtime: NullPointerException at ...
```

### 5xx 자동 격상

`handleMediPriceException`은 `errorCode.getHttpStatus().is5xxServerError()`를 검사해 5xx면 `log.error`로 자동 격상. 4xx는 WARN 유지.

---

## 응답 형식

`ApiResponse`는 `@JsonInclude(NON_NULL)` 적용 — 사용 안 하는 필드는 응답에서 생략.

### 에러 응답 (ErrorCode 기반)

```
HTTP/1.1 404 Not Found
Content-Type: application/json
X-Trace-Id: a95790918534483a83089169b4cc329d

{
  "success": false,
  "error": {
    "code": "H001",
    "message": "병원 정보를 찾을 수 없습니다."
  }
}
```

### 서버 에러 (handleRuntime)

```
HTTP/1.1 500 Internal Server Error
Content-Type: application/json

{
  "success": false,
  "error": {
    "code": "C001",
    "message": "서버 내부 오류가 발생했습니다."
  }
}
```

### 입력 검증 에러 (detail message 포함)

```
HTTP/1.1 400 Bad Request

{
  "success": false,
  "error": {
    "code": "C002",
    "message": "lat: must not be null"
  }
}
```

---

## 규칙

1. **Service 레이어**에서만 도메인 예외를 던진다 — Controller에서 직접 던지지 않는다.
2. `new MediPriceException(...)` 직접 사용 금지 — 반드시 leaf 클래스를 사용한다.
3. 예외 메시지는 기본적으로 `ErrorCode`에 정의된 메시지를 사용한다. 상세 메시지가 필요하면 `(ErrorCode, String detailMessage)` 생성자를 사용할 수 있다.
4. 프론트엔드는 `error.code`로 분기한다. `error.message`는 사용자 표시용.
5. 새 도메인이 추가되면 접두사를 새로 할당하고, 이 문서에 등록한다.
6. **Spring Security 예외**(인증/인가 단계 실패)는 SecurityConfig의 EntryPoint/AccessDeniedHandler가 처리하며, GlobalExceptionHandler에는 등록하지 않는다.
