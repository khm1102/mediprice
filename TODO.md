# TODO

환경 설정(P0 + P0.5) + P1/P1.5 진행 중 결정 보류된 항목과 향후 운영 단계로 미룬 항목 정리.

---

## 🚧 P1 진행 중 추가된 항목

### N1. `/api/internal/batch/sync` 권한 보호
- **현황:** 인증 없이 누구나 호출 가능 — `BatchAdminApiController` MVP 검증용
- **방향:** 운영 배포 전 다음 중 하나:
  - (a) 운영 빌드에서 컨트롤러 자체 제거 (Spring profile 분리)
  - (b) P2 Security 도입 시 admin 권한 보호 (`hasRole('ADMIN')`)
  - (c) 인프라 단(nginx/Cloudflare)에서 IP 제한
- **결정 시점:** 운영 배포 직전

### N2. 배치 cron 주기 조정 — ✅ 적용됨
- 매일 → **매월 1일 새벽 0시** (`@Scheduled(cron = "0 0 0 1 * *")`)로 변경. 비급여 데이터 갱신주기와 정렬

### N3. 심평원 API 운영계정 신청
- **이유:** 개발계정 일 1,000건 제한. 17개 시도 페이징 + 수만 ykiho 가격상세 호출 시 즉시 초과
- **방향:** 공공데이터포털에서 운영계정 활용 신청 (~1주 소요)
- **결정 시점:** 첫 배치 실행 직후 트래픽 초과 발생 시

### N4. 외부 API 회로차단기/재시도
- **현황:** RestClient 단발 호출 + try-catch 빈 결과 fallback
- **방향:** 심평원 API 응답 지연 잦으면 도입
  - Resilience4j 직접 구현 (CLAUDE.md 라이브러리 미허용 정책)
  - 또는 RestClient interceptor에 retry 로직
- **결정 시점:** 운영 안정화 단계

### N5. 배치 트랜잭션 경계 분리 — ✅ 적용됨
- **적용:** 외부 API 호출과 DB write 트랜잭션을 분리했다.
- **방식:** 페이지/청크 단위 writer 빈에서 `@Transactional(REQUIRES_NEW)`로 저장하고, 저장 후 flush/clear로 persistence context를 비운다.
- **효과:** 긴 외부 API 호출 중 connection을 붙잡는 구조를 줄이고, 실패 범위를 페이지/ykiho 단위로 제한한다.

### N6. NonPayItem 메모리 캐시
- **현황:** `HospitalDetailService`에서 항목명 매핑 시 매번 `findAllById` 호출
- **방향:** NonPayItem 전체(~875건)를 `@Cacheable("hiraApiCache")` 또는 별도 캐시
- **결정 시점:** 응답 시간 측정 후

### N7. JSP 페이지 + 카카오맵 + 정적 JS
- **현황:** API/배치만 구현. 사용자 결정으로 프론트는 다음 라운드 보류
- **방향:** PageController + index.jsp + hospital-list.jsp + hospital-detail.jsp + 카카오맵 SDK + api.js/hospital.js
- **결정 시점:** 사용자가 시작 지시 시

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

---

## 🟡 환경 설정 단계 보류 항목 (이전부터 유지)

---

## 🟡 코드/구조 결정 (P1 진행 중 결정 가능)

### D4. `BaseEntity` ID 전략 — `IDENTITY` → `SEQUENCE`  *(부분 해결됨)*
- **현황:** P1에서 도메인 엔티티(NonPayItem/Hospital/Price)는 자체 PK(ykiho/npay_cd/복합키) 사용 → IDENTITY 무관
- **잔여:** `BaseEntity`(Long id IDENTITY)는 P2 Member 도입 시 사용 예정. Member도 단건 upsert면 그대로 OK

### D7. `SecurityConfig` / `AppConfig` 분할
- **현황:** `SecurityConfig` 167줄 (apiChain + pageChain + CORS + EntryPoint + AccessDeniedHandler + helper). `AppConfig`는 PropertySource + JsonMapper + ComponentScan 동시 보유
- **방향:** P2 진입 전까지 다음 분할
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
| E-P1-1 | `GuestSearchCounter` TTL + sticky session 가이드 | `service/GuestSearchCounter` |
| E-P1-2 | `@Cacheable` 위치 격자 키 (`Math.round(lat*1000)/1000.0`) | `service/HospitalService` |
| E-P1-3 | 쿠키 `HttpOnly; Secure; SameSite=Strict; Path=/` | `api/AuthApiController.tokenGuest` |
| E-P1-4 | native query 명명 바인딩 (`:lat`, `@Param("lat")`) 강제 | `repository/HospitalRepository` |

---


## 📅 검증 미완 (부팅 후 실측 필요)

- **V1.** `WebMvcConfig.configureMessageConverters(ServerBuilder)` 실제 호출 여부 — P1 첫 DTO 응답에서 `OffsetDateTime` 직렬화로 확인
- **V2.** Security 필터 단 401 응답에 `traceId`가 잡히는지 — JwtAuthFilter 도입 후 401 트리거로 확인
