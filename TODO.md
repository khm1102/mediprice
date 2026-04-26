# TODO

환경 설정(P0 + P0.5) 단계에서 결정 보류된 항목과 향후 운영 단계로 미룬 항목 정리.
P1 비즈니스 로직 작업 중 또는 운영/배포 단계에서 처리.

---

## 🟡 코드/구조 결정 (P1 진행 중 결정 가능)

### D4. `BaseEntity` ID 전략 — `IDENTITY` → `SEQUENCE`
- **방향:** SEQUENCE 전환 (Hibernate batch insert 활성화 가능)
- **선결 조건:** 심평원 데이터 적재 방식 확인. batch insert가 실제로 필요한가?
  - 단건 upsert면 IDENTITY 유지도 무방
  - 대용량 ETL이면 SEQUENCE + `pooled-lo` 옵티마이저 권장
- **결정 시점:** P1 entity(Hospital, PriceInfo) 작성 직전
- **참고:** [hibernate-orm/issues/SequenceStyleGenerator](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#identifiers-generators-sequence)

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

### F1. `docker-compose.yml`에 app 서비스 추가
- **현황:** DB만 띄움. WAS는 IntelliJ Tomcat으로 로컬 배포
- **방향:** 통합 테스트 환경 필요해지면 추가:
  ```yaml
  app:
    build: .
    depends_on:
      db:
        condition: service_healthy
    env_file: .env
    ports: ["8080:8080"]
  ```

### F2. `Dockerfile` 작성
- **방향:** Tomcat 11 base 이미지 + `build/libs/ROOT.war` 복사 + 외부 SECRET 주입 가능 구조
- **결정 시점:** F1과 함께

### F3. Cloudflare Tunnel 구성
- **방향:** 외부 노출 결정 시. `cloudflared` 설정 + DNS 라우팅
- **결정 시점:** 데모/발표 직전

### F4. 테스트 인프라 도입
- **현황:** JUnit 5만 의존성에 있음, 테스트 코드 0건
- **추가 필요:** `spring-test`, `mockito-core`, `mockito-junit-jupiter`, `assertj-core`, `testcontainers-postgresql` (PostGIS 통합 테스트용)
- **결정 시점:** 첫 테스트 작성 직전 (P1 첫 Service 작성 시점이 자연스러움)

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
