package com.khm1102.mediprice.batch.item;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.NonPayCodeItem;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonPayItemSyncServiceTest {

    @Mock HiraNonPayClient client;
    @Mock NonPayItemBatchWriter writer;

    @InjectMocks NonPayItemSyncService service;

    /** 첫 페이지 + 워커가 처리한 2..N 페이지 모두 합산. */
    @Test
    void aggregatesSavedAcrossAllPages() {
        // totalCount=900, PAGE_SIZE=300 → 3 페이지
        when(client.searchItemCodes(eq(1), anyInt())).thenReturn(body(900, 300));
        when(client.searchItemCodes(eq(2), anyInt())).thenReturn(body(900, 300));
        when(client.searchItemCodes(eq(3), anyInt())).thenReturn(body(900, 300));

        AtomicInteger callCount = new AtomicInteger();
        when(writer.saveBatch(any())).thenAnswer(inv -> {
            callCount.incrementAndGet();
            return ((List<?>) inv.getArgument(0)).size();
        });

        int saved = service.sync();

        assertThat(saved).isEqualTo(900);
        assertThat(callCount.get()).isEqualTo(3);

        verify(client).searchItemCodes(eq(1), anyInt());
        verify(client).searchItemCodes(eq(2), anyInt());
        verify(client).searchItemCodes(eq(3), anyInt());
    }

    /** 첫 페이지 빈 응답 → saved=0, writer 미호출. */
    @Test
    void returnsZeroOnEmptyFirstPage() {
        when(client.searchItemCodes(eq(1), anyInt())).thenReturn(body(0, 0));

        int saved = service.sync();

        assertThat(saved).isZero();
        verify(writer, never()).saveBatch(any());
        verify(client, times(1)).searchItemCodes(anyInt(), anyInt());
    }

    /** 한 페이지로 끝나는 경우 워커 풀 안 띄움. */
    @Test
    void singlePageDoesNotSpawnWorkers() {
        when(client.searchItemCodes(eq(1), anyInt())).thenReturn(body(100, 100));
        when(writer.saveBatch(any())).thenReturn(100);

        int saved = service.sync();

        assertThat(saved).isEqualTo(100);
        verify(client, times(1)).searchItemCodes(anyInt(), anyInt());
        verify(writer, times(1)).saveBatch(any());
    }

    private static HiraBody<NonPayCodeItem> body(int totalCount, int itemCount) {
        HiraBody<NonPayCodeItem> b = new HiraBody<>();
        NonPayCodeItem[] items = new NonPayCodeItem[itemCount];
        for (int i = 0; i < itemCount; i++) {
            items[i] = new NonPayCodeItem("N" + i, "이름", null, null, null, null, null, null);
        }
        b.setItems(List.of(items));
        b.setNumOfRows(itemCount);
        b.setPageNo(1);
        b.setTotalCount(totalCount);
        return b;
    }
}
