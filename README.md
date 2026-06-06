<div align="center">

# MediPrice

**비급여 진료비 비교 플랫폼**

[![Java](https://img.shields.io/badge/Java%2021-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring](https://img.shields.io/badge/Spring%207.0.6-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-framework)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![PostGIS](https://img.shields.io/badge/PostGIS-4EAA25?style=flat-square&logo=postgresql&logoColor=white)](https://postgis.net/)
[![Tomcat](https://img.shields.io/badge/Tomcat%2011-F8DC75?style=flat-square&logo=apachetomcat&logoColor=black)](https://tomcat.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)

</div>

---

<table align="center">
  <tr>
    <td align="center"><img src="https://github.com/kmj228.png" width="100px;"/><br /><sub><b>김민재</b></sub><br /><a href="https://github.com/kmj228">kmj228</a></td>
    <td align="center"><img src="https://github.com/khm1102.png" width="100px;"/><br /><sub><b>김현민</b></sub><br /><a href="https://github.com/khm1102">khm1102</a></td>
    <td align="center"><img src="https://github.com/sanggeon-bot.png" width="100px;"/><br /><sub><b>이상건</b></sub><br /><a href="https://github.com/sanggeon-bot">sanggeon-bot</a></td>
    <td align="center"><img src="https://github.com/boys5210boys5210-stack.png" width="100px;"/><br /><sub><b>정재운</b></sub><br /><a href="https://github.com/boys5210boys5210-stack">boys5210boys5210-stack</a></td>
  </tr>
  <tr>
    <td align="center">PM</td>
    <td align="center">PL</td>
    <td align="center">QA</td>
    <td align="center">Docs</td>
  </tr>
</table>

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [아키텍처](#아키텍처)
- [패키지 구조](#패키지-구조)
- [시작하기](#시작하기)
- [환경변수 설정](#환경변수-설정)
- [API 명세](#api-명세)
- [인증 방식](#인증-방식)
- [문서](#문서)

---

## 프로젝트 소개

도수치료, 영양주사, 추나요법처럼 <br/>
건강보험이 적용되지 않는 비급여 항목은 병원마다 가격이 천차만별이지만, <br/>
환자가 진료 전에 가격을 확인할 공식적인 수단이 없다.<br/>

MediPrice는 건강보험심사평가원(심평원) 공공 API를 기반으로 <br/>
주변 병원의 비급여 진료비를 지도 위에 시각화하는 웹 서비스다. <br/>
원하는 항목을 검색하면 현재 위치 기준으로 가까운 병원들의 가격이 핀으로 표시되고, <br/>
전화하거나 직접 찾아가지 않아도 주변 병원의 비급여 가격을 한눈에 비교할 수 있다.<br/>

---

## 주요 기능

- **지도 기반 가격 시각화** — 네이버맵 API 기반. 주변 병원의 가격 분포를 한눈에 파악. 지도 이동 시 해당 영역 병원 정보 자동 업데이트.
- **비급여 항목 검색** — `/api/items`의 항목 카탈로그를 브라우저에서 키워드 매칭하고, 최대 10개 `npayCd`로 병원 검색.
- **병원 상세 패널** — 비급여 가격 목록, 연락처, 진료과목, 의료장비, 대중교통, 주차/운영 정보를 제공.
- **Google OAuth 로그인** — 약관 동의 후 JWT 쿠키 발급, 회원 탈퇴와 로그아웃 지원.
- **즐겨찾기** — 로그인 회원이 관심 병원을 저장하고 `/favorites` 페이지에서 지도와 목록으로 확인.

---

## 기술 스택

### Backend
[![Java](https://img.shields.io/badge/Java%2021-007396?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring](https://img.shields.io/badge/Spring%207.0.6-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-framework)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)

### Frontend
[![JSP](https://img.shields.io/badge/JSP%20+%20JSTL-D22128?style=for-the-badge&logo=apache&logoColor=white)]()
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![JavaScript](https://img.shields.io/badge/Vanilla%20JS-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)]()
[![Naver Map](https://img.shields.io/badge/Naver%20Map%20API-03C75A?style=for-the-badge&logo=naver&logoColor=white)](https://navermaps.github.io/maps.js.ncp/)

### Infra & DB
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![PostGIS](https://img.shields.io/badge/PostGIS-4EAA25?style=for-the-badge&logo=postgresql&logoColor=white)](https://postgis.net/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Tomcat](https://img.shields.io/badge/Tomcat%2011-F8DC75?style=for-the-badge&logo=apachetomcat&logoColor=black)](https://tomcat.apache.org/)
[![Cloudflare Tunnel](https://img.shields.io/badge/Cloudflare%20Tunnel-F38020?style=for-the-badge&logo=cloudflare&logoColor=white)](https://www.cloudflare.com/products/tunnel/)

---

## 아키텍처

- 순수 Spring Framework 7 + Tomcat 11 WAR 배포 구조다.
- REST API는 DB를 조회하고, 심평원 OpenAPI는 배치 또는 병원 상세 보강 정보에서만 호출한다.
- 배치는 7개 SyncService를 병렬 dispatch한다. 단, 가격 상세(`PriceSyncService`)는 병원 `ykiho` 목록이 필요해 Hospital 완료 뒤 실행한다.
- 병원 상세는 DB 정보와 심평원 상세 API 5개(진료과목, 의료장비, 대중교통, 세부정보, 특수진료)를 병렬 병합한다.

자세한 구조는 `docs/layered-architecture.md`를 기준으로 한다.

---

## 패키지 구조

- `controller/` — JSP Controller와 JSON RestController
- `service/` — 검색/상세/항목/Auth/OAuth/즐겨찾기 비즈니스 로직
- `batch/admin/` — 수동 배치 트리거
- `batch/orchestrator/` — 전체 배치 병렬 실행
- `batch/hospital/`, `batch/item/`, `batch/price/`, `batch/summary/`, `batch/stat/` — 도메인별 sync/writer
- `client/` — 심평원 외부 API client
- `entity/`, `repository/`, `dto/` — JPA 도메인과 API 응답
- `global/` — 설정, 공통 응답, 예외, 필터

---

## 시작하기

### 사전 요구사항

- Docker & Docker Compose
- 심평원 공공 API 키 ([공공데이터포털](https://www.data.go.kr) 발급)
- 네이버맵 API 키 ([네이버 클라우드 플랫폼](https://www.ncloud.com) 발급)

### 실행

```bash
git clone https://github.com/YOUR_REPO/mediprice.git
cd mediprice

# 환경변수 파일 설정 (아래 환경변수 설정 참고)
cp .env.example .env

# 실행
docker-compose up -d
```

서버가 정상적으로 올라오면 `http://localhost:8080` 으로 접속한다.

---

## 환경변수 설정

프로젝트 루트에 `.env` 파일을 생성하고 아래 항목을 채운다.

```env
# 데이터베이스 (Cloudflare Tunnel 프록시 경유 — 호스트 localhost:5774에 cloudflared 리스닝 필요)
DB_URL=jdbc:postgresql://host.docker.internal:5774/mediprice?reWriteBatchedInserts=true
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# JWT
JWT_SECRET=your_jwt_secret_here
JWT_EXPIRATION=86400000

# JWT 쿠키
COOKIE_SECURE=false
COOKIE_SAME_SITE=Lax

# 배치 수동 트리거 보호
BATCH_ADMIN_ENABLED=false
BATCH_ADMIN_SECRET=

# 네이버맵 API
NAVER_MAP_KEY=your_naver_map_key_here

# 심평원 공공 API
HIRA_API_KEY=your_hira_api_key_here
HIRA_API_KEYS=your_hira_key_1,your_hira_key_2

# 캐시
CACHE_TTL_SECONDS=3600
```

---

## API 명세

프론트가 직접 호출하는 주요 API:

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/items` | 비급여 항목 그룹 |
| GET | `/api/hospitals/search?lat&lng&npayCds&radius&sort&limit&wPrice&wDistance` | 위치 + 다중 비급여항목 검색 (v2 PostGIS 프로시저, mixed/price/distance 정렬, 매칭 항목명과 종별 평균 결합) |
| GET | `/api/hospitals/{ykiho}/basics` | 병원 상세 fast — DB only (가격 카드/표) |
| GET | `/api/hospitals/{ykiho}/extras` | 병원 상세 slow — HIRA 5종 캐시 (진료과목/장비/교통/주차·운영/특수진료) |
| GET | `/api/hospitals/{ykiho}` | 병원 상세 통합 응답 (basics + extras 한 번에) |
| GET | `/api/favorites` | 즐겨찾기 목록 (회원 JWT 필요) |
| POST | `/api/favorites` | 즐겨찾기 추가 (회원 JWT 필요) |
| DELETE | `/api/favorites/{ykiho}` | 즐겨찾기 삭제 (회원 JWT 필요) |

운영/디버그 배치 API (`BatchAdminGuard`로 이중 가드 — 둘 다 충족해야 동작):

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/internal/batch/sync` | 전체 배치 |
| POST | `/api/internal/batch/sync/prices` | 가격 상세 단독 |
| POST | `/api/internal/batch/sync/desc` | 항목 설명 단독 |
| POST | `/api/internal/batch/sync/summary` | 가격 요약 단독 |
| POST | `/api/internal/batch/sync/clcd-stat` | 종별 통계 단독 |
| POST | `/api/internal/batch/sync/sido-stat` | 지역 통계 단독 |

- `BATCH_ADMIN_ENABLED=true` (기본 false). 미설정 시 모든 요청 403 (`B001 BATCH_ADMIN_DISABLED`).
- `BATCH_ADMIN_SECRET=<32+ 문자 랜덤>` 설정 + 요청 헤더 `X-Batch-Admin-Secret`이 정확히 일치해야 통과. 둘 중 하나라도 누락/불일치 시 403 (`B003 BATCH_ADMIN_FORBIDDEN`).
- 비교는 상수시간(trim 금지). 운영에서는 두 변수 모두 설정해야 함.

---

## 인증 방식

검색/항목/헬스체크 API는 공개되어 있다. Google OAuth 로그인 후 서버가 `mp_token` JWT 쿠키를 발급하며, `/api/favorites/**`와 `/api/auth/me` 계열은 인증된 회원이 사용한다.

`/api/internal/batch/**`는 `BatchAdminGuard`가 enabled 플래그 + `X-Batch-Admin-Secret` 헤더를 이중 검사한다. 기본 fail-closed (`BATCH_ADMIN_ENABLED=false`, `BATCH_ADMIN_SECRET=`).

## 문서

- `docs/project-overview.md` — 최신 프로젝트 상태와 데이터 스냅샷
- `docs/hira-api-spec.md` — 심평원 API 필드와 DB 가공 규격
- `docs/troubleshooting.md` — 배치/인코딩/통계 코드 장애 이력
- `TODO.md` — 남은 작업과 결정 보류 사항
- `CLAUDE.md` / `AGENTS.md` — 작업 규칙과 코드 컨벤션
