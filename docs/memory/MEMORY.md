# MediPrice 프로젝트 메모리

## 프로젝트 상태 (2026-06-05)

- P0/P0.5 인프라와 공통 예외/로그/Security 기본 구조 완료.
- P1 백엔드 API, PostGIS 검색, 심평원 배치 구현 완료.
- P1.5 상세 API 모델 매핑 수정, SidoStat 코드 수정, 배치 병렬화 완료.
- JSP/네이버맵/정적 JS 화면 구현 완료.
- Google OAuth, JWT 쿠키, 회원 탈퇴, 즐겨찾기 구현 완료.
- Guest 검색 제한 정책은 폐기되었고 적용하지 않는다.

## 구현 완료 항목

- **config/**: AppConfig, WebMvcConfig, WebAppInitializer, JpaConfig, SecurityConfig, SecurityInitializer, CacheConfig, DatabaseInitializer
- **entity/**: Hospital, NonPayItem, Price, PriceSummary, NonPayItemDesc, NonPayItemClcdStat, NonPayItemSidoStat
- **dto/**: ApiResponse, 병원 검색/상세/항목 응답 DTO
- **exception/**: MediPriceException 계층 + GlobalExceptionHandler
- **controller/**: Health, Hospital, NonPayItem, Auth/OAuth, Favorite, Legal, BatchAdmin API
- **service/**: Hospital/NonPayItem/HospitalDetail, Auth/GoogleOAuth/Consent, Favorite
- **entity/**: Hospital, NonPayItem, Price 계열, Member, Favorite
- **batch/**: HIRA batch 7종 + 수동 트리거. `admin/orchestrator/hospital/item/price/summary/stat/support`로 기능별 분리
- **client/**: HIRA 병원정보/비급여/의료기관 상세 API client
- **docker-compose.yml / Dockerfile**: PostgreSQL + PostGIS + app image 실행 구조

## 최신 배치 상태

- 전체 배치는 7개 SyncService를 `hiraBatchExecutor`로 병렬 dispatch한다.
- `PriceSyncService`만 `Hospital.ykiho` 목록이 필요하므로 Hospital 완료 뒤 실행한다.
- 각 SyncService는 외부 API 호출과 DB write 트랜잭션을 분리한다.
- 2026-05-17 로컬 DB 스냅샷: Hospital 79,674, NonPayItem 875, NonPayItemDesc 54, ClcdStat 2,459, SidoStat 6,988, Price 79,428(부분), PriceSummary 122,097(부분).
- Price 계열은 HIRA quota 초과로 완전 적재 전 중단되었다.

## 주요 설계 결정

- 순수 Spring Framework (Boot 아님) → WebAppInitializer로 서블릿 초기화.
- AppConfig(root) + WebMvcConfig(servlet) 이중 컨텍스트.
- SecurityInitializer로 Security 필터 등록.
- 자체 PK 도메인(Hospital/NonPayItem/Price 등)은 BaseEntity id 전략과 분리.
- `Hospital.location`은 native update로 PostGIS geography point를 채운다.
- ConcurrentMapCacheManager는 TTL 미지원. 운영 캐시는 추후 정책 결정.

## 파일 경로

- 소스 루트: `src/main/java/com/khm1102/mediprice/`
- 리소스: `src/main/resources/application.yml`
- JSP: `src/main/webapp/WEB-INF/views/`
- 정적 파일: `src/main/webapp/static/js/`
- 설계 문서: `docs/`
- HIRA 원문/참고 문서: `hira-docs/`
