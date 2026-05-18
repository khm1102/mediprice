package com.khm1102.mediprice.batch.admin;


import com.khm1102.mediprice.batch.item.NonPayItemDescSyncService;
import com.khm1102.mediprice.batch.orchestrator.BatchService;
import com.khm1102.mediprice.batch.price.PriceSyncService;
import com.khm1102.mediprice.batch.stat.NonPayItemClcdStatSyncService;
import com.khm1102.mediprice.batch.stat.NonPayItemSidoStatSyncService;
import com.khm1102.mediprice.batch.summary.PriceSummarySyncService;
import com.khm1102.mediprice.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * 배치 디버그 트리거 — MVP 검증용 임시 엔드포인트.
 * <p>
 * 모든 트리거는 비동기. 응답은 즉시 떨어지고 진행은 로그로 확인.
 * 운영 배포 전 반드시 인증/네트워크 보호 (TODO N1).
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/batch")
public class BatchAdminApiController {

    private final BatchService batchService;
    private final PriceSyncService priceSyncService;
    private final NonPayItemDescSyncService descSyncService;
    private final PriceSummarySyncService summarySyncService;
    private final NonPayItemClcdStatSyncService clcdStatSyncService;
    private final NonPayItemSidoStatSyncService sidoStatSyncService;

    public BatchAdminApiController(BatchService batchService,
                                   PriceSyncService priceSyncService,
                                   NonPayItemDescSyncService descSyncService,
                                   PriceSummarySyncService summarySyncService,
                                   NonPayItemClcdStatSyncService clcdStatSyncService,
                                   NonPayItemSidoStatSyncService sidoStatSyncService) {
        this.batchService = batchService;
        this.priceSyncService = priceSyncService;
        this.descSyncService = descSyncService;
        this.summarySyncService = summarySyncService;
        this.clcdStatSyncService = clcdStatSyncService;
        this.sidoStatSyncService = sidoStatSyncService;
    }

    /** 전체 배치 (7-step) — 수십분~수시간. */
    @PostMapping("/sync")
    public ApiResponse<String> triggerSync() {
        log.info("배치 전체 수동 트리거 요청");
        CompletableFuture.runAsync(batchService::syncAll);
        return ApiResponse.success("batch sync 트리거됨 (백그라운드 실행, 로그 확인)");
    }

    /** Price만 (Hospital ykiho 기반) — Hospital이 이미 채워졌을 때 사용. */
    @PostMapping("/sync/prices")
    public ApiResponse<String> triggerPriceSync() {
        log.info("Price 단독 트리거 요청");
        CompletableFuture.runAsync(priceSyncService::sync);
        return ApiResponse.success("price sync 트리거됨 (백그라운드 실행, 로그 확인)");
    }

    /** 항목 설명(구버전 API) 단독 — NonPayItemDesc 적재. */
    @PostMapping("/sync/desc")
    public ApiResponse<String> triggerDescSync() {
        log.info("NonPayItemDesc 단독 트리거 요청");
        CompletableFuture.runAsync(descSyncService::sync);
        return ApiResponse.success("desc sync 트리거됨 (백그라운드 실행, 로그 확인)");
    }

    /** 병원×항목 가격 요약(HospList2) 단독 — PriceSummary 적재. */
    @PostMapping("/sync/summary")
    public ApiResponse<String> triggerSummarySync() {
        log.info("PriceSummary 단독 트리거 요청");
        CompletableFuture.runAsync(summarySyncService::sync);
        return ApiResponse.success("summary sync 트리거됨 (백그라운드 실행, 로그 확인)");
    }

    /** 종별 통계 단독 — NonPayItemClcdStat 적재. */
    @PostMapping("/sync/clcd-stat")
    public ApiResponse<String> triggerClcdStatSync() {
        log.info("NonPayItemClcdStat 단독 트리거 요청");
        CompletableFuture.runAsync(clcdStatSyncService::sync);
        return ApiResponse.success("clcd-stat sync 트리거됨 (백그라운드 실행, 로그 확인)");
    }

    /** 지역별 통계 단독 — NonPayItemSidoStat 적재. */
    @PostMapping("/sync/sido-stat")
    public ApiResponse<String> triggerSidoStatSync() {
        log.info("NonPayItemSidoStat 단독 트리거 요청");
        CompletableFuture.runAsync(sidoStatSyncService::sync);
        return ApiResponse.success("sido-stat sync 트리거됨 (백그라운드 실행, 로그 확인)");
    }
}
