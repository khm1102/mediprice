-- DatabaseInitializer가 부팅 시 실행. CREATE OR REPLACE로 멱등 보장.
-- 변경 시 본 파일만 수정하면 다음 부팅에서 자동 반영.

CREATE OR REPLACE FUNCTION search_nearby_hospitals(
    p_lat     DOUBLE PRECISION,
    p_lng     DOUBLE PRECISION,
    p_npay_cd VARCHAR,
    p_radius  INTEGER DEFAULT 2000
)
RETURNS JSON AS $$
BEGIN
    RETURN (
        SELECT json_agg(result ORDER BY (result->>'distance')::float)
        FROM (
            SELECT json_build_object(
                'ykiho',    h.ykiho,
                'yadmNm',   h.yadm_nm,
                'addr',     h.addr,
                'clCdNm',   h.cl_cd_nm,
                'telNo',    h.tel_no,
                'curAmt',   p.cur_amt,
                'lat',      h.y_pos,
                'lng',      h.x_pos,
                'distance', ST_Distance(
                    h.location,
                    ST_MakePoint(p_lng, p_lat)::geography
                )
            ) AS result
            FROM Hospital h
            JOIN Price p ON p.ykiho = h.ykiho
            WHERE ST_DWithin(
                h.location,
                ST_MakePoint(p_lng, p_lat)::geography,
                p_radius
            )
            AND p.npay_cd = p_npay_cd
            AND p.adt_end_dd = '99991231'
            LIMIT 20
        ) sub
    );
END;
$$ LANGUAGE plpgsql;
