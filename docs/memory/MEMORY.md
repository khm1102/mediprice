# MediPrice 프로젝트 메모리

## 프로젝트 상태
- 인프라 계층 구현 완료 (Phase 1)
- 서비스/비즈니스 로직 미구현 (Phase 2 대기)
- JwtAuthFilter 미완성 (서비스 구현 후 작업 예정)

## 구현 완료 항목
- **config/**: AppConfig, WebMvcConfig, WebAppInitializer, JpaConfig, SecurityConfig, SecurityInitializer, CacheConfig
- **entity/**: BaseEntity (OffsetDateTime 사용)
- **dto/**: ApiResponse (success/error 패턴, ErrorDetail record)
- **exception/**: 3-tier 계층 (MediPriceException → AuthenticationException/BusinessException → leaf 예외들)
- **exception/auth/**: LoginFailedException, TokenExpiredException, TokenInvalidException, AccessDeniedException
- **exception/business/**: HospitalNotFoundException, PriceNotFoundException, MemberNotFoundException, DuplicateEmailException, GuestSearchLimitExceededException
- **GlobalExceptionHandler**: MediPriceException, Validation, AccessDenied, 404, 405, 500 핸들링
- **controller/**: HealthController (헬스체크용)
- **docker-compose.yml**: PostgreSQL + PostGIS 개발 DB

## 주요 설계 결정
- 순수 Spring Framework (Boot 아님) → WebAppInitializer로 서블릿 초기화
- AppConfig(root) + WebMvcConfig(servlet) 이중 컨텍스트
- SecurityInitializer로 Security 필터 등록
- BaseEntity는 `OffsetDateTime` (TIMESTAMP WITH TIME ZONE 대응)
- ConcurrentMapCacheManager는 TTL 미지원 (주석 명시됨)
- JpaConfig transactionManager는 EntityManagerFactory 직접 주입

## 파일 경로
- 소스 루트: `src/main/java/com/khm1102/mediprice/`
- 리소스: `src/main/resources/application.yml`
- JSP: `src/main/webapp/WEB-INF/views/`
- 정적 파일: `src/main/webapp/static/js/`
- 설계 문서: `docs/`
