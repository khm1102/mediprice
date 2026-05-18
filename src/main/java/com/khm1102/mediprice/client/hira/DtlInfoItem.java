package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 의료기관별상세정보서비스 {@code getDtlInfo2.7} — 세부정보 (주차/진료시간/응급).
 * <p>
 * 한 병원에 단일 row가 반환된다. 필드 중 일부는 null이 흔하다.
 * 응답에는 요일별 진료 시작/종료 시간(`trmtMonStart` 등)이 더 있지만 MVP 화면에서는
 * 평일/토요일 접수시간 + 점심시간 + 휴진 + 응급 여부까지만 사용한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DtlInfoItem(
        String parkQty,
        String parkXpnsYn,
        String parkEtc,
        String rcvWeek,
        String rcvSat,
        String lunchWeek,
        String noTrmtSun,
        String noTrmtHoli,
        String emyDayYn,
        String emyNgtYn
) {
}
