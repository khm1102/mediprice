-- DatabaseInitializer가 부팅 시 실행. CREATE OR REPLACE로 멱등 보장.
-- 변경 시 본 파일만 수정하면 다음 부팅에서 자동 반영.

-- 옛 v1 단일-npayCd 검색 함수 정리 — 신규 환경에는 없으니 IF EXISTS로 안전 DROP.
-- v2 함수와 시그니처가 달라 같은 이름의 중복 정의는 없지만, 운영 DB에 남아 있던
-- 잔존 함수를 다음 부팅에서 자동 청소하기 위해 둔다.
DROP FUNCTION IF EXISTS search_nearby_hospitals(double precision, double precision, varchar, integer);

-- v2 검색: 다중 npayCd + 정렬 모드(mixed/price/distance) + 가중치.
--
-- 핵심:
--   * p.npay_cd = ANY(p_npay_cds)로 다중 코드를 단일 쿼리에서 처리.
--   * DISTINCT ON (h.ykiho) + 내부 ORDER BY (ykiho, 가격ASC, 거리ASC)로 ykiho별 최저가 한 행만 남긴다.
--   * 혼합 점수: 결과 집합 내 MAX(cur_amt) OVER ()로 가격을 [0,1] 정규화, distance/p_radius로 거리 정규화.
--     p_w_price=0.7, p_w_distance=0.3 기본. 낮은 score가 우선.
--   * 외부 ORDER BY는 p_sort로 분기. tie-breaker는 (distance, ykiho).
--
-- 주의: json_agg가 aggregate라서 같은 SELECT 레벨에 ORDER BY/LIMIT를 두면
--       "column ... must appear in the GROUP BY clause" 에러가 난다.
--       정렬+LIMIT는 반드시 'ordered_limited' 내부 subquery에서 끝낸 뒤
--       바깥에서 json_agg만 호출한다. json_agg는 입력 subquery의 행 순서를 유지한다.
CREATE OR REPLACE FUNCTION search_nearby_hospitals_v2(
    p_lat        DOUBLE PRECISION,
    p_lng        DOUBLE PRECISION,
    p_npay_cds   VARCHAR[],
    p_radius     INTEGER          DEFAULT 5000,
    p_sort       VARCHAR          DEFAULT 'mixed',
    p_limit      INTEGER          DEFAULT 50,
    p_w_price    DOUBLE PRECISION DEFAULT 0.7,
    p_w_distance DOUBLE PRECISION DEFAULT 0.3
)
RETURNS JSON AS $$
BEGIN
    RETURN (
        SELECT json_agg(
            json_build_object(
                'ykiho',         ykiho,
                'yadmNm',        yadm_nm,
                'addr',          addr,
                'clCdNm',        cl_cd_nm,
                'telNo',         tel_no,
                'curAmt',        cur_amt,
                'matchedNpayCd', matched_npay_cd,
                'lat',           y_pos,
                'lng',           x_pos,
                'distance',      distance,
                'score',         score
            )
        )
        FROM (
            SELECT
                ykiho, yadm_nm, addr, cl_cd_nm, tel_no,
                x_pos, y_pos, cur_amt, matched_npay_cd, distance, score
            FROM (
                SELECT
                    ykiho, yadm_nm, addr, cl_cd_nm, tel_no,
                    x_pos, y_pos, cur_amt, matched_npay_cd, distance,
                    ( p_w_price    * (cur_amt::float / NULLIF(MAX(cur_amt) OVER (), 0))
                    + p_w_distance * (distance       / NULLIF(p_radius::float, 0))
                    ) AS score
                FROM (
                    SELECT DISTINCT ON (h.ykiho)
                           h.ykiho, h.yadm_nm, h.addr, h.cl_cd_nm, h.tel_no,
                           h.x_pos, h.y_pos,
                           p.cur_amt, p.npay_cd AS matched_npay_cd,
                           ST_Distance(h.location, ST_MakePoint(p_lng, p_lat)::geography) AS distance
                      FROM Hospital h
                      JOIN Price p ON p.ykiho = h.ykiho
                     WHERE ST_DWithin(h.location, ST_MakePoint(p_lng, p_lat)::geography, p_radius)
                       AND (p_npay_cds IS NULL OR array_length(p_npay_cds, 1) IS NULL
                            OR p.npay_cd = ANY(p_npay_cds))
                       AND p.adt_end_dd = '99991231'
                     ORDER BY h.ykiho,
                              p.cur_amt ASC,
                              ST_Distance(h.location, ST_MakePoint(p_lng, p_lat)::geography) ASC
                ) per_hospital
            ) scored
            ORDER BY
                CASE WHEN p_sort = 'distance' THEN distance       END ASC NULLS LAST,
                CASE WHEN p_sort = 'price'    THEN cur_amt::float END ASC NULLS LAST,
                CASE WHEN p_sort = 'mixed'    THEN score          END ASC NULLS LAST,
                distance ASC,
                ykiho    ASC
            LIMIT p_limit
        ) ordered_limited
    );
END;
$$ LANGUAGE plpgsql;
