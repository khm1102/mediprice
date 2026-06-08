package com.khm1102.mediprice.entity;

import com.khm1102.mediprice.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비급여 항목 설명 — 구버전 {@code getNonPaymentItemCodeList} 응답 영속화.
 * <p>
 * 신버전({@link NonPayItem})은 코드+이름만 제공하지만 본 테이블은 일반인용 설명({@code *Dsc})까지 보존.
 * 구버전 분류({@code divCd1}/{@code divCd2}/{@code divCd3})와 신버전 코드({@code npayCd}) 간 매핑은
 * 별도 후속 작업. 본 변경에서는 raw 저장까지만.
 */
@Entity
@Table(
        name = "NonPayItemDesc",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_nonpay_item_desc",
                columnNames = {"div_cd_1", "div_cd_2", "div_cd_3"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NonPayItemDesc extends BaseEntity {

    @Column(name = "div_cd_1", length = 20)
    private String divCd1;

    @Column(name = "div_cd_1_nm", length = 200)
    private String divCd1Nm;

    @Column(name = "div_cd_1_dsc", columnDefinition = "TEXT")
    private String divCd1Dsc;

    @Column(name = "div_cd_2", length = 20)
    private String divCd2;

    @Column(name = "div_cd_2_nm", length = 200)
    private String divCd2Nm;

    @Column(name = "div_cd_2_dsc", columnDefinition = "TEXT")
    private String divCd2Dsc;

    @Column(name = "div_cd_3", length = 20)
    private String divCd3;

    @Column(name = "div_cd_3_nm", length = 200)
    private String divCd3Nm;

    @Column(name = "div_cd_3_dsc", columnDefinition = "TEXT")
    private String divCd3Dsc;

    /**
     * 설명 배치 재수신 시 null 이름/설명이 기존 텍스트를 비우지 않게 한다.
     */
    public void updateFromBatch(NonPayItemDesc source) {
        this.divCd1Nm = keepExistingIfNull(source.divCd1Nm, this.divCd1Nm);
        this.divCd1Dsc = keepExistingIfNull(source.divCd1Dsc, this.divCd1Dsc);
        this.divCd2Nm = keepExistingIfNull(source.divCd2Nm, this.divCd2Nm);
        this.divCd2Dsc = keepExistingIfNull(source.divCd2Dsc, this.divCd2Dsc);
        this.divCd3Nm = keepExistingIfNull(source.divCd3Nm, this.divCd3Nm);
        this.divCd3Dsc = keepExistingIfNull(source.divCd3Dsc, this.divCd3Dsc);
    }

    private static <T> T keepExistingIfNull(T incoming, T existing) {
        return incoming != null ? incoming : existing;
    }
}
