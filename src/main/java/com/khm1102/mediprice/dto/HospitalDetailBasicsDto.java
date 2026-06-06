package com.khm1102.mediprice.dto;

import java.util.List;

/**
 * {@code GET /api/hospitals/{ykiho}/basics} 응답 — DB(병원 기본 + 비급여 가격)만.
 * 외부 HIRA 호출이 없어 캐시 hit/miss 모두 수~수십 ms 안에 끝난다.
 * 프론트는 이 응답으로 가격 카드/가격 표를 즉시 렌더하고 {@code /extras}는 lazy로 합친다.
 */
public record HospitalDetailBasicsDto(
        String ykiho,
        String yadmNm,
        String addr,
        String telNo,
        String clCdNm,
        String hospUrl,
        Integer drTotCnt,
        List<HospitalDetailDto.PriceItem> prices
) {
}
