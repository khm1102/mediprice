package com.khm1102.mediprice.batch;

import com.khm1102.mediprice.client.HiraHospitalClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * 심평원 시도코드 17개. {@link HiraHospitalClient#searchHospitals}의
 * sidoCd 파라미터로 사용.
 * <p>
 * 코드는 HIRA {@code getHospBasisList} 실제 응답으로 검증 — 행정자치부 행정구역코드와 다름.
 */
@Getter
@RequiredArgsConstructor
public enum SidoCode {

    SEOUL("110000", "서울"),
    BUSAN("210000", "부산"),
    INCHEON("220000", "인천"),
    DAEGU("230000", "대구"),
    GWANGJU("240000", "광주"),
    DAEJEON("250000", "대전"),
    ULSAN("260000", "울산"),
    GYEONGGI("310000", "경기"),
    GANGWON("320000", "강원"),
    CHUNGBUK("330000", "충북"),
    CHUNGNAM("340000", "충남"),
    JEONBUK("350000", "전북"),
    JEONNAM("360000", "전남"),
    GYEONGBUK("370000", "경북"),
    GYEONGNAM("380000", "경남"),
    JEJU("390000", "제주"),
    SEJONG("410000", "세종");

    private final String code;
    private final String name;

    public static List<SidoCode> all() {
        return Arrays.stream(values()).toList();
    }
}
