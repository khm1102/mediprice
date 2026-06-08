package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.NonPayItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NonPayItemRepository extends JpaRepository<NonPayItem, String> {

    @Query(value = """
            SELECT
                n.npay_cd AS "npayCd",
                n.npay_kor_nm AS "npayKorNm",
                n.npay_mdiv_cd_nm AS "npayMdivCdNm",
                n.npay_sdiv_cd_nm AS "npaySdivCdNm",
                GREATEST(
                    similarity(
                        COALESCE(n.npay_kor_nm, '') || ' ' ||
                        COALESCE(n.npay_mdiv_cd_nm, '') || ' ' ||
                        COALESCE(n.npay_sdiv_cd_nm, ''),
                        :keyword
                    ),
                    similarity(n.npay_kor_nm, :keyword),
                    similarity(COALESCE(n.npay_sdiv_cd_nm, ''), :keyword)
                ) AS "score"
              FROM NonPayItem n
             WHERE n.adt_end_dd = '99991231'
               AND (
                    (
                        COALESCE(n.npay_kor_nm, '') || ' ' ||
                        COALESCE(n.npay_mdiv_cd_nm, '') || ' ' ||
                        COALESCE(n.npay_sdiv_cd_nm, '')
                    )
                        ILIKE CONCAT('%', :keyword, '%')
                 OR similarity(
                        COALESCE(n.npay_kor_nm, '') || ' ' ||
                        COALESCE(n.npay_mdiv_cd_nm, '') || ' ' ||
                        COALESCE(n.npay_sdiv_cd_nm, ''),
                        :keyword
                    ) >= 0.15
                 OR similarity(n.npay_kor_nm, :keyword) >= 0.15
                 OR similarity(COALESCE(n.npay_sdiv_cd_nm, ''), :keyword) >= 0.15
               )
             ORDER BY
                CASE WHEN LOWER(n.npay_kor_nm) = LOWER(:keyword) THEN 0 ELSE 1 END,
                CASE WHEN n.npay_kor_nm ILIKE CONCAT(:keyword, '%') THEN 0 ELSE 1 END,
                score DESC,
                LENGTH(n.npay_kor_nm) ASC,
                n.npay_kor_nm ASC
             LIMIT :limit
            """, nativeQuery = true)
    List<SearchMatch> searchNaturalLanguageMatches(@Param("keyword") String keyword,
                                                   @Param("limit") int limit);

    interface SearchMatch {
        String getNpayCd();
        String getNpayKorNm();
        String getNpayMdivCdNm();
        String getNpaySdivCdNm();
        Double getScore();
    }
}
