package com.khm1102.mediprice.batch.summary;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.NonPayHospSummaryItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Step 5 — 비급여 가격 요약 동기화 ({@code getNonPaymentItemHospList2}).
 * <p>
 * totalCount ~188,700 — Producer-Consumer로 페이징 호출과 DB 쓰기를 분리.
 * Producer 1개는 단순 페이지 순회 (시도 split 불필요, API가 한 번에 전국 데이터 반환).
 * Consumer 4개는 batch로 모아 {@link PriceSummaryBatchWriter#saveBatch}에 위임.
 * <p>
 * 중간 페이지 응답이 실패/NODATA일 때 종료하지 않고 다음 페이지 시도. failedPages 카운트로 부분 누락 가시화.
 */
@Slf4j
@Service
public class PriceSummarySyncService {

    private static final int PAGE_SIZE = 300;
    private static final int BATCH_SIZE = 100;
    private static final int QUEUE_CAPACITY = 3000;
    private static final int CONSUMER_THREADS = 4;
    private static final long PRODUCER_SLEEP_MS = 100;
    /** 페이지 실패 시 즉시 재시도 횟수. */
    private static final int PAGE_RETRY = 2;

    /** Consumer 종료용 sentinel. reference equality(==)로 비교. */
    private static final NonPayHospSummaryItem POISON_PILL = new NonPayHospSummaryItem(
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null);

    private final HiraNonPayClient client;
    private final PriceSummaryBatchWriter writer;

    public PriceSummarySyncService(HiraNonPayClient client, PriceSummaryBatchWriter writer) {
        this.client = client;
        this.writer = writer;
    }

    public int sync() {
        return syncWithDetail().savedTotal();
    }

    /**
     * sync()의 상세 카운트 버전. 테스트/모니터링에서 failedPages를 확인할 때 사용.
     */
    public SyncSummary syncWithDetail() {
        long start = System.currentTimeMillis();
        BlockingQueue<NonPayHospSummaryItem> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        AtomicInteger savedTotal = new AtomicInteger(0);
        AtomicInteger producedTotal = new AtomicInteger(0);
        AtomicInteger failedPagesTotal = new AtomicInteger(0);

        ExecutorService producerPool = Executors.newSingleThreadExecutor(r -> namedThread(r, "pricesum-prod"));
        ExecutorService consumerPool = Executors.newFixedThreadPool(
                CONSUMER_THREADS, r -> namedThread(r, "pricesum-cons"));

        for (int i = 0; i < CONSUMER_THREADS; i++) {
            consumerPool.submit(() -> consume(queue, savedTotal));
        }

        try {
            producerPool.submit(() -> produce(queue, producedTotal, failedPagesTotal));
            producerPool.shutdown();
            if (!producerPool.awaitTermination(4, TimeUnit.HOURS)) {
                log.warn("PriceSummary producer 4시간 내 미종료 — 강제 종료");
                producerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            producerPool.shutdownNow();
        }

        try {
            for (int i = 0; i < CONSUMER_THREADS; i++) {
                queue.put(POISON_PILL);
            }
            consumerPool.shutdown();
            if (!consumerPool.awaitTermination(4, TimeUnit.HOURS)) {
                log.warn("PriceSummary consumer 4시간 내 미종료 — 강제 종료");
                consumerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            consumerPool.shutdownNow();
        }

        long elapsedMs = System.currentTimeMillis() - start;
        log.info("PriceSummarySyncService 완료 — produced={}, savedTotal={}, failedPages={}, elapsedMs={}",
                producedTotal.get(), savedTotal.get(), failedPagesTotal.get(), elapsedMs);
        return new SyncSummary(savedTotal.get(), producedTotal.get(), failedPagesTotal.get());
    }

    /** sync 결과 요약. failedPages가 0이 아니면 중간 누락이 있었다는 뜻. */
    public record SyncSummary(int savedTotal, int producedTotal, int failedPages) {
    }

    private void produce(BlockingQueue<NonPayHospSummaryItem> queue,
                         AtomicInteger producedTotal,
                         AtomicInteger failedPagesTotal) {
        int produced = 0;
        int failedPages = 0;
        int pageNo = 1;
        int totalPages = -1;
        try {
            while (true) {
                HiraBody<NonPayHospSummaryItem> body = fetchWithRetry(pageNo);

                if (body.isFailed()) {
                    failedPages++;
                    log.warn("PriceSummary 페이지 실패 (pageNo={}, totalPages={})", pageNo, totalPages);
                    if (totalPages < 0) {
                        log.warn("PriceSummary 첫 페이지 실패 — totalCount 미확보로 producer 종료");
                        break;
                    }
                } else if (body.isNoData() || body.safeItems().isEmpty()) {
                    if (pageNo == 1) {
                        log.info("PriceSummary 첫 페이지 NODATA — 정상 종료");
                        break;
                    }
                    failedPages++;
                    log.warn("PriceSummary 중간 페이지 누락 (pageNo={}, totalPages={})", pageNo, totalPages);
                } else {
                    for (NonPayHospSummaryItem dto : body.safeItems()) {
                        queue.put(dto);
                        produced++;
                    }
                    if (totalPages < 0) {
                        totalPages = totalPagesOf(body);
                    }
                }

                if (totalPages > 0 && pageNo >= totalPages) {
                    break;
                }
                pageNo++;
                Thread.sleep(PRODUCER_SLEEP_MS);
            }
            producedTotal.addAndGet(produced);
            failedPagesTotal.addAndGet(failedPages);
            log.info("PriceSummary producer 완료 — produced={}, failedPages={}, totalPages={}",
                    produced, failedPages, totalPages);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("PriceSummary producer 인터럽트");
        } catch (Exception e) {
            log.error("PriceSummary producer 실패: {}", e.getMessage(), e);
        }
    }

    private HiraBody<NonPayHospSummaryItem> fetchWithRetry(int pageNo) throws InterruptedException {
        HiraBody<NonPayHospSummaryItem> body = client.searchHospPriceSummary(pageNo, PAGE_SIZE);
        for (int attempt = 0; attempt < PAGE_RETRY && body.isFailed(); attempt++) {
            Thread.sleep(PRODUCER_SLEEP_MS);
            body = client.searchHospPriceSummary(pageNo, PAGE_SIZE);
        }
        return body;
    }

    private static int totalPagesOf(HiraBody<NonPayHospSummaryItem> body) {
        if (body.getTotalCount() <= 0) {
            return 1;
        }
        return (int) Math.ceil((double) body.getTotalCount() / PAGE_SIZE);
    }

    private void consume(BlockingQueue<NonPayHospSummaryItem> queue, AtomicInteger savedTotal) {
        List<NonPayHospSummaryItem> batch = new ArrayList<>(BATCH_SIZE);
        try {
            while (true) {
                NonPayHospSummaryItem dto = queue.take();
                if (dto == POISON_PILL) {
                    if (!batch.isEmpty()) {
                        savedTotal.addAndGet(writer.saveBatch(batch));
                    }
                    return;
                }
                batch.add(dto);
                if (batch.size() >= BATCH_SIZE) {
                    savedTotal.addAndGet(writer.saveBatch(batch));
                    batch = new ArrayList<>(BATCH_SIZE);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("PriceSummary consumer 실패: {}", e.getMessage(), e);
        }
    }

    private static Thread namedThread(Runnable r, String prefix) {
        Thread t = new Thread(r, prefix + "-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    }
}
