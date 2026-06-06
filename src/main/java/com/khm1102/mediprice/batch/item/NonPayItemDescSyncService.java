package com.khm1102.mediprice.batch.item;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.common.HiraBody;
import com.khm1102.mediprice.client.hira.nonpay.NonPayDescItem;
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
 * Step 4 — 비급여 항목 설명 동기화 (구버전 {@code getNonPaymentItemCodeList}).
 * <p>
 * 신버전이 제공하지 않는 일반인용 설명 텍스트(`*Dsc`)를 적재.
 * 페이지 워커 풀 — UNIQUE upsert는 {@link NonPayItemDescBatchWriter}로 위임.
 */
@Slf4j
@Service
public class NonPayItemDescSyncService {

    private static final int PAGE_SIZE = 100;
    private static final int WORKER_THREADS = 1;

    private final HiraNonPayClient client;
    private final NonPayItemDescBatchWriter writer;

    public NonPayItemDescSyncService(HiraNonPayClient client, NonPayItemDescBatchWriter writer) {
        this.client = client;
        this.writer = writer;
    }

    public int sync() {
        long start = System.currentTimeMillis();
        HiraBody<NonPayDescItem> first = client.searchItemDescList(1, PAGE_SIZE);
        List<NonPayDescItem> firstItems = first.safeItems();
        if (firstItems.isEmpty()) {
            log.warn("NonPayItemDescSyncService 첫 페이지 빈 응답 — 구버전 API 응답 없음 의심");
            return 0;
        }

        AtomicInteger saved = new AtomicInteger(writer.saveBatch(firstItems));
        int totalPages = (int) Math.ceil((double) first.getTotalCount() / PAGE_SIZE);
        if (totalPages <= 1) {
            log.info("NonPayItemDescSyncService 완료 — saved={}, totalPages=1, elapsedMs={}",
                    saved.get(), System.currentTimeMillis() - start);
            return saved.get();
        }

        ExecutorService pool = Executors.newFixedThreadPool(WORKER_THREADS, r -> {
            Thread t = new Thread(r, "nonpaydesc-worker-" + System.nanoTime());
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

        log.info("NonPayItemDescSyncService 완료 — saved={}, totalPages={}, elapsedMs={}",
                saved.get(), totalPages, System.currentTimeMillis() - start);
        return saved.get();
    }

    private int savePage(int pageNo) {
        try {
            HiraBody<NonPayDescItem> body = client.searchItemDescList(pageNo, PAGE_SIZE);
            return writer.saveBatch(body.safeItems());
        } catch (Exception e) {
            log.warn("NonPayItemDesc 페이지 적재 실패 — pageNo={}: {}", pageNo, e.getMessage());
            return 0;
        }
    }
}
