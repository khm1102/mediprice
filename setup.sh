#!/usr/bin/env bash
set -euo pipefail

# MediPrice 로컬 셋업 스크립트 (네이티브 Tomcat)
#   - PostgreSQL+PostGIS는 Docker로 기동 (네이티브 PostGIS 설치는 비현실적)
#   - 앱은 Tomcat 11을 내려받아 .tomcat/ 에 풀고 ROOT.war로 배포
# 사전 요구: JDK 21, Docker
# DB는 빈 상태로 시작 — 스키마는 앱이 부팅하며 자동 생성한다.
#
# 사용법:  ./setup.sh
#   환경변수 override 예) TOMCAT_VERSION=11.0.2 NAVER_MAP_KEY=xxxx ./setup.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

TOMCAT_VERSION="${TOMCAT_VERSION:-11.0.2}"
TOMCAT_HOME="$ROOT_DIR/.tomcat"
DB_CONTAINER="mediprice-db-local"

log()  { printf '\033[1;36m[setup]\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m[setup] %s\033[0m\n' "$*" >&2; exit 1; }

# 1) JDK 21 확인
command -v java >/dev/null 2>&1 || die "JDK 21이 필요합니다 (java 명령을 찾지 못함)."
JAVA_MAJOR="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)"
[ "${JAVA_MAJOR:-0}" -ge 21 ] || die "JDK 21 이상이 필요합니다. 현재: ${JAVA_MAJOR:-unknown}"
log "JDK ${JAVA_MAJOR} 확인"

# 2) Docker로 PostGIS DB 기동 (docker-compose.local.yml의 db 서비스 재사용)
command -v docker >/dev/null 2>&1 || die "Docker가 필요합니다 (DB 기동용)."
log "PostGIS DB 기동"
docker compose -f docker-compose.local.yml up -d db

log "DB 준비 대기"
for _ in $(seq 1 30); do
  if docker exec "$DB_CONTAINER" pg_isready -U mediprice -d mediprice >/dev/null 2>&1; then
    DB_READY=1; break
  fi
  sleep 2
done
[ "${DB_READY:-0}" = "1" ] || die "DB가 준비되지 않았습니다 (timeout)."
log "DB 준비 완료"

# 3) WAR 빌드
log "WAR 빌드 (./gradlew clean war)"
./gradlew --no-daemon clean war
WAR="$ROOT_DIR/build/libs/ROOT.war"
[ -f "$WAR" ] || die "WAR 빌드 실패: $WAR 없음"

# 4) Tomcat 11 다운로드 (이미 있으면 건너뜀)
if [ ! -x "$TOMCAT_HOME/bin/catalina.sh" ]; then
  log "Tomcat ${TOMCAT_VERSION} 다운로드"
  TARBALL="apache-tomcat-${TOMCAT_VERSION}.tar.gz"
  TMP="$(mktemp -d)"
  URL_CDN="https://dlcdn.apache.org/tomcat/tomcat-11/v${TOMCAT_VERSION}/bin/${TARBALL}"
  URL_ARCHIVE="https://archive.apache.org/dist/tomcat/tomcat-11/v${TOMCAT_VERSION}/bin/${TARBALL}"
  curl -fSL "$URL_CDN" -o "$TMP/$TARBALL" 2>/dev/null \
    || curl -fSL "$URL_ARCHIVE" -o "$TMP/$TARBALL" \
    || die "Tomcat 다운로드 실패. TOMCAT_VERSION 환경변수로 존재하는 버전을 지정하세요 (https://tomcat.apache.org/download-11.cgi)."
  mkdir -p "$TOMCAT_HOME"
  tar -xzf "$TMP/$TARBALL" -C "$TOMCAT_HOME" --strip-components=1
  rm -rf "$TMP"
fi

# 5) ROOT.war 배포 (기본 webapps 비우고 컨텍스트 패스 "/")
log "ROOT.war 배포"
rm -rf "$TOMCAT_HOME"/webapps/* 2>/dev/null || true
cp "$WAR" "$TOMCAT_HOME/webapps/ROOT.war"

# 6) 환경변수 주입 후 구동
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/mediprice?reWriteBatchedInserts=true}"
export DB_USERNAME="${DB_USERNAME:-mediprice}"
export DB_PASSWORD="${DB_PASSWORD:-mediprice}"
export JPA_DDL_AUTO="${JPA_DDL_AUTO:-update}"
export JWT_SECRET="${JWT_SECRET:-local-dev-only-secret-change-me-at-least-32-bytes-long}"
export JWT_EXPIRATION="${JWT_EXPIRATION:-86400000}"
export COOKIE_SECURE="${COOKIE_SECURE:-false}"
export COOKIE_SAME_SITE="${COOKIE_SAME_SITE:-Lax}"
export NAVER_MAP_KEY="${NAVER_MAP_KEY:-}"
export HIRA_API_KEY="${HIRA_API_KEY:-your-hira-key}"
export CATALINA_HOME="$TOMCAT_HOME"
export CATALINA_PID="$TOMCAT_HOME/tomcat.pid"

[ -n "$NAVER_MAP_KEY" ] || log "참고: NAVER_MAP_KEY 미설정 — 지도 타일은 비어 보입니다 (export NAVER_MAP_KEY=...)."
log "Tomcat 기동 → http://localhost:8080  (종료: Ctrl+C)"
exec "$TOMCAT_HOME/bin/catalina.sh" run
