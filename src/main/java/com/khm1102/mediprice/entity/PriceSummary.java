package com.khm1102.mediprice.entity;

import com.khm1102.mediprice.global.entity.AbstractAuditEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비급여 가격 요약 — 심평원 {@code getNonPaymentItemHospList2} 응답 영속화.
 * <p>
 * {@link Price}가 (ykiho, npayCd)당 활성 가격 1건만 보존하는 raw 데이터인 반면, 본 테이블은
 * 시기별 row + min/max 범위 + 지역·종별 메타데이터까지 포함한 집계 데이터.
 * <p>
 * 복합키 (ykiho, npayCd, adtFrDd) — 시기별 분리 유지.
 */
@Entity
@Table(
        name = "PriceSummary",
        indexes = {
                @Index(name = "idx_pricesummary_ykiho", columnList = "ykiho"),
                @Index(name = "idx_pricesummary_npay_cd", columnList = "npay_cd"),
                @Index(name = "idx_pricesummary_sggu_npay", columnList = "sggu_cd, npay_cd")
        }
)
@IdClass(PriceSummaryId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PriceSummary extends AbstractAuditEntity {

    @Id
    @Column(name = "ykiho", length = 200)
    private String ykiho;

    @Id
    @Column(name = "npay_cd", length = 20)
    private String npayCd;

    @Id
    @Column(name = "adt_fr_dd", length = 8)
    private String adtFrDd;

    @Column(name = "adt_end_dd", length = 8)
    private String adtEndDd;

    @Column(name = "cl_cd", length = 2)
    private String clCd;

    @Column(name = "cl_cd_nm", length = 100)
    private String clCdNm;

    @Column(name = "sido_cd", length = 6)
    private String sidoCd;

    @Column(name = "sido_cd_nm", length = 100)
    private String sidoCdNm;

    @Column(name = "sggu_cd", length = 6)
    private String sgguCd;

    @Column(name = "sggu_cd_nm", length = 100)
    private String sgguCdNm;

    @Column(name = "yadm_nm", length = 200)
    private String yadmNm;

    @Column(name = "npay_kor_nm", length = 400)
    private String npayKorNm;

    @Column(name = "min_prc")
    private Long minPrc;

    @Column(name = "max_prc")
    private Long maxPrc;

    @Column(name = "url_addr", columnDefinition = "TEXT")
    private String urlAddr;

    /**
     * 요약 배치의 동일 키 row 재수신 시 null 필드는 기존 값을 보존한다.
     */
    public void updateFromBatch(PriceSummary source) {
        this.adtEndDd = keepExistingIfNull(source.adtEndDd, this.adtEndDd);
        this.clCd = keepExistingIfNull(source.clCd, this.clCd);
        this.clCdNm = keepExistingIfNull(source.clCdNm, this.clCdNm);
        this.sidoCd = keepExistingIfNull(source.sidoCd, this.sidoCd);
        this.sidoCdNm = keepExistingIfNull(source.sidoCdNm, this.sidoCdNm);
        this.sgguCd = keepExistingIfNull(source.sgguCd, this.sgguCd);
        this.sgguCdNm = keepExistingIfNull(source.sgguCdNm, this.sgguCdNm);
        this.yadmNm = keepExistingIfNull(source.yadmNm, this.yadmNm);
        this.npayKorNm = keepExistingIfNull(source.npayKorNm, this.npayKorNm);
        this.minPrc = keepExistingIfNull(source.minPrc, this.minPrc);
        this.maxPrc = keepExistingIfNull(source.maxPrc, this.maxPrc);
        this.urlAddr = keepExistingIfNull(source.urlAddr, this.urlAddr);
    }

    private static <T> T keepExistingIfNull(T incoming, T existing) {
        return incoming != null ? incoming : existing;
    }
}
