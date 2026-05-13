# 레이어드 아키텍처

## 패키지 구조

```
com.khm1102.mediprice/
├── controller/           @RestController + @Controller (전부 한 폴더 평면 배치)
│   ├── HospitalApiController       /api/hospitals + /{ykiho}
│   ├── NonPayItemApiController     /api/items
│   ├── HealthApiController         /api/health (JSON probe)
│   ├── HealthController            /health (JSP)
│   └── (BatchAdminApiController는 batch/ 폴더 — 운영 묶음)
├── service/              도메인 서비스 (비즈니스 로직)
│   ├── NonPayItemService           항목 그룹핑 + 코드→이름 룩업
│   ├── HospitalService             PostGIS 프로시저 호출 + JSON 파싱
│   └── HospitalDetailService       DB(병원/가격) + 외부 4개 API 병합
├── entity/               도메인 엔티티 (자체 PK)
│   ├── Hospital                    ykiho PK + native UPDATE location
│   ├── NonPayItem                  npay_cd PK
│   ├── Price                       @IdClass(PriceId) ykiho+npay_cd
│   └── PriceId
├── repository/           Spring Data JPA
│   ├── HospitalRepository          findAllYkiho, searchNearbyJson(::text), updateLocation
│   ├── NonPayItemRepository
│   └── PriceRepository             findAllByYkiho
├── dto/                  도메인 DTO (record)
│   ├── HospitalSummaryDto          /api/hospitals 응답 element
│   ├── HospitalDetailDto           /api/hospitals/{ykiho} 응답 (PriceItem, TrnsprtInfo 중첩)
│   └── NonPayItemGroupDto          /api/items 응답 (Item 중첩)
├── batch/                운영 묶음 — 스케줄러로 도는 모든 것 평면
│   ├── BatchAdminApiController     POST /api/internal/batch/sync, /sync/prices (디버그 트리거)
│   ├── BatchService                @Scheduled(cron "0 0 0 1 * *") 매월 1일
│   ├── NonPayItemSyncService       Step 1
│   ├── HospitalSyncService         Step 2 (SidoCode 17개 순회)
│   ├── PriceSyncService            Step 3 — Hospital.findAllYkiho() 순회
│   ├── PriceYkihoSyncService       ykiho당 별도 트랜잭션
│   └── SidoCode                    17개 시도 enum
├── client/               외부 API 통합
│   ├── HiraHospitalClient          getHospBasisList (시도+페이징)
│   ├── HiraNonPayClient            getNonPaymentItemCodeList2, getNonPaymentItemHospDtlList
│   ├── HiraDetailClient            의료기관 상세 4개 API CompletableFuture 병렬
│   └── hira/                       외부 응답 XML DTO
│       ├── HiraResponse/Header/Body  (generic wrapper, Body는 record 아닌 class — XmlMapper 충돌 회피)
│       ├── HospBasisItem
│       ├── NonPayCodeItem, NonPayDtlItem (ACTIVE_END_DATE='99991231')
│       ├── DgsbjtItem, MedOftItem
│       └── TrnsprtItem, SpclDiagItem
└── global/               cross-cutting 인프라 (한 곳에 묶음)
    ├── common/
    │   └── ApiResponse             record + ErrorDetail 중첩
    ├── config/
    │   ├── AppConfig               @EnableScheduling, JsonMapper, XmlMapper, hiraDetailExecutor
    │   ├── WebMvcConfig            @EnableWebMvc, JSON 컨버터, ContentNegotiation JSON-only
    │   ├── WebAppInitializer       TraceIdFilter + CharacterEncodingFilter
    │   ├── JpaConfig               EntityManagerFactory, Hikari, UTC
    │   ├── SecurityConfig          apiSecurityFilterChain @Order 1 + pageSecurityFilterChain @Order 2
    │   ├── SecurityInitializer
    │   ├── CacheConfig             ConcurrentMapCacheManager
    │   └── DatabaseInitializer     @PostConstruct PostGIS extension/location 컬럼/GIST/procedures.sql
    ├── exception/
    │   ├── ErrorCode               C/A/G/H/M 카테고리
    │   ├── MediPriceException      추상
    │   ├── GlobalExceptionHandler  5xx vs 4xx 분기, BindException 첫 필드에러
    │   ├── auth/                   LoginFailed, TokenExpired/Invalid, AuthorizationDenied, Authentication
    │   └── business/               HospitalNotFound, PriceNotFound, MemberNotFound, DuplicateEmail, GuestSearchLimitExceeded, BusinessException
    ├── filter/
    │   ├── TraceIdFilter           32-hex UUID, X-Trace-Id 양방향, MDC, 위험문자 sanitize
    │   └── AuthAttributeNames      request attribute 키 상수
    └── entity/
        ├── BaseEntity              Long id PK + 시간 필드 (미래 Member용)
        └── AbstractAuditEntity     시간 필드만 (자체 PK 도메인 엔티티용)

src/main/resources/sql/procedures.sql  ← search_nearby_hospitals (PostGIS PL/pgSQL, JSON 반환)
```

