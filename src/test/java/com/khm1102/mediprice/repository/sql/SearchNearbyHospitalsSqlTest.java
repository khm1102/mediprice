package com.khm1102.mediprice.repository.sql;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code search_nearby_hospitals_v2} PL/pgSQL 함수 정의 정적 검증.
 * <p>
 * 정렬 안정성, DISTINCT ON, 다중 npayCd 처리, mixed 점수 정규화, 외부 ORDER BY/LIMIT wrap,
 * Price partial index 등록을 SQL 텍스트 패턴으로 검증한다. 실제 DB 연결 없음.
 * <p>
 * 옛 v1 함수({@code search_nearby_hospitals})는 제거되었고, 운영 DB에 남아 있을 수 있는
 * 잔존 함수는 procedures.sql의 {@code DROP FUNCTION IF EXISTS}로 부팅 시 정리한다.
 */
class SearchNearbyHospitalsSqlTest {

    /** v1 함수는 procedures.sql에 등장하면 안 된다 — v2와 시그니처가 다른 잔재. */
    @Test
    void v1FunctionDefinitionIsRemoved() throws IOException {
        String sql = readSql();
        // CREATE OR REPLACE FUNCTION search_nearby_hospitals( ← v2가 아닌 단독 이름
        Pattern v1Create = Pattern.compile(
                "CREATE\\s+OR\\s+REPLACE\\s+FUNCTION\\s+search_nearby_hospitals\\s*\\(",
                Pattern.CASE_INSENSITIVE);
        assertThat(v1Create.matcher(sql).find())
                .as("v1 search_nearby_hospitals(...) 정의가 다시 들어오면 안 된다")
                .isFalse();
    }

    /** 운영 DB의 잔존 v1 함수 정리를 위해 DROP IF EXISTS가 procedures.sql 안에 있어야 한다. */
    @Test
    void v1FunctionDropIfExistsIsRegistered() throws IOException {
        String sql = readSql();
        Pattern drop = Pattern.compile(
                "DROP\\s+FUNCTION\\s+IF\\s+EXISTS\\s+search_nearby_hospitals\\s*\\(",
                Pattern.CASE_INSENSITIVE);
        assertThat(drop.matcher(sql).find())
                .as("운영 DB 잔존 v1 함수를 청소하는 DROP FUNCTION IF EXISTS가 있어야 한다")
                .isTrue();
    }

    // ── v2 함수 회귀 방지선 ─────────────────────────────────────────────────

    @Test
    void v2FunctionDefinitionExists() throws IOException {
        String sql = readSql();
        assertThat(sql).contains("CREATE OR REPLACE FUNCTION search_nearby_hospitals_v2");
        // 파라미터 시그니처
        assertThat(sql).contains("p_npay_cds");
        assertThat(sql).contains("p_sort");
        assertThat(sql).contains("p_w_price");
        assertThat(sql).contains("p_w_distance");
    }

    /** ykiho별 최저가 1행 보장 — DISTINCT ON 사용 + 내부 정렬에 cur_amt ASC. */
    @Test
    void v2UsesDistinctOnYkihoForLowestPrice() throws IOException {
        String sql = readSql();
        Pattern distinctOn = Pattern.compile(
                "DISTINCT\\s+ON\\s*\\(\\s*h\\.ykiho\\s*\\)",
                Pattern.CASE_INSENSITIVE);
        assertThat(distinctOn.matcher(sql).find())
                .as("v2는 DISTINCT ON (h.ykiho)로 ykiho별 한 행만 남겨야 한다")
                .isTrue();
        assertThat(sql).contains("ORDER BY h.ykiho");
        assertThat(sql).contains("p.cur_amt ASC");
    }

    /** 다중 npayCd 입력은 ANY(p_npay_cds)로 처리해야 한다 (옛 = 단일 동등 매칭은 v2엔 없음). */
    @Test
    void v2UsesAnyOperatorForMultipleNpayCds() throws IOException {
        String sql = readSql();
        Pattern any = Pattern.compile(
                "p\\.npay_cd\\s*=\\s*ANY\\s*\\(\\s*p_npay_cds\\s*\\)",
                Pattern.CASE_INSENSITIVE);
        assertThat(any.matcher(sql).find())
                .as("v2는 p.npay_cd = ANY(p_npay_cds)로 다중 코드를 처리해야 한다")
                .isTrue();
    }

