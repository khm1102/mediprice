package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 의료기관별상세정보서비스 {@code getTrnsprtInfo2.7} — 교통정보.
 * <p>
 * 응답이 단일 item이지만 공통 wrapper와 호환되도록 컬렉션의 첫 element만 사용한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TrnsprtItem(
        String parkYn,
        String parkXpnsYn,
        String parkEtc,
        String trafInfo,
        String parkQty
) {
}
