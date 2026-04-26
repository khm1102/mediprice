# MediPrice — CLAUDE.md
> Claude Code가 이 프로젝트에서 코드를 생성할 때 반드시 따라야 하는 규칙 문서.
> 이 문서에 정의되지 않은 사항은 임의로 판단하지 말고 반드시 질문할 것.

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
  Map         카카오맵 JavaScript SDK

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
Spring Security (JWT 인증/인가 + 비회원 검색 횟수 체크)
    ↓
┌─────────────────────────────────┐
│  @Controller    │  @RestController│
│  JSP 렌더링     │  JSON 응답      │
│  /hospital/**   │  /api/**        │
└─────────────────────────────────┘
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
  로그인 → JWT 발급 → 쿠키 저장 (HttpOnly)
  요청마다 JWT 검증 → 검색 횟수 제한 없음

비회원
  첫 접속 → 임시 Guest JWT 발급 → 쿠키 저장
  검색 3회 초과 → 로그인 유도
  횟수 관리 → 서버 메모리 (ConcurrentHashMap, guestId 기준)
```

### 3.3 캐싱 구조

```
심평원 API 결과 → Spring ConcurrentMapCache
  키: 지역코드 + 진료항목
  만료: 서버 재시작 시 초기화 (프로토타입 수준)
  목적: 동일 요청 반복 시 API 재호출 방지
```

### 3.4 Controller vs RestController 분리

```
@Controller (/hospital/**, /auth/**)
  → JSP 초기 렌더링 담당
  → 페이지 이동, 레이아웃

@RestController (/api/**)
  → JS fetch 요청 처리
  → JSON 응답만 반환
  → Spring Security JWT로 권한 체크

공통
  → Service 레이어는 하나만 (Controller/RestController 공유)
```

---

## 4. API URL 컨벤션

```
# 리소스는 복수형, kebab-case
GET    /api/hospitals                    병원 목록 (위치 기반)
GET    /api/hospitals/{id}               병원 상세
GET    /api/hospitals/{id}/prices        병원 가격 목록
GET    /api/hospitals/search             병원 검색

# 쿼리 파라미터로 필터링
GET    /api/hospitals?lat=37.5&lng=126.9&radius=2&category=vaccination

# 인증
POST   /api/auth/login                   로그인
POST   /api/auth/logout                  로그아웃
POST   /api/auth/register                회원가입
GET    /api/auth/token/guest             비회원 임시 토큰 발급

# JSP 페이지 (Controller)
GET    /                                 메인
GET    /hospitals                        병원 목록 페이지
GET    /hospitals/{id}                   병원 상세 페이지
GET    /auth/login                       로그인 페이지
GET    /auth/register                    회원가입 페이지
```

---

## 5. Java 코딩 컨벤션

> 기준: 아이알엠(IRM) HE Coding Conventions

### 5.1 네이밍

```
패키지     소문자만                     com.mediprice.hospital
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
config/              AppConfig, WebMvcConfig, WebAppInitializer, JpaConfig, SecurityConfig, SecurityInitializer, CacheConfig
entity/              BaseEntity, Hospital, PriceInfo, Member
dto/                 ApiResponse, HospitalDto, HospitalSearchRequest, HospitalResponse
service/             HospitalService, PriceService, AuthService
repository/          HospitalRepository, PriceRepository
controller/          HospitalController, AuthController, HealthController
api/                 HospitalApiController, PriceApiController
filter/              JwtAuthFilter
util/                JwtUtil, GeoUtil
exception/           MediPriceException, ErrorCode, GlobalExceptionHandler
exception/auth/      AuthenticationException, LoginFailedException, TokenExpiredException, TokenInvalidException, AuthorizationDeniedException
exception/business/  BusinessException, HospitalNotFoundException, PriceNotFoundException, MemberNotFoundException, DuplicateEmailException, GuestSearchLimitExceededException
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
├── map.js          ← 카카오맵 초기화, 마커 관리
├── hospital.js     ← 병원 목록, 검색
├── price.js        ← 가격 비교
└── common.js       ← 공통 유틸 함수
```

### 6.5 JS 로드 순서 (JSP에서 반드시 지킬 것)

```jsp
<%-- 항상 이 순서로 로드 --%>
<script src="//dapi.kakao.com/v2/maps/sdk.js?appkey=..."></script>
<script defer src="/static/js/api.js"></script>
<script defer src="/static/js/auth.js"></script>
<script defer src="/static/js/common.js"></script>
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

## 10. 환경 변수 (.env)

```properties
# DB
DB_URL=jdbc:postgresql://db:5432/mediprice
DB_USERNAME=admin
DB_PASSWORD=changeme

# JWT
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=86400000

# 카카오맵
KAKAO_MAP_KEY=your-kakao-key

# 심평원 API
HIRA_API_KEY=your-hira-key

# 캐시
CACHE_TTL_SECONDS=3600
GUEST_SEARCH_LIMIT=3
```