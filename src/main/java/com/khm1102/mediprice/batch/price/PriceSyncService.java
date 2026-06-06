package com.khm1102.mediprice.batch.price;

import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.repository.HospitalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Step 3 — 가격 동기화. ykiho 워커 풀 병렬 패턴.
 * <p>
 * {@link PriceYkihoSyncService#saveOneYkiho}가 이미 ykiho 단위 {@code @Transactional}로
 * 분리되어 있으므로, 본 오케스트레이터는 ykiho 리스트를 워커 풀에 분배만 하면 된다.
 * <p>
 * HIRA API rate limit 보호: {@link #WORKER_THREADS}만 조정하여 동시 호출 수 제한.
 * 페이지 사이 sleep은 {@link PriceYkihoSyncService}가 자체 처리.
 */
@Slf4j
@Service
public class PriceSyncService {

    // HIRA가 8 worker 동시 호출에서 빈 body 반환하는 사례 관측되어 4로 축소.
    private static final int WORKER_THREADS = 4;
    private static final int PROGRESS_LOG_INTERVAL = 500;

    private final HospitalRepository hospitalRepository;
    private final PriceYkihoSyncService ykihoSyncService;

    public PriceSyncService(HospitalRepository hospitalRepository,
                            PriceYkihoSyncService ykihoSyncService) {
        this.hospitalRepository = hospitalRepository;
        this.ykihoSyncService = ykihoSyncService;
    }

    public int sync() {
        long start = System.currentTimeMillis();
        List<String> ykihoList = hospitalRepository.findAllYkiho();
        log.info("PriceSyncService 시작 — ykiho 수: {}, workers: {}", ykihoList.size(), WORKER_THREADS);

        AtomicInteger savedTotal = new AtomicInteger(0);
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger reporting = new AtomicInteger(0);
        AtomicInteger empty = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(WORKER_THREADS, r -> {
            Thread t = new Thread(r, "price-worker-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });

        try {
            List<CompletableFuture<Void>> futures = ykihoList.stream()
                    .map(ykiho -> CompletableFuture.runAsync(() -> {
                        try {
                            PriceYkihoSyncService.SyncResult result = ykihoSyncService.saveOneYkiho(ykiho);
                            savedTotal.addAndGet(result.saved());
                            if (result.status() == HiraBody.Status.FAILED) {
                                failed.incrementAndGet();
                            } else if (result.saved() > 0) {
                                reporting.incrementAndGet();
                            } else {
                                empty.incrementAndGet();
                            }
                        } catch (Exception e) {
                            log.warn("Price sync 실패 (ykiho={}): {}", ykiho, e.getMessage());
                            failed.incrementAndGet();
                        } finally {
                            int n = processed.incrementAndGet();
                            if (n % PROGRESS_LOG_INTERVAL == 0) {
                                log.info("PriceSyncService 진행 — {}/{}, savedTotal={}, reporting={}, empty={}, failed={}",
                                        n, ykihoList.size(),
                                        savedTotal.get(), reporting.get(), empty.get(), failed.get());
                            }
                        }
                    }, pool))
                    .toList();
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
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

        long elapsedMs = System.currentTimeMillis() - start;
        log.info("PriceSyncService 완료 — savedTotal={}, reporting={}, empty={}, failed={} (총 {} ykiho), elapsedMs={}",
                savedTotal.get(), reporting.get(), empty.get(), failed.get(),
                ykihoList.size(), elapsedMs);
        return savedTotal.get();
    }
}
