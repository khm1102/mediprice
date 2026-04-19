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
ApiResponse.error(ErrorCode) → {"error": {"code": "H001", "message": "..."}}
```

---

## ErrorCode 체계

**파일:** `exception/ErrorCode.java`

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
C002  입력값이 올바르지 않습니다.                                400
C003  요청한 리소스를 찾을 수 없습니다.                          404
C004  지원하지 않는 HTTP 메서드입니다.                           405

A001  이메일 또는 비밀번호가 올바르지 않습니다.                   401
A002  토큰이 만료되었습니다.                                    401
A003  유효하지 않은 토큰입니다.                                 401
A004  접근 권한이 없습니다.                                     403

G001  비회원 검색 횟수를 초과했습니다. 로그인 후 이용해주세요.      429

H001  병원 정보를 찾을 수 없습니다.                              404
H002  가격 정보를 찾을 수 없습니다.                              404

M001  회원 정보를 찾을 수 없습니다.                              404
M002  이미 가입된 이메일입니다.                                  409
```

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
    ├── LoginFailedException          → A001
    ├── TokenExpiredException         → A002
    ├── TokenInvalidException         → A003
    └── AccessDeniedException         → A004
```

### 패키지 구조

```
exception/
├── ErrorCode.java              ← 에러 코드 enum
├── MediPriceException.java     ← abstract 베이스 (ErrorCode 필드)
├── GlobalExceptionHandler.java ← @RestControllerAdvice
├── auth/                       ← 인증 관련 예외
│   ├── AuthenticationException.java
│   ├── AccessDeniedException.java
│   ├── LoginFailedException.java
│   ├── TokenExpiredException.java
│   └── TokenInvalidException.java
└── business/                   ← 비즈니스 로직 예외
    ├── BusinessException.java
    ├── DuplicateEmailException.java
    ├── GuestSearchLimitExceededException.java
    ├── HospitalNotFoundException.java
    ├── MemberNotFoundException.java
    └── PriceNotFoundException.java
```

### 설계 원칙

- **MediPriceException:** abstract. 직접 인스턴스화 금지. 반드시 하위 클래스를 통해 사용.
- **중간 클래스 (BusinessException, AuthenticationException):** catch 블록에서 카테고리 단위로 잡을 수 있게 하는 용도.
- **leaf 클래스:** 기본 생성자에서 해당 ErrorCode를 부모에 전달. 1~2줄로 유지.
- **ErrorCode와 1:1 대응:** 각 leaf 예외는 하나의 ErrorCode에 매핑. 같은 코드를 여러 예외가 공유하지 않는다.

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

**파일:** `exception/GlobalExceptionHandler.java`

### 핸들러 우선순위

| 순서 | 핸들러 | 대상 예외 | 로그 레벨 | HTTP 상태 |
|------|--------|-----------|-----------|-----------|
| 1 | `handleMediPriceException` | `MediPriceException` 하위 전체 | `WARN` 한 줄 | ErrorCode에 정의된 상태 |
| 2 | `handleValidation` | `MethodArgumentNotValidException` | `WARN` 한 줄 | 400 (필드 오류 메시지 포함) |
| 3 | `handleAccessDenied` | Spring Security `AccessDeniedException` | `WARN` 한 줄 | 403 |
| 4 | `handleNotFound` | `NoResourceFoundException` / `NoHandlerFoundException` | `WARN` 한 줄 | 404 |
| 5 | `handleMethodNotAllowed` | `HttpRequestMethodNotSupportedException` | `WARN` 한 줄 | 405 |
| 6 | `handleException` | 나머지 `Exception` | `ERROR` + 스택트레이스 | 500 |

### 로그 레벨 분리 기준

- **WARN (1~5번):** 클라이언트 실수이거나 예상된 비즈니스 예외. 한 줄 로그로 충분.
- **ERROR (6번):** 여기에 도달하면 **진짜 서버 버그**. 스택트레이스를 포함해서 즉시 조사 필요.

---

## 응답 형식

### 에러 응답 (ErrorCode 기반)

```
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "success": false,
  "data": null,
  "error": {
    "code": "H001",
    "message": "병원 정보를 찾을 수 없습니다."
  }
}
```

### 서버 에러 (5번 핸들러)

```
HTTP/1.1 500 Internal Server Error
Content-Type: application/json

{
  "success": false,
  "data": null,
  "error": {
    "code": "C001",
    "message": "서버 내부 오류가 발생했습니다."
  }
}
```

---

## 규칙

1. **Service 레이어**에서만 예외를 던진다 — Controller에서 직접 예외를 던지지 않는다.
2. `new MediPriceException(...)` 직접 사용 금지 — 반드시 leaf 클래스를 사용한다.
3. 예외 메시지는 기본적으로 `ErrorCode`에 정의된 메시지를 사용한다. 상세 메시지가 필요하면 `(ErrorCode, String detailMessage)` 생성자를 사용할 수 있다.
4. 프론트엔드는 `error.code`로 분기한다. `error.message`는 사용자 표시용.
5. 새 도메인이 추가되면 접두사를 새로 할당하고, 이 문서에 등록한다.
