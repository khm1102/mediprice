# TODO

현재 구현 상태 기준으로 남은 운영 위험, 데이터 적재 안정화, UI 보강 항목을 정리한다.

---

## 🚧 P1 진행 중 추가된 항목

### N1. `/api/internal/batch/**` 권한 보호 — ✅ 1차 적용 (BatchAdminGuard)
- **적용:** `BatchAdminGuard`가 두 단계 가드. ① `batch.admin-enabled` (기본 false) → B001. ② `batch.admin-secret` 설정 + 요청 `X-Batch-Admin-Secret` 헤더 정확 일치 → 불일치/누락/blank 시 B003. 상수시간 비교, trim 금지.
- **운영 설정:** `BATCH_ADMIN_ENABLED=true` + `BATCH_ADMIN_SECRET=<openssl rand -base64 48 결과>` 둘 다 필요. 한쪽만 켜면 fail-closed로 모든 요청 403.
- **잔여(이중 방어 권장):** SecurityConfig 단에서 `/api/internal/**`에 `hasRole('ADMIN')` 추가 또는 인프라(nginx/Cloudflare) IP 제한. 현재는 컨트롤러 가드만 신뢰.

### N2. 배치 cron 주기 조정 — ✅ 적용됨
- 매일 → **매월 1일 새벽 0시** (`@Scheduled(cron = "0 0 0 1 * *")`)로 변경. 비급여 데이터 갱신주기와 정렬

### N3. 심평원 API 운영계정 신청
- **이유:** 개발계정 일 1,000건 제한. 17개 시도 페이징 + 수만 ykiho 가격상세 호출 시 즉시 초과
- **방향:** 공공데이터포털에서 운영계정 활용 신청 (~1주 소요)
- **결정 시점:** 첫 배치 실행 직후 트래픽 초과 발생 시

### N4. 외부 API 실패/empty 응답 구분
- **현황:** HIRA 클라이언트가 실패, body null, 진짜 NODATA를 모두 빈 body처럼 반환할 수 있다.
- **위험:** 가격 상세/요약처럼 페이지가 많은 API에서 중간 페이지 누락이 성공처럼 끝날 수 있다.
- **방향:** resultCode/resultMsg 기반 상태 구분, 실패 카운트, 중간 페이지 retry, 최종 실패 로그를 배치별로 명확히 남긴다.
- **결정 시점:** 가격 계열 재적재 전

### N5. 배치 트랜잭션 경계 분리 — ✅ 적용됨
- **적용:** 외부 API 호출과 DB write 트랜잭션을 분리했다.
- **방식:** 페이지/청크 단위 writer 빈에서 `@Transactional(REQUIRES_NEW)`로 저장하고, 저장 후 flush/clear로 persistence context를 비운다.
- **효과:** 긴 외부 API 호출 중 connection을 붙잡는 구조를 줄이고, 실패 범위를 페이지/ykiho 단위로 제한한다.

### N6. NonPayItem 메모리 캐시
- **현황:** `HospitalDetailService`에서 항목명 매핑 시 매번 `findAllById` 호출
- **방향:** NonPayItem 전체(~875건)를 `@Cacheable("hiraApiCache")` 또는 별도 캐시
- **결정 시점:** 응답 시간 측정 후

### N7. 상세 화면 정보 섹션 정리
- **현황:** JSP + 네이버맵 + 정적 JS 화면은 구현됨. 다만 병원 상세 패널에서 의료장비/진료시간/주차 정보 표시가 DTO 의미와 어긋나는 부분이 있다.
- **방향:** `medOftList`는 의료장비로, `operatingInfo`는 진료시간/응급 운영으로, `parkingInfo`는 주차 정보로 분리 렌더링.
- **결정 시점:** 상세 화면 QA 전

### N8. Price 배치 파이프라인화
- **현황:** 전체 배치는 7개 sync 작업을 `hiraBatchExecutor`로 동시에 dispatch한다. 단, `PriceSyncService`는 `Hospital.ykiho` 목록이 필요해 `HospitalSyncService` 완료 뒤에 실행된다.
- **결정적 이유:** 가격 상세 API `getNonPaymentItemHospDtlList`의 입력이 병원 식별자 `ykiho`다. 따라서 Price는 Hospital 결과 없이 독립 실행할 수 없다.
- **개선 방향:** `HospitalSyncService`가 `ykiho`를 수집하는 즉시 큐에 publish하고, Price worker들이 그 큐를 소비하게 만든다. 그러면 Hospital 전체 완료를 기다리지 않고 Price 적재를 시작할 수 있다.
- **결정 시점:** 가격 전체 재적재 전. 개발 키 quota가 회복되거나 운영계정 사용 가능할 때 적용 가치가 큼.

