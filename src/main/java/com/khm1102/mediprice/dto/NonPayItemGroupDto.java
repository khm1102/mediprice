package com.khm1102.mediprice.dto;

import java.util.List;

/**
 * {@code GET /api/items} 응답 — 중분류({@code npayMdivCdNm}) 기준 그룹.
 */
public record NonPayItemGroupDto(String mdivCdNm, List<Item> items) {

    public record Item(String npayCd, String npayKorNm) {
    }
}
