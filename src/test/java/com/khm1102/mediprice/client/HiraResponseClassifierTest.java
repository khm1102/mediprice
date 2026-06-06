package com.khm1102.mediprice.client;

import com.khm1102.mediprice.client.hira.common.HiraBody;
import com.khm1102.mediprice.client.hira.common.HiraHeader;
import com.khm1102.mediprice.client.hira.common.HiraResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HiraResponseClassifierTest {

    @Test
    void normalHeaderMarksBodyNormalAndPreservesItems() {
        HiraBody<String> source = new HiraBody<>();
        source.setItems(List.of("A", "B"));
        source.setPageNo(9);
        HiraResponse<String> response = new HiraResponse<>(
                new HiraHeader(HiraHeader.NORMAL_CODE, "NORMAL SERVICE."), source);

        HiraBody<String> classified = classify(response, 9);

        assertThat(classified).isSameAs(source);
        assertThat(classified.isNormal()).isTrue();
        assertThat(classified.safeItems()).containsExactly("A", "B");
        assertThat(classified.getPageNo()).isEqualTo(9);
    }

    @Test
    void missingHeaderWithBodyIsTreatedAsNormalForLenientHiraResponses() {
        HiraBody<String> source = new HiraBody<>();
        source.setItems(List.of("A"));

        HiraBody<String> classified = classify(new HiraResponse<>(null, source), 4);

        assertThat(classified).isSameAs(source);
        assertThat(classified.isNormal()).isTrue();
        assertThat(classified.safeItems()).containsExactly("A");
    }

    @Test
    void noDataHeaderWinsEvenWhenBodyIsMissing() {
        HiraResponse<String> response = new HiraResponse<>(
                new HiraHeader(HiraHeader.NODATA_CODE, "NODATA_ERROR"), null);

        HiraBody<String> classified = classify(response, 3);

        assertThat(classified.isNoData()).isTrue();
        assertThat(classified.isFailed()).isFalse();
        assertThat(classified.getPageNo()).isEqualTo(3);
        assertThat(classified.safeItems()).isEmpty();
    }

    @Test
    void trafficLimitHeaderIsFailedEvenWhenBodyExists() {
        HiraBody<String> source = new HiraBody<>();
        source.setItems(List.of("stale-looking-item"));
        HiraResponse<String> response = new HiraResponse<>(
                new HiraHeader(HiraHeader.TRAFFIC_LIMIT_CODE, "LIMITED NUMBER OF SERVICE REQUESTS EXCEEDS ERROR."),
                source);

        HiraBody<String> classified = classify(response, 12);

        assertThat(classified).isNotSameAs(source);
        assertThat(classified.isFailed()).isTrue();
        assertThat(classified.getPageNo()).isEqualTo(12);
        assertThat(classified.safeItems()).isEmpty();
    }

    @Test
    void nullResponseAndNullBodyAreFailedWithPageNumber() {
        HiraBody<String> nullResponse = classify(null, 6);
        HiraBody<String> nullBody = classify(
                new HiraResponse<>(new HiraHeader(HiraHeader.NORMAL_CODE, "NORMAL SERVICE."), null), 7);

        assertThat(nullResponse.isFailed()).isTrue();
        assertThat(nullResponse.getPageNo()).isEqualTo(6);
        assertThat(nullBody.isFailed()).isTrue();
        assertThat(nullBody.getPageNo()).isEqualTo(7);
    }

    private static HiraBody<String> classify(HiraResponse<String> response, int pageNo) {
        return HiraResponseClassifier.classify(
                response,
                "testOperation",
                pageNo,
                "pageNo=" + pageNo,
                LoggerFactory.getLogger(HiraResponseClassifierTest.class));
    }
}
