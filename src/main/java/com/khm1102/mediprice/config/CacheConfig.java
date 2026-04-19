package com.khm1102.mediprice.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    // ConcurrentMapCacheManager는 TTL을 지원하지 않음
    // application.yml의 cache.ttl-seconds 설정은 현재 미적용 상태 (서버 재시작 시 캐시 초기화)
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("hiraApiCache");
    }
}
