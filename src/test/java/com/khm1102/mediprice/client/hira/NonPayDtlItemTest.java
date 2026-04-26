package com.khm1102.mediprice.client.hira;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NonPayDtlItemTest {

    /** 심평원 무기한 마커 99991231이면 현재 유효 가격. 배치는 이것만 저장. */
    @Test
    void isActiveTrueWhenEndDateIs99991231() {
        NonPayDtlItem item = new NonPayDtlItem("YKH", "N001", "박피", 50_000L, "20240101", "99991231");

        assertThat(item.isActive()).isTrue();
    }

    /** 종료일 지난 항목은 만료. */
    @Test
    void isActiveFalseWhenEndDateIsPast() {
        NonPayDtlItem item = new NonPayDtlItem("YKH", "N001", "박피", 50_000L, "20200101", "20231231");

        assertThat(item.isActive()).isFalse();
    }

    /** 필드 빠진 응답 와도 NPE 안 터지고 false. */
    @Test
    void isActiveFalseWhenEndDateIsNull() {
        NonPayDtlItem item = new NonPayDtlItem("YKH", "N001", "박피", 50_000L, "20240101", null);

        assertThat(item.isActive()).isFalse();
    }
}
