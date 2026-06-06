# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Claude Code가 이 프로젝트에서 코드를 생성할 때 반드시 따라야 하는 규칙 문서.
> 이 문서에 정의되지 않은 사항은 임의로 판단하지 말고 반드시 질문할 것.

---

## 0. 명령어 (Commands)

```bash
# 빌드 및 테스트
./gradlew clean build          # 전체 빌드 (테스트 포함)
./gradlew test                 # 테스트만 실행
./gradlew test --tests "com.khm1102.mediprice.service.HospitalServiceTest"  # 단일 테스트 클래스
./gradlew test --tests "*HospitalService*"   # 패턴 매칭
./gradlew war                  # ROOT.war 패키징 (build/libs/ROOT.war)

# Docker (앱만 — DB는 외부 Cloudflare Tunnel 프록시 사용)
cp .env.example .env                          # 최초 1회, .env에 실제 값 입력
docker network create mediprice-network       # 최초 1회 (infra의 nginx가 붙는 외부 네트워크)
docker-compose up -d                          # 운영 이미지 pull 후 시작 (ghcr.io)
docker-compose up -d --build                  # 로컬 빌드 후 시작 (이미지 없거나 소스 변경 시)
docker-compose logs -f app                    # 앱 로그
docker-compose down                           # 종료

# 배포 (CI/CD)
# master 브랜치에 push하면 GitHub Actions가 자동으로:
#   1. ghcr.io/khm1102/mediprice:{latest, sha-<short>} 빌드 & 푸시
#   2. 서버에 SSH로 들어가 docker compose pull && up -d
#   3. /api/health 헬스체크 통과까지 대기 (실패 시 로그 출력 후 실패 처리)

# 헬스체크
curl http://localhost:8080/api/health
```

---

## 1. 프로젝트 개요

```
서비스명    MediPrice (메디프라이스)
목적        비급여 진료비 비교 플랫폼 (프로토타입)
성격        학교 팀 프로젝트 (발표용 프로토타입)
```

---

## 2. 기술 스택 (확정, 변경 금지)

```
Backend
  Framework   Spring Framework 7.0.6 (Spring Boot 아님, 순수 Spring)
  Language    Java 21
  Build       Gradle (Groovy DSL) — Maven 사용 금지
  ORM         JPA + Hibernate
  Security    Spring Security + JWT (세션 사용 금지)
  DB Pool     HikariCP
  Cache       Spring ConcurrentMapCache (Redis, Caffeine 사용 금지)
  설정은 .yaml로 환경변수를 받게 한 다음 .env에서 실질적으로 사용자가 값을 넣도록

Frontend
  Template    JSP + JSTL 3.0
  Script      Vanilla JS ES2025 (React, Vue, jQuery 사용 금지)
  Style       Tailwind CSS CDN (빌드 도구 없음, npm 사용 금지)
  HTTP        fetch API (axios 사용 금지)
  Map         네이버맵 JavaScript SDK v3

Database
  Engine      PostgreSQL + PostGIS 확장
  Migration   없음 (JPA ddl-auto 사용)
  데이터베이스는 docker 컴포즈로 개발 데이터베이스 띄우기

Infra
  WAS         Apache Tomcat 11
  Container   Docker + Docker Compose
  Tunnel      Cloudflare Tunnel
  Log         docker logs (별도 수집 도구 없음)
```

---

## 3. 아키텍처

### 3.1 요청 흐름

```
브라우저 요청
    ↓
Spring Security (JWT 인증/인가)
    ↓
┌────────────────────────────────────────────────┐
│  @Controller                  │  @RestController│
│  JSP 렌더링                   │  JSON 응답      │
│  /, /hospitals, /hospital     │  /api/**        │
│  /favorites, /auth/**         │                 │
│  /legal/**, /health           │                 │
└────────────────────────────────────────────────┘
    ↓
Service (비즈니스 로직)
    ↓
Repository (JPA)
    ↓
PostgreSQL + PostGIS
```

### 3.2 인증 구조

```
세션 사용 금지 — JWT로 완전 통일

회원
  Google OAuth → /auth/oauth2/authorize/google
  콜백 → 신규 회원이면 /auth/consent (약관 동의)
  JWT 발급 → mp_token HttpOnly 쿠키 저장 (Path=/, Max-Age=JWT_EXPIRATION, SameSite 기본 Lax)
  요청마다 JwtAuthFilter가 mp_token을 검증해 MemberPrincipal로 인증

비회원
  검색 API 공개 접근 가능 (별도 제한 정책 없음)
  필요 시 /api/auth/token/guest → 임시 Guest JWT 발급
  Guest JWT는 향후 사용자 추적용으로만 두며 검색 차단/카운팅 용도가 아님
```

