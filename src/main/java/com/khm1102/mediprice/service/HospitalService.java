package com.khm1102.mediprice.service;

import com.khm1102.mediprice.repository.HospitalRepository;
import com.khm1102.mediprice.dto.HospitalSummaryDto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * 병원 검색 — PostGIS 프로시저 {@code search_nearby_hospitals} 호출 후 JSON 파싱.
 * 프로시저는 거리 기준 정렬 + 20건 제한된 JSON 배열 반환.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class HospitalService {

    private static final TypeReference<List<HospitalSummaryDto>> SUMMARY_LIST_TYPE =
            new TypeReference<>() {};

    private final HospitalRepository repository;
    private final JsonMapper jsonMapper;

    public HospitalService(HospitalRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    public List<HospitalSummaryDto> searchNearby(double lat, double lng, String npayCd, int radius) {
        String json = repository.searchNearbyJson(lat, lng, npayCd, radius);
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return jsonMapper.readValue(json, SUMMARY_LIST_TYPE);
        } catch (Exception e) {
            log.warn("search_nearby_hospitals 결과 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }
}
