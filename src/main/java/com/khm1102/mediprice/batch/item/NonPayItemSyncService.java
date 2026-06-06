package com.khm1102.mediprice.batch.item;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.common.HiraBody;
import com.khm1102.mediprice.client.hira.nonpay.NonPayCodeItem;
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
 * Step 1 — 비급여 항목 코드 동기화 ({@code getNonPaymentItemCodeList2}).
 * <p>
 * 첫 페이지로 totalCount 확인 후 페이지 2..N을 워커 풀에 분배. 각 워커가
 * {@link NonPayItemBatchWriter#saveBatch}로 위임 — 페이지 단위 REQUIRES_NEW 트랜잭션.
 */
@Slf4j
@Service
public class NonPayItemSyncService {

    private static final int PAGE_SIZE = 300;
    private static final int WORKER_THREADS = 3;

    private final HiraNonPayClient client;
    private final NonPayItemBatchWriter writer;

    public NonPayItemSyncService(HiraNonPayClient client, NonPayItemBatchWriter writer) {
        this.client = client;
        this.writer = writer;
    }

    public int sync() {
        long start = System.currentTimeMillis();
        HiraBody<NonPayCodeItem> first = client.searchItemCodes(1, PAGE_SIZE);
        List<NonPayCodeItem> firstItems = first.safeItems();
        if (firstItems.isEmpty()) {
            log.warn("NonPayItemSyncService 첫 페이지 빈 응답");
            return 0;
        }

        AtomicInteger saved = new AtomicInteger(writer.saveBatch(firstItems));
        int totalPages = (int) Math.ceil((double) first.getTotalCount() / PAGE_SIZE);
        if (totalPages <= 1) {
            log.info("NonPayItemSyncService 완료 — saved={}, totalPages=1, elapsedMs={}",
                    saved.get(), System.currentTimeMillis() - start);
            return saved.get();
        }

        ExecutorService pool = Executors.newFixedThreadPool(WORKER_THREADS, r -> {
            Thread t = new Thread(r, "nonpayitem-worker-" + System.nanoTime());
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
                if (!pool.awaitTermination(2, TimeUnit.HOURS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pool.shutdownNow();
            }
        }

        log.info("NonPayItemSyncService 완료 — saved={}, totalPages={}, elapsedMs={}",
                saved.get(), totalPages, System.currentTimeMillis() - start);
        return saved.get();
    }

    private int savePage(int pageNo) {
        try {
            HiraBody<NonPayCodeItem> body = client.searchItemCodes(pageNo, PAGE_SIZE);
            return writer.saveBatch(body.safeItems());
        } catch (Exception e) {
            log.warn("NonPayItem 페이지 적재 실패 — pageNo={}: {}", pageNo, e.getMessage());
            return 0;
        }
    }
}
