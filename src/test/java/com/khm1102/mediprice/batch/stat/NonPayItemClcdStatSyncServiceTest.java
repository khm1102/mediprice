package com.khm1102.mediprice.batch.stat;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.NonPayClcdStatItem;
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
class NonPayItemClcdStatSyncServiceTest {

    @Mock HiraNonPayClient client;
    @Mock NonPayItemClcdStatBatchWriter writer;

    @InjectMocks NonPayItemClcdStatSyncService service;

    /** 다중 페이지 — 모든 페이지 통합. writer가 wide→long 변환을 책임. */
    @Test
    void aggregatesAcrossMultiplePages() {
        when(client.searchClcdStat(eq(1), anyInt())).thenReturn(body(900, 300));
        when(client.searchClcdStat(eq(2), anyInt())).thenReturn(body(900, 300));
        when(client.searchClcdStat(eq(3), anyInt())).thenReturn(body(900, 300));

        // Writer가 페이지마다 ~1200개 long row (300 × 4종별) 반환한다고 가정
        when(writer.saveBatch(any())).thenReturn(1200);

        int saved = service.sync();

        assertThat(saved).isEqualTo(3600);
        verify(client).searchClcdStat(eq(1), anyInt());
        verify(client).searchClcdStat(eq(2), anyInt());
        verify(client).searchClcdStat(eq(3), anyInt());
    }

    /** 빈 첫 페이지. */
    @Test
    void returnsZeroOnEmptyFirstPage() {
        when(client.searchClcdStat(eq(1), anyInt())).thenReturn(body(0, 0));

        int saved = service.sync();

        assertThat(saved).isZero();
        verify(writer, never()).saveBatch(any());
    }

    private static HiraBody<NonPayClcdStatItem> body(int totalCount, int itemCount) {
        HiraBody<NonPayClcdStatItem> b = new HiraBody<>();
        NonPayClcdStatItem[] items = new NonPayClcdStatItem[itemCount];
        for (int i = 0; i < itemCount; i++) {
            items[i] = new NonPayClcdStatItem("N" + i, "이름", "20260517",
                    1L, 1L, 1L, 1L,
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
