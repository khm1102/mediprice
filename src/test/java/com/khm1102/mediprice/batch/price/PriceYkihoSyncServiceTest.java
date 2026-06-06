package com.khm1102.mediprice.batch.price;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.common.HiraBody;
import com.khm1102.mediprice.client.hira.nonpay.NonPayDtlItem;
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
    @Mock PriceYkihoCleanupService cleanupService;
    @Mock EntityManager em;

    private PriceYkihoSyncService service;

    @BeforeEach
    void setUp() {
        service = new PriceYkihoSyncService(client, cleanupService);
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

    /**
     * 1페이지 NODATA = HIRA가 명시적으로 가격 없음 응답.
     * 기존 활성 가격이 남아 있으면 사용자에게 stale로 노출되므로
     * cleanupService.removeAllActiveByYkiho(REQUIRES_NEW)를 호출해 정리한다.
     */
    @Test
    void firstPageNoDataClearsActivePricesForYkiho() {
        when(client.searchHospPriceDetail("YK1", 1, 100)).thenReturn(HiraBody.noData(1));
        when(cleanupService.removeAllActiveByYkiho("YK1")).thenReturn(3);

        PriceYkihoSyncService.SyncResult result = service.saveOneYkiho("YK1");

        assertThat(result.saved()).isZero();
        assertThat(result.status()).isEqualTo(HiraBody.Status.NORMAL);
        verify(cleanupService).removeAllActiveByYkiho("YK1");
        verify(cleanupService, never())
                .removeStaleByYkiho(anyString(), anyCollection());
    }

    /**
     * NODATA cleanup 실패 시 FAILED를 반환해 재시도 신호를 보존한다.
     * 옛 구현은 swallow + NORMAL이라 stale 가격이 다음 배치까지 살아남았다.
     */
    @Test
    void firstPageNoDataReturnsFailedWhenCleanupFails() {
        when(client.searchHospPriceDetail("YK1", 1, 100)).thenReturn(HiraBody.noData(1));
        when(cleanupService.removeAllActiveByYkiho("YK1"))
                .thenThrow(new RuntimeException("db down"));

        PriceYkihoSyncService.SyncResult result = service.saveOneYkiho("YK1");

        assertThat(result.status()).isEqualTo(HiraBody.Status.FAILED);
        assertThat(result.saved()).isZero();
    }

    /** 1페이지 FAILED는 재시도 후에도 실패면 FAILED 반환. 어떤 정리도 호출되면 안 됨. */
    @Test
    void firstPageFailedAfterRetries() {
        when(client.searchHospPriceDetail(eq("YK1"), eq(1), anyInt()))
                .thenReturn(HiraBody.failed(1));

        PriceYkihoSyncService.SyncResult result = service.saveOneYkiho("YK1");

        assertThat(result.status()).isEqualTo(HiraBody.Status.FAILED);
        assertThat(result.saved()).isZero();
        verify(cleanupService, never())
                .removeStaleByYkiho(anyString(), anyCollection());
        verify(cleanupService, never()).removeAllActiveByYkiho(anyString());
    }

    /** 1페이지 NORMAL + 2페이지 NODATA가 중간에 들어오면 FAILED. 어떤 정리도 호출되면 안 됨. */
    @Test
    void middlePageNoDataIsTreatedAsFailure() {
        when(client.searchHospPriceDetail("YK1", 1, 100))
                .thenReturn(normalBody(1, 150, List.of(item("N001", 10000L), item("N002", 20000L))));
        when(client.searchHospPriceDetail("YK1", 2, 100))
                .thenReturn(HiraBody.noData(2));

        PriceYkihoSyncService.SyncResult result = service.saveOneYkiho("YK1");

        assertThat(result.status()).isEqualTo(HiraBody.Status.FAILED);
        assertThat(result.saved()).isEqualTo(2);
        verify(cleanupService, never())
                .removeStaleByYkiho(anyString(), anyCollection());
        verify(cleanupService, never()).removeAllActiveByYkiho(anyString());
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
        verify(cleanupService).removeStaleByYkiho(eq("YK1"), captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder("N001", "N002", "N003");
    }

    /**
     * stale 정리 실패는 best-effort — 데이터 저장이 정상이면 NORMAL 유지.
     * REQUIRES_NEW로 outer 트랜잭션이 격리되므로 저장된 데이터는 commit된다.
     */
    @Test
    void staleCleanupFailureKeepsNormalStatus() {
        when(client.searchHospPriceDetail("YK1", 1, 100))
                .thenReturn(normalBody(1, 50, List.of(item("N001", 10000L))));
        when(cleanupService.removeStaleByYkiho(eq("YK1"), anyCollection()))
                .thenThrow(new RuntimeException("db down"));

        PriceYkihoSyncService.SyncResult result = service.saveOneYkiho("YK1");

        assertThat(result.status()).isEqualTo(HiraBody.Status.NORMAL);
        assertThat(result.saved()).isEqualTo(1);
        verify(em).merge(any());
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
        verify(cleanupService).removeStaleByYkiho(eq("YK1"), eq(java.util.Set.of("N001")));
    }
}
