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
 * 비급여 항목 × 의료기관 종별 가격 통계 — 심평원 {@code getNonPaymentItemClcdList} 영속화.
 * <p>
 * API 응답이 한 row에 종별 컬럼을 wide로 펼치는 구조라, SyncService에서 long 형태
 * (npayCd, clcdKey)로 정규화하여 저장. {@code clcd_key}는 {@code "All"/"Usgh"/"Hosp"/"Gnhp"} 등.
 */
@Entity
@Table(
        name = "NonPayItemClcdStat",
        indexes = {
                @Index(name = "idx_clcd_stat_npay_cd", columnList = "npay_cd")
        }
)
@IdClass(NonPayItemClcdStatId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NonPayItemClcdStat extends AbstractAuditEntity {

    @Id
    @Column(name = "npay_cd", length = 20)
    private String npayCd;

    @Id
    @Column(name = "clcd_key", length = 10)
    private String clcdKey;

    @Id
    @Column(name = "std_date", length = 8)
    private String stdDate;

    @Column(name = "prc_avg")
    private Long prcAvg;

    @Column(name = "prc_mid")
    private Long prcMid;

    @Column(name = "prc_min")
    private Long prcMin;

    @Column(name = "prc_max")
    private Long prcMax;

    /**
     * 동일 기준일 통계 재수신 시 일부 통계값 null이 기존 값을 지우지 않게 한다.
     */
    public void updateFromBatch(NonPayItemClcdStat source) {
        this.prcAvg = keepExistingIfNull(source.prcAvg, this.prcAvg);
        this.prcMid = keepExistingIfNull(source.prcMid, this.prcMid);
        this.prcMin = keepExistingIfNull(source.prcMin, this.prcMin);
        this.prcMax = keepExistingIfNull(source.prcMax, this.prcMax);
    }

    private static <T> T keepExistingIfNull(T incoming, T existing) {
        return incoming != null ? incoming : existing;
    }
}
