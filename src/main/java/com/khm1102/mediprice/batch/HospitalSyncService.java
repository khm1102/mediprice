package com.khm1102.mediprice.batch;


import com.khm1102.mediprice.client.HiraHospitalClient;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.HospBasisItem;
import com.khm1102.mediprice.entity.Hospital;
import com.khm1102.mediprice.repository.HospitalRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Step 2 — 병원 동기화 ({@code getHospBasisList1}).
 * <p>
 * 17개 시도 순회 × 페이징. 본 entity 저장 후 location은 native UPDATE
 * ({@link HospitalRepository#updateLocation})로 별도 채움.
 */
@Slf4j
@Service
public class HospitalSyncService {

    private static final int PAGE_SIZE = 100;

    private final HiraHospitalClient client;
    private final HospitalRepository hospitalRepository;

    @PersistenceContext
    private EntityManager em;

    public HospitalSyncService(HiraHospitalClient client, HospitalRepository hospitalRepository) {
        this.client = client;
        this.hospitalRepository = hospitalRepository;
    }

    @Transactional
    public int sync() {
        int saved = 0;
        for (SidoCode sido : SidoCode.all()) {
            saved += syncSido(sido);
        }
        log.info("HospitalSyncService 완료 — savedTotal={}", saved);
        return saved;
    }

    private int syncSido(SidoCode sido) {
        int saved = 0;
        int pageNo = 1;
        while (true) {
            HiraBody<HospBasisItem> body = client.searchHospitals(sido.getCode(), pageNo, PAGE_SIZE);
            if (body.safeItems().isEmpty()) {
                break;
            }
            for (HospBasisItem dto : body.safeItems()) {
                try {
                    em.merge(toEntity(dto));
                    if (dto.xPos() != null && dto.yPos() != null) {
                        hospitalRepository.updateLocation(dto.ykiho(), dto.xPos(), dto.yPos());
                    }
                    saved++;
                } catch (Exception e) {
                    log.warn("Hospital 저장 실패 (ykiho={}): {}", dto.ykiho(), e.getMessage());
                }
            }
            int totalPages = (int) Math.ceil((double) body.getTotalCount() / PAGE_SIZE);
            if (pageNo >= totalPages) {
                break;
            }
            pageNo++;
            sleepBetweenCalls();
        }
        log.info("HospitalSyncService — sido={}, saved={}", sido.getName(), saved);
        return saved;
    }

    private Hospital toEntity(HospBasisItem dto) {
        return Hospital.builder()
                .ykiho(dto.ykiho())
                .yadmNm(dto.yadmNm())
                .clCd(dto.clCd())
                .clCdNm(dto.clCdNm())
                .addr(dto.addr())
                .xPos(dto.xPos())
                .yPos(dto.yPos())
                .telNo(dto.telno())
                .hospUrl(dto.hospUrl())
                .drTotCnt(dto.drTotCnt())
                .sidoCdNm(dto.sidoCdNm())
                .sgguCdNm(dto.sgguCdNm())
                .build();
    }

    private void sleepBetweenCalls() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
