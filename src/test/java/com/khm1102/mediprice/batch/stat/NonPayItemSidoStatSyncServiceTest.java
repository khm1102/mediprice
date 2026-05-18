package com.khm1102.mediprice.batch.stat;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.NonPaySidoStatItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonPayItemSidoStatSyncServiceTest {

    @Mock HiraNonPayClient client;
    @Mock NonPayItemSidoStatBatchWriter writer;

    @InjectMocks NonPayItemSidoStatSyncService service;

    /** 다중 페이지 — writer 호출 횟수와 saved 합산. */
    @Test
    void aggregatesAcrossMultiplePages() {
        when(client.searchSidoStat(eq(1), anyInt())).thenReturn(body(900, 300));
        when(client.searchSidoStat(eq(2), anyInt())).thenReturn(body(900, 300));
        when(client.searchSidoStat(eq(3), anyInt())).thenReturn(body(900, 300));

        // Writer가 wide→long으로 약 5400개 row 반환 (300 × 평균 18)
        when(writer.saveBatch(any())).thenReturn(5400);

        int saved = service.sync();

        assertThat(saved).isEqualTo(16200);
        verify(client).searchSidoStat(eq(1), anyInt());
        verify(client).searchSidoStat(eq(2), anyInt());
        verify(client).searchSidoStat(eq(3), anyInt());
    }

    /** 빈 첫 페이지. */
    @Test
    void returnsZeroOnEmptyFirstPage() {
        when(client.searchSidoStat(eq(1), anyInt())).thenReturn(body(0, 0));

        int saved = service.sync();

        assertThat(saved).isZero();
        verify(writer, never()).saveBatch(any());
    }

    private static HiraBody<NonPaySidoStatItem> body(int totalCount, int itemCount) {
        HiraBody<NonPaySidoStatItem> b = new HiraBody<>();
        NonPaySidoStatItem[] items = new NonPaySidoStatItem[itemCount];
        for (int i = 0; i < itemCount; i++) {
            items[i] = new NonPaySidoStatItem("N" + i, "이름", "20260517",
                    1L, 1L, 1L, 1L,
                    1L, 1L, 1L, 1L,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null);
        }
        b.setItems(List.of(items));
        b.setNumOfRows(itemCount);
        b.setPageNo(1);
        b.setTotalCount(totalCount);
        return b;
    }
}
