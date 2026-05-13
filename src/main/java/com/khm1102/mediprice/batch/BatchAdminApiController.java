package com.khm1102.mediprice.batch;


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

    public BatchAdminApiController(BatchService batchService, PriceSyncService priceSyncService) {
        this.batchService = batchService;
        this.priceSyncService = priceSyncService;
    }

    /** 전체 배치 (NonPayItem + Hospital + Price) — 30~90분. */
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
}