### N9. Price/PriceSummary 부분 적재 재개 정책
- **현황:** 2026-05-17 로컬 배치에서 가격 API quota 초과로 Price 계열 배치가 중단됐다. DB에 저장된 부분 데이터는 보존됨.
- **스냅샷:** `Price` 79,428건, `PriceSummary` 122,097건.
- **결정 필요:** 
  - truncate 후 전체 재적재: 데이터 정합성은 단순하지만 시간이 오래 걸리고 quota를 다시 크게 사용.
  - PK 기준 upsert로 이어받기: 저장분을 보존하지만 “어디까지 성공했는지”를 별도 추적해야 함.
- **권장:** 운영계정 확보 전까지는 truncate 재시도보다 부분 데이터 보존 + 진행률/실패 로그 기반 재개 방식을 설계.

### N10. SidoStat 누락 지역 확인
- **현황:** `getNonPaymentItemSidoCdList`는 공식 문서/실제 응답의 시도 약어가 불완전하다.
- **수정 완료:** 광주 `Kw`, 울산 `Usn`, 경북 `Ksb`, 경남 `Ksn` 매핑 반영 후 `NonPayItemSidoStat` 재적재 완료.
- **남은 확인:** 부산(`Bs`), 충북(`Cb`), 전북(`Jb`), 전남(`Jn`), 제주(`Jj`) 필드는 현재 문서/응답에서 확인되지 않았다.
- **방향:** 실제 API 샘플을 항목별로 더 넓게 수집해 필드 존재 여부를 확정하고, 없으면 “API 미제공”으로 문서화.

### N11. 회원 전용 API의 GUEST 역할 차단
- **현황:** `SecurityConfig` API 체인은 `/api/favorites/**`와 `/api/auth/me`를 `authenticated()`로만 보호한다. `FavoriteApiController`는 `@AuthenticationPrincipal MemberPrincipal`만 주입받고 역할(`role`)은 검사하지 않는다. `MemberPrincipal.isGuest()`가 정의되어 있지만 컨트롤러에서 호출되지 않는다.
- **위험:** Guest JWT(`role=GUEST`, `memberId=null`)로 회원 API를 호출하면 SecurityContext는 인증된 상태로 통과한다. 즐겨찾기 추가 시 `memberId=null` 상태로 저장 시도 → NPE 또는 DB 제약 위반.
- **방향:** (a) SecurityConfig API 체인에서 `/api/favorites/**`와 `/api/auth/me`에 `hasRole("MEMBER")` 적용, 또는 (b) 각 컨트롤러 진입 시 `MemberPrincipal.isGuest()`로 가드.
- **결정 시점:** 운영 배포 전 (보안 P0).

### N12. `search_nearby_hospitals` 정렬 안정성 — ✅ 해결됨 (함수 자체 제거)
- v2 통합 후 옛 `search_nearby_hospitals` 함수와 `/api/hospitals?npayCd=` 엔드포인트는 모두 제거됐다.
- 현행 `search_nearby_hospitals_v2`는 외부 `ORDER BY` tie-breaker로 `distance ASC, ykiho ASC`를 보장한다.

### N13. 상세 캐시 TTL 부재로 인한 stale
- **현황:** `CacheConfig`는 TTL을 지원하지 않는 `ConcurrentMapCacheManager("hiraApiCache")`를 사용한다. `HospitalDetailService.lookupDetail(ykiho)`의 `@Cacheable("hiraApiCache")` 항목은 서버 재시작 전까지 만료되지 않는다. `application.yml`의 `cache.ttl-seconds=3600`은 현재 코드에서 적용되지 않는다.
- **위험:** 배치로 가격이 갱신되어도 캐시 히트가 발생하면 stale 응답이 사용자에게 반환된다 (서버 라이프타임 동안 누적).
- **방향:** (a) 배치 완료 시 명시적 `@CacheEvict` 트리거, (b) Spring `Cache` SPI를 활용해 TTL을 지원하는 매니저로 전환 (현재 Caffeine/Redis 금지 정책 유지하면서 검토), (c) 캐시 키에 배치 버전을 포함.
- **결정 시점:** 가격 데이터 완전 적재 후 사용자 영향 측정 시.

