# MediPrice — 비급여 진료비 비교 플랫폼

> 발표 대상: 교수님 및 학부생 | 작성 기준: 2026-05-19
>
> 이 문서는 발표 대본 성격의 요약본이다. 정확한 API/배치 규격은 `docs/hira-api-spec.md`, 장애 이력은 `docs/troubleshooting.md`를 기준으로 한다.

---

## 1. 프로젝트 소개

### 1.1 배경 및 문제 정의

국내 의료 체계는 건강보험이 적용되는 **급여 항목**과, 병원이 가격을 자율적으로 책정하는 **비급여 항목**으로 구분됩니다. MRI, 도수치료, 임플란트, 라식 등 비급여 항목은 법적으로 가격 공개 의무가 있으나, 정보가 분산되어 있어 환자가 직접 비교하기 어렵습니다.

- 심평원(건강보험심사평가원)은 비급여 가격 데이터를 공공 API로 제공하지만, 이를 지도와 연결하여 직관적으로 비교하는 서비스는 없음
- 환자는 전화·방문으로 일일이 가격을 문의하거나, 파편화된 커뮤니티 정보에 의존

### 1.2 서비스 목표

**"내 주변 병원의 비급여 진료비를 지도에서 한눈에 비교한다"**

| 핵심 기능 | 설명 |
|---|---|
| 위치 기반 병원 검색 | 현재 위치·반경 기준으로 비급여 항목을 신고한 병원 목록 조회 |
| 가격 비교 | MRI·도수치료·임플란트 등 항목별 병원 간 가격 비교 |
| 병원 상세 정보 | 비급여 가격 + 진료과목·의료장비·교통정보 통합 제공 |

### 1.3 데이터 현황 (MVP 기준)

| 구분 | 수치 |
|---|---|
| 수집 병원 수 | 79,674개 |
| 비급여 항목 코드 | 875개 |
| 가격 상세 데이터 | 79,428건 (quota 초과로 부분 적재) |
| 가격 요약 데이터 | 122,097건 (quota 초과로 부분 적재) |
| 수집 완료 지역 | 병원 기본정보 전국 17개 시도 완료 |

> 가격 계열 데이터는 심평원 개발 키 호출량 제한으로 완전 적재 전 중단되었다. DB에는 부분 적재분이 보존되어 있다.

---

## 2. 개발 목표 및 방법론

### 2.1 MVP 우선 전략

초기 단계에서 전체 기능을 완성하기보다 **핵심 가치를 가장 빠르게 검증할 수 있는 최소 기능 집합(MVP)**을 먼저 구현하고, 사용자 피드백과 개발 과정에서 발견한 개선점을 기반으로 기능을 점진적으로 추가하는 방식을 택했습니다.

```
P0  인프라 (Docker / PostgreSQL / Tomcat / Spring 부팅)       ✅ 완료
P0.5  Security 분리 / TraceIdFilter / 통일 로그 포맷          ✅ 완료
P1  심평원 배치 / REST API 3개 / PostGIS / 단위 테스트          ✅ 완료
P1.5  상세 API 매핑 수정 / Sido 통계 수정 / 배치 병렬화          ✅ 완료
다음  JSP 페이지 + 지도 SDK + 정적 JS                           대기
P2  회원가입 / 로그인 (JWT) / 즐겨찾기                         보류
```

### 2.2 공공데이터 활용 전략

심평원 공공 API를 배치로 분리해 DB에 먼저 저장합니다. 전체 데이터를 실시간으로 가져오기에는 호출량과 응답 시간이 크기 때문에, 월 1회 배치를 통해 DB를 갱신하고 서비스 API는 DB를 직접 조회합니다. 현재 전체 배치는 병렬 dispatch 구조이며, Price만 Hospital의 `ykiho` 목록에 의존합니다.

### 2.3 프로젝트 기원

