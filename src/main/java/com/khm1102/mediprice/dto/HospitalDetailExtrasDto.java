package com.khm1102.mediprice.dto;

import java.util.List;

/**
 * {@code GET /api/hospitals/{ykiho}/extras} 응답 — 외부 HIRA 5종(진료과목/장비/교통/주차·운영/특수진료)만.
 * {@code HospitalDetailHiraCache}가 ykiho 단위로 캐시한다 → cache hit이면 즉시, miss면 500~2000ms.
 * 프론트는 이 응답으로 접이식 부가 섹션을 렌더한다.
 */
public record HospitalDetailExtrasDto(
        String ykiho,
        List<String> dgsbjtList,
        List<String> medOftList,
        List<HospitalDetailDto.TransitItem> transitList,
        HospitalDetailDto.ParkingInfo parkingInfo,
        HospitalDetailDto.OperatingInfo operatingInfo,
        List<String> spclDiagList
) {
}
