# 레이어드 아키텍처

## 패키지 구조

```
com.khm1102.mediprice/
├── controller/           @RestController + @Controller (전부 한 폴더 평면 배치)
│   ├── HospitalApiController       /api/hospitals/search + /{ykiho}[/basics|/extras]
│   ├── NonPayItemApiController     /api/items
│   ├── AuthController/AuthApiController  Google OAuth + JWT 쿠키
│   ├── FavoriteController/FavoriteApiController  즐겨찾기 페이지/API
│   ├── HealthApiController         /api/health (JSON probe)
│   ├── HealthController            /health (JSP)
│   ├── LegalController             /legal/** 약관/개인정보/위치약관
│   └── (BatchAdminApiController는 batch/ 폴더 — 운영 묶음)
├── service/              도메인 서비스 (비즈니스 로직)
│   ├── NonPayItemService           항목 그룹핑 + 코드→이름 룩업
│   ├── HospitalService             PostGIS 프로시저 호출 + JSON 파싱
│   ├── HospitalDetailService       DB(병원/가격) + 외부 5개 API 병합
│   ├── AuthService                 OAuth 회원 처리 + 탈퇴
│   ├── GoogleOAuthService          Google token/userinfo 호출
│   ├── ConsentService              신규 회원 약관 동의 임시 저장
│   └── FavoriteService             즐겨찾기 조회/추가/삭제
├── entity/               도메인 엔티티 (자체 PK)
│   ├── Hospital                    ykiho PK + native UPDATE location
│   ├── NonPayItem                  npay_cd PK
│   ├── Price                       @IdClass(PriceId) ykiho+npay_cd
│   ├── PriceId
│   ├── Member                      Google OAuth 회원
│   └── Favorite                    회원별 병원 즐겨찾기
├── repository/           Spring Data JPA
│   ├── HospitalRepository          findAllYkiho, searchNearbyV2Json(::text), updateLocation
│   ├── NonPayItemRepository
│   ├── PriceRepository             findAllByYkihoAndAdtEndDd
│   ├── MemberRepository
│   └── FavoriteRepository
├── dto/                  도메인 DTO (record)
│   ├── HospitalSummaryDto          /api/hospitals/search 응답 element (matched 항목명·종별 평균 포함)
│   ├── HospitalDetailBasicsDto     /api/hospitals/{ykiho}/basics 응답 (DB only)
│   ├── HospitalDetailExtrasDto     /api/hospitals/{ykiho}/extras 응답 (HIRA 5종)
│   ├── HospitalDetailDto           /api/hospitals/{ykiho} 통합 응답 (PriceItem, transit/parking/operating 중첩)
│   ├── NonPayItemGroupDto          /api/items 응답 (Item 중첩)
│   └── FavoriteDto                 /api/favorites 응답 element
├── batch/                운영 묶음 — 스케줄러/수동 트리거/도메인별 적재
│   ├── admin/                    BatchAdminApiController — POST /api/internal/batch/**
│   ├── orchestrator/             BatchService — 전체 배치 병렬 dispatch
│   ├── hospital/                 HospitalSyncService, HospitalBatchWriter
│   ├── item/                     NonPayItem/NonPayItemDesc sync + writer
│   ├── price/                    PriceSyncService, PriceYkihoSyncService
│   ├── summary/                  PriceSummary sync + writer
│   ├── stat/                     ClcdStat/SidoStat sync + writer
│   └── support/                  SidoCode 등 배치 공통 코드
├── client/               외부 API 통합
│   ├── HiraHospitalClient          getHospBasisList (시도+페이징)
│   ├── HiraNonPayClient            비급여 항목/가격/통계 API
│   ├── HiraDetailClient            의료기관 상세 5개 API CompletableFuture 병렬
│   └── hira/                       외부 응답 XML DTO
│       ├── HiraResponse/Header/Body  (generic wrapper, Body는 record 아닌 class — XmlMapper 충돌 회피)
│       ├── HospBasisItem
│       ├── NonPayCodeItem, NonPayDtlItem (ACTIVE_END_DATE='99991231')
│       ├── DgsbjtItem, MedOftItem
│       └── TrnsprtItem, DtlInfoItem, SpclDiagItem
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
    │   ├── ErrorCode               C/A/H/F 카테고리
    │   ├── MediPriceException      추상
    │   ├── GlobalExceptionHandler  5xx vs 4xx 분기, BindException 첫 필드에러
    │   ├── auth/                   AuthenticationException
    │   └── business/               HospitalNotFound, FavoriteNotFound, FavoriteAlreadyExists, BusinessException
    ├── filter/
    │   ├── TraceIdFilter           32-hex UUID, X-Trace-Id 양방향, MDC, 위험문자 sanitize
    │   ├── AuthAttributeNames      request attribute 키 상수
    │   └── JwtAuthFilter           mp_token 쿠키 JWT → SecurityContext
    └── entity/
        ├── BaseEntity              Long id PK + 시간 필드 (Member/Favorite용)
        └── AbstractAuditEntity     시간 필드만 (자체 PK 도메인 엔티티용)

src/main/resources/sql/procedures.sql  ← search_nearby_hospitals_v2 (PostGIS PL/pgSQL, JSON 반환)
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
                         └── search_nearby_hospitals_v2(lat,lng,npayCds[],radius,sort,limit,wPrice,wDistance) PL/pgSQL
```

