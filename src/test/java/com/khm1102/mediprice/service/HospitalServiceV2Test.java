package com.khm1102.mediprice.service;

import com.khm1102.mediprice.dto.HospitalSummaryDto;
import com.khm1102.mediprice.entity.NonPayItemClcdStat;
import com.khm1102.mediprice.repository.HospitalRepository;
import com.khm1102.mediprice.repository.NonPayItemClcdStatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalServiceV2Test {

    @Mock HospitalRepository repository;
    @Mock NonPayItemService nonPayItemService;
    @Mock NonPayItemClcdStatRepository clcdStatRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private HospitalService service;

    @BeforeEach
    void setUp() {
        service = new HospitalService(repository, nonPayItemService, clcdStatRepository, jsonMapper);
    }

    /** v2 검색 — matched npayCd 집합으로 NonPayItem을 1회 batch 조회해 항목명을 결합. */
    @Test
    void enrichesResultsWithMatchedNameInSingleBatch() {
        String json = """
                [
                  {"ykiho":"YK1","yadmNm":"A의원","matchedNpayCd":"N001","curAmt":50000,"distance":120.0,"score":0.3,"lat":37.5,"lng":127.0},
                  {"ykiho":"YK2","yadmNm":"B의원","matchedNpayCd":"N002","curAmt":80000,"distance":300.0,"score":0.5,"lat":37.5,"lng":127.0}
                ]
                """;
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(json);
        when(nonPayItemService.lookupNamesByCodes(any()))
                .thenReturn(Map.of("N001", "도수치료", "N002", "MRI 척추"));

        List<HospitalSummaryDto> result = service.searchNearbyV2(
                37.5, 127.0, List.of("N001", "N002"), 5000, "mixed", 50, 0.7, 0.3);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).matchedNpayKorNm()).isEqualTo("도수치료");
        assertThat(result.get(1).matchedNpayKorNm()).isEqualTo("MRI 척추");
        verify(nonPayItemService).lookupNamesByCodes(any());
    }

    /** 결과가 비면 NonPayItem 조회를 하지 않는다. */
    @Test
    void skipsNameLookupWhenNoResults() {
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(null);

        List<HospitalSummaryDto> result = service.searchNearbyV2(
                37.5, 127.0, List.of("N001"), 5000, "mixed", 50, 0.7, 0.3);

        assertThat(result).isEmpty();
        verify(nonPayItemService, never()).lookupNamesByCodes(any());
    }

    /** sort 인자가 허용 외 값이면 mixed로 보정해 repository에 전달. */
    @Test
    void coercesInvalidSortToMixed() {
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble())).thenReturn("[]");
        ArgumentCaptor<String> sortCaptor = ArgumentCaptor.forClass(String.class);

        service.searchNearbyV2(37.5, 127.0, List.of("N001"), 5000, "wrong", 50, 0.7, 0.3);

        verify(repository).searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                sortCaptor.capture(), anyInt(), anyDouble(), anyDouble());
        assertThat(sortCaptor.getValue()).isEqualTo("mixed");
    }

    /** limit이 200을 초과하면 200으로 clamp된다. */
    @Test
    void clampsLimitToUpperBound() {
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble())).thenReturn("[]");
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);

        service.searchNearbyV2(37.5, 127.0, List.of("N001"), 5000, "mixed", 999, 0.7, 0.3);

        verify(repository).searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), limitCaptor.capture(), anyDouble(), anyDouble());
        assertThat(limitCaptor.getValue()).isEqualTo(200);
    }

    /** 허용된 sort 값(price)은 그대로 전달된다. */
    @Test
    void preservesAllowedSort() {
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble())).thenReturn("[]");
        ArgumentCaptor<String> sortCaptor = ArgumentCaptor.forClass(String.class);

        service.searchNearbyV2(37.5, 127.0, List.of("N001"), 5000, "price", 30, 0.7, 0.3);

        verify(repository).searchNearbyV2Json(anyDouble(), anyDouble(), any(), eq(5000),
                sortCaptor.capture(), anyInt(), anyDouble(), anyDouble());
        assertThat(sortCaptor.getValue()).isEqualTo("price");
    }

    // ── 가중치 폴백 회귀 방지선 ─────────────────────────────────────────────

    /** 음수 가중치는 기본값(0.7/0.3)으로 silent 폴백된다. */
    @Test
    void negativeWeightsAreCoercedToDefaults() {
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble())).thenReturn("[]");

        ArgumentCaptor<Double> priceCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> distanceCaptor = ArgumentCaptor.forClass(Double.class);

        service.searchNearbyV2(37.5, 127.0, List.of("N001"), 5000, "mixed", 50, -1.0, 0.3);

        verify(repository).searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), priceCaptor.capture(), distanceCaptor.capture());
        assertThat(priceCaptor.getValue()).isEqualTo(0.7);
        assertThat(distanceCaptor.getValue()).isEqualTo(0.3);
    }

    /** 합이 0이면 기본값으로 폴백 (정렬 점수 0/0 회피). */
    @Test
    void zeroSumWeightsAreCoercedToDefaults() {
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble())).thenReturn("[]");

        ArgumentCaptor<Double> priceCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> distanceCaptor = ArgumentCaptor.forClass(Double.class);

        service.searchNearbyV2(37.5, 127.0, List.of("N001"), 5000, "mixed", 50, 0.0, 0.0);

        verify(repository).searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), priceCaptor.capture(), distanceCaptor.capture());
        assertThat(priceCaptor.getValue()).isEqualTo(0.7);
        assertThat(distanceCaptor.getValue()).isEqualTo(0.3);
    }

    /** 합이 1을 초과해도 정상 입력으로 그대로 전달 — SQL이 정규화한다. */
    @Test
    void weightsSummingAboveOneArePreserved() {
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble())).thenReturn("[]");

        ArgumentCaptor<Double> priceCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> distanceCaptor = ArgumentCaptor.forClass(Double.class);

        service.searchNearbyV2(37.5, 127.0, List.of("N001"), 5000, "mixed", 50, 1.5, 0.8);

        verify(repository).searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), priceCaptor.capture(), distanceCaptor.capture());
        assertThat(priceCaptor.getValue()).isEqualTo(1.5);
        assertThat(distanceCaptor.getValue()).isEqualTo(0.8);
    }

    // ── 파서 엣지 케이스 (옛 HospitalServiceTest에서 이관) ────────────────────

    /** 빈 문자열 응답도 빈 리스트로 안전 처리 — 잘못된 트리거가 아닌 무결과로 본다. */
    @Test
    void returnsEmptyWhenJsonBlank() {
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble())).thenReturn("   ");

        List<HospitalSummaryDto> result = service.searchNearbyV2(
                37.5, 127.0, List.of("N001"), 5000, "mixed", 50, 0.7, 0.3);

        assertThat(result).isEmpty();
        verify(nonPayItemService, never()).lookupNamesByCodes(any());
    }

    /** 응답 JSON이 깨졌어도 예외를 밖으로 던지지 않고 빈 리스트 + WARN 로그 — 장애 격리. */
    @Test
    void returnsEmptyWhenParsingFails() {
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble())).thenReturn("{not valid json");

        List<HospitalSummaryDto> result = service.searchNearbyV2(
                37.5, 127.0, List.of("N001"), 5000, "mixed", 50, 0.7, 0.3);

        assertThat(result).isEmpty();
        verify(nonPayItemService, never()).lookupNamesByCodes(any());
    }

    // ── 동종 평균 enrichment (avgAmt / diffPct) ──────────────────────────────

    /**
     * clCdNm '의원' → clcdKey 'Gnhp' 매칭. 평균 100,000원, 자기 가격 80,000원이면 diffPct -20%.
     */
    @Test
    void enrichesWithMatchedClcdAverageAndComputesDiffPercent() {
        String json = """
                [{"ykiho":"YK1","yadmNm":"A의원","clCdNm":"의원","matchedNpayCd":"N001",
                  "curAmt":80000,"distance":150.0,"score":0.3,"lat":37.5,"lng":127.0}]
                """;
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble())).thenReturn(json);
        when(nonPayItemService.lookupNamesByCodes(any())).thenReturn(Map.of("N001", "도수치료"));
        when(clcdStatRepository.findAllByNpayCdIn(any())).thenReturn(List.of(
                NonPayItemClcdStat.builder()
                        .npayCd("N001").clcdKey("Gnhp").stdDate("20260301").prcAvg(100_000L).build()
        ));

        List<HospitalSummaryDto> result = service.searchNearbyV2(
                37.5, 127.0, List.of("N001"), 5000, "mixed", 50, 0.7, 0.3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).avgAmt()).isEqualTo(100_000L);
        assertThat(result.get(0).diffPct()).isEqualTo(-20.0);
    }

    /**
     * 정확한 clcdKey 매칭이 없으면 "All" 평균으로 폴백.
     */
    @Test
    void fallsBackToAllAverageWhenClcdKeyNotFound() {
        String json = """
                [{"ykiho":"YK1","yadmNm":"A요양원","clCdNm":"요양원","matchedNpayCd":"N001",
                  "curAmt":50000,"distance":150.0,"score":0.3,"lat":37.5,"lng":127.0}]
                """;
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble())).thenReturn(json);
        when(nonPayItemService.lookupNamesByCodes(any())).thenReturn(Map.of("N001", "도수치료"));
        when(clcdStatRepository.findAllByNpayCdIn(any())).thenReturn(List.of(
                NonPayItemClcdStat.builder()
                        .npayCd("N001").clcdKey("All").stdDate("20260301").prcAvg(60_000L).build()
        ));

        List<HospitalSummaryDto> result = service.searchNearbyV2(
                37.5, 127.0, List.of("N001"), 5000, "mixed", 50, 0.7, 0.3);

        // 요양원은 매핑에 없으니 "All"로 폴백. 60,000 평균 대비 50,000 = -16.67%
        assertThat(result.get(0).avgAmt()).isEqualTo(60_000L);
        assertThat(result.get(0).diffPct()).isCloseTo(-16.666, within(0.01));
    }

    /**
     * (npayCd, clcdKey) 단위로 가장 최신 stdDate만 사용 — 옛 통계는 무시.
     */
    @Test
    void picksLatestStdDatePerKey() {
        String json = """
                [{"ykiho":"YK1","yadmNm":"A의원","clCdNm":"의원","matchedNpayCd":"N001",
                  "curAmt":100000,"distance":150.0,"score":0.3,"lat":37.5,"lng":127.0}]
                """;
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble())).thenReturn(json);
        when(nonPayItemService.lookupNamesByCodes(any())).thenReturn(Map.of("N001", "X"));
        when(clcdStatRepository.findAllByNpayCdIn(any())).thenReturn(List.of(
                NonPayItemClcdStat.builder()
                        .npayCd("N001").clcdKey("Gnhp").stdDate("20240301").prcAvg(50_000L).build(),
                NonPayItemClcdStat.builder()
                        .npayCd("N001").clcdKey("Gnhp").stdDate("20260301").prcAvg(80_000L).build()
        ));

        List<HospitalSummaryDto> result = service.searchNearbyV2(
                37.5, 127.0, List.of("N001"), 5000, "mixed", 50, 0.7, 0.3);

        // 최신 stdDate(20260301)의 80,000을 써서 100,000 → +25%
        assertThat(result.get(0).avgAmt()).isEqualTo(80_000L);
        assertThat(result.get(0).diffPct()).isEqualTo(25.0);
    }

    /** 평균이 없거나 0이면 avgAmt/diffPct는 null로 그대로 둔다 — 부정확 비교 회피. */
    @Test
    void leavesStatNullWhenAverageMissingOrZero() {
        String json = """
                [{"ykiho":"YK1","yadmNm":"A의원","clCdNm":"의원","matchedNpayCd":"N001",
                  "curAmt":50000,"distance":150.0,"score":0.3,"lat":37.5,"lng":127.0}]
                """;
        when(repository.searchNearbyV2Json(anyDouble(), anyDouble(), any(), anyInt(),
                anyString(), anyInt(), anyDouble(), anyDouble())).thenReturn(json);
        when(nonPayItemService.lookupNamesByCodes(any())).thenReturn(Map.of("N001", "X"));
        when(clcdStatRepository.findAllByNpayCdIn(any())).thenReturn(List.of(
                NonPayItemClcdStat.builder()
                        .npayCd("N001").clcdKey("Gnhp").stdDate("20260301").prcAvg(0L).build()
        ));

        List<HospitalSummaryDto> result = service.searchNearbyV2(
                37.5, 127.0, List.of("N001"), 5000, "mixed", 50, 0.7, 0.3);

        assertThat(result.get(0).avgAmt()).isNull();
        assertThat(result.get(0).diffPct()).isNull();
    }

    /** clCdNm → clcdKey 매핑 단위 — 알 수 없는 라벨은 "All". */
    @Test
    void clCdNmToClcdKeyHandlesKnownAndUnknownLabels() {
        assertThat(HospitalService.clCdNmToClcdKey("상급종합")).isEqualTo("Usgh");
        assertThat(HospitalService.clCdNmToClcdKey("종합병원")).isEqualTo("Hosp");
        assertThat(HospitalService.clCdNmToClcdKey("병원")).isEqualTo("Hosp");
        assertThat(HospitalService.clCdNmToClcdKey("의원")).isEqualTo("Gnhp");
        assertThat(HospitalService.clCdNmToClcdKey("한의원")).isEqualTo("Gnhp");
        assertThat(HospitalService.clCdNmToClcdKey("치과의원")).isEqualTo("Gnhp");
        assertThat(HospitalService.clCdNmToClcdKey("요양원")).isEqualTo("All");
        assertThat(HospitalService.clCdNmToClcdKey(null)).isEqualTo("All");
        assertThat(HospitalService.clCdNmToClcdKey("")).isEqualTo("All");
    }
}
