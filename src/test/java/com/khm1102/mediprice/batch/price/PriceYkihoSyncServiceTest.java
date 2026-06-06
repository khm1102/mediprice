package com.khm1102.mediprice.batch.price;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.NonPayDtlItem;
import com.khm1102.mediprice.repository.PriceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceYkihoSyncServiceTest {

    @Mock HiraNonPayClient client;
    @Mock PriceRepository priceRepository;
    @Mock EntityManager em;

    private PriceYkihoSyncService service;

    @BeforeEach
    void setUp() {
        service = new PriceYkihoSyncService(client, priceRepository);
        ReflectionTestUtils.setField(service, "em", em);
    }

    private static HiraBody<NonPayDtlItem> normalBody(int pageNo, int totalCount, List<NonPayDtlItem> items) {
        HiraBody<NonPayDtlItem> body = new HiraBody<>();
        body.setItems(items);
        body.setPageNo(pageNo);
        body.setTotalCount(totalCount);
        body.setStatus(HiraBody.Status.NORMAL);
        return body;
    }

    private static NonPayDtlItem item(String npayCd, long curAmt) {
        return new NonPayDtlItem("YK1", npayCd, npayCd + "-name", curAmt, "20240101", "99991231");
    }

    /** 1페이지 NODATA면 정상 종료 (saved=0, NORMAL). stale 정리는 호출되지 않는다. */
    @Test
    void firstPageNoDataIsNormalTermination() {
        when(client.searchHospPriceDetail("YK1", 1, 100)).thenReturn(HiraBody.noData(1));

        PriceYkihoSyncService.SyncResult result = service.saveOneYkiho("YK1");

        assertThat(result.saved()).isZero();
        assertThat(result.status()).isEqualTo(HiraBody.Status.NORMAL);
        verify(priceRepository, never())
                .removeStaleByYkiho(anyString(), anyCollection());
    }

    /** 1페이지 FAILED는 재시도 후에도 실패면 FAILED 반환. stale 정리 호출 안 됨. */
    @Test
    void firstPageFailedAfterRetries() {
        when(client.searchHospPriceDetail(eq("YK1"), eq(1), anyInt()))
                .thenReturn(HiraBody.failed(1));

        PriceYkihoSyncService.SyncResult result = service.saveOneYkiho("YK1");

        assertThat(result.status()).isEqualTo(HiraBody.Status.FAILED);
        assertThat(result.saved()).isZero();
        verify(priceRepository, never())
                .removeStaleByYkiho(anyString(), anyCollection());
    }

    /** 1페이지 NORMAL + 2페이지 NODATA가 중간에 들어오면 FAILED. */
    @Test
    void middlePageNoDataIsTreatedAsFailure() {
        when(client.searchHospPriceDetail("YK1", 1, 100))
                .thenReturn(normalBody(1, 150, List.of(item("N001", 10000L), item("N002", 20000L))));
        when(client.searchHospPriceDetail("YK1", 2, 100))
                .thenReturn(HiraBody.noData(2));

        PriceYkihoSyncService.SyncResult result = service.saveOneYkiho("YK1");

        assertThat(result.status()).isEqualTo(HiraBody.Status.FAILED);
        assertThat(result.saved()).isEqualTo(2);
        verify(priceRepository, never())
                .removeStaleByYkiho(anyString(), anyCollection());
    }

    /** 모든 페이지 정상이면 NORMAL + stale 정리 호출. activeCodes에 본 npayCd 모두 포함. */
    @Test
    void normalCompletionTriggersStaleRemoval() {
        when(client.searchHospPriceDetail("YK1", 1, 100))
                .thenReturn(normalBody(1, 150, List.of(item("N001", 10000L), item("N002", 20000L))));
        when(client.searchHospPriceDetail("YK1", 2, 100))
                .thenReturn(normalBody(2, 150, List.of(item("N003", 30000L))));

        PriceYkihoSyncService.SyncResult result = service.saveOneYkiho("YK1");

        assertThat(result.status()).isEqualTo(HiraBody.Status.NORMAL);
        assertThat(result.saved()).isEqualTo(3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(priceRepository).removeStaleByYkiho(eq("YK1"), captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder("N001", "N002", "N003");
    }

    /** isActive=false 항목은 saved 카운트되지 않고 활성 집합에도 들어가지 않는다. */
    @Test
    void inactiveItemsAreSkipped() {
        NonPayDtlItem expired = new NonPayDtlItem("YK1", "N999", "name", 1L, "20200101", "20231231");
        when(client.searchHospPriceDetail("YK1", 1, 100))
                .thenReturn(normalBody(1, 50, List.of(item("N001", 10000L), expired)));

        PriceYkihoSyncService.SyncResult result = service.saveOneYkiho("YK1");

        assertThat(result.status()).isEqualTo(HiraBody.Status.NORMAL);
        assertThat(result.saved()).isEqualTo(1);
        verify(em).merge(any());
        verify(priceRepository).removeStaleByYkiho(eq("YK1"), eq(java.util.Set.of("N001")));
    }
}