### N14. Price stale row 정리 정책
- **현황:** `PriceYkihoSyncService`는 `entityManager.merge()`로 upsert만 수행한다. 조회 단계에서는 `adt_end_dd='99991231'`로 활성 가격만 필터하지만, HIRA에서 가격이 빠진 경우에도 DB row가 `99991231`로 남아 있을 수 있다.
- **위험:** HIRA 측 가격 종료/철회를 감지하지 못해 stale 가격이 활성으로 노출될 수 있다.
- **방향:** ykiho 단위 동기화 결과에 없는 `(ykiho, npayCd)` 조합을 logical delete 또는 `adt_end_dd` 갱신 처리하는 sweep 추가.
- **결정 시점:** 가격 전체 재적재 후 데이터 정합성 검증 단계.

---

## 🟡 환경 설정 단계 보류 항목 (이전부터 유지)

---

## 🟡 코드/구조 결정 (P1 진행 중 결정 가능)

### D4. `BaseEntity` ID 전략 — `IDENTITY` → `SEQUENCE`  *(부분 해결됨)*
- **현황:** P1에서 도메인 엔티티(NonPayItem/Hospital/Price)는 자체 PK(ykiho/npay_cd/복합키) 사용 → IDENTITY 무관
- **잔여:** `Member`와 `Favorite`는 `BaseEntity`를 사용한다. 단건 insert/soft delete 중심이라 현재는 유지 가능.

### D7. `SecurityConfig` / `AppConfig` 분할
- **현황:** `SecurityConfig` 167줄 (apiChain + pageChain + CORS + EntryPoint + AccessDeniedHandler + helper). `AppConfig`는 PropertySource + JsonMapper + ComponentScan 동시 보유
- **방향:** 운영 보강 전 다음 분할 검토
  - `SecurityConfig` (체인 2개) / `CorsConfig` / `ApiAuthExceptionConfig`
  - `RootConfig` (ComponentScan) / `PropertyConfig` / `JsonConfig`
- **결정 시점:** P1 도중 자연스럽게, P2 직전엔 완료

---

## 🔒 정책 결정 (운영 배포 전)

### E1. `JpaConfig` EAGER 부팅 정책
- **현상:** DB 다운 시 Tomcat 부팅 실패 (`Could not obtain connection ...`)
- **선택지:**
  - (a) 의도된 fail-fast로 둠 (현재) — 운영자가 DB부터 확실히 띄움
  - (b) `LocalContainerEntityManagerFactoryBean.setBootstrapExecutor`로 비동기 부팅 → DB 일시 다운 시에도 앱은 살아있음
- **권장:** (a) 유지하되 docker-compose에 `depends_on: db: condition: service_healthy` 추가 (F1과 묶음)

### E2. `X-Trace-Id` 응답 헤더 외부 노출
- **현상:** 모든 응답에 `X-Trace-Id` 헤더 echo (현재 `setExposedHeaders` 포함)
- **선택지:**
  - (a) 노출 (클라이언트 디버깅 편의)
  - (b) 차단 (공격자에게 요청 그루핑 핸들 제공 차단, 서버 로그에서만 확인)
- **권장:** Cloudflare Tunnel 단계에서 결정. 내부 직원 디버깅용이면 (a), 일반 노출이면 (b)

### E3. `CORS_ALLOWED_ORIGINS=*` 방어
- **현상:** `setAllowedOriginPatterns` 사용 → `*` 허용 가능 + `allowCredentials=true` 결합 시 위험
- **방향:** `setAllowedOrigins`로 전환 + `*`/`?` 포함 시 부팅 거부 검증 추가
- **결정 시점:** 운영 배포 전

### E4. DB pool 30 vs Tomcat thread 200 정합성
- **현상:** 비율 6.6:1 — 트래픽 증가 시 thread는 놀고 connection 부족
- **선택지:**
  - (a) Tomcat `maxThreads=50`으로 줄이고 pool 30 유지
  - (b) pool 80으로 늘리고 PostgreSQL `max_connections` 동시 조정
