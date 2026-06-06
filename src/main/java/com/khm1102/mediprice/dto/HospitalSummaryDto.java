package com.khm1102.mediprice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code GET /api/hospitals/search?...} 응답 element.
 * <p>
 * {@code search_nearby_hospitals_v2} 프로시저가 {@code matchedNpayCd}, {@code matchedNpayKorNm},
 * {@code score}까지 채운다. Jackson은 null 필드를 직렬화하지 않도록 {@code @JsonInclude(NON_NULL)} 적용.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HospitalSummaryDto(
        String ykiho,
        String yadmNm,
        String addr,
        String clCdNm,
        String telNo,
        Long curAmt,
        Double distance,
        Double lat,
        Double lng,
        String matchedNpayCd,
        String matchedNpayKorNm,
        Double score,
        Long avgAmt,
        Double diffPct
) {

    /** 매칭 항목명을 추가/교체한 복제본 — 다른 필드는 그대로. */
    public HospitalSummaryDto withMatchedNpayKorNm(String name) {
        return new HospitalSummaryDto(
                ykiho, yadmNm, addr, clCdNm, telNo, curAmt, distance, lat, lng,
                matchedNpayCd, name, score, avgAmt, diffPct);
    }

    /** (npayCd × clCd) 평균과 자체 가격 대비 %를 결합한 복제본. */
    public HospitalSummaryDto withStat(Long avgAmt, Double diffPct) {
        return new HospitalSummaryDto(
                ykiho, yadmNm, addr, clCdNm, telNo, curAmt, distance, lat, lng,
                matchedNpayCd, matchedNpayKorNm, score, avgAmt, diffPct);
    }
}
