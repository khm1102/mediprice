package com.khm1102.mediprice.dto;

/**
 * {@code GET /api/hospitals?lat&lng&npayCd&radius} 응답 element.
 * <p>
 * PostGIS 프로시저 {@code search_nearby_hospitals}가 반환하는 JSON 구조와 1:1 매핑.
 */
public record HospitalSummaryDto(
        String ykiho,
        String yadmNm,
        String addr,
        String clCdNm,
        String telNo,
        Long curAmt,
        Double distance,
        Double lat,
        Double lng
) {
}
