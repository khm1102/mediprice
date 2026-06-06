package com.khm1102.mediprice.batch.item;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.common.HiraBody;
import com.khm1102.mediprice.client.hira.nonpay.NonPayDescItem;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonPayItemDescSyncServiceTest {

    @Mock HiraNonPayClient client;
    @Mock NonPayItemDescBatchWriter writer;

    @InjectMocks NonPayItemDescSyncService service;

    /** 54건 (단일 페이지) 정상 적재. */
    @Test
    void aggregatesSavedFromSinglePage() {
        when(client.searchItemDescList(eq(1), anyInt())).thenReturn(body(54, 54));
        when(writer.saveBatch(any())).thenReturn(54);

        int saved = service.sync();

        assertThat(saved).isEqualTo(54);
        verify(client, times(1)).searchItemDescList(anyInt(), anyInt());
        verify(writer).saveBatch(any());
    }

    /** 다중 페이지: 첫 페이지 + 워커 페이지 합산. */
    @Test
    void aggregatesAcrossMultiplePages() {
        // totalCount=150, PAGE_SIZE=100 → 2 페이지
        when(client.searchItemDescList(eq(1), anyInt())).thenReturn(body(150, 100));
        when(client.searchItemDescList(eq(2), anyInt())).thenReturn(body(150, 50));

        when(writer.saveBatch(any())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());

        int saved = service.sync();

        assertThat(saved).isEqualTo(150);
        verify(client).searchItemDescList(eq(1), anyInt());
        verify(client).searchItemDescList(eq(2), anyInt());
    }

    /** 첫 페이지 빈 응답 → 0 반환. */
    @Test
    void returnsZeroOnEmptyFirstPage() {
        when(client.searchItemDescList(eq(1), anyInt())).thenReturn(body(0, 0));

        int saved = service.sync();

        assertThat(saved).isZero();
        verify(writer, never()).saveBatch(any());
    }

    private static HiraBody<NonPayDescItem> body(int totalCount, int itemCount) {
        HiraBody<NonPayDescItem> b = new HiraBody<>();
        NonPayDescItem[] items = new NonPayDescItem[itemCount];
        for (int i = 0; i < itemCount; i++) {
            items[i] = new NonPayDescItem(
                    "A", "상급병실료", "설명", "A" + i, "1인실", "설명2", null, null, null);
        }
        b.setItems(List.of(items));
        b.setNumOfRows(itemCount);
        b.setPageNo(1);
        b.setTotalCount(totalCount);
        return b;
    }
}
