package com.khm1102.mediprice.batch.hospital;

import com.khm1102.mediprice.client.hira.HospBasisItem;
import com.khm1102.mediprice.entity.Hospital;
import com.khm1102.mediprice.repository.HospitalRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hospital INSERT/UPDATE 배치 writer — Producer-Consumer의 Consumer 측 트랜잭션 단위.
 * <p>
 * 별도 빈으로 분리한 이유: HospitalSyncService에서 self-call 시 @Transactional 프록시가
 * 우회되므로, Spring AOP가 정상적으로 트랜잭션을 시작하도록 외부 빈 호출이 필요.
 */
@Slf4j
@Service
public class HospitalBatchWriter {

    private final HospitalRepository hospitalRepository;

    @PersistenceContext
    private EntityManager em;

    public HospitalBatchWriter(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    /**
     * 한 batch (50~100건)을 단일 트랜잭션으로 저장. flush/clear로 영속성 컨텍스트를
     * 비워 N² dirty-check 비용을 차단한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveBatch(List<HospBasisItem> batch) {
        int saved = 0;
        for (HospBasisItem dto : batch) {
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
        em.flush();
        em.clear();
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
}