> `mp_token`은 `MpTokenCookieFactory`가 HttpOnly로 발급한다. `Secure`와 `SameSite`는 `COOKIE_SECURE`, `COOKIE_SAME_SITE`로 제어한다.

### 3.3 캐싱 구조

```
심평원 API 결과 → Spring ConcurrentMapCache
  키: 지역코드 + 진료항목
  만료: 서버 재시작 시 초기화 (프로토타입 수준)
  목적: 동일 요청 반복 시 API 재호출 방지
```

### 3.4 Controller vs RestController 분리

```
@Controller  (/, /hospitals, /hospital, /favorites, /auth/**, /legal/**, /health)
  → JSP 초기 렌더링 담당
  → 페이지 이동, 레이아웃

@RestController (/api/**)
  → JS fetch 요청 처리
  → JSON 응답만 반환 (ApiResponse<T>)
  → Spring Security JWT로 권한 체크

공통
  → Service 레이어는 하나만 (Controller/RestController 공유)
```

### 3.5 부트스트랩 구조

Spring Boot가 아니므로 `@SpringBootApplication` 없음. 진입점:

```
WebAppInitializer (AbstractAnnotationConfigDispatcherServletInitializer 상속)
  → getRootConfigClasses(): AppConfig, JpaConfig, SecurityConfig, CacheConfig
  → getServletConfigClasses(): WebMvcConfig
  → 등록 필터: TraceIdFilter (MDC UUID), CharacterEncodingFilter (UTF-8)

DatabaseInitializer (@Component + @PostConstruct)
  → 서버 시작 시 sql/procedures.sql 자동 실행
  → search_nearby_hospitals_v2() PostgreSQL 함수 생성
```

### 3.6 배치 (HIRA API 동기화)

```
batch/ 패키지 — 심평원 데이터 초기 적재 및 갱신
  ├─ admin/          BatchAdminApiController
  ├─ orchestrator/   BatchService
  ├─ hospital/       HospitalSyncService + HospitalBatchWriter
  ├─ item/           NonPayItem / NonPayItemDesc sync + writer
  ├─ price/          PriceSyncService + PriceYkihoSyncService
  ├─ summary/        PriceSummary sync + writer
  ├─ stat/           ClcdStat / SidoStat sync + writer
  └─ support/        SidoCode 등 공통 배치 코드

BatchAdminApiController  /api/internal/batch/sync{, /prices, /desc, /summary, /clcd-stat, /sido-stat}
  (운영 중 데이터 갱신 시 사용)

전체 배치는 hiraBatchExecutor에서 병렬 dispatch한다.
단, PriceSyncService는 getNonPaymentItemHospDtlList 호출에 Hospital.ykiho가 필요하므로
Hospital 완료 뒤에 실행한다. 다음 개선 방향은 Hospital producer가 ykiho를 queue로 흘리고
Price worker가 즉시 소비하는 파이프라인이다.

각 SyncService는 외부 API 호출과 DB write 트랜잭션을 분리한다. 페이지/청크/ykiho writer에서
REQUIRES_NEW로 저장하고 flush/clear하여 장시간 connection 점유를 피한다.

다중 API 키: HiraServiceKeyProvider가 HIRA_API_KEYS(콤마 구분) + HIRA_API_KEY(fallback)을
            라운드로빈으로 분배해 일일 호출 한도(키당 10,000건) 회피.
```

### 3.7 분산 추적

```
TraceIdFilter → 모든 요청에 UUID 생성 → MDC(traceId) 저장
logback.xml → [%X{traceId}] 패턴으로 모든 로그에 포함
docker logs로 traceId 기준 요청 추적 가능
```

---

## 4. API URL 컨벤션

> 리소스는 복수형, kebab-case. 신규 endpoint 추가 시 아래 표와 같은 형태로 맞춘다.