MediPrice는 원래 **토이 프로젝트**로 기획되어 Next.js + Nest.js 모노레포 형태로 추진할 예정이었습니다. 웹 프로그래밍 팀 프로젝트 기회를 활용해 아이디어를 MVP로 구체화하였으며, 수업 환경에 맞춰 기술 스택을 조정하였습니다.

---

## 3. 기술 스택

| 계층 | 기술 | 버전 |
|---|---|---|
| **Frontend** | JSP + JSTL 3.0 | Jakarta EE |
| | Tailwind CSS CDN | (빌드 도구 없음) |
| | Vanilla JS ES2025 / fetch API | — |
| | Naver Maps JavaScript SDK v3 | v3 |
| **Backend** | Spring Framework | 7.0.6 |
| | Java | 21 |
| | Gradle (Groovy DSL) | — |
| | JPA + Hibernate | 7.x |
| | HikariCP | — |
| | Spring ConcurrentMapCache | — |
| | Apache Tomcat | 11 |
| **Database** | PostgreSQL + PostGIS | 16 |
| **Infra** | Docker + Docker Compose | — |
| | GitHub Actions (CI/CD) | — |
| | GHCR (이미지 레지스트리) | — |
| | Cloudflare (DNS + WAF + CDN + Tunnel) | — |
| | nginx + fail2ban | — |
| **AI Agent** | Claude / Claude Code | (코드 구현) |
| | Codex | (코드 리뷰·PR 검사) |

---

## 4. 기술 선택 근거

### 4.1 Spring Framework vs Spring Boot

**결론: 순수 Spring Framework 선택**

현대적인 Java 웹 개발에서는 Spring Boot + Thymeleaf 조합이 가장 간결하나, 다음 이유로 순수 Spring Framework를 선택하였습니다.

1. **JSP 사용**: 수업에서 다룬 뷰 기술이 JSP이며, 팀원 대부분의 프론트엔드 경험이 제한적이었습니다. JSP는 팀원들이 이미 학습한 기술이므로 학습 비용 없이 투입 가능하며, 기술은 원리를 이해하고 사용하는 것이 중요하다고 판단하였습니다.
2. **Spring Boot의 JSP 제약**: Spring Boot는 내장 Tomcat과 실행 가능한 JAR를 기본으로 하며, JSP는 WAR 배포와 외장 서블릿 컨테이너를 전제로 하는 구조적 제약이 있습니다. 무리하게 Boot를 사용하면 추가 설정 비용이 발생합니다.
3. **프레임워크 구조 학습**: `WebAppInitializer`, `AppConfig`, `WebMvcConfig` 등 순수 Spring의 초기화 흐름을 직접 구성함으로써 프레임워크가 제공하는 기능의 원리를 파악할 수 있습니다.
4. **개발 환경**: 기존 STS(Eclipse) 방식이 아닌 **IntelliJ + Gradle** 조합으로 의존성을 명시적으로 관리하여 재현 가능한 빌드 환경을 확보하였습니다.

### 4.2 PostgreSQL + PostGIS

**결론: 위치 기반 검색과 관계형 JOIN을 동시에 충족하는 유일한 무료 선택지**

심평원 데이터는 병원코드(`ykiho`)를 기준으로 세 테이블(`Hospital`, `NonPayItem`, `Price`)이 연결되는 정규화된 구조입니다. 이 구조에서 다음 요구사항이 발생합니다.

| 요구사항 | 이유 |
|---|---|
| 복잡한 JOIN | 병원 기본정보 + 비급여 항목명 + 가격을 한 쿼리로 조합 |
| 대용량 처리 | 병원 79,674건, 가격 상세 79,428건 부분 적재 + 월별 upsert |
| 위치 기반 검색 | 사용자 좌표 기준 반경 내 병원 정렬 |
| JSON 집계 | `json_agg()` 함수로 1:N 관계를 단일 쿼리로 처리 |

**PostgreSQL + PostGIS를 선택한 구체적 근거:**

