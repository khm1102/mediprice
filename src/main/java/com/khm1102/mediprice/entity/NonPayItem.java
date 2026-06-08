package com.khm1102.mediprice.entity;

import com.khm1102.mediprice.global.entity.AbstractAuditEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비급여 항목 코드 — 심평원 {@code getNonPaymentItemCodeList2} 응답을 영속화.
 * <p>
 * PK는 {@code npay_cd}. 배치는 코드 기준으로 upsert.
 */
@Entity
@Table(name = "NonPayItem")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NonPayItem extends AbstractAuditEntity {

    @Id
    @Column(name = "npay_cd", length = 20)
    private String npayCd;

    @Column(name = "npay_kor_nm", length = 400, nullable = false)
    private String npayKorNm;

    @Column(name = "npay_mdiv_cd", length = 20)
    private String npayMdivCd;

    @Column(name = "npay_mdiv_cd_nm", length = 200)
    private String npayMdivCdNm;

    @Column(name = "npay_sdiv_cd", length = 20)
    private String npaySdivCd;

    @Column(name = "npay_sdiv_cd_nm", length = 200)
    private String npaySdivCdNm;

    @Column(name = "adt_fr_dd", length = 8)
    private String adtFrDd;

    @Column(name = "adt_end_dd", length = 8)
    private String adtEndDd;

    /**
     * 배치 upsert 시 HIRA 응답의 일시적 null이 기존 코드/분류/기간 정보를 비우지 않게 한다.
     */
    public void updateFromBatch(NonPayItem source) {
        this.npayKorNm = keepExistingIfNull(source.npayKorNm, this.npayKorNm);
        this.npayMdivCd = keepExistingIfNull(source.npayMdivCd, this.npayMdivCd);
        this.npayMdivCdNm = keepExistingIfNull(source.npayMdivCdNm, this.npayMdivCdNm);
        this.npaySdivCd = keepExistingIfNull(source.npaySdivCd, this.npaySdivCd);
        this.npaySdivCdNm = keepExistingIfNull(source.npaySdivCdNm, this.npaySdivCdNm);
        this.adtFrDd = keepExistingIfNull(source.adtFrDd, this.adtFrDd);
        this.adtEndDd = keepExistingIfNull(source.adtEndDd, this.adtEndDd);
    }

    private static <T> T keepExistingIfNull(T incoming, T existing) {
        return incoming != null ? incoming : existing;
    }
}
