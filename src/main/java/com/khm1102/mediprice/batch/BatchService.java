package com.khm1102.mediprice.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 매월 1일 새벽 0시 cron으로 3-step 동기화. 수동 트리거는
 * {@link BatchAdminApiController#triggerSync}에서 호출 가능.
 * <p>
 * 비급여 데이터 갱신주기는 월 1회 (hira-docs)이므로 cron도 월 1회로 정렬.
 */
@Slf4j
@Service
public class BatchService {

    private final NonPayItemSyncService nonPayItemSyncService;
    private final HospitalSyncService hospitalSyncService;
    private final PriceSyncService priceSyncService;

    public BatchService(NonPayItemSyncService nonPayItemSyncService,
                        HospitalSyncService hospitalSyncService,
                        PriceSyncService priceSyncService) {
        this.nonPayItemSyncService = nonPayItemSyncService;
        this.hospitalSyncService = hospitalSyncService;
        this.priceSyncService = priceSyncService;
    }

    @Scheduled(cron = "0 0 0 1 * *")
    public void syncAll() {
        long start = System.currentTimeMillis();
        log.info("BatchService.syncAll 시작");
        try {
            int items = nonPayItemSyncService.sync();
            int hospitals = hospitalSyncService.sync();
            int prices = priceSyncService.sync();
            log.info("BatchService.syncAll 완료 — items={}, hospitals={}, prices={}, elapsedMs={}",
                    items, hospitals, prices, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("BatchService.syncAll 전체 실패", e);
        }
    }
}
