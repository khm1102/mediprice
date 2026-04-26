# MediPrice 프로젝트 개요

## 서비스 소개

**MediPrice(메디프라이스)** — 비급여 진료비 비교 플랫폼

병원마다 제각각인 비급여 진료비(MRI, 도수치료, 임플란트 등)를 지도 기반으로 한눈에 비교할 수 있는 서비스.

- **성격:** 학교 팀 프로젝트 (발표용 프로토타입 → MVP)
- **데이터 출처:** 건강보험심사평가원(심평원) 공공 OpenAPI

## 진행 상태 (2026-04-26)

| 단계 | 내용 | 상태 |
|---|---|---|
| **P0** | 인프라 (Docker/PG/Tomcat/Spring 부팅) | ✅ 완료 |
| **P0.5** | SecurityFilterChain 분리, TraceIdFilter, Hikari/Hibernate 보강, 통일 로그 | ✅ 완료 |
| **P1** | 심평원 3개 API 동기화 + REST API 3개 + PostGIS 프로시저 + 단위 테스트 45개 | ✅ 완료 |
| **다음 라운드** | JSP 페이지 + 카카오맵 SDK + 정적 JS | 대기 |
| **P2** | 회원가입/로그인 (JWT), 즐겨찾기, AI 추천 등 | 보류 |

**비회원 기능 미구현 확정** — Guest JWT/검색 횟수 제한 모두 없음. 모든 `/api/**`는 비인증 통과.

## 핵심 기능 (구현 완료)

### 1. 비급여 항목 카탈로그
- 심평원 항목 코드 875개를 중분류 기준으로 그룹핑해 제공
- 검색 셀렉트박스/카테고리 옵션 채울 때 사용

### 2. 위치 기반 병원 검색
- PostGIS `search_nearby_hospitals(lat, lng, npayCd, radius)` PL/pgSQL
- 거리 기준 정렬 + 20건 제한
- 가격 신고된 병원만 노출 (현재 1,558개)

### 3. 병원 상세
- DB(병원 + 비급여 가격 + 항목명 매핑) + 외부 4개 API 실시간 병합
- 외부 API: 진료과목, 의료장비, 교통, 특수진료
- 4개 API CompletableFuture 병렬 호출 (`hiraDetailExecutor` 풀)

## 데이터 흐름

### 배치 (월 1회 + 수동 트리거)
```
@Scheduled "0 0 0 1 * *"  또는  POST /api/internal/batch/sync
    ↓
NonPayItemSyncService → 항목 코드 upsert
    ↓
HospitalSyncService → 시도 17개 × 페이징 → 병원 upsert + ST_MakePoint location
    ↓
PriceSyncService → DB ykiho 순회 → ykiho당 가격상세 → Price upsert
                   (adt_end_dd='99991231'만 저장)
```

### 실시간 (사용자 검색)
```
GET /api/items
    → DB의 NonPayItem 전체 (중분류 그룹핑)

GET /api/hospitals?lat&lng&npayCd&radius
    → PostGIS 프로시저 호출 → JSON 배열 반환

GET /api/hospitals/{ykiho}
    ├── DB: Hospital + Price(active) + 이름 매핑
    └── 외부 4개 API 병렬 호출 (장애 시 빈 결과 fallback)
```

## API 엔드포인트

### 프론트가 호출 (3개)
| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/items` | 비급여 항목 그룹 |
| GET | `/api/hospitals?lat&lng&npayCd&radius` | 근거리 병원 검색 |
| GET | `/api/hospitals/{ykiho}` | 병원 상세 |

### 운영/디버그 (FE 호출 X)
| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/health` | JSON probe |
| GET | `/health` | JSP probe |
| POST | `/api/internal/batch/sync` | 전체 배치 트리거 |
| POST | `/api/internal/batch/sync/prices` | 가격만 트리거 |

> Postman 컬렉션: `postman/MediPrice.postman_collection.json`.

응답은 모두 `ApiResponse<T>` 래핑 — `{ "success": bool, "data": T?, "error": {code,message}? }`.

## 데이터 실태와 한계 (반드시 인지)