    /** 외부 ORDER BY가 모드별 분기(distance/price/mixed)를 갖고 tie-breaker로 (distance, ykiho)를 포함. */
    @Test
    void v2OuterOrderBranchesBySortAndKeepsTieBreaker() throws IOException {
        String sql = readSql();
        assertThat(sql).contains("CASE WHEN p_sort = 'distance' THEN distance");
        assertThat(sql).contains("CASE WHEN p_sort = 'price'    THEN cur_amt::float");
        assertThat(sql).contains("CASE WHEN p_sort = 'mixed'    THEN score");
        // tie-breaker
        Pattern tieBreaker = Pattern.compile(
                "distance\\s+ASC\\s*,\\s*ykiho\\s+ASC",
                Pattern.CASE_INSENSITIVE);
        assertThat(tieBreaker.matcher(sql).find())
                .as("v2 외부 ORDER BY는 distance ASC, ykiho ASC tie-breaker를 가져야 한다")
                .isTrue();
    }

    /** 혼합 점수: 가격 정규화(MAX OVER) + 거리 정규화(p_radius) + 가중치. */
    @Test
    void v2MixedScoreNormalizesPriceAndDistance() throws IOException {
        String sql = readSql();
        assertThat(sql).contains("MAX(cur_amt) OVER ()");
        assertThat(sql).contains("p_w_price");
        assertThat(sql).contains("p_w_distance");
        assertThat(sql).contains("p_radius::float");
    }

    /** v2에는 LIMIT p_limit (옛 함수는 LIMIT 100). */
    @Test
    void v2UsesParameterizedLimit() throws IOException {
        String sql = readSql();
        Pattern limit = Pattern.compile("LIMIT\\s+p_limit", Pattern.CASE_INSENSITIVE);
        assertThat(limit.matcher(sql).find())
                .as("v2는 LIMIT p_limit으로 자른다")
                .isTrue();
    }

    /**
     * 회귀 방지: ORDER BY/LIMIT는 반드시 {@code ordered_limited} 내부 subquery에서 닫혀야 한다.
     * <p>
     * 옛 구현은 {@code SELECT json_agg(...) FROM (scored) scored ORDER BY scored.distance LIMIT p_limit} 모양이라
     * Postgres가 aggregate query에서 비집계 컬럼을 ORDER BY로 참조한다고 에러를 던졌다
     * ({@code column "scored.distance" must appear in the GROUP BY clause...}).
     * 이제 ORDER BY/LIMIT는 ordered_limited 안에서 종료되고 외부는 json_agg만 호출한다.
     */
    @Test
    void v2WrapsOrderByAndLimitInsideOrderedLimitedSubquery() throws IOException {
        String sql = readSql();
        Pattern wrap = Pattern.compile(
                "LIMIT\\s+p_limit\\s*\\)\\s*ordered_limited",
                Pattern.CASE_INSENSITIVE);
        assertThat(wrap.matcher(sql).find())
                .as("LIMIT p_limit는 ordered_limited subquery 안에서 닫혀야 한다")
                .isTrue();
        // 외부 SELECT 레벨에는 ORDER BY가 없어야 한다 (aggregate 충돌 회귀 방지).
        // ordered_limited 다음에는 닫는 괄호와 함수 종결만 와야 한다.
        Pattern tail = Pattern.compile(
                "ordered_limited\\s*\\)\\s*;\\s*END\\s*;",
                Pattern.CASE_INSENSITIVE);
        assertThat(tail.matcher(sql).find())
                .as("ordered_limited 뒤에는 RETURN을 닫는 ); END; 만 와야 한다")
                .isTrue();
    }

    private static String readSql() throws IOException {
        return new String(
                new ClassPathResource("sql/procedures.sql").getContentAsByteArray(),
                StandardCharsets.UTF_8);
    }

    // ── Price partial index DDL (DatabaseInitializer) 정적 검증 ──────────────

    /** v2 검색용 partial composite index가 DatabaseInitializer에 등록되어야 한다. */
    @Test
    void priceActivePartialIndexDdlIsRegistered() throws IOException {
        String src = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/khm1102/mediprice/global/config/DatabaseInitializer.java"));
        assertThat(src)
                .as("PRICE_ACTIVE_PARTIAL_INDEX_DDL 상수가 있어야 한다")
                .contains("PRICE_ACTIVE_PARTIAL_INDEX_DDL");
        // 컬럼 순서: 선두 npay_cd, 그 다음 ykiho, 마지막 cur_amt.
        assertThat(src).contains("idx_price_active_npay_ykiho_amt");
        assertThat(src).contains("ON Price (npay_cd, ykiho, cur_amt)");
        // partial — 활성 가격만 인덱스에 포함
        assertThat(src).contains("WHERE adt_end_dd = '99991231'");
        // initialize()에서 새 DDL을 실제로 호출해야 한다.
        Pattern executeCall = Pattern.compile(
                "execute\\s*\\(\\s*conn\\s*,\\s*PRICE_ACTIVE_PARTIAL_INDEX_DDL\\s*\\)",
                Pattern.CASE_INSENSITIVE);
        assertThat(executeCall.matcher(src).find())
                .as("initialize()에서 execute(conn, PRICE_ACTIVE_PARTIAL_INDEX_DDL) 호출이 보여야 한다")
                .isTrue();
    }
}
