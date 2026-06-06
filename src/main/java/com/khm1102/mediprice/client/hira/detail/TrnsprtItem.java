package com.khm1102.mediprice.client.hira.detail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 의료기관별상세정보서비스 {@code getTrnsprtInfo2.7} — 대중교통 정보.
 * <p>
 * 한 병원에 복수 row (지하철 노선 + 버스 노선들)로 반환된다.
 * <p>
 * 주차 정보는 이 API가 아니라 {@code getDtlInfo2.7}에 있다 — {@link DtlInfoItem} 참고.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TrnsprtItem(
        String trafNm,
        String lineNo,
        String arivPlc,
        String dir,
        String dist
) {
}
