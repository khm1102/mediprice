package com.khm1102.mediprice.batch.support;

import com.khm1102.mediprice.client.HiraHospitalClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 심평원 시도코드 17개. {@link HiraHospitalClient#searchHospitals}의
 * sidoCd 파라미터로 사용.
 * <p>
 * 코드는 HIRA {@code getHospBasisList} 실제 응답으로 검증 — 행정자치부 행정구역코드와 다름.
 * {@code abbr}는 {@code getNonPaymentItemSidoCdList} 응답이 시도 통계를 wide 컬럼으로 펼칠 때
 * 사용하는 영문 약어 ({@code Sl}/{@code Bs}/...).
 */
@Getter
@RequiredArgsConstructor
public enum SidoCode {

    SEOUL("110000", "서울", "Sl"),
    BUSAN("210000", "부산", "Bs"),
    INCHEON("220000", "인천", "Ich"),
    DAEGU("230000", "대구", "Tg"),
    GWANGJU("240000", "광주", "Kw"),
    DAEJEON("250000", "대전", "Dj"),
    ULSAN("260000", "울산", "Usn"),
    GYEONGGI("310000", "경기", "Kyg"),
    GANGWON("320000", "강원", "Kaw"),
    CHUNGBUK("330000", "충북", "Cb"),
    CHUNGNAM("340000", "충남", "Ccn"),
    JEONBUK("350000", "전북", "Jb"),
    JEONNAM("360000", "전남", "Jn"),
    GYEONGBUK("370000", "경북", "Ksb"),
    GYEONGNAM("380000", "경남", "Ksn"),
    JEJU("390000", "제주", "Jj"),
    SEJONG("410000", "세종", "Sj");

    private final String code;
    private final String name;
    /** {@code getNonPaymentItemSidoCdList} 응답 wide 컬럼 약어. */
    private final String abbr;

    public static List<SidoCode> all() {
        return Arrays.stream(values()).toList();
    }

    /** API 응답의 시도 약어로 SidoCode 조회 (대소문자 구분). 매칭 안 되면 empty. */
    public static Optional<SidoCode> byAbbr(String abbr) {
        if (abbr == null) {
            return Optional.empty();
        }
        for (SidoCode s : values()) {
            if (s.abbr.equals(abbr)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
