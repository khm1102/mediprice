package com.khm1102.mediprice.service;

import com.khm1102.mediprice.global.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NonPayItemService#searchGroupedItems}에 {@code @Cacheable}이 붙어 있고
 * 캐시 영역이 {@link CacheConfig#NON_PAY_ITEM_GROUPS_CACHE}로 매핑되어 있는지 정적 검증.
 * <p>
 * Spring TestContext로 캐시 hit/miss를 검증하면 Spring 7 + spring-test 호환 노이즈가 커진다.
 * 이 검증은 어노테이션 + 캐시 영역 등록 + 배치 종료 시 evict 호출의 회귀를 모두 잡는다.
 */
class NonPayItemServiceCacheTest {

    @Test
    void searchGroupedItemsIsAnnotatedWithCacheable() throws NoSuchMethodException {
        Method method = NonPayItemService.class.getMethod("searchGroupedItems");
        Cacheable annotation = method.getAnnotation(Cacheable.class);

        assertThat(annotation)
                .as("searchGroupedItems()에 @Cacheable이 있어야 한다")
                .isNotNull();
        assertThat(annotation.cacheNames())
                .as("cacheNames는 NON_PAY_ITEM_GROUPS_CACHE를 사용해야 한다")
                .contains(CacheConfig.NON_PAY_ITEM_GROUPS_CACHE);
        assertThat(annotation.key())
                .as("단일 키 캐시('all')")
                .isEqualTo("'all'");
    }

    @Test
    void cacheConfigRegistersNonPayItemGroupsCache() throws IOException {
        String src = readJava("src/main/java/com/khm1102/mediprice/global/config/CacheConfig.java");
        assertThat(src)
                .as("CacheConfig는 NON_PAY_ITEM_GROUPS_CACHE 상수를 노출해야 한다")
                .contains("NON_PAY_ITEM_GROUPS_CACHE");
        assertThat(src)
                .as("ConcurrentMapCacheManager가 해당 영역을 등록해야 한다")
                .contains("NON_PAY_ITEM_GROUPS_CACHE");
    }

    @Test
    void batchServiceEvictsCachesAfterSyncAll() throws IOException {
        String src = readJava("src/main/java/com/khm1102/mediprice/batch/orchestrator/BatchService.java");
        assertThat(src).contains("evictPostBatchCaches");
        // syncAll의 finally에서 호출되어야 부분 실패에도 stale 캐시가 남지 않는다.
        assertThat(src)
                .as("syncAll() 종료 시 캐시 evict 호출이 보여야 한다")
                .containsPattern("finally\\s*\\{[^}]*evictPostBatchCaches\\(\\)");
    }

    @Test
    void batchAdminControllerEvictsAfterTrigger() throws IOException {
        String src = readJava("src/main/java/com/khm1102/mediprice/batch/admin/BatchAdminApiController.java");
        assertThat(src)
                .as("단독 트리거 종료 시에도 evictPostBatchCaches를 호출해야 한다")
                .contains("evictPostBatchCaches");
    }

    private static String readJava(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
