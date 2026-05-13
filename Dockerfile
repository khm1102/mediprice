# syntax=docker/dockerfile:1.7

# ───── Stage 1: Build WAR ─────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Gradle wrapper + 메타파일 먼저 복사 → 의존성 캐시 레이어 분리
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 소스 복사 후 테스트 + WAR 빌드 (테스트 실패 시 이미지 빌드 실패 = 배포 차단)
COPY src src
RUN ./gradlew --no-daemon clean test war

# ───── Stage 2: Tomcat 11 runtime ─────
FROM tomcat:11.0-jdk21-temurin

# 기본 webapps(manager 등) 제거 — 보안
RUN rm -rf /usr/local/tomcat/webapps/* /usr/local/tomcat/webapps.dist 2>/dev/null || true

# WAR을 ROOT.war로 배포 → context path "/"
COPY --from=build /workspace/build/libs/ROOT.war /usr/local/tomcat/webapps/ROOT.war

# 환경변수는 docker run --env-file .env 로 주입.
# 필수: DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, HIRA_API_KEY, KAKAO_MAP_KEY
# 자세한 키 목록은 application.yml + .env 참고.

EXPOSE 8080

# Tomcat 기본 entrypoint(catalina.sh run) 사용