**구조 결정 이력**: 한때 DDD-스타일 도메인 폴더(`hospital/`, `nonpayitem/`, `price/`)로 옮겼다가 over-engineering 판단으로 layered로 되돌림 — 도메인 4~5개 규모엔 평면 layered가 가장 빠른 탐색.

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
    └── pageSecurityFilterChain (@Order 2, /**)       → 페이지 흐름 (현재 permitAll)
    ↓
DispatcherServlet (ContentNegotiation: Accept 헤더 무시 + 항상 JSON)
    ↓
┌──────────────────────────────────────┐
│  @Controller         @RestController │
│  JSP 렌더링           JSON 응답       │
│  /health             /api/**          │
└──────────────────────────────────────┘
    ↓
Service (비즈니스 로직)
    ↓
Repository (JPA) ──────► PostgreSQL + PostGIS
                         ├── Hospital.location (GEOGRAPHY POINT 4326, GIST 인덱스)
                         └── search_nearby_hospitals(lat,lng,npayCd,radius) PL/pgSQL
```

> **JSON-only 정책**: jackson-dataformat-xml이 클래스패스에 있어 (Hira 파싱용) Spring이 XML 응답 컨버터를 자동 등록함. 브라우저가 `Accept: application/xml;q=0.9`를 보내서 XML이 골라지는 함정 → `WebMvcConfig.configureContentNegotiation`에서 `ignoreAcceptHeader(true)` + `defaultContentType(APPLICATION_JSON)` 강제.

## 외부 데이터 흐름 (배치)

```
@Scheduled cron "0 0 0 1 * *" (매월 1일 0시) → BatchService.syncAll()
                  + 수동: POST /api/internal/batch/sync (BatchAdminApiController)
    ↓
NonPayItemSyncService → HiraNonPayClient.searchItemCodes (페이징)
    └─► NonPayItem upsert (npay_cd PK, EntityManager.merge)
    ↓
HospitalSyncService → HiraHospitalClient.searchHospitals (시도 17 × 페이징)
    └─► Hospital upsert (ykiho PK) + native UPDATE location = ST_MakePoint(...)
    ↓
PriceSyncService → hospitalRepository.findAllYkiho() (DB의 ykiho 직접 순회)
    └─► PriceYkihoSyncService.saveOneYkiho(ykiho) per ykiho — 트랜잭션 분리
        └─► HiraNonPayClient.searchHospPriceDetail(ykiho, page) (페이징)
            └─► Price upsert (ykiho+npay_cd 복합키, adt_end_dd='99991231'만)

* 외부 API 호출 사이 Thread.sleep(100) — rate limit 보호
* 개별 항목/페이지 실패는 log.warn + 계속 진행 (예외 전파 금지)
* getNonPaymentItemHospList2(가격 요약)는 page 2부터 timeout 빈발로 폐기 — Hospital ykiho 직접 사용
* PriceSyncService는 100 ykiho마다 진행률 로그 (reporting/empty 구분)
```

## 실시간 외부 호출 (병원 상세)

```
GET /api/hospitals/{ykiho} → HospitalDetailService.lookupDetail(ykiho)
    ├── DB: Hospital + Price(adt_end_dd='99991231' 필터) + NonPayItemService.lookupNamesByCodes(이름 매핑)
    └── HiraDetailClient.fetchAll(ykiho) — 4개 API CompletableFuture.allOf
        ├── getDgsbjtInfo2.7    (진료과목)
        ├── getMedOftInfo2.7    (의료장비)
        ├── getTrnsprtInfo2.7   (교통, Optional 단일 item)
        └── getSpclDiagInfo2.7  (특수진료)
            * hiraDetailExecutor 풀 (core 4 / max 8)
            * 개별 API 실패는 빈 리스트 fallback
```

## 컨텍스트 분리 (Root vs Servlet)

순수 Spring + WAR 배포라 root/servlet 컨텍스트가 분리되어 있다.

| 컨텍스트 | 설정 클래스 | 등록 빈 |
|---|---|---|
| **Root** | `global/config/{AppConfig, JpaConfig, SecurityConfig, CacheConfig, DatabaseInitializer}` | PSPC, JsonMapper/XmlMapper, hiraDetailExecutor, DataSource, EntityManagerFactory, TransactionManager, CacheManager, SecurityFilterChain ×2, CorsConfigurationSource, EntryPoint, AccessDeniedHandler |
| **Servlet** | `global/config/WebMvcConfig` | ViewResolver, Controller/RestController/ControllerAdvice 빈, ContentNegotiation |

원칙: 컨트롤러는 servlet 컨텍스트에만, 그 외는 root에. 양쪽 ComponentScan은 mirror 관계 (root는 컨트롤러 exclude, servlet은 컨트롤러만 include). JsonMapper처럼 양쪽이 공유하는 빈은 root에 둔다.

## 레이어별 규칙

### Controller / RestController
- Controller와 RestController는 **같은 Service를 공유**한다.
- Controller는 JSP 뷰 이름을 반환하고, RestController는 `ApiResponse<T>`를 반환한다.
- 모든 REST 응답은 `ApiResponse<T>` 래핑 — `GlobalExceptionHandler`가 에러도 같은 포맷으로 통일.
- Controller에 비즈니스 로직을 작성하지 않는다.

### Service
- `@Service` + `@Transactional(readOnly = true)` 기본. write는 메서드별 `@Transactional`.
- 하나의 Service가 Controller와 RestController 양쪽에서 호출된다.
- 예외는 `MediPriceException` 계열로 던진다 — `GlobalExceptionHandler`가 처리.

### Repository
- Spring Data JPA의 `JpaRepository<Entity, IdType>`을 상속한다.
- PostGIS 네이티브 쿼리는 `@Query(nativeQuery = true)` + 명명 바인딩(`:lat`, `@Param("lat")`).
- JSON 반환 PL/pgSQL은 `::text` 캐스트 — Hibernate JSON FormatMapper 회피.

### Entity
- 자체 PK 도메인(Hospital/NonPayItem/Price)은 `AbstractAuditEntity` 상속 (시간 필드만).
- 추상 PK가 필요한 미래 엔티티(예: Member)는 `BaseEntity` 상속 (`@Id Long id` + 시간 필드).
- 논리 삭제는 `deletedDttm` 필드 사용 (null이면 활성).
- UPSERT는 `EntityManager.merge()` (자체 PK라 INSERT/UPDATE 자동 분기).
- `Hospital.location`은 JTS Point 매핑 회피 — JPA에 매핑 X, native UPDATE로 `ST_MakePoint(x,y)::geography` 채움.

### Batch
- 평면 배치 — sync 서비스 4개 + Orchestrator + 디버그 트리거 + SidoCode 모두 `batch/` 한 폴더.
- write 책임은 도메인이 아닌 batch가 가짐 (도메인 service는 read 위주).
- 트랜잭션 경계: Price만 ykiho당 분리(`PriceYkihoSyncService.saveOneYkiho`), 나머지는 전체 sync 한 트랜잭션.
- 외부 API 회복력: 단발 호출 + try-catch 빈 결과 fallback. 회로차단기 미적용 (TODO).

## ApiResponse — 공통 응답 DTO

`global/common/ApiResponse.java` — Java 21 record + Jackson 3 native.

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ErrorDetail error) {
    public record ErrorDetail(String code, String message) {}

    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> error(ErrorCode code) { ... }
    public static <T> ApiResponse<T> error(ErrorCode code, String detailMessage) { ... }
}
```

응답 포맷:
```json
// 성공
{ "success": true, "data": { ... } }

// 실패 (NON_NULL이라 data 생략)
{ "success": false, "error": { "code": "H001", "message": "병원 정보를 찾을 수 없습니다." } }
```

프론트 처리:
```js
const result = await api.get('/api/hospitals?lat=37.5&lng=126.9&npayCd=...');
if (result.success) {
    renderHospitalList(result.data);
} else {
    alert(result.error.message);
    console.warn('error code:', result.error.code);
}
```

## 테스트 (P1 단위 테스트)

위치: `src/test/java/com/khm1102/mediprice/...` — 운영 코드와 같은 패키지 미러.

| 테스트 | 검증 |
|---|---|
| `service/HospitalDetailServiceTest` | 병원 없음 → 예외, Price 활성 필터, 코드→이름 매핑 fallback, bundle null 필터링, trnsprt Optional |
| `service/HospitalServiceTest` | 정상 JSON 파싱, null/blank/파싱실패 모두 빈 리스트 (장애 격리) |
| `service/NonPayItemServiceTest` | 그룹핑+활성+가나다순, 빈 코드 리스트, 일부 미존재 코드 |
| `global/exception/GlobalExceptionHandlerTest` | 5xx vs 4xx 분기, BindException 첫 필드에러, MissingParam detail, RuntimeException 격상 |
| `global/exception/ErrorCodeTest` | 모든 enum 값 초기화, 5xx는 INTERNAL_ERROR 하나만 |
| `global/common/ApiResponseTest` | success/error/error+detail 팩토리 |
| `global/filter/TraceIdFilterTest` | UUID 생성, 정상 헤더 전파, 위험문자 sanitize, MDC 누수 방지 |
| `client/hira/HiraBodyTest` | empty 팩토리, safeItems null 처리 |
| `client/hira/NonPayDtlItemTest` | isActive 99991231/과거/null |

총 45 테스트 (`./gradlew test`). 메서드명은 영문 camelCase + 한 줄 한국어 javadoc으로 의도 표시.

**의존성**: JUnit 5 BOM + Mockito 5.20 + AssertJ 3.27 + junit-jupiter-params.

**커버리지 공백 (별도 인프라 필요)**:
- 배치 sync 5개 — Testcontainers Postgres + WireMock 필요
- 컨트롤러 3개 — `@WebMvcTest` + MockMvc 필요
- Repository — `@DataJpaTest` + Testcontainers + PostGIS 필요
- HiraDetailClient 병렬 호출 — WireMock 필요
