package com.khm1102.mediprice.batch.admin;


import com.khm1102.mediprice.batch.item.NonPayItemDescSyncService;
import com.khm1102.mediprice.batch.orchestrator.BatchService;
import com.khm1102.mediprice.batch.price.PriceSyncService;
import com.khm1102.mediprice.batch.stat.NonPayItemClcdStatSyncService;
import com.khm1102.mediprice.batch.stat.NonPayItemSidoStatSyncService;
import com.khm1102.mediprice.batch.summary.PriceSummarySyncService;
import com.khm1102.mediprice.global.common.ApiResponse;
import com.khm1102.mediprice.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 배치 디버그 트리거 — MVP 검증용 임시 엔드포인트.
 * <p>
 * 모든 트리거는 비동기. 응답은 즉시 떨어지고 진행은 로그로 확인.
 * <p>
 * 보안:
 * <ul>
 *   <li>{@link BatchAdminGuard}가 enabled 플래그 + secret 헤더를 모두 검사. 둘 다 통과해야만
 *       핸들러 본문(B002 idempotency 체크와 async dispatch)에 도달.</li>
 *   <li>비동기 실행은 {@code batchAdminExecutor} 풀에 위임 — 테스트는 direct executor를
 *       주입해 동기 실행으로 만들 수 있다.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/batch")
public class BatchAdminApiController {

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private final BatchAdminGuard guard;
    private final BatchService batchService;
    private final PriceSyncService priceSyncService;
    private final NonPayItemDescSyncService descSyncService;
    private final PriceSummarySyncService summarySyncService;
    private final NonPayItemClcdStatSyncService clcdStatSyncService;
    private final NonPayItemSidoStatSyncService sidoStatSyncService;
    private final Executor adminExecutor;

    public BatchAdminApiController(BatchAdminGuard guard,
                                   BatchService batchService,
                                   PriceSyncService priceSyncService,
                                   NonPayItemDescSyncService descSyncService,
                                   PriceSummarySyncService summarySyncService,
                                   NonPayItemClcdStatSyncService clcdStatSyncService,
                                   NonPayItemSidoStatSyncService sidoStatSyncService,
                                   @Qualifier("batchAdminExecutor") Executor adminExecutor) {
        this.guard = guard;
        this.batchService = batchService;
        this.priceSyncService = priceSyncService;
        this.descSyncService = descSyncService;
        this.summarySyncService = summarySyncService;
        this.clcdStatSyncService = clcdStatSyncService;
        this.sidoStatSyncService = sidoStatSyncService;
        this.adminExecutor = adminExecutor;
    }

    /** 전체 배치 (7-step) — 수십분~수시간. */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> triggerSync(HttpServletRequest request) {
        guard.requirePermission(request);
        return trigger("batch sync", batchService::syncAll);
    }

    /** Price만 (Hospital ykiho 기반) — Hospital이 이미 채워졌을 때 사용. */
    @PostMapping("/sync/prices")
    public ResponseEntity<ApiResponse<String>> triggerPriceSync(HttpServletRequest request) {
        guard.requirePermission(request);
        return trigger("price sync", priceSyncService::sync);
    }

    /** 항목 설명(구버전 API) 단독 — NonPayItemDesc 적재. */
    @PostMapping("/sync/desc")
    public ResponseEntity<ApiResponse<String>> triggerDescSync(HttpServletRequest request) {
        guard.requirePermission(request);
        return trigger("desc sync", descSyncService::sync);
    }

    /** 병원×항목 가격 요약(HospList2) 단독 — PriceSummary 적재. */
    @PostMapping("/sync/summary")
    public ResponseEntity<ApiResponse<String>> triggerSummarySync(HttpServletRequest request) {
        guard.requirePermission(request);
        return trigger("summary sync", summarySyncService::sync);
    }

    /** 종별 통계 단독 — NonPayItemClcdStat 적재. */
    @PostMapping("/sync/clcd-stat")
    public ResponseEntity<ApiResponse<String>> triggerClcdStatSync(HttpServletRequest request) {
        guard.requirePermission(request);
        return trigger("clcd-stat sync", clcdStatSyncService::sync);
    }

    /** 지역별 통계 단독 — NonPayItemSidoStat 적재. */
    @PostMapping("/sync/sido-stat")
    public ResponseEntity<ApiResponse<String>> triggerSidoStatSync(HttpServletRequest request) {
        guard.requirePermission(request);
        return trigger("sido-stat sync", sidoStatSyncService::sync);
    }

    private ResponseEntity<ApiResponse<String>> trigger(String name, Runnable job) {
        if (!RUNNING.compareAndSet(false, true)) {
            log.warn("배치 수동 트리거 차단 — already running (name={})", name);
            return error(ErrorCode.BATCH_ALREADY_RUNNING);
        }
        log.info("{} 수동 트리거 요청", name);
        CompletableFuture.runAsync(() -> {
            try {
                job.run();
            } finally {
                // 단독 트리거(가격/요약/통계 등)도 stale 캐시를 그대로 두면 사용자에게 옛 값이 노출된다.
                try {
                    batchService.evictPostBatchCaches();
                } catch (Exception evictError) {
                    log.warn("배치 후 캐시 evict 실패 (name={}): {}", name, evictError.getMessage());
                }
                RUNNING.set(false);
            }
        }, adminExecutor);
        return ResponseEntity.ok(ApiResponse.success(name + " 트리거됨 (백그라운드 실행, 로그 확인)"));
    }

    private ResponseEntity<ApiResponse<String>> error(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }
}
