# MediPrice 프로젝트 개요

## 서비스 소개

**MediPrice(메디프라이스)** — 비급여 진료비 비교 플랫폼

병원마다 제각각인 비급여 진료비(MRI, 도수치료, 임플란트 등)를 지도 기반으로 비교하는 학교 팀 프로젝트용 MVP.

- **성격:** 학교 팀 프로젝트, 발표용 프로토타입
- **데이터 출처:** 건강보험심사평가원(심평원) 공공데이터포털 OpenAPI
- **기본 전략:** 외부 API를 실시간 검색에 직접 쓰지 않고, 월 배치로 DB에 적재한 뒤 서비스 API는 DB를 조회한다.

## 진행 상태 (2026-06-05)

| 단계 | 내용 | 상태 |
|---|---|---|
| **P0** | Docker/PostgreSQL/PostGIS/Tomcat/Spring 기본 부팅 | 완료 |
| **P0.5** | SecurityFilterChain 분리, TraceIdFilter, Hikari/Hibernate 보강, 통일 로그 | 완료 |
| **P1** | 심평원 배치 7종, REST API 3개, PostGIS 검색 프로시저, 테스트 94개 | 완료 |
| **P1.5** | 심평원 상세 API 5종 매핑 수정, Sido 통계 코드 수정, 배치 병렬화 | 완료 |
| **P2** | JSP/네이버맵/정적 JS 화면, Google OAuth, JWT 쿠키, 즐겨찾기 | 진행/구현됨 |
| **다음 라운드** | 배치 안정화, 운영 보호, 가격 데이터 완전 적재, 상세 UI 보강 | 대기 |

검색 API는 공개 상태이며, Guest JWT 기반 검색 제한 정책은 적용하지 않는다. 회원 기능은 Google OAuth + JWT 쿠키 기반으로 구현되어 있고, 즐겨찾기 API는 인증된 회원을 전제로 한다.

## 핵심 기능

### 1. 비급여 항목 카탈로그

- `getNonPaymentItemCodeList2` 결과를 `NonPayItem`에 적재한다.
- 2026-05-17 로컬 배치 기준 875개 항목이 적재되었다.
- 프론트 검색 셀렉트박스와 카테고리 옵션의 원천 데이터다.

### 2. 위치 기반 병원 검색

- 병원 기본정보는 `getHospBasisList`를 시도별/페이지별로 전체 수집한다.
- PostGIS `search_nearby_hospitals_v2(lat, lng, npayCds[], radius, sort, limit, wPrice, wDistance)` 프로시저로 거리·가격·혼합 점수 정렬을 한 호출에서 처리한다.
- `Hospital.location`은 JPA 엔티티 필드로 매핑하지 않고 native update로 `ST_MakePoint(x, y)::geography`를 채운다.

### 3. 병원 상세

- DB의 `Hospital`, `Price`, `NonPayItem`을 조합해 기본 상세와 가격 목록을 만든다.
- 심평원 의료기관별 상세정보서비스 5개 API를 실시간 병렬 호출한다.
- 호출 API: 진료과목, 의료장비, 대중교통, 세부정보(주차/진료시간/응급), 특수진료.
- 외부 상세 API 일부가 실패해도 해당 섹션만 빈 값으로 fallback한다.

### 4. 화면과 회원 기능

- JSP 화면: `/`, `/hospitals`, `/hospital`, `/favorites`, `/auth/consent`, `/legal/**`.
- 지도: 네이버맵 JavaScript SDK v3 + 커스텀 마커 클러스터링.
- 인증: Google OAuth 시작/콜백/약관 동의 후 `mp_token` JWT 쿠키 발급.
- 즐겨찾기: 회원별 병원 저장, 삭제, 상태 조회와 즐겨찾기 페이지 지도 표시.

## 데이터 흐름

### 배치

```
@Scheduled "0 0 0 1 * *" 또는 POST /api/internal/batch/sync
    ↓
BatchService.syncAll()
    ├─ NonPayItemSyncService          항목 코드
    ├─ HospitalSyncService            병원 기본정보
    ├─ NonPayItemDescSyncService      구버전 항목 설명
    ├─ PriceSummarySyncService        병원×항목 가격 요약
    ├─ NonPayItemClcdStatSyncService  의료기관 종별 통계
    ├─ NonPayItemSidoStatSyncService  시도별 통계
    └─ PriceSyncService               병원별 가격 상세
```

