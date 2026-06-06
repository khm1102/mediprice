package com.khm1102.mediprice.client.hira.stat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 비급여진료비정보서비스 {@code getNonPaymentItemClcdList} 응답 item.
 * <p>
 * 한 row가 단일 항목({@code npayCd})에 대한 모든 의료기관 종별 통계를 wide 컬럼으로 펼친 구조.
 * SyncService에서 종별별 long row로 정규화하여 {@code NonPayItemClcdStat}에 저장.
 * <p>
 * 알려진 종별 키: {@code All}(전체), {@code Usgh}(상급종합), {@code Hosp}(종합병원·병원),
 * {@code Gnhp}(의원).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NonPayClcdStatItem(
        String npayCd,
        String npayKorNm,
        String stdDate,
        Long prcAvgAll, Long prcMinAll, Long prcMaxAll, Long middAvgAll,
        Long prcAvgUsgh, Long prcMinUsgh, Long prcMaxUsgh, Long middAvgUsgh,
        Long prcAvgHosp, Long prcMinHosp, Long prcMaxHosp, Long middAvgHosp,
        Long prcAvgGnhp, Long prcMinGnhp, Long prcMaxGnhp, Long middAvgGnhp
) {

    /** 정규화 헬퍼: 종별 key → {@link StatValues}. 4개 통계 모두 null인 종별은 entry 생략. */
    public Map<String, StatValues> asStatByClcd() {
        Map<String, StatValues> result = new LinkedHashMap<>();
        put(result, "All", prcAvgAll, middAvgAll, prcMinAll, prcMaxAll);
        put(result, "Usgh", prcAvgUsgh, middAvgUsgh, prcMinUsgh, prcMaxUsgh);
        put(result, "Hosp", prcAvgHosp, middAvgHosp, prcMinHosp, prcMaxHosp);
        put(result, "Gnhp", prcAvgGnhp, middAvgGnhp, prcMinGnhp, prcMaxGnhp);
        return result;
    }

    private static void put(Map<String, StatValues> map, String key, Long avg, Long mid, Long min, Long max) {
        StatValues values = new StatValues(avg, mid, min, max);
        if (!values.isAllNull()) {
            map.put(key, values);
        }
    }
}
