package com.khm1102.mediprice.batch.stat;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.NonPayClcdStatItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Step 6 — 의료기관 종별 가격 통계 동기화 ({@code getNonPaymentItemClcdList}).
 * <p>
 * 페이지 워커 풀 — wide→long 변환은 {@link NonPayItemClcdStatBatchWriter}로 위임.
 */
@Slf4j
@Service
public class NonPayItemClcdStatSyncService {

    private static final int PAGE_SIZE = 300;
    private static final int WORKER_THREADS = 3;

    private final HiraNonPayClient client;
    private final NonPayItemClcdStatBatchWriter writer;

    public NonPayItemClcdStatSyncService(HiraNonPayClient client,
                                          NonPayItemClcdStatBatchWriter writer) {
        this.client = client;
        this.writer = writer;
    }

    public int sync() {
        long start = System.currentTimeMillis();
        HiraBody<NonPayClcdStatItem> first = client.searchClcdStat(1, PAGE_SIZE);
        List<NonPayClcdStatItem> firstItems = first.safeItems();
        if (firstItems.isEmpty()) {
            log.warn("NonPayItemClcdStatSyncService 첫 페이지 빈 응답");
            return 0;
        }

        AtomicInteger saved = new AtomicInteger(writer.saveBatch(firstItems));
        int totalPages = (int) Math.ceil((double) first.getTotalCount() / PAGE_SIZE);
        if (totalPages <= 1) {
            log.info("NonPayItemClcdStatSyncService 완료 — saved={}, totalPages=1, elapsedMs={}",
                    saved.get(), System.currentTimeMillis() - start);
            return saved.get();
        }

        ExecutorService pool = Executors.newFixedThreadPool(WORKER_THREADS, r -> {
            Thread t = new Thread(r, "clcdstat-worker-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        try {
            List<CompletableFuture<Integer>> futures = IntStream.rangeClosed(2, totalPages)
                    .mapToObj(page -> CompletableFuture.supplyAsync(() -> savePage(page), pool))
                    .toList();
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            for (CompletableFuture<Integer> f : futures) {
                saved.addAndGet(f.join());
            }
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(1, TimeUnit.HOURS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pool.shutdownNow();
            }
        }

        log.info("NonPayItemClcdStatSyncService 완료 — saved={}, totalPages={}, elapsedMs={}",
                saved.get(), totalPages, System.currentTimeMillis() - start);
        return saved.get();
    }

    private int savePage(int pageNo) {
        try {
            HiraBody<NonPayClcdStatItem> body = client.searchClcdStat(pageNo, PAGE_SIZE);
            return writer.saveBatch(body.safeItems());
        } catch (Exception e) {
            log.warn("ClcdStat 페이지 적재 실패 — pageNo={}: {}", pageNo, e.getMessage());
            return 0;
        }
    }
}
