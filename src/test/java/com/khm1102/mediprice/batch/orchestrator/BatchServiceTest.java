package com.khm1102.mediprice.batch.orchestrator;

import com.khm1102.mediprice.batch.hospital.HospitalSyncService;
import com.khm1102.mediprice.batch.item.NonPayItemDescSyncService;
import com.khm1102.mediprice.batch.item.NonPayItemSyncService;
import com.khm1102.mediprice.batch.price.PriceSyncService;
import com.khm1102.mediprice.batch.stat.NonPayItemClcdStatSyncService;
import com.khm1102.mediprice.batch.stat.NonPayItemSidoStatSyncService;
import com.khm1102.mediprice.batch.summary.PriceSummarySyncService;
import com.khm1102.mediprice.global.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 전체 배치 오케스트레이션 — 7개 SyncService 동시 dispatch + Price만 Hospital chaining 검증.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchServiceTest {

    @Mock NonPayItemSyncService nonPayItemSyncService;
    @Mock HospitalSyncService hospitalSyncService;
    @Mock PriceSyncService priceSyncService;
    @Mock NonPayItemDescSyncService descSyncService;
    @Mock PriceSummarySyncService summarySyncService;
    @Mock NonPayItemClcdStatSyncService clcdStatSyncService;
    @Mock NonPayItemSidoStatSyncService sidoStatSyncService;
    @Mock CacheManager cacheManager;
    @Mock Cache nonPayItemGroupsCache;
    @Mock Cache hospitalDetailHiraCache;

    @InjectMocks BatchService batchService;

    /** 7개 SyncService 모두 정확히 1번씩 호출. */
    @Test
    void invokesAllSevenStepsExactlyOnce() {
        Executor pool = Executors.newFixedThreadPool(8);
        injectExecutor(batchService, pool);

        when(nonPayItemSyncService.sync()).thenReturn(100);
        when(hospitalSyncService.sync()).thenReturn(200);
        when(priceSyncService.sync()).thenReturn(300);
        when(descSyncService.sync()).thenReturn(50);
        when(summarySyncService.sync()).thenReturn(1000);
        when(clcdStatSyncService.sync()).thenReturn(60);
        when(sidoStatSyncService.sync()).thenReturn(70);

        batchService.syncAll();

        verify(nonPayItemSyncService, times(1)).sync();
        verify(hospitalSyncService, times(1)).sync();
        verify(priceSyncService, times(1)).sync();
        verify(descSyncService, times(1)).sync();
        verify(summarySyncService, times(1)).sync();
        verify(clcdStatSyncService, times(1)).sync();
        verify(sidoStatSyncService, times(1)).sync();
    }

    /** Price는 Hospital 완료 후에만 시작. */
    @Test
    void priceStartsOnlyAfterHospitalCompletes() {
        Executor pool = Executors.newFixedThreadPool(8);
        injectExecutor(batchService, pool);

        AtomicBoolean hospitalDone = new AtomicBoolean(false);
        AtomicBoolean priceObservedHospitalDone = new AtomicBoolean(false);
        CountDownLatch hospitalCalled = new CountDownLatch(1);

        when(hospitalSyncService.sync()).thenAnswer(inv -> {
            hospitalCalled.countDown();
            Thread.sleep(150);  // Hospital이 좀 걸리도록
            hospitalDone.set(true);
            return 200;
        });
        when(priceSyncService.sync()).thenAnswer(inv -> {
            // Price 시작 시점에 Hospital이 끝나 있어야 한다
            priceObservedHospitalDone.set(hospitalDone.get());
            return 300;
        });
        when(nonPayItemSyncService.sync()).thenReturn(100);
        when(descSyncService.sync()).thenReturn(50);
        when(summarySyncService.sync()).thenReturn(1000);
        when(clcdStatSyncService.sync()).thenReturn(60);
        when(sidoStatSyncService.sync()).thenReturn(70);

        batchService.syncAll();

        assertThat(priceObservedHospitalDone.get()).isTrue();
        verify(priceSyncService).sync();
    }

    /** Hospital과 의존성 없는 5개는 Hospital 진행 중에 동시 시작 — 진짜 병렬. (Price는 Hospital 의존이라 제외) */
    @Test
    void independentStepsRunInParallelWithHospital() throws InterruptedException {
        Executor pool = Executors.newFixedThreadPool(8);
        injectExecutor(batchService, pool);

        // 5개 (NonPayItem, Desc, Summary, Clcd, Sido) — Hospital 진행 중 시작되어야 함
        CountDownLatch independentStarted = new CountDownLatch(5);
        AtomicBoolean allFiveStartedDuringHospital = new AtomicBoolean(false);

        when(hospitalSyncService.sync()).thenAnswer(inv -> {
            // 5개가 200ms 안에 시작했는지 확인
            allFiveStartedDuringHospital.set(independentStarted.await(500, TimeUnit.MILLISECONDS));
            return 200;
        });

        Runnable countDown = independentStarted::countDown;
        when(nonPayItemSyncService.sync()).thenAnswer(inv -> { countDown.run(); return 100; });
        when(descSyncService.sync()).thenAnswer(inv -> { countDown.run(); return 50; });
        when(summarySyncService.sync()).thenAnswer(inv -> { countDown.run(); return 1000; });
        when(clcdStatSyncService.sync()).thenAnswer(inv -> { countDown.run(); return 60; });
        when(sidoStatSyncService.sync()).thenAnswer(inv -> { countDown.run(); return 70; });
        when(priceSyncService.sync()).thenReturn(300);  // chained — 측정 대상 아님

        batchService.syncAll();

        // Hospital이 끝나기 전에 5개가 모두 시작 → 진짜 병렬 dispatch 증명
        assertThat(allFiveStartedDuringHospital.get()).isTrue();
    }

    /** 한 step 예외 발생해도 다른 step은 정상 호출 (병렬 실행). */
    @Test
    void exceptionInOneStepDoesNotBlockOthers() {
        Executor pool = Executors.newFixedThreadPool(8);
        injectExecutor(batchService, pool);

        when(nonPayItemSyncService.sync()).thenThrow(new RuntimeException("simulated"));
        when(hospitalSyncService.sync()).thenReturn(200);
        when(priceSyncService.sync()).thenReturn(300);
        when(descSyncService.sync()).thenReturn(50);
        when(summarySyncService.sync()).thenReturn(1000);
        when(clcdStatSyncService.sync()).thenReturn(60);
        when(sidoStatSyncService.sync()).thenReturn(70);

        batchService.syncAll();

        // NonPayItem 예외에도 다른 6개는 모두 호출됨 — CompletableFuture는 독립
        verify(hospitalSyncService).sync();
        verify(priceSyncService).sync();
        verify(descSyncService).sync();
        verify(summarySyncService).sync();
        verify(clcdStatSyncService).sync();
        verify(sidoStatSyncService).sync();
    }

    @Test
    void syncAllEvictsUserFacingCachesOnSuccess() {
        Executor pool = Runnable::run;
        injectExecutor(batchService, pool);
        stubCacheLookup();
        when(nonPayItemSyncService.sync()).thenReturn(100);
        when(hospitalSyncService.sync()).thenReturn(200);
        when(priceSyncService.sync()).thenReturn(300);
        when(descSyncService.sync()).thenReturn(50);
        when(summarySyncService.sync()).thenReturn(1000);
        when(clcdStatSyncService.sync()).thenReturn(60);
        when(sidoStatSyncService.sync()).thenReturn(70);

        batchService.syncAll();

        verify(nonPayItemGroupsCache).clear();
        verify(hospitalDetailHiraCache).clear();
    }

    @Test
    void syncAllEvictsUserFacingCachesEvenWhenAStepThrows() {
        Executor pool = Runnable::run;
        injectExecutor(batchService, pool);
        stubCacheLookup();
        when(nonPayItemSyncService.sync()).thenThrow(new RuntimeException("quota exceeded"));
        when(hospitalSyncService.sync()).thenReturn(200);
        when(priceSyncService.sync()).thenReturn(300);
        when(descSyncService.sync()).thenReturn(50);
        when(summarySyncService.sync()).thenReturn(1000);
        when(clcdStatSyncService.sync()).thenReturn(60);
        when(sidoStatSyncService.sync()).thenReturn(70);

        batchService.syncAll();

        verify(nonPayItemGroupsCache).clear();
        verify(hospitalDetailHiraCache).clear();
    }

    private static void injectExecutor(BatchService service, Executor executor) {
        try {
            var field = BatchService.class.getDeclaredField("batchExecutor");
            field.setAccessible(true);
            field.set(service, executor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void stubCacheLookup() {
        when(cacheManager.getCache(CacheConfig.NON_PAY_ITEM_GROUPS_CACHE)).thenReturn(nonPayItemGroupsCache);
        when(cacheManager.getCache(CacheConfig.HOSPITAL_DETAIL_HIRA_CACHE)).thenReturn(hospitalDetailHiraCache);
    }
}
