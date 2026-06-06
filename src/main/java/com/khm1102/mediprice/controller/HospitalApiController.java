package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.dto.HospitalDetailBasicsDto;
import com.khm1102.mediprice.dto.HospitalDetailDto;
import com.khm1102.mediprice.dto.HospitalDetailExtrasDto;
import com.khm1102.mediprice.dto.HospitalSummaryDto;
import com.khm1102.mediprice.global.common.ApiResponse;
import com.khm1102.mediprice.global.exception.ErrorCode;
import com.khm1102.mediprice.global.exception.business.BusinessException;
import com.khm1102.mediprice.service.HospitalDetailService;
import com.khm1102.mediprice.service.HospitalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalApiController {

    private static final int DEFAULT_RADIUS_METERS = 5000;
    private static final int DEFAULT_LIMIT = 50;
    private static final double DEFAULT_W_PRICE = 0.7;
    private static final double DEFAULT_W_DISTANCE = 0.3;

    /** 검색 반경 허용 범위(m). 너무 작으면 거의 모든 결과가 사라지고, 너무 크면 DB가 unnecessary scan을 한다. */
    private static final int MIN_RADIUS_METERS = 100;
    private static final int MAX_RADIUS_METERS = 50_000;
    /** npayCd 다중 입력 상한. 프론트는 보통 10개 안팎이고, 20개를 넘으면 broad keyword 안내로 유도한다. */
    private static final int MAX_NPAY_CDS = 20;
    /** 각 npayCd 길이 한계 — DB 컬럼 길이(20)에 맞춰 보수적으로. */
    private static final int MAX_NPAY_CD_LEN = 20;
    private static final Pattern NPAY_CD_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");

    /** 좌표 허용 범위 — WGS84 표준. NaN/Infinity는 PostGIS ST_MakePoint에 그대로 들어가면 함수 실패. */
    private static final double LAT_MIN = -90.0;
    private static final double LAT_MAX = 90.0;
    private static final double LNG_MIN = -180.0;
    private static final double LNG_MAX = 180.0;

    private final HospitalService hospitalService;
    private final HospitalDetailService detailService;

    public HospitalApiController(HospitalService hospitalService, HospitalDetailService detailService) {
        this.hospitalService = hospitalService;
        this.detailService = detailService;
    }

    /**
     * 다중 npayCd + 정렬 모드를 단일 호출로 처리하는 검색.
     * <p>
     * 쿼리:
     * <ul>
     *   <li>{@code lat, lng} — 사용자 좌표 (필수)</li>
     *   <li>{@code npayCds} — 콤마 구분 npayCd 목록 (1개 이상 필수)</li>
     *   <li>{@code radius} — 미터, 기본 5000</li>
     *   <li>{@code sort} — {@code mixed | price | distance}, 기본 {@code mixed}</li>
     *   <li>{@code limit} — 1~200, 기본 50</li>
     * </ul>
     * 가중치({@code wPrice}, {@code wDistance})는 운영 튜닝용 옵션 — 평소엔 기본값 사용.
     */
    @GetMapping("/search")
    public ApiResponse<List<HospitalSummaryDto>> searchHospitalsV2(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String npayCds,
            @RequestParam(defaultValue = "" + DEFAULT_RADIUS_METERS) int radius,
            @RequestParam(defaultValue = "mixed") String sort,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit,
            @RequestParam(defaultValue = "" + DEFAULT_W_PRICE) double wPrice,
            @RequestParam(defaultValue = "" + DEFAULT_W_DISTANCE) double wDistance) {
        validateCoordinates(lat, lng);
        List<String> codes = parseNpayCds(npayCds);
        validateSearchInputs(radius, codes);
        return ApiResponse.success(hospitalService.searchNearbyV2(
                lat, lng, codes, radius, sort, limit, wPrice, wDistance));
    }

    /**
     * 좌표 유한성 + WGS84 범위 검증.
     * <p>
     * {@code Double.isFinite}로 NaN/Infinity를 차단한다 — 그대로 service까지 흘러가면 PostGIS
     * {@code ST_MakePoint(NaN, NaN)::geography}가 함수 실패 (이상 좌표는 절대 발생하면 안 되는
     * 입력이므로 명시적으로 400 INVALID_INPUT으로 응답).
     */
    private static void validateCoordinates(double lat, double lng) {
        if (!Double.isFinite(lat) || lat < LAT_MIN || lat > LAT_MAX) {
            throw new InvalidInputException(
                    "lat는 -90~90 사이의 유한한 값이어야 합니다.");
        }
        if (!Double.isFinite(lng) || lng < LNG_MIN || lng > LNG_MAX) {
            throw new InvalidInputException(
                    "lng는 -180~180 사이의 유한한 값이어야 합니다.");
        }
    }

    /**
     * radius 범위 + npayCds 개수/패턴/길이 검증.
     * service 단의 sort 보정/limit clamp/가중치 폴백과 함께 입력 신뢰선을 형성한다.
     */
    private static void validateSearchInputs(int radius, List<String> codes) {
        if (radius < MIN_RADIUS_METERS || radius > MAX_RADIUS_METERS) {
            throw new InvalidInputException(
                    "radius는 " + MIN_RADIUS_METERS + "~" + MAX_RADIUS_METERS + "m 범위여야 합니다.");
        }
        if (codes.isEmpty()) {
            throw new InvalidInputException("npayCds는 최소 1개 이상의 npayCd가 필요합니다.");
        }
        if (codes.size() > MAX_NPAY_CDS) {
            throw new InvalidInputException(
                    "npayCds는 최대 " + MAX_NPAY_CDS + "개까지 지정할 수 있습니다.");
        }
        for (String code : codes) {
            if (code.length() > MAX_NPAY_CD_LEN || !NPAY_CD_PATTERN.matcher(code).matches()) {
                throw new InvalidInputException(
                        "npayCd는 영문/숫자 1~" + MAX_NPAY_CD_LEN + "자여야 합니다 (예: ABZ010001).");
            }
        }
    }

    @GetMapping("/{ykiho}")
    public ApiResponse<HospitalDetailDto> lookupHospital(@PathVariable String ykiho) {
        return ApiResponse.success(detailService.lookupDetail(ykiho));
    }

    /** 가격 카드/표를 위한 fast 응답 — DB only. */
    @GetMapping("/{ykiho}/basics")
    public ApiResponse<HospitalDetailBasicsDto> lookupBasics(@PathVariable String ykiho) {
        return ApiResponse.success(detailService.lookupBasics(ykiho));
    }

    /** 진료과목/장비/교통/주차·운영/특수진료를 위한 slow 응답 — HIRA 5종 캐시. */
    @GetMapping("/{ykiho}/extras")
    public ApiResponse<HospitalDetailExtrasDto> lookupExtras(@PathVariable String ykiho) {
        return ApiResponse.success(detailService.lookupExtras(ykiho));
    }

    private static List<String> parseNpayCds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    /** {@code C002} 매핑 — 컨트롤러 내부 전용. */
    private static final class InvalidInputException extends BusinessException {
        InvalidInputException(String detail) {
            super(ErrorCode.INVALID_INPUT, detail);
        }
    }
}
