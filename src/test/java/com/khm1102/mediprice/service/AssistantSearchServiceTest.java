package com.khm1102.mediprice.service;

import com.khm1102.mediprice.dto.AssistantHospitalSearchResponse;
import com.khm1102.mediprice.dto.HospitalSummaryDto;
import com.khm1102.mediprice.repository.NonPayItemRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantSearchServiceTest {

    @Mock NonPayItemRepository nonPayItemRepository;
    @Mock HospitalService hospitalService;

    private AssistantSearchService service() {
        return new AssistantSearchService(nonPayItemRepository, hospitalService);
    }

    @Test
    void cheapQueryInfersPriceSortAndCallsHospitalSearch() {
        when(nonPayItemRepository.searchNaturalLanguageMatches("도수치료", 20))
                .thenReturn(List.of(match("MX1220000", "이학요법료/도수치료")));
        when(hospitalService.searchNearbyV2(anyDouble(), anyDouble(), anyList(),
                anyInt(), eq("price"), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of(new HospitalSummaryDto(
                        "YK1", "A의원", null, null, null, 50000L,
                        120.0, 37.5, 127.0, "MX1220000", "이학요법료/도수치료",
                        0.2, null, null)));

        AssistantHospitalSearchResponse res = service().search(
                "도수치료 싼 병원", 37.5, 127.0, 5000, null, 50);

        assertThat(res.interpretedSort()).isEqualTo("price");
        assertThat(res.matchedItems()).extracting("npayCd").containsExactly("MX1220000");
        assertThat(res.hospitals()).hasSize(1);
        verify(hospitalService).searchNearbyV2(
                eq(37.5), eq(127.0), eq(List.of("MX1220000")), eq(5000),
                eq("price"), eq(50), eq(0.7), eq(0.3));
    }

    @Test
    void nearbyQueryInfersDistanceSort() {
        when(nonPayItemRepository.searchNaturalLanguageMatches("MRI", 20))
                .thenReturn(List.of(match("1032A", "자기공명영상진단료(MRI-기본검사)")));
        when(hospitalService.searchNearbyV2(anyDouble(), anyDouble(), anyList(),
                anyInt(), eq("distance"), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        AssistantHospitalSearchResponse res = service().search(
                "근처 MRI", 37.5, 127.0, 5000, null, 50);

        assertThat(res.interpretedSort()).isEqualTo("distance");
        verify(hospitalService).searchNearbyV2(anyDouble(), anyDouble(), anyList(),
                anyInt(), eq("distance"), anyInt(), anyDouble(), anyDouble());
    }

    @Test
    void explicitSortWinsOverQueryHint() {
        when(nonPayItemRepository.searchNaturalLanguageMatches("MRI", 20))
                .thenReturn(List.of(match("1032A", "자기공명영상진단료(MRI-기본검사)")));
        when(hospitalService.searchNearbyV2(anyDouble(), anyDouble(), anyList(),
                anyInt(), eq("mixed"), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        AssistantHospitalSearchResponse res = service().search(
                "근처 MRI", 37.5, 127.0, 5000, "mixed", 50);

        assertThat(res.interpretedSort()).isEqualTo("mixed");
    }

    @Test
    void removesStopwordsBeforeSearchingItems() {
        when(nonPayItemRepository.searchNaturalLanguageMatches("초음파", 20))
                .thenReturn(List.of(match("E78000000", "초음파 이용")));
        when(hospitalService.searchNearbyV2(anyDouble(), anyDouble(), anyList(),
                anyInt(), anyString(), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        service().search("초음파 병원 찾아줘", 37.5, 127.0, 5000, null, 50);

        verify(nonPayItemRepository).searchNaturalLanguageMatches("초음파", 20);
    }

    @Test
    void oneLetterStopwordsDoNotDamageWords() {
        assertThat(AssistantSearchService.toSearchKeyword("데이터 MRI 병원"))
                .isEqualTo("데이터 MRI");
        assertThat(AssistantSearchService.toSearchKeyword("MRI 곳"))
                .isEqualTo("MRI");
    }

    @Test
    void noMatchesReturnsEmptyResponseWithoutHospitalSearch() {
        when(nonPayItemRepository.searchNaturalLanguageMatches("알수없는질문", 20))
                .thenReturn(List.of());

        AssistantHospitalSearchResponse res = service().search(
                "알수없는질문", 37.5, 127.0, 5000, null, 50);

        assertThat(res.matchedItems()).isEmpty();
        assertThat(res.hospitals()).isEmpty();
        verify(hospitalService, never()).searchNearbyV2(anyDouble(), anyDouble(), anyList(),
                anyInt(), anyString(), anyInt(), anyDouble(), anyDouble());
    }

    @Test
    @SuppressWarnings("unchecked")
    void duplicatedMatchedCodesAreCollapsedBeforeHospitalSearch() {
        when(nonPayItemRepository.searchNaturalLanguageMatches("MRI", 20))
                .thenReturn(List.of(
                        match("1032A", "자기공명영상진단료(MRI-기본검사)"),
                        match("1032A", "자기공명영상진단료(MRI-기본검사)")));
        when(hospitalService.searchNearbyV2(anyDouble(), anyDouble(), anyList(),
                anyInt(), eq("mixed"), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of());
        ArgumentCaptor<List<String>> codesCaptor = ArgumentCaptor.forClass(List.class);

        service().search("MRI", 37.5, 127.0, 5000, null, 50);

        verify(hospitalService).searchNearbyV2(anyDouble(), anyDouble(), codesCaptor.capture(),
                anyInt(), eq("mixed"), anyInt(), anyDouble(), anyDouble());
        assertThat(codesCaptor.getValue()).containsExactly("1032A");
    }

    private static NonPayItemRepository.SearchMatch match(String code, String name) {
        return new TestSearchMatch(code, name);
    }

    private record TestSearchMatch(String npayCd,
                                   String npayKorNm,
                                   String npayMdivCdNm,
                                   String npaySdivCdNm,
                                   Double score)
            implements NonPayItemRepository.SearchMatch {

        private TestSearchMatch(String npayCd, String npayKorNm) {
            this(npayCd, npayKorNm, "중분류", "소분류", 0.9);
        }

        @Override
        public String getNpayCd() {
            return npayCd;
        }

        @Override
        public String getNpayKorNm() {
            return npayKorNm;
        }

        @Override
        public String getNpayMdivCdNm() {
            return npayMdivCdNm;
        }

        @Override
        public String getNpaySdivCdNm() {
            return npaySdivCdNm;
        }

        @Override
        public Double getScore() {
            return score;
        }
    }
}
