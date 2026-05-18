package com.khm1102.mediprice.dto;

import java.util.List;

/**
 * {@code GET /api/hospitals/{ykiho}} 응답.
 * <p>
 * DB(Hospital + Price + NonPayItem 매칭) + 의료기관 상세 5개 API 결과 병합.
 */
public record HospitalDetailDto(
        String ykiho,
        String yadmNm,
        String addr,
        String telNo,
        String clCdNm,
        String hospUrl,
        Integer drTotCnt,
        List<PriceItem> prices,
        List<String> dgsbjtList,
        List<String> medOftList,
        List<TransitItem> transitList,
        ParkingInfo parkingInfo,
        OperatingInfo operatingInfo,
        List<String> spclDiagList
) {

    public record PriceItem(String npayCd, String npayKorNm, Long curAmt) {
    }

    /** 대중교통 1건 (지하철/버스 노선). */
    public record TransitItem(String trafNm, String lineNo, String arivPlc, String dir, String dist) {
    }

    /** 주차 정보 (모든 필드 null이면 DTO 자체가 null). */
    public record ParkingInfo(String parkQty, String parkXpnsYn, String parkEtc) {
    }

    /** 진료시간 + 응급 운영 (모든 필드 null이면 DTO 자체가 null). */
    public record OperatingInfo(
            String rcvWeek,
            String rcvSat,
            String lunchWeek,
            String noTrmtSun,
            String noTrmtHoli,
            String emyDayYn,
            String emyNgtYn) {
    }
}
