package com.khm1102.mediprice.batch.hospital;


import com.khm1102.mediprice.client.HiraHospitalClient;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.HospBasisItem;
import com.khm1102.mediprice.batch.support.SidoCode;
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
 * Step 2 — 병원 동기화 ({@code getHospBasisList1}). Producer-Consumer 패턴.
 * <p>
 * Producer 풀은 17개 시도 × 페이징을 병렬로 호출하여 DTO를 큐에 push.
 * Consumer 풀은 큐에서 batch(BATCH_SIZE)로 take하여 {@link HospitalBatchWriter}를 통해
 * 별도 트랜잭션으로 저장. 단일 거대 트랜잭션 + N² dirty-check 문제를 동시 해결.
 * <p>
 * 종료: 모든 Producer 작업 완료 후 Consumer 수만큼 poison pill을 큐에 push해 graceful shutdown.
 */
@Slf4j
@Service
public class HospitalSyncService {

    private static final int PAGE_SIZE = 100;
    private static final int BATCH_SIZE = 50;
    private static final int QUEUE_CAPACITY = 2000;
    // Producer 2 — HIRA가 동시 4 connection에서 page 1 거부 사례 관측되어 보수적으로 축소.
    // Consumer 4 유지 — DB 쓰기는 여유.
    private static final int PRODUCER_THREADS = 2;
    private static final int CONSUMER_THREADS = 4;
    private static final long PRODUCER_SLEEP_MS = 100;

    /** Consumer 종료용 sentinel. reference equality(==)로 비교하므로 모든 필드 null이어도 무관. */
    private static final HospBasisItem POISON_PILL =
            new HospBasisItem(null, null, null, null, null, null, null, null, null, null, null, null);

    private final HiraHospitalClient client;
    private final HospitalBatchWriter writer;

    public HospitalSyncService(HiraHospitalClient client, HospitalBatchWriter writer) {
        this.client = client;
        this.writer = writer;
    }

    public int sync() {
        long start = System.currentTimeMillis();
        BlockingQueue<HospBasisItem> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        AtomicInteger savedTotal = new AtomicInteger(0);
        AtomicInteger producedTotal = new AtomicInteger(0);

        ExecutorService producerPool = Executors.newFixedThreadPool(
                PRODUCER_THREADS, r -> namedThread(r, "hosp-prod"));
        ExecutorService consumerPool = Executors.newFixedThreadPool(
                CONSUMER_THREADS, r -> namedThread(r, "hosp-cons"));

        // Consumer N개 기동 — POISON_PILL을 만나면 종료.
        for (int i = 0; i < CONSUMER_THREADS; i++) {
            consumerPool.submit(() -> consume(queue, savedTotal));
        }

        // Producer는 시도 17개를 분배해 페이징 호출. submit 후 await로 모든 producer 완료 대기.
        try {
            for (SidoCode sido : SidoCode.all()) {
                producerPool.submit(() -> produce(sido, queue, producedTotal));
            }
            producerPool.shutdown();
            if (!producerPool.awaitTermination(2, TimeUnit.HOURS)) {
                log.warn("Hospital producer pool 2시간 내 미종료 — 강제 종료");
                producerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            producerPool.shutdownNow();
        }

        // 모든 Producer 종료 후 Consumer 수만큼 poison pill 투입.
        try {
            for (int i = 0; i < CONSUMER_THREADS; i++) {
                queue.put(POISON_PILL);
            }
            consumerPool.shutdown();
            if (!consumerPool.awaitTermination(2, TimeUnit.HOURS)) {
                log.warn("Hospital consumer pool 2시간 내 미종료 — 강제 종료");
                consumerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            consumerPool.shutdownNow();
        }

        long elapsedMs = System.currentTimeMillis() - start;
        log.info("HospitalSyncService 완료 — produced={}, savedTotal={}, elapsedMs={}",
                producedTotal.get(), savedTotal.get(), elapsedMs);
        return savedTotal.get();
    }

    /** 시도 1개 Producer — 페이지 순회하면서 DTO를 큐에 push.
     * <p>
     * 첫 페이지 빈 응답은 종료 (totalCount 모름). 중간 페이지 빈 응답은 일시 장애 가능성이 있어
     * skip 후 다음 페이지로 진행 — 첫 페이지에서 받은 totalPages 기준으로 종료. */
    private void produce(SidoCode sido, BlockingQueue<HospBasisItem> queue, AtomicInteger producedTotal) {
        int produced = 0;
        int pageNo = 1;
        int totalPages = -1;
        try {
            while (true) {
                HiraBody<HospBasisItem> body = client.searchHospitals(sido.getCode(), pageNo, PAGE_SIZE);
                List<HospBasisItem> items = body.safeItems();

                if (pageNo == 1) {
                    if (items.isEmpty()) {
                        // 첫 페이지 빈 응답: totalCount 모르므로 종료. quota/매핑 오류 의심.
                        log.warn("Hospital producer 첫 페이지 빈 응답 — sido={} (HIRA quota 초과 또는 sidoCd 매핑 오류 의심)",
                                sido.getName());
                        break;
                    }
                    totalPages = (int) Math.ceil((double) body.getTotalCount() / PAGE_SIZE);
                } else if (items.isEmpty()) {
                    // 중간 페이지 빈 응답: 일시 장애로 가정. skip하고 다음 페이지 진행.
                    log.warn("Hospital producer 중간 페이지 빈 응답 — sido={}, pageNo={}/{} (skip)",
                            sido.getName(), pageNo, totalPages);
                }

                for (HospBasisItem dto : items) {
                    queue.put(dto);
                    produced++;
                }

                if (pageNo >= totalPages) {
                    break;
                }
                pageNo++;
                Thread.sleep(PRODUCER_SLEEP_MS);
            }
            producedTotal.addAndGet(produced);
            log.info("Hospital producer 완료 — sido={}, produced={}", sido.getName(), produced);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Hospital producer 인터럽트 — sido={}", sido.getName());
        } catch (Exception e) {
            log.error("Hospital producer 실패 — sido={}: {}", sido.getName(), e.getMessage(), e);
        }
    }

    /** Consumer — BATCH_SIZE만큼 모이면 writer 호출. POISON_PILL 만나면 잔여 batch flush 후 종료. */
    private void consume(BlockingQueue<HospBasisItem> queue, AtomicInteger savedTotal) {
        List<HospBasisItem> batch = new ArrayList<>(BATCH_SIZE);
        try {
            while (true) {
                HospBasisItem dto = queue.take();
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
            log.error("Hospital consumer 실패: {}", e.getMessage(), e);
        }
    }

    private static Thread namedThread(Runnable r, String prefix) {
        Thread t = new Thread(r, prefix + "-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    }
}
