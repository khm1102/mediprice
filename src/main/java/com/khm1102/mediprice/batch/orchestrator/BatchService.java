package com.khm1102.mediprice.batch.orchestrator;

import com.khm1102.mediprice.batch.hospital.HospitalSyncService;
import com.khm1102.mediprice.batch.item.NonPayItemDescSyncService;
import com.khm1102.mediprice.batch.item.NonPayItemSyncService;
import com.khm1102.mediprice.batch.price.PriceSyncService;
import com.khm1102.mediprice.batch.stat.NonPayItemClcdStatSyncService;
import com.khm1102.mediprice.batch.stat.NonPayItemSidoStatSyncService;
import com.khm1102.mediprice.batch.summary.PriceSummarySyncService;
import com.khm1102.mediprice.global.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 매월 1일 새벽 0시 cron으로 전체 7개 SyncService를 병렬 실행.
 * 수동 트리거는 {@code /api/internal/batch/sync}에서 호출 가능.
 * <p>
 * 비급여 데이터 갱신주기는 월 1회 (hira-docs)이므로 cron도 월 1회로 정렬.
 * <p>
 * 의존성:
 * <ul>
 *   <li>{@link PriceSyncService}는 {@link HospitalSyncService} 완료 후 시작 — ykiho 목록을
 *       hospital 테이블에서 SELECT 하기 때문에 hospital 적재가 끝나야 함.</li>
 *   <li>그 외 5개({@link NonPayItemSyncService}, {@link HospitalSyncService},
 *       {@link NonPayItemDescSyncService}, {@link PriceSummarySyncService},
 *       {@link NonPayItemClcdStatSyncService}, {@link NonPayItemSidoStatSyncService})는
 *       서로 의존성 없음 (em.merge 기반이라 FK strict 검증 없음).</li>
 * </ul>
 * <p>
 * 각 SyncService는 내부에 자체 워커 풀을 갖고 있으므로, {@code hiraBatchExecutor}는
 * BatchService 레벨에서 7개 SyncService 호출을 동시 dispatch하는 용도.
 */
@Slf4j
@Service
public class BatchService {

    private final NonPayItemSyncService nonPayItemSyncService;
    private final HospitalSyncService hospitalSyncService;
    private final PriceSyncService priceSyncService;
    private final NonPayItemDescSyncService nonPayItemDescSyncService;
    private final PriceSummarySyncService priceSummarySyncService;
    private final NonPayItemClcdStatSyncService clcdStatSyncService;
    private final NonPayItemSidoStatSyncService sidoStatSyncService;
    private final Executor batchExecutor;
    private final CacheManager cacheManager;

    public BatchService(NonPayItemSyncService nonPayItemSyncService,
                        HospitalSyncService hospitalSyncService,
                        PriceSyncService priceSyncService,
                        NonPayItemDescSyncService nonPayItemDescSyncService,
                        PriceSummarySyncService priceSummarySyncService,
                        NonPayItemClcdStatSyncService clcdStatSyncService,
                        NonPayItemSidoStatSyncService sidoStatSyncService,
                        @Qualifier("hiraBatchExecutor") Executor batchExecutor,
                        CacheManager cacheManager) {
        this.nonPayItemSyncService = nonPayItemSyncService;
        this.hospitalSyncService = hospitalSyncService;
        this.priceSyncService = priceSyncService;
        this.nonPayItemDescSyncService = nonPayItemDescSyncService;
        this.priceSummarySyncService = priceSummarySyncService;
        this.clcdStatSyncService = clcdStatSyncService;
        this.sidoStatSyncService = sidoStatSyncService;
        this.batchExecutor = batchExecutor;
        this.cacheManager = cacheManager;
    }

    /**
     * 배치 dispatch 종료(또는 부분 실패) 후 호출해 항목 그룹/병원 상세 캐시를 비운다.
     * 배치 직후 stale 데이터가 사용자에게 노출되지 않게 하는 1차 방어선.
     */
    public void evictPostBatchCaches() {
        evictCache(CacheConfig.NON_PAY_ITEM_GROUPS_CACHE);
        evictCache(CacheConfig.HOSPITAL_DETAIL_HIRA_CACHE);
    }

    private void evictCache(String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache != null) {
            cache.clear();
            log.info("캐시 evict — {}", name);
        }
    }

    @Scheduled(cron = "0 0 0 1 * *")
    public void syncAll() {
        long start = System.currentTimeMillis();
        log.info("BatchService.syncAll 시작 — 7개 SyncService 병렬 dispatch");
        try {
            CompletableFuture<Integer> itemsF =
                    CompletableFuture.supplyAsync(nonPayItemSyncService::sync, batchExecutor);
            CompletableFuture<Integer> hospitalsF =
                    CompletableFuture.supplyAsync(hospitalSyncService::sync, batchExecutor);
            // Price만 Hospital ykiho에 의존 — chaining으로 표현
            CompletableFuture<Integer> pricesF =
                    hospitalsF.thenApplyAsync(unused -> priceSyncService.sync(), batchExecutor);
            CompletableFuture<Integer> descF =
                    CompletableFuture.supplyAsync(nonPayItemDescSyncService::sync, batchExecutor);
            CompletableFuture<Integer> summaryF =
                    CompletableFuture.supplyAsync(priceSummarySyncService::sync, batchExecutor);
            CompletableFuture<Integer> clcdStatF =
                    CompletableFuture.supplyAsync(clcdStatSyncService::sync, batchExecutor);
            CompletableFuture<Integer> sidoStatF =
                    CompletableFuture.supplyAsync(sidoStatSyncService::sync, batchExecutor);

            CompletableFuture.allOf(
                    itemsF, hospitalsF, pricesF, descF, summaryF, clcdStatF, sidoStatF
            ).join();

            log.info("BatchService.syncAll 완료 — items={}, hospitals={}, prices={}, " +
                            "desc={}, summary={}, clcdStat={}, sidoStat={}, elapsedMs={}",
                    itemsF.join(), hospitalsF.join(), pricesF.join(),
                    descF.join(), summaryF.join(), clcdStatF.join(), sidoStatF.join(),
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("BatchService.syncAll 전체 실패", e);
        } finally {
            // 부분 실패에서도 stale 캐시를 그대로 두면 사용자에게 옛 가격이 노출된다 → 항상 evict.
            evictPostBatchCaches();
        }
    }
}