```
# REST API (프론트가 직접 호출)
GET    /api/items                          비급여 항목 그룹
GET    /api/hospitals/search               위치 + 다중 비급여항목 검색 (search_nearby_hospitals_v2)
                                            ?lat&lng&npayCds(콤마구분, 1~20개)
                                            &radius(100~50000m, 기본 5000)
                                            &sort=mixed|price|distance(기본 mixed)
                                            &limit(1~200, 기본 50)
                                            &wPrice&wDistance(mixed 점수 가중치, 기본 0.7/0.3)
GET    /api/hospitals/{ykiho}/basics       병원 상세 fast — DB only (가격 카드/표)
GET    /api/hospitals/{ykiho}/extras       병원 상세 slow — HIRA 5종 캐시
GET    /api/hospitals/{ykiho}              병원 상세 통합 응답 (basics + extras)
GET    /api/favorites                      즐겨찾기 목록 (회원)
POST   /api/favorites                      즐겨찾기 추가 (회원)
DELETE /api/favorites/{ykiho}              즐겨찾기 삭제 (회원)
GET    /api/favorites/{ykiho}/status       즐겨찾기 여부 (회원)
GET    /api/health                         JSON 헬스체크

# 인증
GET    /auth/oauth2/authorize/google       Google OAuth 시작
GET    /auth/oauth2/callback               Google OAuth 콜백
GET    /auth/consent                       약관 동의 페이지 (신규 회원)
POST   /auth/consent                       약관 동의 후 회원 등록
POST   /api/auth/logout                    로그아웃 (mp_token 쿠키 삭제)
GET    /api/auth/token/guest               비회원 임시 Guest JWT 발급
GET    /api/auth/me                        현재 회원 정보 (회원)
DELETE /api/auth/me                        회원 탈퇴 (회원)

# 운영/디버그 — BatchAdminGuard 가드 (BATCH_ADMIN_ENABLED=true + X-Batch-Admin-Secret 일치 필수)
# 기본 fail-closed (admin-enabled=false / admin-secret=빈문자 → 모든 요청 403 B001/B003)
POST   /api/internal/batch/sync            전체 배치 트리거
POST   /api/internal/batch/sync/prices     Price 단독
POST   /api/internal/batch/sync/desc       NonPayItemDesc 단독
POST   /api/internal/batch/sync/summary    PriceSummary 단독
POST   /api/internal/batch/sync/clcd-stat  종별 통계 단독
POST   /api/internal/batch/sync/sido-stat  지역 통계 단독

# JSP 페이지 (Controller)
GET    /                                   메인 (랜딩)
GET    /hospitals                          병원 검색 (지도 + 리스트 + 상세 패널)
GET    /hospital?ykiho=...                 병원 상세 페이지
GET    /favorites                          즐겨찾기 페이지 (회원)
GET    /auth/consent                       약관 동의 페이지
GET    /legal/terms                        이용약관
GET    /legal/privacy                      개인정보처리방침
GET    /legal/location                     위치기반서비스약관
GET    /health                             JSP 헬스체크
```

---

## 5. Java 코딩 컨벤션

> 기준: 아이알엠(IRM) HE Coding Conventions

### 5.1 네이밍

```
패키지     소문자만                     com.khm1102.mediprice.hospital
클래스     PascalCase + 명사            HospitalService, PriceInfo
인터페이스 PascalCase + 명사/형용사     HospitalRepository, Cacheable
메서드     camelCase + 동사로 시작      searchHospitals(), lookupHospital()
변수       camelCase                    hospitalList, currentPage
상수       UPPER_SNAKE_CASE             MAX_SEARCH_RADIUS, DEFAULT_PAGE_SIZE
테스트     클래스명 + Test              HospitalServiceTest
```

### 5.2 메서드 동사 규칙

```
create    생성 (순서 무관)
remove    실제 DB 삭제
delete    논리 삭제 (삭제 플래그)
update    수정
search    검색 (결과 여러 개)
lookup    단건 조회 (결과 1개)
exists    존재 확인
validate  검증
generate  생성
```

### 5.3 DB 네이밍

```
테이블    PascalCase                   Hospital, PriceInfo, Member
컬럼      snake_case                   hira_code, created_dttm, hospital_name
약어      desc, dttm, id 허용
```

### 5.4 코드 스타일

```java
// 중괄호: K&R 스타일
public class HospitalService {
    public List<Hospital> searchHospitals(double lat, double lng) {
        if (lat == 0) {
            throw new IllegalArgumentException("위도 필수");
        }
    }
}

// else/catch는 닫는 중괄호와 같은 줄
try {
    writeLog();
} catch (IOException e) {
    handleError(e);
} finally {
    cleanup();
}

// 애노테이션은 새 줄
@Service
@Transactional
public class HospitalService { }

// 한 줄에 한 문장
int width = 0;
int height = 0;

// 배열 선언
String[] names;   // O
String names[];   // X

// import 와일드카드 금지
import java.util.List;    // O
import java.util.*;       // X (static import만 허용)

// 탭 들여쓰기 (4 spaces)
```

