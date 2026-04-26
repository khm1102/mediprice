package com.khm1102.mediprice.dto;

import java.util.List;

/**
 * {@code GET /api/hospitals/{ykiho}} 응답.
 * <p>
 * DB(Hospital + Price + NonPayItem 매칭) + 의료기관 상세 4개 API 결과 병합.
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
        TrnsprtInfo trnsprtInfo,
        List<String> spclDiagList
) {

    public record PriceItem(String npayCd, String npayKorNm, Long curAmt) {
    }

    public record TrnsprtInfo(String parkYn, String parkXpnsYn, String parkEtc, String trafInfo, String parkQty) {
    }
}
