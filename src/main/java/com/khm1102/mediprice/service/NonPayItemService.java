package com.khm1102.mediprice.service;

import com.khm1102.mediprice.entity.NonPayItem;
import com.khm1102.mediprice.global.config.CacheConfig;
import com.khm1102.mediprice.repository.NonPayItemRepository;
import com.khm1102.mediprice.dto.NonPayItemGroupDto;

import com.khm1102.mediprice.client.hira.nonpay.NonPayDtlItem;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 비급여 항목 코드 조회.
 * {@code adt_end_dd = '99991231'}만 노출 (현재 유효 항목).
 */
@Service
@Transactional(readOnly = true)
public class NonPayItemService {

    private final NonPayItemRepository repository;

    public NonPayItemService(NonPayItemRepository repository) {
        this.repository = repository;
    }

    /**
     * {@code GET /api/items} — 중분류명 기준 그룹핑.
     * <p>
     * 비급여 항목은 월 1회 배치로만 변경되므로 단일 키({@code 'all'})로 캐시한다.
     * 배치 종료 시 {@link com.khm1102.mediprice.batch.orchestrator.BatchService}가 캐시를 evict한다.
     * 매 호출 findAll(875건) + Java Stream groupingBy 비용을 캐시 hit 시 마이크로초 단위로 줄인다.
     */
    @Cacheable(cacheNames = CacheConfig.NON_PAY_ITEM_GROUPS_CACHE, key = "'all'")
    public List<NonPayItemGroupDto> searchGroupedItems() {
        return repository.findAll().stream()
                .filter(NonPayItemService::isActive)
                .filter(item -> item.getNpayMdivCdNm() != null)
                .collect(Collectors.groupingBy(NonPayItem::getNpayMdivCdNm))
                .entrySet().stream()
                .map(entry -> new NonPayItemGroupDto(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(item -> new NonPayItemGroupDto.Item(item.getNpayCd(), item.getNpayKorNm()))
                                .sorted(Comparator.comparing(NonPayItemGroupDto.Item::npayKorNm))
                                .toList()
                ))
                .sorted(Comparator.comparing(NonPayItemGroupDto::mdivCdNm))
                .toList();
    }

    /** Price → 항목명 매핑용 — HospitalDetailService에서 사용. */
    public Map<String, String> lookupNamesByCodes(Collection<String> codes) {
        return repository.findAllById(codes).stream()
                .collect(Collectors.toMap(NonPayItem::getNpayCd, NonPayItem::getNpayKorNm,
                        (a, b) -> a, java.util.LinkedHashMap::new));
    }

    private static boolean isActive(NonPayItem item) {
        return NonPayDtlItem.ACTIVE_END_DATE.equals(item.getAdtEndDd());
    }
}
