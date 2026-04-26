package com.khm1102.mediprice.entity;

import com.khm1102.mediprice.global.entity.AbstractAuditEntity;
import com.khm1102.mediprice.repository.HospitalRepository;

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
 * 병원 기본정보 — 심평원 {@code getHospBasisList1} 응답을 영속화.
 * <p>
 * PK는 암호화 요양기호 {@code ykiho}. 위치는 {@code location GEOGRAPHY(POINT, 4326)} 컬럼이지만
 * JPA에 매핑하지 않고 {@link HospitalRepository#updateLocation}로 native UPDATE한다
 * (DatabaseInitializer가 컬럼/인덱스 생성).
 */
@Entity
@Table(name = "Hospital")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Hospital extends AbstractAuditEntity {

    @Id
    @Column(name = "ykiho", length = 200)
    private String ykiho;

    @Column(name = "yadm_nm", length = 200, nullable = false)
    private String yadmNm;

    @Column(name = "cl_cd", length = 2)
    private String clCd;

    @Column(name = "cl_cd_nm", length = 100)
    private String clCdNm;

    @Column(name = "addr", length = 500)
    private String addr;

    @Column(name = "x_pos")
    private Double xPos;

    @Column(name = "y_pos")
    private Double yPos;

    @Column(name = "tel_no", length = 30)
    private String telNo;

    @Column(name = "hosp_url", length = 500)
    private String hospUrl;

    @Column(name = "dr_tot_cnt")
    private Integer drTotCnt;

    @Column(name = "sido_cd_nm", length = 100)
    private String sidoCdNm;

    @Column(name = "sggu_cd_nm", length = 100)
    private String sgguCdNm;
}