- **PostGIS `ST_DWithin`**: MySQL/MariaDB도 공간 기능을 제공하지만, PostgreSQL의 PostGIS는 OGC GIS 표준에 충실하며 `GEOGRAPHY` 타입으로 구면 거리를 정확하게 계산합니다. `ST_DWithin`은 GIST 인덱스와 결합하여 수만 건의 좌표를 빠르게 검색합니다.
- **`search_nearby_hospitals()` 저장 프로시저**: 위치 기반 검색 + 비급여 가격 JOIN + 거리 계산을 단일 PL/pgSQL 함수로 캡슐화하여 네트워크 왕복을 줄이고 결과를 JSON으로 직접 반환합니다.
- **`json_agg()` 함수**: 병원 1건에 연결된 N개의 가격 항목을 서버에서 배열로 집계하여 애플리케이션 코드의 N+1 문제를 방지합니다.
- **무료 오픈소스 + Docker 공식 이미지**: `postgis/postgis` Docker 이미지로 개발·운영 환경을 동일하게 구성할 수 있습니다.

### 4.3 Cloudflare Tunnel

**결론: DB 포트를 외부에 노출하지 않고 안전하게 원격 접근**

```
개발자 PC (로컬)
    ↓  cloudflared access
Cloudflare Edge (TLS)
    ↓  Outbound 전용 TCP 터널
VPS 서버 localhost:5432 (PostgreSQL)
```

- 방화벽에 인바운드 포트를 열지 않아도 됩니다. 서버의 `cloudflared` 데몬이 Cloudflare Edge에 아웃바운드 연결을 유지하며, 요청은 이 터널을 역방향으로 전달됩니다.
- SSH나 VPN 없이도 Cloudflare 인증을 통과한 개발자만 DB에 접근 가능합니다.
- 운영 환경에서는 앱 컨테이너가 `localhost:5432`로 직접 접근하므로 터널 없이 동작합니다.

### 4.4 JWT Stateless 인증

**결론: 세션을 완전히 배제하고 수평 확장 가능한 구조 확보**

- HTTP 세션은 서버 메모리에 상태를 저장하므로, 서버가 2대 이상으로 늘어날 경우 세션 동기화 문제가 발생합니다.
- JWT는 토큰 자체에 인증 정보를 포함하므로 어느 서버에서 검증해도 동일한 결과를 얻습니다.
- 로그인 시 JWT를 발급하여 `HttpOnly` 쿠키에 저장합니다. `HttpOnly` 속성으로 JavaScript에서 토큰에 접근할 수 없어 XSS 공격을 방지합니다.
- 현재 MVP에서는 인증 없이 전체 공개(`permitAll`)이며, 회원 기능은 P2에서 도입할 예정입니다. SecurityFilterChain은 이미 구성되어 있어 JwtAuthFilter 추가만으로 활성화됩니다.

### 4.5 Docker + GitHub Actions CI/CD

**결론: "내 PC에서는 되는데" 문제 제거 + 원클릭 자동 배포**

```
git push origin master
    → GitHub Actions CI (빌드 + 테스트)
    → Docker 이미지 빌드 & GHCR push
    → Self-hosted Runner: docker compose pull && up -d
    → /api/health 60초 폴링 (실패 시 파이프라인 실패)
```

- **재현 가능한 환경**: `Dockerfile` + `docker-compose.yml`로 개발·운영 환경 완전 일치
- **롤백**: `ghcr.io/khm1102/mediprice:sha-XXXXXX` 태그로 특정 커밋 이미지로 즉시 복귀
- **Self-hosted Runner**: 서버에 상주하는 Runner가 배포를 실행하여 외부 CI 서버가 서버에 SSH 접근할 필요 없음
- **fail2ban 연동**: nginx 로그를 분석하여 로그인 브루트포스 시도를 자동 차단

---

## 5. AI 에이전트 활용 방식

