package com.khm1102.mediprice.dto;

public record AssistantHospitalSearchRequest(
        String query,
        double lat,
        double lng,
        Integer radius,
        String sort,
        Integer limit
) {
}