### 5.5 패키지 구조별 클래스 명

```
global/config/                AppConfig, WebMvcConfig, WebAppInitializer, JpaConfig, SecurityConfig, SecurityInitializer, CacheConfig, DatabaseInitializer
global/common/                ApiResponse (record + ErrorDetail 중첩)
global/entity/                BaseEntity, AbstractAuditEntity
global/exception/             MediPriceException, ErrorCode, GlobalExceptionHandler, PageExceptionHandler
global/exception/auth/        AuthenticationException
global/exception/business/    BusinessException, HospitalNotFoundException, FavoriteNotFoundException, FavoriteAlreadyExistsException
global/filter/                TraceIdFilter, JwtAuthFilter, AuthAttributeNames
global/security/              MemberPrincipal
entity/                       Hospital, NonPayItem, Price (+ PriceId), PriceSummary, NonPayItemDesc, NonPayItemClcdStat, NonPayItemSidoStat, Member, Favorite
dto/                          HospitalSummaryDto, HospitalDetailDto, NonPayItemGroupDto, FavoriteDto
repository/                   HospitalRepository, NonPayItemRepository, PriceRepository, MemberRepository, FavoriteRepository
service/                      HospitalService, HospitalDetailService, NonPayItemService, AuthService, GoogleOAuthService, ConsentService, FavoriteService
controller/                   HospitalController, HospitalApiController, NonPayItemApiController, AuthController, AuthApiController, FavoriteController, FavoriteApiController, HealthController, HealthApiController, LegalController
                              ※ @Controller와 @RestController를 한 폴더에 평면 배치 — 별도 api/ 폴더 없음
util/                         JwtUtil
client/                       HiraHospitalClient, HiraNonPayClient, HiraDetailClient
client/hira/                  HiraResponse / HiraHeader / HiraBody + item XML DTO들
batch/admin/                  BatchAdminApiController (수동 트리거)
batch/orchestrator/           BatchService (전체 배치 dispatch)
batch/hospital/               HospitalSyncService, HospitalBatchWriter
batch/item/                   NonPayItemSyncService, NonPayItemDescSyncService + writer
batch/price/                  PriceSyncService, PriceYkihoSyncService + writer
batch/summary/                PriceSummarySyncService + writer
batch/stat/                   NonPayItemClcdStatSyncService, NonPayItemSidoStatSyncService + writer
batch/support/                SidoCode 등 배치 공통 코드
```

---

## 6. JavaScript 코딩 컨벤션

### 6.1 네이밍

```
파일명          kebab-case                hospital-map.js, price-list.js
함수명          camelCase + 동사          fetchHospitals(), renderMarkers()
변수명          camelCase                 hospitalList, currentPosition
상수            UPPER_SNAKE_CASE          MAX_RETRY_COUNT, BASE_URL
이벤트 핸들러   handle 접두사             handleSearchClick(), handleMapDrag()
fetch 함수      fetch 접두사              fetchHospitals(), fetchPriceList()
render 함수     render 접두사             renderMarkers(), renderHospitalCard()
```

### 6.2 필수 규칙

```js
// var 사용 금지, const/let만 사용
const hospitalList = [];     // O
var hospitalList = [];       // X

// 화살표 함수 사용
const fetchHospitals = async (lat, lng) => { };    // O
function fetchHospitals(lat, lng) { };             // 선호하지 않음

// async/await 사용 (Promise.then 지양)
const hospitals = await fetchHospitals(lat, lng);  // O
fetchHospitals().then(data => { });                // X

// Optional Chaining 적극 사용
const name = hospital?.info?.name ?? '정보 없음';

// 세미콜론 필수
const name = 'MediPrice';    // O
const name = 'MediPrice'     // X
```

### 6.3 fetch 공통 유틸 (반드시 사용)

```js
// static/js/api.js — 모든 HTTP 요청은 이 유틸을 통해서만
const api = {
    async get(url) {
        const res = await fetch(url, {
            headers: { 'Authorization': `Bearer ${getToken()}` }
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    },
    async post(url, data) {
        const res = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            },
            body: JSON.stringify(data)
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
    }
};
```

### 6.4 파일 구조

```
static/js/
├── api.js          ← fetch 공통 유틸 (필수, 항상 먼저 로드)
├── auth.js         ← 토큰 관리, 로그인/로그아웃
├── map.js          ← 네이버맵 초기화, 마커 관리
├── hospital.js     ← 병원 목록, 검색
└── common.js       ← 공통 유틸 함수
```

### 6.5 JS 로드 순서 (JSP에서 반드시 지킬 것)

