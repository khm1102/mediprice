package com.khm1102.mediprice.client.hira.common;

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
        assertThat(body.isNormal()).isTrue();
    }

    /** 신규 noData 팩토리는 NODATA 상태와 빈 items, 페이지 번호를 보존한다. */
    @Test
    void noDataFactoryProducesNoDataStatus() {
        HiraBody<String> body = HiraBody.noData(3);

        assertThat(body.getStatus()).isEqualTo(HiraBody.Status.NODATA);
        assertThat(body.isNoData()).isTrue();
        assertThat(body.isFailed()).isFalse();
        assertThat(body.isNormal()).isFalse();
        assertThat(body.getPageNo()).isEqualTo(3);
        assertThat(body.safeItems()).isEmpty();
    }

    /** 신규 failed 팩토리는 FAILED 상태. 기존 empty()는 backward compat용 alias. */
    @Test
    void failedFactoryProducesFailedStatus() {
        HiraBody<String> failed = HiraBody.failed(5);
        HiraBody<String> empty = HiraBody.empty(5);

        assertThat(failed.getStatus()).isEqualTo(HiraBody.Status.FAILED);
        assertThat(failed.isFailed()).isTrue();
        assertThat(empty.getStatus()).isEqualTo(HiraBody.Status.FAILED);
        assertThat(empty.isFailed()).isTrue();
    }

    /** 새로 만든 빈 body는 NORMAL 기본값을 가져 XML 역직렬화 결과와 호환된다. */
    @Test
    void defaultStatusIsNormal() {
        HiraBody<String> body = new HiraBody<>();
        assertThat(body.getStatus()).isEqualTo(HiraBody.Status.NORMAL);
        assertThat(body.isNormal()).isTrue();
    }
}
