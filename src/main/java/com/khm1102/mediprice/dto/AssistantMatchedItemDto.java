package com.khm1102.mediprice.dto;

public record AssistantMatchedItemDto(
        String npayCd,
        String npayKorNm,
        String npayMdivCdNm,
        String npaySdivCdNm,
        Double score
) {
}
