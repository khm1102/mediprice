# MediPrice 시스템 아키텍처

> 비급여 진료비 비교 플랫폼 — Spring Framework 7 · PostgreSQL 16 · Docker

```mermaid
flowchart TB

    %% ══════════════════════════════════════════════════════════
    %%  CI/CD
    %% ══════════════════════════════════════════════════════════
    subgraph CICD["⚙️  CI/CD Pipeline  ―  GitHub Actions"]
        direction LR

        DEV["👨‍💻 Developer\n────────────────────\n기능 개발 후\ngit push origin master"]

        CI["🔨 CI Job  ·  ubuntu-latest\n────────────────────\n./gradlew assemble → ROOT.war\n./gradlew test → JUnit 5\nJava 21  ·  Gradle Groovy DSL"]

        DOCKER["🐳 Docker Build & Push\n────────────────────\nBuildKit multi-platform\nlinux/amd64 + linux/arm64\n레이어 캐시: GitHub Actions Cache"]

        GHCR["📦 GHCR\n────────────────────\nghcr.io/khm1102/mediprice:latest\nghcr.io/khm1102/mediprice:sha-XXXXXX\n(sha 태그: 롤백 대비)"]

        RUNNER["🤖 Self-hosted Runner  ·  서버 상주\n────────────────────\n① git reset --hard origin/master\n② docker compose pull\n③ docker compose up -d --no-build\n④ /api/health 60초 폴링\n   → 실패 시 로그 출력 + 파이프라인 실패"]

        DEV   -->|"① git push master"| CI
        CI    -->|"② 테스트 통과"| DOCKER
        DOCKER -->|"③ 이미지 push"| GHCR
        GHCR  -->|"④ docker compose pull"| RUNNER
    end

    %% ══════════════════════════════════════════════════════════
    %%  Browser
    %% ══════════════════════════════════════════════════════════
    BROWSER["🌐 사용자 브라우저\n────────────────────\nVanilla JS ES2025  ·  fetch API\nTailwind CSS CDN  ·  Naver Maps SDK v3\n비회원 검색 3회 제한"]

    %% ══════════════════════════════════════════════════════════
    %%  Cloudflare Edge
    %% ══════════════════════════════════════════════════════════
    subgraph CF["☁️  Cloudflare Edge"]
        direction TB

        DNS["🔖 DNS\n────────────────────\nmediprice.khm1102.com\nA레코드 → 서버 IP 라우팅"]

        WAF["🛡️ WAF + DDoS 방어\n────────────────────\n악성 요청 차단  ·  Bot 관리\nRate Limiting  ·  IP 차단"]

        CDN["📡 CDN + HTTPS\n────────────────────\n정적 파일 엣지 캐싱\nTLS 1.3  ·  HTTP/2\nHTTPS 강제 리다이렉트"]
    end

    %% ══════════════════════════════════════════════════════════
    %%  VPS Server
    %% ══════════════════════════════════════════════════════════
    subgraph SERVER["🖥️  VPS Server  (Self-hosted Linux)"]
        direction TB

        NGINX["🔀 nginx  :443 / :80\n────────────────────\nSSL 종단  ·  리버스 프록시\nUpstream → mediprice-app:8080\nfail2ban 연동 (브루트포스 차단)"]

        subgraph APP["🐳 mediprice-app  Docker Container  :8080"]
            direction TB

            TOMCAT["🐈 Apache Tomcat 11\n────────────────────\nWAS  ·  ROOT.war 실행\nJVM Java 21  ·  HTTP NIO Connector\nCATALINA_HOME = /usr/local/tomcat"]

            SEC["🔐 Spring Security  +  JWT\n────────────────────\nStateless 인증 (세션 완전 배제)\n회원 → JWT 발급 → HttpOnly Cookie\n비회원 → Guest JWT 자동 발급\nTraceIdFilter: 요청마다 UUID → MDC\nlogback: [traceId] 패턴으로 추적"]

            subgraph MVC["Spring MVC  ―  요청 처리 계층"]
                direction LR
                CTRL["📄 @Controller\n────────────────────\nRoute: /hospital/**  /auth/**\nJSP + JSTL 3.0 렌더링\n서버사이드 HTML 반환\n(스크립틀릿 금지 · EL만 사용)"]
                RCTRL["🔌 @RestController\n────────────────────\nRoute: /api/**\nJSON 응답  ·  fetch API 전용\n병원 검색  ·  가격 비교\n배치 수동 트리거 /api/internal/batch"]
            end

            SVC["⚙️ Service Layer\n────────────────────\nHospitalService  ·  NonPayItemService\nHospitalDetailService  ·  AuthService\n비즈니스 로직 집중 계층\nController와 Repository 사이 중재"]

            JPA["🗄️ JPA + Hibernate 7  +  HikariCP\n────────────────────\nORM: Entity ↔ PostgreSQL 테이블 매핑\nHikariCP pool=30  ·  min-idle=10\nPostGIS Spatial 연동\nST_DWithin: 반경 내 병원 위치 검색"]

            CACHE["💾 Spring ConcurrentMapCache\n────────────────────\n심평원 API 응답 캐싱\n키: 지역코드 + 비급여항목코드\n만료: 서버 재시작 시 초기화\nRedis 미사용 (프로토타입 수준)"]

            TOMCAT --> SEC
            SEC    --> CTRL & RCTRL
            CTRL & RCTRL --> SVC
            SVC    --> JPA & CACHE
        end

        subgraph BATCH["⏰ Batch  ―  심평원 데이터 동기화  (매월 1일 00:00  +  수동 트리거)"]
            direction LR
            B1["NonPayItemSync\n────────────\n비급여 항목 코드\nDB upsert"]
            B2["HospitalSync\n────────────\n전국 병원 기본 정보\nYkiho 기준 upsert"]
            B3["PriceSync\n────────────\nHospital.ykiho 기반\n가격 상세 갱신"]
            B4["Desc/Summary/Stat\n────────────\n항목 설명·가격요약\n종별/시도 통계"]
            B1 -. 병렬 .- B2
            B1 -. 병렬 .- B4
            B2 -->|Hospital 완료 후| B3
        end

        CTUNNEL["🚇 cloudflared  (Cloudflare Tunnel Client)\n────────────────────\n역할: 외부 포트 노출 없이 DB 접근\n바인딩: localhost:5774 → PostgreSQL:5432\nOutbound only — 방화벽 인바운드 불필요"]

        DB[("🐘 PostgreSQL 16  +  PostGIS\n────────────────────\n도커 컨테이너  ·  localhost:5432\nPostGIS: 지리공간 인덱스 (GIST)\nsearch_nearby_hospitals() 저장 프로시저\nDatabaseInitializer @PostConstruct 자동 등록\n테이블: Hospital · NonPayItem · Price")]

        RUNNER -->|"docker compose\npull + up -d"| APP
    end

    %% ══════════════════════════════════════════════════════════
    %%  External Services
    %% ══════════════════════════════════════════════════════════
    subgraph EXT["🌍  External Services"]
        direction TB

        HIRA["🏥 심평원 HIRA API\n────────────────────\n공공데이터포털 REST API (XML 응답)\n배치: 7개 SyncService 병렬 dispatch\nPrice만 Hospital.ykiho 의존\n실시간: 병원 상세 5개 API 병렬 조회\nhiraDetailExecutor ThreadPool (core=5, max=10)"]

        NAVER["🗺️ Naver Maps SDK v3\n────────────────────\nJavaScript 지도 라이브러리\n브라우저에서 직접 CDN 로드\n병원 위치 마커 렌더링\n역지오코딩: 좌표 → 주소 변환\n현재 위치 기반 주변 병원 탐색"]
    end

    %% ══════════════════════════════════════════════════════════
    %%  Connections
    %% ══════════════════════════════════════════════════════════
    BROWSER  -->|"① HTTPS 요청\nmediprice.khm1102.com"| DNS
    DNS      -->|"IP 라우팅"| WAF
    WAF      -->|"필터링 통과"| CDN
    CDN      -->|"HTTP :80\n(mediprice-network)"| NGINX
    NGINX    -->|"HTTP :8080\nreverse proxy"| TOMCAT

    JPA      -->|"JDBC\nlocalhost:5774"| CTUNNEL
    CTUNNEL  -->|"TCP :5432"| DB

    BATCH    -->|"HTTP REST (XML)\n공공데이터포털"| HIRA
    RCTRL    -->|"실시간 상세 조회\n4 API CompletableFuture"| HIRA

    BROWSER  -->|"② Naver Maps SDK\nCDN 직접 로드"| NAVER

    %% ══════════════════════════════════════════════════════════
    %%  Styles
    %% ══════════════════════════════════════════════════════════
    classDef cicdStyle  fill:#24292e,color:#fff,stroke:#555
    classDef springStyle fill:#6DB33F,color:#fff,stroke:#4a7c2f
    classDef infraStyle  fill:#1971c2,color:#fff,stroke:#1864ab
    classDef dbStyle     fill:#2f6f9f,color:#fff,stroke:#1e4d6f
    classDef extStyle    fill:#495057,color:#fff,stroke:#343a40
    classDef cfStyle     fill:#F48120,color:#fff,stroke:#c06010
    classDef browserStyle fill:#f1f3f5,color:#212529,stroke:#adb5bd

    class DEV,CI,DOCKER,GHCR,RUNNER   cicdStyle
    class TOMCAT,SEC,CTRL,RCTRL,SVC,JPA,CACHE,B1,B2,B3  springStyle
    class NGINX,CTUNNEL  infraStyle
    class DB  dbStyle
    class HIRA,NAVER  extStyle
    class DNS,WAF,CDN  cfStyle
    class BROWSER  browserStyle
```
