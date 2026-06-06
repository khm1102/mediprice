package com.khm1102.mediprice.client.hira.stat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 비급여진료비정보서비스 {@code getNonPaymentItemSidoCdList} 응답 item.
 * <p>
 * 한 row가 단일 항목({@code npayCd})에 대한 전체 + 17개 시도 통계를 wide 컬럼으로 펼친 구조.
 * SyncService에서 시도 키별 long row로 정규화하여 {@code NonPayItemSidoStat}에 저장.
 * <p>
 * 시도 약어 (심평원 docx 명세 기준): {@code Sl}(서울) {@code Bs}(부산) {@code Tg}(대구) {@code Ich}(인천)
 * {@code Kw}(광주) {@code Dj}(대전) {@code Usn}(울산) {@code Sj}(세종) {@code Kyg}(경기)
 * {@code Kaw}(강원) {@code Cb}(충북) {@code Ccn}(충남) {@code Jb}(전북) {@code Jn}(전남)
 * {@code Ksb}(경북) {@code Ksn}(경남) {@code Jj}(제주).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NonPaySidoStatItem(
        String npayCd,
        String npayKorNm,
        String stdDate,
        Long prcAvgAll, Long prcMinAll, Long prcMaxAll, Long middAvgAll,
        Long prcAvgSl, Long prcMinSl, Long prcMaxSl, Long middAvgSl,
        Long prcAvgBs, Long prcMinBs, Long prcMaxBs, Long middAvgBs,
        Long prcAvgTg, Long prcMinTg, Long prcMaxTg, Long middAvgTg,
        Long prcAvgIch, Long prcMinIch, Long prcMaxIch, Long middAvgIch,
        Long prcAvgKw, Long prcMinKw, Long prcMaxKw, Long middAvgKw,
        Long prcAvgDj, Long prcMinDj, Long prcMaxDj, Long middAvgDj,
        Long prcAvgUsn, Long prcMinUsn, Long prcMaxUsn, Long middAvgUsn,
        Long prcAvgSj, Long prcMinSj, Long prcMaxSj, Long middAvgSj,
        Long prcAvgKyg, Long prcMinKyg, Long prcMaxKyg, Long middAvgKyg,
        Long prcAvgKaw, Long prcMinKaw, Long prcMaxKaw, Long middAvgKaw,
        Long prcAvgCb, Long prcMinCb, Long prcMaxCb, Long middAvgCb,
        Long prcAvgCcn, Long prcMinCcn, Long prcMaxCcn, Long middAvgCcn,
        Long prcAvgJb, Long prcMinJb, Long prcMaxJb, Long middAvgJb,
        Long prcAvgJn, Long prcMinJn, Long prcMaxJn, Long middAvgJn,
        Long prcAvgKsb, Long prcMinKsb, Long prcMaxKsb, Long middAvgKsb,
        Long prcAvgKsn, Long prcMinKsn, Long prcMaxKsn, Long middAvgKsn,
        Long prcAvgJj, Long prcMinJj, Long prcMaxJj, Long middAvgJj
) {

    /** 정규화 헬퍼: 시도 key → {@link StatValues}. 4개 통계 모두 null인 시도는 entry 생략. */
    public Map<String, StatValues> asStatBySido() {
        Map<String, StatValues> result = new LinkedHashMap<>();
        put(result, "All", prcAvgAll, middAvgAll, prcMinAll, prcMaxAll);
        put(result, "Sl", prcAvgSl, middAvgSl, prcMinSl, prcMaxSl);
        put(result, "Bs", prcAvgBs, middAvgBs, prcMinBs, prcMaxBs);
        put(result, "Tg", prcAvgTg, middAvgTg, prcMinTg, prcMaxTg);
        put(result, "Ich", prcAvgIch, middAvgIch, prcMinIch, prcMaxIch);
        put(result, "Kw", prcAvgKw, middAvgKw, prcMinKw, prcMaxKw);
        put(result, "Dj", prcAvgDj, middAvgDj, prcMinDj, prcMaxDj);
        put(result, "Usn", prcAvgUsn, middAvgUsn, prcMinUsn, prcMaxUsn);
        put(result, "Sj", prcAvgSj, middAvgSj, prcMinSj, prcMaxSj);
        put(result, "Kyg", prcAvgKyg, middAvgKyg, prcMinKyg, prcMaxKyg);
        put(result, "Kaw", prcAvgKaw, middAvgKaw, prcMinKaw, prcMaxKaw);
        put(result, "Cb", prcAvgCb, middAvgCb, prcMinCb, prcMaxCb);
        put(result, "Ccn", prcAvgCcn, middAvgCcn, prcMinCcn, prcMaxCcn);
        put(result, "Jb", prcAvgJb, middAvgJb, prcMinJb, prcMaxJb);
        put(result, "Jn", prcAvgJn, middAvgJn, prcMinJn, prcMaxJn);
        put(result, "Ksb", prcAvgKsb, middAvgKsb, prcMinKsb, prcMaxKsb);
        put(result, "Ksn", prcAvgKsn, middAvgKsn, prcMinKsn, prcMaxKsn);
        put(result, "Jj", prcAvgJj, middAvgJj, prcMinJj, prcMaxJj);
        return result;
    }

    private static void put(Map<String, StatValues> map, String key, Long avg, Long mid, Long min, Long max) {
        StatValues values = new StatValues(avg, mid, min, max);
        if (!values.isAllNull()) {
            map.put(key, values);
        }
    }
}
