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
 * 비급여 항목 × 시도 가격 통계 — 심평원 {@code getNonPaymentItemSidoCdList} 영속화.
 * <p>
 * API 응답이 한 row에 시도 컬럼을 wide로 펼치는 구조라, SyncService에서 long 형태
 * (npayCd, sidoKey)로 정규화하여 저장. {@code sido_key}는 약어 ({@code "All"/"Sl"/.../"Jj"}).
 */
@Entity
@Table(
        name = "NonPayItemSidoStat",
        indexes = {
                @Index(name = "idx_sido_stat_npay_cd", columnList = "npay_cd"),
                @Index(name = "idx_sido_stat_sido_key", columnList = "sido_key")
        }
)
@IdClass(NonPayItemSidoStatId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NonPayItemSidoStat extends AbstractAuditEntity {

    @Id
    @Column(name = "npay_cd", length = 20)
    private String npayCd;

    @Id
    @Column(name = "sido_key", length = 10)
    private String sidoKey;

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
}
