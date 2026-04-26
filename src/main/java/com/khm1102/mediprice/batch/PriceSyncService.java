package com.khm1102.mediprice.batch;

import com.khm1102.mediprice.repository.HospitalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Step 3 — 가격 동기화.
 * <p>
 * Hospital 테이블의 ykiho 전체에 대해 {@link PriceYkihoSyncService#saveOneYkiho}를 호출.
 * 외부 API의 {@code getNonPaymentItemHospList2}(가격 요약)는 page 2부터 timeout 빈발로 폐기 — 우리가 이미
 * Hospital 테이블에 ykiho를 보유하므로 외부 API에서 다시 수집할 필요 없음.
 * <p>
 * ykiho당 별도 트랜잭션({@link PriceYkihoSyncService})이라 1600+ 순회 동안 DB connection을 오래 점유하지 않는다.
 * 진행률은 100 ykiho마다 로그 출력. 완료 시 가격 신고 병원 수 / 빈 응답 병원 수 통계 출력.
 */
@Slf4j
@Service
public class PriceSyncService {

    private static final int PROGRESS_LOG_INTERVAL = 100;

    private final HospitalRepository hospitalRepository;
    private final PriceYkihoSyncService ykihoSyncService;

    public PriceSyncService(HospitalRepository hospitalRepository,
                            PriceYkihoSyncService ykihoSyncService) {
        this.hospitalRepository = hospitalRepository;
        this.ykihoSyncService = ykihoSyncService;
    }

    public int sync() {
        List<String> ykihoList = hospitalRepository.findAllYkiho();
        log.info("PriceSyncService 시작 — ykiho 수: {}", ykihoList.size());

        int savedTotal = 0;
        int processed = 0;
        int reportingHospitals = 0;
        int emptyHospitals = 0;

        for (String ykiho : ykihoList) {
            int saved = ykihoSyncService.saveOneYkiho(ykiho);
            savedTotal += saved;
            if (saved > 0) {
                reportingHospitals++;
            } else {
                emptyHospitals++;
            }
            processed++;
            if (processed % PROGRESS_LOG_INTERVAL == 0) {
                log.info("PriceSyncService 진행 — {}/{}, savedTotal={}, reporting={}, empty={}",
                        processed, ykihoList.size(), savedTotal, reportingHospitals, emptyHospitals);
            }
            sleepBetweenYkiho();
        }

        log.info("PriceSyncService 완료 — savedTotal={}, reporting={}, empty={} (총 {} ykiho)",
                savedTotal, reportingHospitals, emptyHospitals, ykihoList.size());
        return savedTotal;
    }

    /** ykiho 간 sleep — 외부 API rate limit 보호. ykiho 내부 페이지 sleep은 PriceYkihoSyncService에서. */
    private void sleepBetweenYkiho() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
