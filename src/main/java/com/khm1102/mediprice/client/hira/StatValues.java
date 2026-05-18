package com.khm1102.mediprice.client.hira;

/**
 * 비급여 통계 1세트 — 평균/중간/최저/최고가. 모든 필드 nullable (API가 일부 누락 가능).
 * <p>
 * {@link NonPayClcdStatItem#asStatByClcd()}, {@link NonPaySidoStatItem#asStatBySido()}의 반환 타입.
 */
public record StatValues(Long avg, Long mid, Long min, Long max) {

    /** 4개 필드 모두 null이면 의미 없는 통계 — entry 자체를 생략하는 헬퍼. */
    public boolean isAllNull() {
        return avg == null && mid == null && min == null && max == null;
    }
}