### 5.1 "AI로 만들었다"가 아닌, AI와 협업 구조를 설계했다

AI 에이전트(Claude Code, Codex)는 코드를 작성하는 도구입니다. 그러나 에이전트에게 맥락(Context) 없이 지시하면 프로젝트 전체를 이해하지 못한 채 부분적으로만 동작하는 코드를 생성합니다. 이 프로젝트에서는 에이전트가 프로젝트를 **정확하게 이해하고 일관된 코드를 생성**하도록 협업 구조를 설계하였습니다.

### 5.2 협업 구조 설계 방법

#### CLAUDE.md / AGENTS.md — 에이전트 전용 규칙 문서

팀 프로젝트에서 항상 지켜지지 않던 코딩 컨벤션, 기술 스택 제약, API URL 규칙, DB 네이밍, 금지 사항 등을 `CLAUDE.md`(Claude용)와 `AGENTS.md`(Codex용)에 문서화하였습니다. 에이전트는 코드를 생성하기 전에 이 파일을 먼저 읽고, 정의되지 않은 사항은 임의로 판단하지 않고 질문하도록 지시하였습니다.

#### docs/ 폴더 — 에이전트 공통 참조 문서

```
docs/
├── project-overview.md      서비스 개요, 구현 상태, 데이터 현황
├── layered-architecture.md  패키지 구조, 요청 흐름, 레이어별 규칙
├── authentication.md        Security 구조, JWT 설계
├── error-handling.md        ErrorCode 체계, GlobalExceptionHandler
├── null-safety.md           JSpecify 컨벤션
└── feature-spec.md          기능 명세, 화면 구성
```

에이전트가 대화 세션을 새로 시작하더라도 동일한 맥락에서 시작할 수 있도록, 프로젝트의 설계 결정과 현재 상태를 docs 폴더에 항상 최신 상태로 유지하였습니다.

#### 메모리 파일 커밋 (`docs/memory/MEMORY.md`)

Claude와 Codex의 대화 내역과 중요한 결정 사항을 별도의 `.md` 파일로 커밋하였습니다. 이를 통해 팀원 누구든 에이전트를 사용할 때 동일한 컨텍스트에서 시작할 수 있으며, 에이전트의 응답 일관성을 높입니다.

### 5.3 역할 분리

| 에이전트 | 역할 |
|---|---|
| **Claude Code** | 기능 구현 (신규 코드 작성, 리팩토링, 문서 작성) |
| **Codex** | 코드 리뷰, PR 검사 (컨벤션 위반, 잠재적 버그 탐지) |

---

## 6. 핵심 기능 구현

### 6.1 위치 기반 병원 검색 (PostGIS)

**`search_nearby_hospitals()` PL/pgSQL 저장 프로시저**

```sql
-- 핵심 쿼리 개념 (간략화)
SELECT h.*, p.npay_cd, p.price, p.unit,
    ST_Distance(h.location, ST_MakePoint(lng, lat)::geography) AS distance
FROM Hospital h
JOIN Price p ON h.ykiho = p.ykiho
WHERE ST_DWithin(h.location, ST_MakePoint(lng, lat)::geography, radius_meters)
    AND p.npay_cd = target_npay_cd
    AND p.adt_end_dd = '99991231'   -- 현재 유효한 가격만
ORDER BY distance
LIMIT 20;
```

- `GEOGRAPHY(POINT, 4326)` 타입 + GIST 인덱스로 구면 거리 기반 검색
- `adt_end_dd = '99991231'` 조건으로 심평원의 유효 가격 행만 필터링
- 저장 프로시저가 JSON을 직접 반환하여 애플리케이션 레이어의 매핑 비용 최소화
- `DatabaseInitializer`가 `@PostConstruct`로 서버 시작 시 프로시저를 자동 등록

### 6.2 심평원 배치 동기화