현재 구조는 7개 작업을 `hiraBatchExecutor`에 동시에 dispatch한다. 단, `PriceSyncService`만 `Hospital` 결과에 의존한다. 가격 상세 API가 병원 식별자 `ykiho`를 입력값으로 받기 때문에, DB에 `Hospital.ykiho` 목록이 먼저 확보되어야 한다.

Price의 다음 개선 방향은 `HospitalSyncService`가 생산한 `ykiho`를 큐/파이프라인으로 바로 넘겨 가격 워커가 병렬 소비하게 만드는 것이다. 이렇게 하면 전체 Hospital 완료를 기다리지 않고도 Price 적재를 시작할 수 있다.

다중 API 키는 `HIRA_API_KEYS=k1,k2,k3,...`를 우선 사용하고, 값이 없으면 `HIRA_API_KEY`를 fallback으로 사용한다. `HiraServiceKeyProvider`가 라운드로빈으로 키를 선택한다.

### 실시간 사용자 API

```
GET /api/items
    → DB의 NonPayItem 전체를 중분류 기준 그룹핑

GET /api/hospitals/search?lat&lng&npayCds&radius&sort&limit&wPrice&wDistance
    → search_nearby_hospitals_v2 PostGIS 프로시저 호출 (다중 npayCd + 정렬 모드 + 가중치)
    → mixed/price/distance 정렬, ykiho별 DISTINCT ON 최저가 JSON 반환
    → service가 매칭 항목명(NonPayItem) + 종별 평균(NonPayItemClcdStat) batch enrich

GET /api/hospitals/{ykiho}/basics
    → DB only fast — Hospital + Price(active) + NonPayItem 이름 매핑

GET /api/hospitals/{ykiho}/extras
    → HIRA 5종 캐시 — 진료과목/장비/교통/주차·운영/특수진료 (외부 API 5개 병렬)

GET /api/hospitals/{ykiho}
    → 통합 응답 (basics + extras 합본)

GET /api/favorites, POST /api/favorites, DELETE /api/favorites/{ykiho}
    → JWT 회원 인증 후 Favorite soft delete/restore 기반 저장
```

## API 엔드포인트

### 프론트가 호출

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/items` | 비급여 항목 그룹 |
| GET | `/api/hospitals/search?lat&lng&npayCds&radius&sort&limit&wPrice&wDistance` | 위치 + 다중 비급여항목 검색 (v2) |
| GET | `/api/hospitals/{ykiho}/basics` | 병원 상세 fast — DB only |
| GET | `/api/hospitals/{ykiho}/extras` | 병원 상세 slow — HIRA 5종 캐시 |
| GET | `/api/hospitals/{ykiho}` | 병원 상세 통합 응답 |
| GET | `/api/favorites` | 회원 즐겨찾기 목록 |
| POST | `/api/favorites` | 회원 즐겨찾기 추가 |
| DELETE | `/api/favorites/{ykiho}` | 회원 즐겨찾기 삭제 |

### 운영/디버그

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/health` | JSON probe |
| GET | `/health` | JSP probe |
| POST | `/api/internal/batch/sync` | 전체 배치 트리거 |
| POST | `/api/internal/batch/sync/prices` | Price 단독 |
| POST | `/api/internal/batch/sync/desc` | NonPayItemDesc 단독 |
| POST | `/api/internal/batch/sync/summary` | PriceSummary 단독 |
| POST | `/api/internal/batch/sync/clcd-stat` | 종별 통계 단독 |
| POST | `/api/internal/batch/sync/sido-stat` | 지역 통계 단독 |

`/api/internal/batch/**`는 `BatchAdminGuard`가 `batch.admin-enabled=true`와
`X-Batch-Admin-Secret` 헤더 일치를 모두 확인한 뒤에만 실행한다. 기본값은 fail-closed다.

모든 REST 응답은 `ApiResponse<T>` 형태다.

