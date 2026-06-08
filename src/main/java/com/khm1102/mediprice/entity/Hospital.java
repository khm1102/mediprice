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
 * 병원 기본정보 — 심평원 {@code getHospBasisList} 응답을 영속화.
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

    /**
     * 배치 upsert 시 HIRA가 일시적으로 비운 optional 필드가 기존 DB 값을 null로 덮지 않게 한다.
     * 필수값인 병원명도 기존 row에서는 null 입력이면 기존 값을 보존한다.
     */
    public void updateFromBatch(Hospital source) {
        this.yadmNm = keepExistingIfNull(source.yadmNm, this.yadmNm);
        this.clCd = keepExistingIfNull(source.clCd, this.clCd);
        this.clCdNm = keepExistingIfNull(source.clCdNm, this.clCdNm);
        this.addr = keepExistingIfNull(source.addr, this.addr);
        this.xPos = keepExistingIfNull(source.xPos, this.xPos);
        this.yPos = keepExistingIfNull(source.yPos, this.yPos);
        this.telNo = keepExistingIfNull(source.telNo, this.telNo);
        this.hospUrl = keepExistingIfNull(source.hospUrl, this.hospUrl);
        this.drTotCnt = keepExistingIfNull(source.drTotCnt, this.drTotCnt);
        this.sidoCdNm = keepExistingIfNull(source.sidoCdNm, this.sidoCdNm);
        this.sgguCdNm = keepExistingIfNull(source.sgguCdNm, this.sgguCdNm);
    }

    private static <T> T keepExistingIfNull(T incoming, T existing) {
        return incoming != null ? incoming : existing;
    }
}
