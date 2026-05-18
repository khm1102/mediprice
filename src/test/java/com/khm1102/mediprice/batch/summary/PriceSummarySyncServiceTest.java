package com.khm1102.mediprice.batch.summary;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.NonPayHospSummaryItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PriceSummarySyncServiceTest {

    @Mock HiraNonPayClient client;
    @Mock PriceSummaryBatchWriter writer;

    @InjectMocks PriceSummarySyncService service;

    /** Producer-Consumer 전체 흐름: 페이징 종료 + writer 호출 + 카운트 누적 검증. */
    @Test
    void producesAndConsumesAcrossPages() {
        // 2페이지: 1페이지 5건, 2페이지 3건, totalCount=8, PAGE_SIZE=300 → 1페이지에 다 들어감
        // 실제는 PAGE_SIZE=300이므로 totalCount=600으로 두 페이지 만들기
        AtomicInteger callCount = new AtomicInteger(0);
        when(client.searchHospPriceSummary(anyInt(), anyInt())).thenAnswer(inv -> {
            int page = inv.getArgument(0);
            callCount.incrementAndGet();
            if (page == 1) {
                return body(600, sampleBatch(300));
            } else if (page == 2) {
                return body(600, sampleBatch(300));
            }
            return body(600);
        });

        // writer는 들어온 배치 size만큼 saved 반환
        when(writer.saveBatch(any())).thenAnswer(inv -> {
            List<NonPayHospSummaryItem> b = inv.getArgument(0);
            return b.size();
        });

        int saved = service.sync();

        assertThat(saved).isEqualTo(600);
        // writer.saveBatch가 여러 번 호출되었음 (batch 100 단위)
        verify(writer, atLeastOnce()).saveBatch(any());
        // 페이지 호출 2번
        assertThat(callCount.get()).isEqualTo(2);
    }

    /** 첫 페이지 빈 응답 — saved=0, writer 미호출. */
    @Test
    void returnsZeroOnEmptyFirstPage() {
        when(client.searchHospPriceSummary(anyInt(), anyInt())).thenReturn(body(0));

        int saved = service.sync();

        assertThat(saved).isZero();
    }

    private static NonPayHospSummaryItem[] sampleBatch(int n) {
        NonPayHospSummaryItem[] arr = new NonPayHospSummaryItem[n];
        for (int i = 0; i < n; i++) {
            arr[i] = new NonPayHospSummaryItem(
                    "YK" + i, "병원" + i, "01", "상급종합",
                    "110000", "서울", "110016", "종로구",
                    "ABZ010001", "상급병실료/1인실",
                    "1010A", "상급병실료", "1010A010", "1인실", "1010A010", "1인실",
                    100_000L, 200_000L, "20260101", "99991231", "http://url");
        }
        return arr;
    }

    private static HiraBody<NonPayHospSummaryItem> body(int totalCount, NonPayHospSummaryItem... items) {
        HiraBody<NonPayHospSummaryItem> b = new HiraBody<>();
        b.setItems(List.of(items));
        b.setNumOfRows(items.length);
        b.setPageNo(1);
        b.setTotalCount(totalCount);
        return b;
    }
}
