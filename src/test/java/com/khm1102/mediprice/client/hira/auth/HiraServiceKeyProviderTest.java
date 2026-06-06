package com.khm1102.mediprice.client.hira.auth;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HiraServiceKeyProviderTest {

    /** 다중 키 우선. csv 키들이 순서대로 라운드로빈. */
    @Test
    void roundRobinsCsvKeys() {
        HiraServiceKeyProvider p = new HiraServiceKeyProvider("a,b,c", "");

        assertThat(p.next()).isEqualTo("a");
        assertThat(p.next()).isEqualTo("b");
        assertThat(p.next()).isEqualTo("c");
        assertThat(p.next()).isEqualTo("a");
    }

    /** csv 비었으면 단일 키 fallback. 매번 같은 값 반환. */
    @Test
    void fallsBackToSingleKey() {
        HiraServiceKeyProvider p = new HiraServiceKeyProvider("", "single");

        assertThat(p.next()).isEqualTo("single");
        assertThat(p.next()).isEqualTo("single");
        assertThat(p.next()).isEqualTo("single");
    }

    /** csv와 단일 키 모두 있으면 합쳐서 라운드로빈. 중복은 제거. */
    @Test
    void mergesCsvAndSingleKeyDeduplicated() {
        HiraServiceKeyProvider p = new HiraServiceKeyProvider("a,b", "c");

        assertThat(p.keys()).containsExactly("a", "b", "c");

        // 단일 키가 csv에 이미 있으면 중복 제거
        HiraServiceKeyProvider dedup = new HiraServiceKeyProvider("a,b,c", "a");
        assertThat(dedup.keys()).containsExactly("a", "b", "c");
    }

    /** 키 0개면 부팅 실패. */
    @Test
    void failsWhenNoKeys() {
        assertThatThrownBy(() -> new HiraServiceKeyProvider("", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HIRA API 키 미설정");

        assertThatThrownBy(() -> new HiraServiceKeyProvider(null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    /** csv 공백 / null 토큰 무시. */
    @Test
    void ignoresBlankTokens() {
        HiraServiceKeyProvider p = new HiraServiceKeyProvider("a, , b,,c", "");
        assertThat(p.keys()).containsExactly("a", "b", "c");
    }

    /** 멀티스레드 next() — 인덱스 충돌 없이 모든 호출이 키 중 하나를 반환. */
    @Test
    void nextIsThreadSafe() throws InterruptedException {
        HiraServiceKeyProvider p = new HiraServiceKeyProvider("a,b,c,d,e", "");
        int threads = 16;
        int iterationsPerThread = 1000;
        AtomicInteger errors = new AtomicInteger(0);
        Set<String> seen = ConcurrentHashMap.newKeySet();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        String key = p.next();
                        if (key == null || !Set.of("a", "b", "c", "d", "e").contains(key)) {
                            errors.incrementAndGet();
                        }
                        seen.add(key);
                    }
                });
            }
        } finally {
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(errors.get()).isZero();
        assertThat(seen).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    }
}