### 규모
- 병원 3,669 (location 매핑 99.9%)
- 비급여 항목 875, 가격 신고된 항목 648
- 가격 신고 병원 **1,558 (전체 42%)**, 가격 row 94,962개

### 시도 커버리지 — 절반만
**수집 완료**: 서울(391 신고), 부산(329), 경기(193), 대구(187), 광주(161), 대전(103), 인천(98), 울산(82), 세종(14)

**미수집**: 경북, 경남, 전북, 전남, 충북, 충남, 강원, 제주 — 외부 API timeout으로 배치가 멈춤. 지방 사용자는 검색 결과 0건.

### 항목 분포 — 핵심 문제
가격 신고 top 15 중 **12개가 "제증명수수료"**(진단서/사본/영문진단서 등 행정 비용). 평균 1만원 이하라 비교 가치 거의 없음.

의료시술 신고 현황:
- **도수치료**: 971개 병원 (의료시술 1위)
- **MRI** 척추/관절/뇌 부위별: 480~580개
- **임플란트** (재료별): 11~113개
- **라식/라섹**: 20 / 26
- **모발이식**: 5~14
- **보톡스/필러/쌍커풀/지방흡입/박피**: 신고 0 (심평원 의무신고 대상이 아님)

### 솔직한 포지셔닝
"비급여 진료비 비교"라는 슬로건의 현실 = **MRI / 도수치료 / 임플란트 비교 사이트**. 미용시술 비교는 데이터가 없어 사실상 불가. 발표/MVP에서 이 한계를 인정하면서 강점(거리+가격 결합 검색, 부위별 MRI 비교)을 강조하는 게 정직한 접근.

## 페이지 구성

| 경로 | 종류 | 설명 | 단계 |
|---|---|---|---|
| `/api/items` | JSON | 비급여 항목 — 중분류 그룹 | ✅ |
| `/api/hospitals` | JSON | 위치 기반 병원 검색 | ✅ |
| `/api/hospitals/{ykiho}` | JSON | 병원 상세 | ✅ |
| `/api/health` | JSON | probe | ✅ |
| `/health` | JSP | probe | ✅ |
| `/api/internal/batch/sync` | JSON | 디버그 트리거 | ✅ (운영 전 제거 또는 보호 필요) |
| `/` | JSP | 메인 (검색 입력 + 지도 미리보기) | 다음 라운드 |
| `/hospitals` | JSP | 지도 + 병원 리스트 | 다음 라운드 |
| `/hospitals/{ykiho}` | JSP | 병원 상세 페이지 | 다음 라운드 |
| `/auth/login`, `/auth/register` | JSP | 로그인/회원가입 | P2 (보류) |

## 알려진 TODO / 위험 요소

- **N1**: `BatchAdminApiController`가 비인증 노출. 외부에서 누구나 배치 트리거 가능 → 심평원 일 1만건 즉시 소진 위험. `@Profile("dev")` 또는 IP 화이트리스트 필요.
- **N2**: 누락 8개 시도 백필 — Hospital-only 트리거 추가 또는 timeout 늘려 재실행.
- **N3**: 항목 응답에서 "제증명수수료" 그룹 분리/숨김 — UX 노이즈.
- **N4**: 외부 API 회복력 — 단발 호출 + 빈 결과 fallback. 회로차단기/재시도 미적용.
- **N5**: `PriceSyncService` 외 sync는 전체 트랜잭션 — 운영 부하 모니터링 후 분리 검토.
- **N6**: 심평원 운영계정 신청 (개발 일 1만건 한계).

## 참고 문서

- `docs/feature-spec.md` — 기능 명세서 (사용자 플로우, 화면 와이어프레임 — 작성 중)
- `docs/layered-architecture.md` — 패키지/요청 흐름/레이어 규칙
- `docs/authentication.md` — Security 구조 (현재는 비인증 통과)
- `docs/error-handling.md` — ErrorCode 체계 + GlobalExceptionHandler
- `docs/null-safety.md` — JSpecify 컨벤션
- `CLAUDE.md` — 코드 컨벤션 (Java/JS/JSP/DB)