```
@Scheduled cron "0 0 0 1 * *" (매월 1일 0시) + 수동 트리거
    ↓
BatchService.syncAll()
    ├─ NonPayItemSyncService
    ├─ HospitalSyncService
    ├─ NonPayItemDescSyncService
    ├─ PriceSummarySyncService
    ├─ NonPayItemClcdStatSyncService
    ├─ NonPayItemSidoStatSyncService
    └─ PriceSyncService (Hospital 완료 후 실행)
```

- 전체 배치는 executor로 병렬 dispatch한다.
- Price만 병원 식별자 `ykiho`가 필요하므로 Hospital 결과에 의존한다.
- 개별 항목·페이지 실패는 `log.warn` 후 계속 진행한다.
- DB write는 페이지/청크/ykiho 단위 트랜잭션으로 분리한다.
- 수동 트리거: `POST /api/internal/batch/sync`

### 6.3 병원 상세 — 실시간 외부 API 병렬 호출

```java
// HiraDetailClient.fetchAll(ykiho) — 5개 API CompletableFuture.allOf
CompletableFuture<List<DgsbjtItem>>   dgsbjtFuture  // 진료과목
CompletableFuture<List<MedOftItem>>   medOftFuture  // 의료장비
CompletableFuture<List<TrnsprtItem>>   trnsprtFuture // 대중교통
CompletableFuture<Optional<DtlInfoItem>> dtlFuture   // 주차/진료시간
CompletableFuture<List<SpclDiagItem>> spclDiagFuture // 특수진료

CompletableFuture.allOf(dgsbjtFuture, medOftFuture, trnsprtFuture, dtlFuture, spclDiagFuture).join();
```

- `hiraDetailExecutor` 스레드 풀 (core=5, max=10)로 5개 API를 병렬 호출
- 개별 API 실패 시 빈 리스트 fallback — 일부 외부 API 장애가 전체 응답을 막지 않음
- DB 조회(병원 + 가격)와 외부 API 호출을 결합하여 단일 응답으로 반환

### 6.4 분산 추적 (TraceId)

```
모든 요청
    ↓ TraceIdFilter (가장 앞)
UUID 32자 생성 → MDC(traceId) 저장 → X-Trace-Id 응답 헤더 포함
    ↓
logback.xml → [%X{traceId}] 패턴으로 모든 로그에 포함
```

`docker logs`에서 `traceId`로 grep하면 단일 요청의 전체 로그를 추적할 수 있습니다.

---

## 7. 시스템 아키텍처

> 전체 Mermaid 다이어그램: `architecture.md` 참조

### 7.1 요청 흐름

```
사용자 브라우저
    ↓ HTTPS
Cloudflare (DNS → WAF → CDN)
    ↓ HTTP
nginx (리버스 프록시 + SSL 종단 + fail2ban 연동)
    ↓ HTTP :8080
Apache Tomcat 11 (ROOT.war)
    ↓
TraceIdFilter → CharacterEncodingFilter
    ↓
Spring Security (apiSecurityFilterChain @Order 1 / pageSecurityFilterChain @Order 2)
    ↓
┌──────────────────────────────┐
│  @Controller  │  @RestController│
│  JSP 렌더링   │  JSON 응답     │
│  /health 등   │  /api/**       │
└──────────────────────────────┘
    ↓
Service Layer (비즈니스 로직)
    ↓
JPA + HikariCP → PostgreSQL + PostGIS
```

### 7.2 배포 파이프라인

```
git push origin master
    ↓ GitHub Actions CI
    빌드 (./gradlew assemble) + 테스트 (JUnit 5)
    ↓ Docker Build & Push
    ghcr.io/khm1102/mediprice:latest
    ghcr.io/khm1102/mediprice:sha-XXXXXX  (롤백용)
    ↓ Self-hosted Runner (서버 상주)
    docker compose pull && docker compose up -d
    ↓ 헬스체크
    GET /api/health — 60초 폴링, 실패 시 파이프라인 실패
```