## 로컬 배치 스냅샷 (2026-05-17)

| 테이블 | 적재 상태 | 비고 |
|---|---:|---|
| `Hospital` | 79,674 | 전국 병원 기본정보 완료 |
| `NonPayItem` | 875 | 현행 비급여 항목 코드 완료 |
| `NonPayItemDesc` | 54 | 구버전 분류/설명 완료 |
| `NonPayItemClcdStat` | 2,459 | 종별 통계 완료 |
| `NonPayItemSidoStat` | 6,988 | 지역 통계 재적재 완료, 일부 시도 필드는 API/docx에서 부재 |
| `Price` | 79,428 | 가격 상세 부분 적재, quota 초과로 중단 |
| `PriceSummary` | 122,097 | 가격 요약 부분 적재, quota 초과/중단 영향 |

`Price`와 `PriceSummary`는 DB에 저장된 부분 데이터가 보존되어 있다. 다만 모든 병원/항목의 가격이 완전하다고 보면 안 된다. 다음 quota window 또는 운영계정 전환 후 이어서 검증해야 한다.

## 알려진 데이터 한계

- 가격 상세 API는 `ykiho`별 호출량이 매우 커서 개발 키로는 전체 완주가 어렵다.
- `getNonPaymentItemSidoCdList`의 지역별 통계 필드는 공식 문서와 실제 응답이 완전히 일치하지 않는다.
- 현재 확인된 지역 통계 응답 key는 `All`, `Sl`, `Tg`, `Ich`, `Kw`, `Dj`, `Usn`, `Sj`, `Kyg`, `Kaw`, `Ccn`, `Ksb`, `Ksn`이다.
- 부산(`Bs`), 충북(`Cb`), 전북(`Jb`), 전남(`Jn`), 제주(`Jj`)는 현재 응답/문서 기준으로 별도 필드가 확인되지 않았다.
- 미용시술(보톡스/필러/쌍꺼풀/지방흡입 등)은 심평원 비급여 신고 데이터에서 충분히 제공되지 않아 비교 서비스 핵심 항목으로 삼기 어렵다.

## 알려진 TODO / 위험 요소

- `BatchAdminApiController`는 운영 배포 전 인증/프로파일/IP 제한으로 보호해야 한다.
- HIRA 응답 실패와 진짜 데이터 없음이 같은 empty body로 취급될 수 있어, 가격 계열 배치의 부분 적재 감지와 재개 정책 보강이 필요하다.
- 상세 캐시는 `ConcurrentMapCache` 기반이라 TTL이 없고, 배치 후 가격 변경분이 서버 재시작 전까지 상세 응답에 남을 수 있다.
- 회원 JWT 쿠키의 HttpOnly/Secure/SameSite 정책과 GUEST의 회원 API 접근 차단을 정리해야 한다.
- 심평원 운영계정 신청이 필요하다. 개발 키는 가격 상세 전체 적재에 부족하다.
- Price 배치는 Hospital 전체 완료 후 시작하는 구조다. Hospital producer가 `ykiho`를 내보내고 Price worker가 바로 소비하는 파이프라인으로 개선할 수 있다.
- Price/PriceSummary 부분 적재분은 재실행 정책을 정해야 한다. truncate 후 전체 재적재인지, PK 기준 upsert로 이어받을지 결정이 필요하다.
- `getNonPaymentItemSidoCdList`의 누락 지역 필드는 공식 문서와 실제 응답을 추가 대조해야 한다.

## 참고 문서

- `docs/hira-api-spec.md` — 심평원 API 응답 구조와 DB 가공 방식
- `docs/troubleshooting.md` — 이번 배치/인코딩/통계 코드 장애 이력
- `docs/feature-spec.md` — 기능 명세서와 사용자 플로우
- `docs/layered-architecture.md` — 패키지/요청 흐름/레이어 규칙
- `docs/authentication.md` — Security 구조
- `docs/error-handling.md` — ErrorCode 체계 + GlobalExceptionHandler
- `docs/null-safety.md` — JSpecify 컨벤션
- `CLAUDE.md` — 프로젝트 작업 규칙