- **결정 시점:** 부하 시험 후

### E5. `traceId` 형식
- **현상:** UUID 32자 hex (단일 인스턴스 가정)
- **선택지:**
  - (a) 그대로 (단일 인스턴스 한정)
  - (b) W3C `traceparent` 표준 채택 (분산 추적/Cloudflare 통합 시)
- **결정 시점:** 멀티 인스턴스 또는 분산 추적 도입 시

### E6. 운영 logback
- **현상:** 콘솔 단일 appender (`docker logs`로만 수집)
- **선택지:**
  - (a) 그대로 (CLAUDE.md 정책 — 별도 수집 도구 없음)
  - (b) file rolling + JSON encoder + ELK/Loki 도입
- **결정 시점:** 운영 모니터링 정책 결정 시

---

## 🚢 배포/운영 인프라

### F1. `docker-compose.yml` app 서비스 — ✅ 적용됨
- **현황:** DB와 app 컨테이너 기반 실행 흐름이 구성되어 있다.
- **잔여:** 운영 배포 전 profile, secret 주입, healthcheck, Cloudflare Tunnel 연결 정책만 확정하면 된다.

### F2. `Dockerfile` 작성 — ✅ 적용됨
- **현황:** WAR 빌드 후 Tomcat 11 기반 app image를 만들 수 있다.
- **잔여:** 이미지 태그/배포 레지스트리/운영 secret 주입 방식 결정.

### F3. Cloudflare Tunnel 구성
- **방향:** 외부 노출 결정 시. `cloudflared` 설정 + DNS 라우팅
- **결정 시점:** 데모/발표 직전

### F4. 테스트 인프라 도입 — ✅ 적용됨
- **현황:** JUnit/Mockito/AssertJ/Spring Test 기반 테스트가 구성되어 있다.
- **최근 검증:** `./gradlew test` 기준 94 tests, 0 failures, 0 errors.
- **잔여:** PostGIS 통합 테스트는 Testcontainers 기반으로 별도 추가 필요.

### F5. `JWT_SECRET` 강한 키로 교체
- **현황:** `.env`에 `dev-secret-key-change-in-production` (31바이트)
- **문제:** P1에서 `JwtUtil` 32바이트 검증 추가 시 부팅 실패
- **방법:** `openssl rand -base64 48` 결과로 교체
- **결정 시점:** P1 `JwtUtil` 작성 직전

---

## 🧪 P1 진행 중 자연스럽게 처리 (Phase E)

| # | 항목 | 위치 |
|---|---|---|
| E-P1-1 | `@Cacheable` 위치 격자 키 (`Math.round(lat*1000)/1000.0`) | `service/HospitalService` |
| E-P1-2 | 쿠키 `HttpOnly; Secure; SameSite=Strict; Path=/` | `AuthController`, `AuthApiController` |
| E-P1-3 | native query 명명 바인딩 (`:lat`, `@Param("lat")`) 강제 | `repository/HospitalRepository` |

### E-P1-2 보강 (현재 상태)
- `AuthController.setTokenCookie()`와 `AuthApiController.guestToken()`이 발급하는 `mp_token` 쿠키는 현재 `HttpOnly`, `Secure`, `SameSite` 속성이 모두 미설정 상태다. XSS·CSRF 노출 위험.
- HttpOnly로 전환하면 `static/js/auth.js`가 로그인 상태 확인을 위해 쿠키를 직접 디코딩하는 로직을 못 쓰게 된다. 대안으로 `GET /api/auth/me` 호출로 상태를 확인하도록 함께 바꾸어야 한다.
- 운영 도메인이 HTTPS로 고정되면 `Secure=true`와 `SameSite=Lax`(또는 `Strict`)를 환경변수로 토글할 수 있게 한다.

---


## 📅 검증 미완 (부팅 후 실측 필요)

- **V1.** `WebMvcConfig.configureMessageConverters(ServerBuilder)` 실제 호출 여부 — P1 첫 DTO 응답에서 `OffsetDateTime` 직렬화로 확인
- **V2.** Security 필터 단 401 응답에 `traceId`가 잡히는지 — JwtAuthFilter 도입 후 401 트리거로 확인