### 7.3 컨텍스트 분리 (Spring 순수 WAR 구조)

Spring Boot가 아니므로 Root Context와 Servlet Context가 분리됩니다.

| 컨텍스트 | 담당 설정 | 주요 빈 |
|---|---|---|
| **Root** | AppConfig, JpaConfig, SecurityConfig, CacheConfig | DataSource, EntityManagerFactory, SecurityFilterChain, CacheManager |
| **Servlet** | WebMvcConfig | Controller, RestController, ViewResolver, ContentNegotiation |

---

## 8. 한계 및 개선 방향

### 8.1 데이터 한계

| 한계 | 내용 |
|---|---|
| **가격 부분 적재** | 병원 기본정보는 전국 수집 완료, 가격 계열은 개발 키 quota 초과로 부분 적재 |
| **항목 편중** | 가격 신고 상위 12개 중 대부분이 제증명수수료(1만원 이하) — 비교 가치 낮음 |
| **미용시술 불가** | 보톡스·필러·지방흡입·쌍커풀 등 → 심평원 의무 신고 대상이 아니므로 데이터 없음 |

**실질적 유용 항목**: 도수치료 971개 병원, MRI(척추·관절·뇌 부위별) 480~580개 병원, 임플란트 11~113개 병원

### 8.2 기술적 한계

| 한계 | 내용 | 개선 방향 |
|---|---|---|
| 캐시 TTL 없음 | `ConcurrentMapCache`는 만료 정책 미지원 — 서버 재시작 시 초기화 | Caffeine Cache 도입 (단, 현재 금지) / 배치 주기와 일치하도록 수동 evict |
| 테스트 커버리지 공백 | 단위 테스트 94개는 통과, PostGIS/배치 통합 테스트는 부족 | Testcontainers + WireMock 도입 |
| 배치 엔드포인트 노출 | `POST /api/internal/batch/sync`가 비인증 공개 — 누구나 배치 트리거 가능 | `@Profile("dev")` 또는 IP 화이트리스트 적용 |
| 외부 API 회복력 제한 | retry/backoff는 있으나 quota 초과와 장기 장애에 대한 checkpoint 부족 | checkpoint + 운영계정 + 재개 정책 |
| 회원 기능 미구현 | JWT SecurityFilterChain 구성만 완료, JwtAuthFilter·AuthService 미작성 | P2 도입 (구조 준비 완료) |

### 8.3 솔직한 포지셔닝

비급여 진료비 비교 서비스의 현실적 강점은 **MRI·도수치료·임플란트** 영역입니다. 미용시술 분야는 심평원 의무 신고 대상이 아니므로 데이터 자체가 존재하지 않습니다. 서비스의 강점인 **거리 + 가격 결합 검색**, **부위별 MRI 비교**를 중심으로 포지셔닝하는 것이 정직하고 효과적입니다.

---

## 9. 정리

MediPrice는 단순한 CRUD 애플리케이션을 넘어, 다음 요소들을 통합한 프로토타입입니다.

- **공공 데이터 파이프라인**: 심평원 API 배치 동기화로 병원 79,674건과 가격 부분 데이터를 DB에 구축
- **위치 기반 검색**: PostGIS + PL/pgSQL 저장 프로시저로 정확한 구면 거리 기반 병원 탐색
- **AI 에이전트 협업**: 코드 컨벤션·설계 문서를 구조화하여 에이전트가 일관된 코드를 생성하도록 환경 설계
- **자동화된 배포**: GitHub Actions + Docker + Self-hosted Runner로 커밋부터 서비스 반영까지 자동화
- **프로덕션 수준의 인프라**: Cloudflare WAF, nginx, fail2ban, TraceId 기반 분산 추적

프로토타입의 한계를 인정하면서도, 각 기술 선택에 명확한 근거가 있으며 개선 경로가 설계된 상태로 남아 있습니다.
