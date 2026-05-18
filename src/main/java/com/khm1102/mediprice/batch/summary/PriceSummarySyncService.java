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
 */
@Slf4j
@Service
public class PriceSummarySyncService {

    private static final int PAGE_SIZE = 300;
    private static final int BATCH_SIZE = 100;
    private static final int QUEUE_CAPACITY = 3000;
    private static final int CONSUMER_THREADS = 4;
    private static final long PRODUCER_SLEEP_MS = 100;

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
        long start = System.currentTimeMillis();
        BlockingQueue<NonPayHospSummaryItem> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        AtomicInteger savedTotal = new AtomicInteger(0);
        AtomicInteger producedTotal = new AtomicInteger(0);

        ExecutorService producerPool = Executors.newSingleThreadExecutor(r -> namedThread(r, "pricesum-prod"));
        ExecutorService consumerPool = Executors.newFixedThreadPool(
                CONSUMER_THREADS, r -> namedThread(r, "pricesum-cons"));

        for (int i = 0; i < CONSUMER_THREADS; i++) {
            consumerPool.submit(() -> consume(queue, savedTotal));
        }

        try {
            producerPool.submit(() -> produce(queue, producedTotal));
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
        log.info("PriceSummarySyncService 완료 — produced={}, savedTotal={}, elapsedMs={}",
                producedTotal.get(), savedTotal.get(), elapsedMs);
        return savedTotal.get();
    }

    private void produce(BlockingQueue<NonPayHospSummaryItem> queue, AtomicInteger producedTotal) {
        int produced = 0;
        int pageNo = 1;
        try {
            while (true) {
                HiraBody<NonPayHospSummaryItem> body = client.searchHospPriceSummary(pageNo, PAGE_SIZE);
                List<NonPayHospSummaryItem> items = body.safeItems();
                if (items.isEmpty()) {
                    if (pageNo == 1) {
                        log.warn("PriceSummary producer 첫 페이지 빈 응답 — quota/응답 이상 의심");
                    }
                    break;
                }
                for (NonPayHospSummaryItem dto : items) {
                    queue.put(dto);
                    produced++;
                }
                int totalPages = (int) Math.ceil((double) body.getTotalCount() / PAGE_SIZE);
                if (pageNo >= totalPages) {
                    break;
                }
                pageNo++;
                Thread.sleep(PRODUCER_SLEEP_MS);
            }
            producedTotal.addAndGet(produced);
            log.info("PriceSummary producer 완료 — produced={}", produced);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("PriceSummary producer 인터럽트");
        } catch (Exception e) {
            log.error("PriceSummary producer 실패: {}", e.getMessage(), e);
        }
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
