package com.khm1102.mediprice.client;

import com.khm1102.mediprice.client.hira.common.HiraBody;
import com.khm1102.mediprice.client.hira.common.HiraHeader;
import com.khm1102.mediprice.client.hira.common.HiraResponse;
import org.slf4j.Logger;

/**
 * HIRA XML 공통 응답을 배치가 이해하는 상태로 분류한다.
 */
final class HiraResponseClassifier {

    private HiraResponseClassifier() {
    }

    static <T> HiraBody<T> classify(
            HiraResponse<T> response,
            String operation,
            int pageNo,
            String context,
            Logger log) {
        if (response == null) {
            log.warn("{} response null ({})", operation, context);
            return HiraBody.failed(pageNo);
        }
        HiraHeader header = response.header();
        if (header != null && header.isNoData()) {
            return HiraBody.noData(pageNo);
        }
        if (response.body() == null) {
            log.warn("{} body null ({}, resultCode={})",
                    operation, context, header == null ? "null" : header.resultCode());
            return HiraBody.failed(pageNo);
        }
        if (header == null || header.isSuccess()) {
            response.body().setStatus(HiraBody.Status.NORMAL);
            return response.body();
        }
        log.warn("{} resultCode={} resultMsg={} ({})",
                operation, header.resultCode(), header.resultMsg(), context);
        return HiraBody.failed(pageNo);
    }
}
