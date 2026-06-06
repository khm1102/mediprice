package com.khm1102.mediprice.service;

import com.khm1102.mediprice.dto.HospitalSummaryDto;
import com.khm1102.mediprice.entity.NonPayItemClcdStat;
import com.khm1102.mediprice.repository.HospitalRepository;
import com.khm1102.mediprice.repository.NonPayItemClcdStatRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 병원 검색.
 * <p>
 * {@link #searchNearbyV2} — 다중 npayCd + 정렬 모드 + 매칭 항목명 결합({@code search_nearby_hospitals_v2}).
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class HospitalService {

    private static final TypeReference<List<HospitalSummaryDto>> SUMMARY_LIST_TYPE =
            new TypeReference<>() {};

    public static final Set<String> ALLOWED_SORTS = Set.of("mixed", "price", "distance");

    /** mixed 정렬 점수의 기본 가중치 — 사용자가 비정상 값을 넣으면 silent 폴백한다. */
    public static final double DEFAULT_W_PRICE = 0.7;
    public static final double DEFAULT_W_DISTANCE = 0.3;

    private final HospitalRepository repository;
    private final NonPayItemService nonPayItemService;
    private final NonPayItemClcdStatRepository clcdStatRepository;
    private final JsonMapper jsonMapper;

    public HospitalService(HospitalRepository repository,
                           NonPayItemService nonPayItemService,
                           NonPayItemClcdStatRepository clcdStatRepository,
                           JsonMapper jsonMapper) {
        this.repository = repository;
        this.nonPayItemService = nonPayItemService;
        this.clcdStatRepository = clcdStatRepository;
        this.jsonMapper = jsonMapper;
    }

    /**
     * v2 검색 — 다중 npayCd + 정렬 모드. matchedNpayCd로 항목명을 한 번에 결합한다.
     *
     * @param sort      'mixed' | 'price' | 'distance' — 허용 외 값은 'mixed'로 보정
     * @param limit     1 ~ 200 사이로 clamp
     * @param wPrice    혼합 점수 가격 가중치 (기본 0.7)
     * @param wDistance 혼합 점수 거리 가중치 (기본 0.3)
     */
    public List<HospitalSummaryDto> searchNearbyV2(double lat, double lng,
                                                   List<String> npayCds, int radius,
                                                   String sort, int limit,
                                                   double wPrice, double wDistance) {
        String resolvedSort = ALLOWED_SORTS.contains(sort) ? sort : "mixed";
        int resolvedLimit = Math.max(1, Math.min(limit, 200));
        String[] codes = npayCds == null ? new String[0]
                : npayCds.stream().filter(Objects::nonNull).distinct().toArray(String[]::new);

        double[] resolvedWeights = resolveWeights(wPrice, wDistance);
        String json = repository.searchNearbyV2Json(
                lat, lng, codes, radius, resolvedSort, resolvedLimit,
                resolvedWeights[0], resolvedWeights[1]);
        List<HospitalSummaryDto> rows = parse(json);
        if (rows.isEmpty()) {
            return rows;
        }
        return enrichWithStat(enrichWithMatchedName(rows));
    }

    /**
     * 결과 행의 matchedNpayCd 집합을 한 번에 모아 항목명을 일괄 조회하고 각 행에 부여한다.
     * NonPayItem 조회는 검색 1회당 1회만.
     */
    private List<HospitalSummaryDto> enrichWithMatchedName(List<HospitalSummaryDto> rows) {
        Set<String> matchedCodes = new LinkedHashSet<>();
        for (HospitalSummaryDto row : rows) {
            if (row.matchedNpayCd() != null) {
                matchedCodes.add(row.matchedNpayCd());
            }
        }
        if (matchedCodes.isEmpty()) {
            return rows;
        }
        Map<String, String> nameByCode = nonPayItemService.lookupNamesByCodes(matchedCodes);
        return rows.stream()
                .map(row -> row.matchedNpayCd() == null
                        ? row
                        : row.withMatchedNpayKorNm(nameByCode.get(row.matchedNpayCd())))
                .toList();
    }

    /**
     * (npayCd × clCd) 평균과 자체 가격 대비 %를 결합한다.
     * <ul>
     *   <li>대상 npayCd 집합을 한 번의 batch query로 NonPayItemClcdStat에서 가져온다.</li>
     *   <li>(npayCd, clcdKey)별로 가장 최신 stdDate 행만 남긴다.</li>
     *   <li>병원의 clCdNm을 clcdKey로 매핑해 평균을 찾는다. 정확한 종별 평균이 없으면 "All" 평균으로 폴백.</li>
     *   <li>평균이 0/null이면 비율을 계산하지 않고 행을 그대로 둔다.</li>
     * </ul>
     * 호출 1회당 ClcdStatRepository 조회 1회 — 결과 카드 수와 무관.
     */
    private List<HospitalSummaryDto> enrichWithStat(List<HospitalSummaryDto> rows) {
        Set<String> codes = new LinkedHashSet<>();
        for (HospitalSummaryDto row : rows) {
            if (row.matchedNpayCd() != null) {
                codes.add(row.matchedNpayCd());
            }
        }
        if (codes.isEmpty()) {
            return rows;
        }
        List<NonPayItemClcdStat> stats = clcdStatRepository.findAllByNpayCdIn(codes);
        Map<String, NonPayItemClcdStat> latestByKey = pickLatestPerKey(stats);
        return rows.stream()
                .map(row -> applyStat(row, latestByKey))
                .toList();
    }

    /** (npayCd|clcdKey) 단위로 stdDate가 가장 큰 행만 남긴다. */
    private static Map<String, NonPayItemClcdStat> pickLatestPerKey(List<NonPayItemClcdStat> stats) {
        Map<String, NonPayItemClcdStat> latest = new HashMap<>();
        for (NonPayItemClcdStat s : stats) {
            String key = s.getNpayCd() + "|" + s.getClcdKey();
            NonPayItemClcdStat prev = latest.get(key);
            if (prev == null || compareStdDate(s.getStdDate(), prev.getStdDate()) > 0) {
                latest.put(key, s);
            }
        }
        return latest;
    }

    private static int compareStdDate(String a, String b) {
        if (a == null) return b == null ? 0 : -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    /** 한 행에 종별 평균을 결합. 종별 매칭이 안 되면 "All" 폴백. 평균이 없으면 행을 그대로. */
    private static HospitalSummaryDto applyStat(HospitalSummaryDto row,
                                                Map<String, NonPayItemClcdStat> latestByKey) {
        if (row.matchedNpayCd() == null || row.curAmt() == null) {
            return row;
        }
        String clcdKey = clCdNmToClcdKey(row.clCdNm());
        NonPayItemClcdStat stat = lookupStat(row.matchedNpayCd(), clcdKey, latestByKey);
        if (stat == null && !"All".equals(clcdKey)) {
            stat = lookupStat(row.matchedNpayCd(), "All", latestByKey);
        }
        if (stat == null || stat.getPrcAvg() == null || stat.getPrcAvg() <= 0L) {
            return row;
        }
        long avg = stat.getPrcAvg();
        double diffPct = (double) (row.curAmt() - avg) / avg * 100.0;
        return row.withStat(avg, diffPct);
    }

    private static NonPayItemClcdStat lookupStat(String npayCd, String clcdKey,
                                                 Map<String, NonPayItemClcdStat> latestByKey) {
        return latestByKey.get(npayCd + "|" + clcdKey);
    }

    /**
     * Hospital.clCdNm(한글) → NonPayItemClcdStat.clcdKey(영문 약어) 매핑.
     * HIRA 통계는 4개 버킷 (All/Usgh/Hosp/Gnhp)이라 다대일 매핑. 알 수 없으면 "All" 폴백.
     */
    static String clCdNmToClcdKey(String clCdNm) {
        if (clCdNm == null) {
            return "All";
        }
        return switch (clCdNm.trim()) {
            case "상급종합" -> "Usgh";
            case "종합병원", "병원", "요양병원", "한방병원", "정신병원", "치과병원" -> "Hosp";
            case "의원", "한의원", "치과의원" -> "Gnhp";
            default -> "All";
        };
    }

    /**
     * 가중치 정규화: 음수거나 합이 0 이하면 silent로 기본값(0.7/0.3) 폴백 + WARN 로그.
     * SQL이 결과 집합 내 MAX OVER로 정규화하므로 합이 1 초과/미만은 그대로 허용.
     */
    static double[] resolveWeights(double wPrice, double wDistance) {
        if (wPrice < 0 || wDistance < 0 || (wPrice + wDistance) <= 0) {
            log.warn("비정상 가중치(wPrice={}, wDistance={}) — 기본값으로 폴백", wPrice, wDistance);
            return new double[]{DEFAULT_W_PRICE, DEFAULT_W_DISTANCE};
        }
        return new double[]{wPrice, wDistance};
    }

    private List<HospitalSummaryDto> parse(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return jsonMapper.readValue(json, SUMMARY_LIST_TYPE);
        } catch (Exception e) {
            log.warn("search_nearby_hospitals_v2 결과 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }
}
