package com.khm1102.mediprice.global.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    /** 비급여 항목 그룹핑 캐시 ({@code /api/items}). 배치 종료 시 evict. */
    public static final String NON_PAY_ITEM_GROUPS_CACHE = "nonPayItemGroupsCache";
    /** 병원 상세 외부 HIRA 5종 API 응답 캐시. */
    public static final String HOSPITAL_DETAIL_HIRA_CACHE = "hospitalDetailHiraCache";

    // ConcurrentMapCacheManager는 TTL을 지원하지 않음
    // application.yml의 cache.ttl-seconds 설정은 현재 미적용 상태 (서버 재시작 시 캐시 초기화)
    //
    // hiraApiCache              — 항목 코드 등 정적 데이터 캐시용 (legacy)
    // hospitalDetailHiraCache   — 병원 상세 외부 HIRA 5종 API 응답 캐시. DB 가격은 캐시하지 않음.
    // nonPayItemGroupsCache     — /api/items 응답 (875건 그룹핑 결과). 배치 종료 시 evict.
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "hiraApiCache",
                HOSPITAL_DETAIL_HIRA_CACHE,
                NON_PAY_ITEM_GROUPS_CACHE);
    }
}
