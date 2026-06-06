package com.khm1102.mediprice.service;

import com.khm1102.mediprice.client.HiraDetailClient;
import com.khm1102.mediprice.client.HiraDetailClient.HospitalDetailBundle;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 병원 상세의 외부 HIRA 5종 API 결과만 캐시하는 컴포넌트.
 * <p>
 * 옛 구현은 {@link HospitalDetailService#lookupDetail}에 {@code @Cacheable}을 걸어
 * DB 가격까지 함께 캐시했다. TTL 없는 {@code ConcurrentMapCacheManager}에서는 배치 후 가격이 변해도
 * 서버 재시작 전까지 stale 가격이 그대로 노출됐다.
 * 이제 본 컴포넌트가 HIRA 외부 호출만 캐시하고, DB 가격 조회는 매 호출 새로 수행한다.
 */
@Component
public class HospitalDetailHiraCache {

    private final HiraDetailClient detailClient;

    public HospitalDetailHiraCache(HiraDetailClient detailClient) {
        this.detailClient = detailClient;
    }

    /**
     * ykiho 단위로 HIRA 5종 API 병합 결과를 캐시한다.
     * Spring 캐시 프록시는 같은 빈 내부 호출에서는 동작하지 않으므로 이 메서드는 {@link HospitalDetailService}가
     * 외부 호출로만 사용한다.
     */
    @Cacheable(cacheNames = "hospitalDetailHiraCache", key = "#ykiho")
    public HospitalDetailBundle lookupBundle(String ykiho) {
        return detailClient.fetchAll(ykiho);
    }
}
