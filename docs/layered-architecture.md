# 레이어드 아키텍처

## 패키지 구조

```
com.khm1102.mediprice/
├── config/          ← Spring 설정 (AppConfig, JpaConfig, SecurityConfig 등)
├── filter/          ← Servlet Filter (JwtAuthFilter)
├── controller/      ← @Controller — JSP 페이지 렌더링 (/hospital/**, /auth/**)
├── api/             ← @RestController — JSON API (/api/**)
├── service/         ← 비즈니스 로직 (Controller + RestController 공유)
├── repository/      ← JPA Repository
├── entity/          ← JPA Entity
├── dto/             ← 요청/응답 DTO
├── exception/       ← 커스텀 예외 + 글로벌 핸들러
└── util/            ← 유틸 클래스 (JwtUtil, GeoUtil)
```

## 요청 흐름

```
브라우저 요청
    ↓
JwtAuthFilter (Spring Security FilterChain)
    ↓
Spring Security (인증 여부 판단)
    ↓
┌──────────────────────────────────────┐
│  @Controller         @RestController │
│  JSP 렌더링           JSON 응답       │
│  /hospital/**        /api/**         │
└──────────────────────────────────────┘
    ↓
Service (비즈니스 로직)
    ↓
Repository (JPA)
    ↓
PostgreSQL + PostGIS
```

## 레이어별 규칙

### Controller / RestController
- Controller와 RestController는 **같은 Service를 공유**한다.
- Controller는 JSP 뷰 이름을 반환하고, RestController는 `ApiResponse<T>`를 반환한다.
- Controller에 비즈니스 로직을 작성하지 않는다.

### Service
- `@Service` + `@Transactional` 조합.
- 하나의 Service가 Controller와 RestController 양쪽에서 호출된다.
- 예외는 `MediPriceException`으로 던진다 — GlobalExceptionHandler가 처리.

### Repository
- Spring Data JPA의 `JpaRepository<Entity, Long>`을 상속한다.
- PostGIS 쿼리는 `@Query` 네이티브 쿼리로 작성한다.

### Entity
- 모든 엔티티는 `BaseEntity`를 상속한다.
- 논리 삭제는 `deletedDttm` 필드 사용 (null이면 활성 상태).

## BaseEntity — 공통 감사 필드

모든 엔티티가 상속하는 추상 클래스. 테이블마다 공통으로 필요한 id와 시간 필드를 제공한다.

**파일:** `entity/BaseEntity.java`

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_dttm", nullable = false, updatable = false)
    private OffsetDateTime createdDttm;

    @Column(name = "updated_dttm", nullable = false)
    private OffsetDateTime updatedDttm;

    @Column(name = "deleted_dttm")
    private OffsetDateTime deletedDttm;   // null이면 활성 상태
}
```

### 자동 시간 세팅

| 콜백 | 시점 | 동작 |
|------|------|------|
| `@PrePersist` | INSERT 직전 | `createdDttm`, `updatedDttm` = 현재 시각 |
| `@PreUpdate` | UPDATE 직전 | `updatedDttm` = 현재 시각 |

### 논리 삭제

물리 삭제(`DELETE`)를 하지 않고, `deletedDttm`에 삭제 시각을 기록한다.

```java
// 삭제 처리
hospital.softDelete();

// 조회 시 삭제된 데이터 제외
@Query("SELECT h FROM Hospital h WHERE h.deletedDttm IS NULL")
```

### 엔티티 작성 예시

```java
@Entity
@Table(name = "Hospital")
public class Hospital extends BaseEntity {
    // id, createdDttm, updatedDttm, deletedDttm은 BaseEntity에서 상속
    // 엔티티 고유 필드만 선언
    @Column(name = "hira_code", unique = true, nullable = false)
    private String hiraCode;

    @Column(name = "name", nullable = false)
    private String name;
}
```

## ApiResponse — 공통 응답 DTO

모든 REST API(`/api/**`)의 응답을 감싸는 제네릭 래퍼.

**파일:** `dto/ApiResponse.java`

### 응답 포맷

```json
// 성공
{ "success": true,  "data": { ... }, "error": null }

// 실패
{ "success": false, "data": null,    "error": "에러 메시지" }
```

### 사용법

```java
// RestController에서 성공 응답
@GetMapping("/api/hospitals")
public ResponseEntity<ApiResponse<List<HospitalDto>>> searchHospitals(...) {
    List<HospitalDto> result = hospitalService.searchHospitals(...);
    return ResponseEntity.ok(ApiResponse.success(result));
}

// 데이터 없이 성공
return ResponseEntity.ok(ApiResponse.success(null));
```

### 프론트엔드(JS)에서 처리

```js
const result = await api.get('/api/hospitals?lat=37.5&lng=126.9');
if (result.success) {
    renderHospitalList(result.data);
} else {
    alert(result.error);
}
```
