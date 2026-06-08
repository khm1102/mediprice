package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.dto.HospitalDetailBasicsDto;
import com.khm1102.mediprice.dto.HospitalDetailExtrasDto;
import com.khm1102.mediprice.dto.HospitalSummaryDto;
import com.khm1102.mediprice.dto.AssistantHospitalSearchRequest;
import com.khm1102.mediprice.dto.AssistantHospitalSearchResponse;
import com.khm1102.mediprice.global.common.ApiResponse;
import com.khm1102.mediprice.global.exception.ErrorCode;
import com.khm1102.mediprice.global.exception.business.BusinessException;
import com.khm1102.mediprice.service.AssistantSearchService;
import com.khm1102.mediprice.service.HospitalDetailService;
import com.khm1102.mediprice.service.HospitalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalApiControllerSearchTest {

    @Mock HospitalService hospitalService;
    @Mock HospitalDetailService detailService;
    @Mock AssistantSearchService assistantSearchService;

    private HospitalApiController controller() {
        return new HospitalApiController(hospitalService, detailService, assistantSearchService);
    }

    /** /search는 npayCds(콤마 구분)를 파싱해 service에 List로 전달. */
    @Test
    void searchParsesCommaSeparatedNpayCds() {
        when(hospitalService.searchNearbyV2(anyDouble(), anyDouble(), anyList(),
                anyInt(), anyString(), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        ApiResponse<List<HospitalSummaryDto>> res = controller().searchHospitalsV2(
                37.5, 127.0, "N001,N002,N003", 5000, "mixed", 50, 0.7, 0.3);

        assertThat(res.success()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> codesCaptor = ArgumentCaptor.forClass(List.class);
        verify(hospitalService).searchNearbyV2(eq(37.5), eq(127.0), codesCaptor.capture(),
                eq(5000), eq("mixed"), eq(50), eq(0.7), eq(0.3));
        assertThat(codesCaptor.getValue()).containsExactly("N001", "N002", "N003");
    }

    @Test
    void assistantSearchDelegatesToServiceWithDefaults() {
        AssistantHospitalSearchResponse stub = new AssistantHospitalSearchResponse(
                "도수치료 싼 병원", "price", List.of(), "도수치료 기준으로 검색했어요.", List.of());
        when(assistantSearchService.search("도수치료 싼 병원", 37.5, 127.0, 5000, null, 50))
                .thenReturn(stub);

        ApiResponse<AssistantHospitalSearchResponse> res = controller().searchHospitalsByAssistant(
                new AssistantHospitalSearchRequest("도수치료 싼 병원", 37.5, 127.0, null, null, null));

        assertThat(res.success()).isTrue();
        assertThat(res.data()).isSameAs(stub);
        verify(assistantSearchService).search("도수치료 싼 병원", 37.5, 127.0, 5000, null, 50);
    }

    @Test
    void assistantSearchRejectsBlankQuery() {
        assertThatThrownBy(() -> controller().searchHospitalsByAssistant(
                new AssistantHospitalSearchRequest(" ", 37.5, 127.0, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void assistantSearchRejectsTooLongQuery() {
        assertThatThrownBy(() -> controller().searchHospitalsByAssistant(
                new AssistantHospitalSearchRequest("가".repeat(201), 37.5, 127.0, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void assistantSearchRejectsInvalidRadiusAndLimit() {
        assertThatThrownBy(() -> controller().searchHospitalsByAssistant(
                new AssistantHospitalSearchRequest("MRI", 37.5, 127.0, 99, null, 50)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> controller().searchHospitalsByAssistant(
                new AssistantHospitalSearchRequest("MRI", 37.5, 127.0, 5000, null, 201)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void searchTrimsDropsEmptyTokensAndDeduplicatesNpayCds() {
        when(hospitalService.searchNearbyV2(anyDouble(), anyDouble(), anyList(),
                anyInt(), anyString(), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        controller().searchHospitalsV2(
                37.5, 127.0, " N001, ,N002,N001,, ", 5000, "mixed", 50, 0.7, 0.3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> codesCaptor = ArgumentCaptor.forClass(List.class);
        verify(hospitalService).searchNearbyV2(eq(37.5), eq(127.0), codesCaptor.capture(),
                eq(5000), eq("mixed"), eq(50), eq(0.7), eq(0.3));
        assertThat(codesCaptor.getValue()).containsExactly("N001", "N002");
    }

    /** 빈 npayCds → BusinessException(C002 INVALID_INPUT). */
    @Test
    void rejectsBlankNpayCds() {
        assertThatThrownBy(() -> controller().searchHospitalsV2(
                37.5, 127.0, "", 5000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    /** /{ykiho}/basics는 service.lookupBasics를 호출한다. */
    @Test
    void basicsDelegatesToService() {
        HospitalDetailBasicsDto stub = new HospitalDetailBasicsDto(
                "YK1", "A의원", "서울", "02-1", "의원", null, null, List.of());
        when(detailService.lookupBasics("YK1")).thenReturn(stub);

        ApiResponse<HospitalDetailBasicsDto> res = controller().lookupBasics("YK1");

        assertThat(res.success()).isTrue();
        assertThat(res.data()).isSameAs(stub);
    }

    /** /{ykiho}/extras는 service.lookupExtras를 호출한다. */
    @Test
    void extrasDelegatesToService() {
        HospitalDetailExtrasDto stub = new HospitalDetailExtrasDto(
                "YK1", List.of(), List.of(), List.of(), null, null, List.of());
        when(detailService.lookupExtras("YK1")).thenReturn(stub);

        ApiResponse<HospitalDetailExtrasDto> res = controller().lookupExtras("YK1");

        assertThat(res.success()).isTrue();
        assertThat(res.data()).isSameAs(stub);
    }

    // ── 입력 방어 회귀 방지선 ────────────────────────────────────────────────

    /** radius < 100m → INVALID_INPUT. */
    @Test
    void rejectsTooSmallRadius() {
        assertThatThrownBy(() -> controller().searchHospitalsV2(
                37.5, 127.0, "N001", 50, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    /** radius > 50_000m → INVALID_INPUT. */
    @Test
    void rejectsTooLargeRadius() {
        assertThatThrownBy(() -> controller().searchHospitalsV2(
                37.5, 127.0, "N001", 100_000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    /** npayCds 21개 이상 → INVALID_INPUT. */
    @Test
    void rejectsTooManyNpayCds() {
        String codes = java.util.stream.IntStream.rangeClosed(1, 21)
                .mapToObj(i -> String.format("N%03d", i))
                .collect(java.util.stream.Collectors.joining(","));

        assertThatThrownBy(() -> controller().searchHospitalsV2(
                37.5, 127.0, codes, 5000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    /** npayCd에 영문/숫자 외 문자가 있으면 → INVALID_INPUT. */
    @Test
    void rejectsNpayCdWithInvalidCharacters() {
        assertThatThrownBy(() -> controller().searchHospitalsV2(
                37.5, 127.0, "N001;DROP", 5000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    /** npayCd가 20자 초과면 → INVALID_INPUT. */
    @Test
    void rejectsTooLongNpayCd() {
        String tooLong = "A".repeat(21);
        assertThatThrownBy(() -> controller().searchHospitalsV2(
                37.5, 127.0, tooLong, 5000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    /** 정상 입력은 service까지 도달. */
    @Test
    void allowsValidInputsAtBoundaries() {
        when(hospitalService.searchNearbyV2(anyDouble(), anyDouble(), anyList(),
                anyInt(), anyString(), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        // 경계값 — radius 100 / 50000, npayCds 20개
        String codes = java.util.stream.IntStream.rangeClosed(1, 20)
                .mapToObj(i -> String.format("N%03d", i))
                .collect(java.util.stream.Collectors.joining(","));

        controller().searchHospitalsV2(37.5, 127.0, codes, 100, "mixed", 50, 0.7, 0.3);
        controller().searchHospitalsV2(37.5, 127.0, codes, 50_000, "mixed", 50, 0.7, 0.3);

        verify(hospitalService, org.mockito.Mockito.times(2))
                .searchNearbyV2(anyDouble(), anyDouble(), anyList(), anyInt(),
                        anyString(), anyInt(), anyDouble(), anyDouble());
    }

    // ── 좌표 검증 회귀 방지선 (lat/lng isFinite + WGS84 범위) ────────────────

    /** NaN 좌표는 PostGIS ST_MakePoint를 깨뜨리므로 사전에 INVALID_INPUT으로 차단. */
    @Test
    void rejectsNanLatitude() {
        assertThatThrownBy(() -> controller().searchHospitalsV2(
                Double.NaN, 127.0, "N001", 5000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsNanLongitude() {
        assertThatThrownBy(() -> controller().searchHospitalsV2(
                37.5, Double.NaN, "N001", 5000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsInfinityCoordinates() {
        assertThatThrownBy(() -> controller().searchHospitalsV2(
                Double.POSITIVE_INFINITY, 127.0, "N001", 5000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> controller().searchHospitalsV2(
                37.5, Double.NEGATIVE_INFINITY, "N001", 5000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    /** lat -90~90 범위 밖은 거부. 90.01은 fail, ±90 경계는 통과. */
    @Test
    void rejectsLatitudeOutOfRange() {
        assertThatThrownBy(() -> controller().searchHospitalsV2(
                90.01, 127.0, "N001", 5000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> controller().searchHospitalsV2(
                -90.01, 127.0, "N001", 5000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    /** lng -180~180 범위 밖은 거부. */
    @Test
    void rejectsLongitudeOutOfRange() {
        assertThatThrownBy(() -> controller().searchHospitalsV2(
                37.5, 180.01, "N001", 5000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> controller().searchHospitalsV2(
                37.5, -180.01, "N001", 5000, "mixed", 50, 0.7, 0.3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    /** ±90, ±180 경계값은 통과해서 service까지 도달해야 한다. */
    @Test
    void allowsCoordinateBoundaries() {
        when(hospitalService.searchNearbyV2(anyDouble(), anyDouble(), anyList(),
                anyInt(), anyString(), anyInt(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        controller().searchHospitalsV2(90.0, 180.0, "N001", 5000, "mixed", 50, 0.7, 0.3);
        controller().searchHospitalsV2(-90.0, -180.0, "N001", 5000, "mixed", 50, 0.7, 0.3);

        verify(hospitalService, org.mockito.Mockito.times(2))
                .searchNearbyV2(anyDouble(), anyDouble(), anyList(), anyInt(),
                        anyString(), anyInt(), anyDouble(), anyDouble());
    }
}
