package com.khm1102.mediprice.batch;

import com.khm1102.mediprice.client.HiraHospitalClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * 심평원 시도코드 17개. {@link HiraHospitalClient#searchHospitals}의
 * sidoCd 파라미터로 사용.
 */
@Getter
@RequiredArgsConstructor
public enum SidoCode {

    SEOUL("110000", "서울"),
    BUSAN("210000", "부산"),
    DAEGU("220000", "대구"),
    INCHEON("230000", "인천"),
    GWANGJU("240000", "광주"),
    DAEJEON("250000", "대전"),
    ULSAN("260000", "울산"),
    SEJONG("310000", "세종"),
    GYEONGGI("410000", "경기"),
    GANGWON("420000", "강원"),
    CHUNGBUK("430000", "충북"),
    CHUNGNAM("440000", "충남"),
    JEONBUK("450000", "전북"),
    JEONNAM("460000", "전남"),
    GYEONGBUK("470000", "경북"),
    GYEONGNAM("480000", "경남"),
    JEJU("500000", "제주");

    private final String code;
    private final String name;

    public static List<SidoCode> all() {
        return Arrays.stream(values()).toList();
    }
}
