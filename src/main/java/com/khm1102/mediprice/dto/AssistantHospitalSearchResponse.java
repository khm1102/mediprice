package com.khm1102.mediprice.dto;

import java.util.List;

public record AssistantHospitalSearchResponse(
        String query,
        String interpretedSort,
        List<AssistantMatchedItemDto> matchedItems,
        String message,
        List<HospitalSummaryDto> hospitals
) {
}
