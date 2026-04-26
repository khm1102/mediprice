package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.service.HospitalService;
import com.khm1102.mediprice.service.HospitalDetailService;
import com.khm1102.mediprice.dto.HospitalSummaryDto;
import com.khm1102.mediprice.dto.HospitalDetailDto;

import com.khm1102.mediprice.global.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalApiController {

    private static final int DEFAULT_RADIUS_METERS = 2000;

    private final HospitalService hospitalService;
    private final HospitalDetailService detailService;

    public HospitalApiController(HospitalService hospitalService, HospitalDetailService detailService) {
        this.hospitalService = hospitalService;
        this.detailService = detailService;
    }

    @GetMapping
    public ApiResponse<List<HospitalSummaryDto>> searchHospitals(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String npayCd,
            @RequestParam(defaultValue = "" + DEFAULT_RADIUS_METERS) int radius) {
        return ApiResponse.success(hospitalService.searchNearby(lat, lng, npayCd, radius));
    }

    @GetMapping("/{ykiho}")
    public ApiResponse<HospitalDetailDto> lookupHospital(@PathVariable String ykiho) {
        return ApiResponse.success(detailService.lookupDetail(ykiho));
    }
}
