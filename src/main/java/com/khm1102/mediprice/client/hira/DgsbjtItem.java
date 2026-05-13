package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 의료기관별상세정보서비스 {@code getDgsbjtInfo2.7} — 진료과목.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DgsbjtItem(
        String dgsbjtCd,
        String dgsbjtCdNm,
        Integer dgsbjtPrSdrCnt
) {
}
