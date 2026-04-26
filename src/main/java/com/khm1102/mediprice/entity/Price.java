package com.khm1102.mediprice.entity;

import com.khm1102.mediprice.global.entity.AbstractAuditEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비급여 가격 — 심평원 {@code getNonPaymentItemHospDtlList} 응답을 영속화.
 * <p>
 * 복합키 {@code (ykiho, npay_cd)}. {@code adt_end_dd = '99991231'}인 현재 유효 가격만 저장.
 */
@Entity
@Table(name = "Price")
@IdClass(PriceId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Price extends AbstractAuditEntity {

    @Id
    @Column(name = "ykiho", length = 200)
    private String ykiho;

    @Id
    @Column(name = "npay_cd", length = 20)
    private String npayCd;

    @Column(name = "cur_amt")
    private Long curAmt;

    @Column(name = "adt_fr_dd", length = 8)
    private String adtFrDd;

    @Column(name = "adt_end_dd", length = 8)
    private String adtEndDd;
}
