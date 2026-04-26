# 레이어드 아키텍처

## 패키지 구조

```
com.khm1102.mediprice/
├── config/          ← Spring 설정 (AppConfig, WebMvcConfig, WebAppInitializer,
│                       JpaConfig, SecurityConfig, SecurityInitializer, CacheConfig)
├── filter/          ← Servlet Filter (TraceIdFilter, AuthAttributeNames, JwtAuthFilter[P1])
├── controller/      ← @Controller — JSP 페이지 렌더링 (/hospitals/**, /auth/**)
├── api/             ← @RestController — JSON API (/api/**)
├── service/         ← 비즈니스 로직 (Controller + RestController 공유) [P1~]
├── repository/      ← JPA Repository [P1~]
├── entity/          ← JPA Entity (BaseEntity + 도메인 [P1~])
├── dto/             ← 요청/응답 DTO (ApiResponse + 도메인 DTO [P1~])
├── exception/       ← 커스텀 예외 + 글로벌 핸들러
└── util/            ← 유틸 클래스 (JwtUtil, GeoUtil) [P1~]
```

## 요청 흐름

```
브라우저 요청
    ↓
TraceIdFilter (가장 앞 — 모든 로그에 traceId 부여)
    ↓
CharacterEncodingFilter (UTF-8)
    ↓
springSecurityFilterChain (SecurityInitializer가 등록)
    ├── apiSecurityFilterChain  (@Order 1, /api/**)   → 인증 실패 시 JSON 응답
    └── pageSecurityFilterChain (@Order 2, /**)       → 페이지 흐름 (P2에서 redirect)
    ↓
DispatcherServlet
    ↓
┌──────────────────────────────────────┐
│  @Controller         @RestController │
│  JSP 렌더링           JSON 응답       │
│  /hospitals/**       /api/**         │
└──────────────────────────────────────┘
    ↓
Service (비즈니스 로직)
    ↓
Repository (JPA + Hibernate Spatial)
    ↓
PostgreSQL + PostGIS
```

## 컨텍스트 분리 (Root vs Servlet)

순수 Spring + WAR 배포라 root/servlet 컨텍스트가 분리되어 있다.

| 컨텍스트 | 설정 클래스 | 등록 빈 |
|---|---|---|
| **Root** | `AppConfig`, `JpaConfig`, `SecurityConfig`, `CacheConfig` | PSPC, JsonMapper, DataSource, EntityManagerFactory, TransactionManager, CacheManager, SecurityFilterChain ×2, CorsConfigurationSource, EntryPoint, AccessDeniedHandler |
| **Servlet** | `WebMvcConfig` | ViewResolver, Controller/RestController/ControllerAdvice 빈 |

원칙: 컨트롤러는 servlet 컨텍스트에만, 그 외는 root에. 양쪽 ComponentScan은 mirror 관계 (root는 컨트롤러 exclude, servlet은 컨트롤러만 include). JsonMapper처럼 양쪽이 공유하는 빈은 root에 둔다 (자식 컨텍스트가 부모 빈 주입 가능).

## 레이어별 규칙

### Controller / RestController
- Controller와 RestController는 **같은 Service를 공유**한다.
- Controller는 JSP 뷰 이름을 반환하고, RestController는 `ApiResponse<T>`를 반환한다.
- Controller에 비즈니스 로직을 작성하지 않는다.

### Service [P1~]
- `@Service` + `@Transactional` 조합.
- 하나의 Service가 Controller와 RestController 양쪽에서 호출된다.
- 예외는 `MediPriceException`으로 던진다 — `GlobalExceptionHandler`가 처리.

### Repository [P1~]
- Spring Data JPA의 `JpaRepository<Entity, Long>`을 상속한다.
- PostGIS 쿼리는 `@Query` 네이티브 쿼리 + 명명 바인딩(`:lat`, `@Param("lat")`)을 사용한다.

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

> Hibernate 설정에 `hibernate.jdbc.time_zone=UTC` 적용 — `OffsetDateTime`이 컨테이너 TZ에 영향받지 않고 UTC로 정규화되어 저장/조회.

### 논리 삭제

물리 삭제(`DELETE`)를 하지 않고, `deletedDttm`에 삭제 시각을 기록한다.

```java
// 논리 삭제 (CLAUDE.md §5.2: delete = 논리, remove = 물리)
hospital.delete();

// 조회 시 삭제된 데이터 제외
@Query("SELECT h FROM Hospital h WHERE h.deletedDttm IS NULL")
```

### 엔티티 작성 예시

```java
@Entity
@Table(name = "Hospital")
public class Hospital extends BaseEntity {
    // id, createdDttm, updatedDttm, deletedDttm은 BaseEntity에서 상속
    @Column(name = "hira_code", unique = true, nullable = false)
    private String hiraCode;

    @Column(name = "name", nullable = false)
    private String name;
}
```

## ApiResponse — 공통 응답 DTO

모든 REST API(`/api/**`)의 응답을 감싸는 제네릭 래퍼. Java 21 record + Jackson 3 native 지원.

**파일:** `dto/ApiResponse.java`

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ErrorDetail error) {
    public record ErrorDetail(String code, String message) {}

    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> error(ErrorCode code) { ... }
    public static <T> ApiResponse<T> error(ErrorCode code, String detailMessage) { ... }
}
```

> 응답 모델 전용. 외부 API 클라이언트의 입력 모델로 재사용하지 않는다.

### 응답 포맷

`@JsonInclude(NON_NULL)` 적용 — 사용하지 않는 필드(`data`/`error`)는 응답 JSON에서 생략된다.

```json
// 성공
{ "success": true, "data": { ... } }

// 실패
{ "success": false, "error": { "code": "H001", "message": "병원 정보를 찾을 수 없습니다." } }
```

### 사용법

```java
// RestController에서 성공 응답
@GetMapping("/api/hospitals")
public ResponseEntity<ApiResponse<List<HospitalDto>>> searchHospitals(...) {
    List<HospitalDto> result = hospitalService.searchHospitals(...);
    return ResponseEntity.ok(ApiResponse.success(result));
}
```

### 프론트엔드(JS)에서 처리

```js
const result = await api.get('/api/hospitals?lat=37.5&lng=126.9');
if (result.success) {
    renderHospitalList(result.data);
} else {
    alert(result.error.message);          // ErrorDetail.message
    console.warn('error code:', result.error.code);
}
```
