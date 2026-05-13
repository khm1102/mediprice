package com.khm1102.mediprice.client.hira;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HiraBodyTest {

    /** API 실패했을 때 fallback으로 쓰는 빈 body. pageNo는 챙겨야 함. */
    @Test
    void emptyFactoryPreservesPageNoAndReturnsEmptyItems() {
        HiraBody<String> body = HiraBody.empty(7);

        assertThat(body.getPageNo()).isEqualTo(7);
        assertThat(body.safeItems()).isEmpty();
    }

    /** items 비어 있을 때 sync 쪽에서 null 체크 안 하게 빈 리스트로 받아주는지. */
    @Test
    void safeItemsReturnsEmptyListWhenItemsIsNull() {
        HiraBody<String> body = new HiraBody<>();

        assertThat(body.getItems()).isNull();
        assertThat(body.safeItems()).isEmpty();
    }
}