> **JSON-only 정책**: jackson-dataformat-xml이 클래스패스에 있어 (Hira 파싱용) Spring이 XML 응답 컨버터를 자동 등록함. 브라우저가 `Accept: application/xml;q=0.9`를 보내서 XML이 골라지는 함정 → `WebMvcConfig.configureContentNegotiation`에서 `ignoreAcceptHeader(true)` + `defaultContentType(APPLICATION_JSON)` 강제.

## 외부 데이터 흐름 (배치)

```
@Scheduled cron "0 0 0 1 * *" (매월 1일 0시) → BatchService.syncAll()
                  + 수동: POST /api/internal/batch/sync
                    (BatchAdminApiController, BatchAdminGuard enabled + secret)
    ↓
BatchService가 hiraBatchExecutor로 7개 작업 dispatch
    ├─ NonPayItemSyncService          → NonPayItem upsert
    ├─ HospitalSyncService            → Hospital upsert + location native UPDATE
    ├─ NonPayItemDescSyncService      → NonPayItemDesc upsert
    ├─ PriceSummarySyncService        → PriceSummary upsert
    ├─ NonPayItemClcdStatSyncService  → NonPayItemClcdStat long table upsert
    ├─ NonPayItemSidoStatSyncService  → NonPayItemSidoStat long table upsert
    └─ PriceSyncService               → Hospital 완료 후 findAllYkiho() 기반 Price upsert

* Price만 Hospital 결과에 의존한다. getNonPaymentItemHospDtlList 입력값이 ykiho이기 때문이다.
* 각 SyncService는 외부 API 호출과 DB write 트랜잭션을 분리한다.
* DB write는 페이지/청크/ykiho writer에서 REQUIRES_NEW로 실행하고 flush/clear한다.
* 개별 항목/페이지 실패는 log.warn + 계속 진행 (예외 전파 금지)
* PriceSyncService는 진행률 로그에서 processed/saved/reporting/empty를 구분한다.
* 다음 개선안: Hospital producer가 ykiho를 queue에 publish하고 Price worker가 즉시 소비하는 파이프라인.
```

## 실시간 외부 호출 (병원 상세)

```
GET /api/hospitals/{ykiho}        → HospitalDetailService.lookupDetail(ykiho) (basics+extras 합본)
GET /api/hospitals/{ykiho}/basics → HospitalDetailService.lookupBasics(ykiho) (DB only, fast)
GET /api/hospitals/{ykiho}/extras → HospitalDetailService.lookupExtras(ykiho) (HIRA 5종, slow)
    ├── DB: Hospital + Price(adt_end_dd='99991231' 필터) + NonPayItemService.lookupNamesByCodes(이름 매핑)
    └── HiraDetailClient.fetchAll(ykiho) — 5개 API CompletableFuture.allOf
        ├── getDgsbjtInfo2.7    (진료과목)
        ├── getMedOftInfo2.7    (의료장비)
        ├── getTrnsprtInfo2.7   (대중교통, 복수 row)
        ├── getDtlInfo2.7       (주차/진료시간/응급)
        └── getSpclDiagInfo2.7  (특수진료)
            * hiraDetailExecutor 풀 (core 5 / max 10)
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
- 기능 기준 패키지 — `admin`, `orchestrator`, `hospital`, `item`, `price`, `summary`, `stat`, `support`로 분리한다.
- write 책임은 도메인이 아닌 batch가 가짐 (도메인 service는 read 위주).
- 트랜잭션 경계: 외부 API 호출과 DB write를 분리하고, 페이지/청크/ykiho 단위 writer에서 `REQUIRES_NEW`를 사용한다.
- 외부 API 회복력: endpoint별 retry/backoff + 실패 격리. 회로차단기는 미적용 (TODO).

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
const result = await api.get('/api/hospitals/search?lat=37.5&lng=126.9&npayCds=...&sort=mixed');
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
| `service/HospitalDetailServiceTest` | 병원 없음 → 예외, Price 활성 필터, 코드→이름 매핑 fallback, 상세 bundle null 필터링 |
| `service/HospitalServiceTest` | 정상 JSON 파싱, null/blank/파싱실패 모두 빈 리스트 (장애 격리) |
| `service/NonPayItemServiceTest` | 그룹핑+활성+가나다순, 빈 코드 리스트, 일부 미존재 코드 |
| `global/exception/GlobalExceptionHandlerTest` | 5xx vs 4xx 분기, BindException 첫 필드에러, MissingParam detail, RuntimeException 격상 |
| `global/exception/ErrorCodeTest` | 모든 enum 값 초기화, 5xx는 INTERNAL_ERROR 하나만 |
| `global/common/ApiResponseTest` | success/error/error+detail 팩토리 |
| `global/filter/TraceIdFilterTest` | UUID 생성, 정상 헤더 전파, 위험문자 sanitize, MDC 누수 방지 |
| `client/hira/HiraBodyTest` | empty 팩토리, safeItems null 처리 |
| `client/hira/NonPayDtlItemTest` | isActive 99991231/과거/null |

총 94 테스트 (`./gradlew test`). 메서드명은 영문 camelCase + 한 줄 한국어 javadoc으로 의도 표시.

**의존성**: JUnit 5 BOM + Mockito 5.20 + AssertJ 3.27 + junit-jupiter-params.

**커버리지 공백 (별도 인프라 필요)**:
- 배치 sync 통합 검증 — Testcontainers Postgres + WireMock 필요
- 컨트롤러 3개 — `@WebMvcTest` + MockMvc 필요
- Repository — `@DataJpaTest` + Testcontainers + PostGIS 필요
- 전체 배치 병렬 orchestration — executor/의존성 순서 검증 필요