```jsp
<%-- 항상 이 순서로 로드 --%>
<script type="text/javascript" src="https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${naverMapKey}"></script>
<script defer src="/static/js/api.js"></script>
<script defer src="/static/js/auth.js"></script>
<script defer src="/static/js/common.js"></script>
<script defer src="/static/js/MarkerClustering.js"></script>
<script defer src="/static/js/map.js"></script>   <%-- 페이지별 --%>
```

---

## 7. JSP 컨벤션

```jsp
<%-- JSTL import는 반드시 jakarta 패키지 사용 --%>
<%@ taglib prefix="c"      uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt"    uri="jakarta.tags.fmt" %>
<%@ taglib prefix="form"   uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%-- JSP에 Java 코드 직접 작성 금지 (스크립틀릿 금지) --%>
<% String name = "test"; %>    // X
${name}                        // O (EL 사용)

<%-- 데이터는 EL로만 출력 --%>
${hospital.name}
<c:forEach var="h" items="${hospitals}">
    ${h.name}
</c:forEach>
```

---

## 8. DB 스키마 컨벤션

```sql
-- 테이블: PascalCase
CREATE TABLE Hospital (
    id            BIGSERIAL PRIMARY KEY,
    hira_code     VARCHAR(20)  UNIQUE NOT NULL,   -- 심평원 코드
    name          VARCHAR(100) NOT NULL,
    type          VARCHAR(50),                     -- 병원 종류
    address       VARCHAR(200),
    location      GEOGRAPHY(POINT, 4326),          -- PostGIS 위치
    phone         VARCHAR(20),
    created_dttm  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_dttm  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_dttm  TIMESTAMP WITH TIME ZONE NULL    -- 논리 삭제
);

-- PostGIS 인덱스 (위치 검색 성능)
CREATE INDEX idx_hospital_location ON Hospital USING GIST(location);

-- 위치 기반 검색 표준 쿼리
SELECT *,
    ST_Distance(location, ST_MakePoint(:lng, :lat)::geography) AS distance
FROM Hospital
WHERE ST_DWithin(location, ST_MakePoint(:lng, :lat)::geography, :radiusMeters)
    AND deleted_dttm IS NULL
ORDER BY distance
LIMIT 20;
```

---

## 9. 금지 사항

```
절대 사용 금지
  Spring Boot                  (순수 Spring Framework만)
  Maven                        (Gradle Groovy DSL만)
  jQuery                       (Vanilla JS만)
  axios                        (fetch API만)
  React / Vue / Angular        (JSP + Vanilla JS만)
  Redis / Caffeine             (Spring 메모리 캐시만)
  세션(HttpSession)            (JWT만)
  JSP 스크립틀릿 (<% %>)       (EL + JSTL만)
  var 키워드                   (const/let만)
  Promise.then 체이닝          (async/await만)
  import java.util.*           (명시적 import만)
  PostGIS 없이 Haversine 사용  (ST_DWithin 사용)
```

---

## 9.5 Git 커밋 규칙

```
절대 추가 금지
  Co-Authored-By: Claude... 트레일러   (commit 메시지에 도구/AI 출처 표시 금지)
  🤖 Generated with [Claude Code]      (이모지 + 생성 표기 금지)
```

GitHub은 `Co-Authored-By:` 트레일러를 자동으로 공동 저자로 표시한다 (commit 페이지 + contributors 그래프). MediPrice 저장소는 사람 저자만 표기한다 — 도구/AI 출처는 PR 설명이나 별도 메모로 충분.

커밋 메시지 컨벤션:
- Conventional Commits 접두사 (`feat`, `fix`, `refactor`, `docs`, `test`, `build`, `chore`, `ci`)
- 한국어 + 짧게 (1줄, 60자 이내). 자세한 내용은 PR 설명에.
- 본문은 **why**만 짧게 (필요 시). **what**은 diff가 말해줌.

---

## 10. 환경 변수 (.env)

```properties
# DB
DB_URL=jdbc:postgresql://db:5432/mediprice
DB_USERNAME=admin
DB_PASSWORD=changeme

# JWT
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=86400000

# 네이버맵
NAVER_MAP_KEY=your-naver-key

# 심평원 API — 단일 키 fallback
HIRA_API_KEY=your-hira-key
# 심평원 API — 다중 키 (콤마 구분, 라운드로빈). 일일 한도 10,000건/키 회피용.
HIRA_API_KEYS=key1,key2,key3,key4,key5

# 캐시
CACHE_TTL_SECONDS=3600
```
