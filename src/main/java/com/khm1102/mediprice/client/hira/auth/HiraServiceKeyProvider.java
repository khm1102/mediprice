package com.khm1102.mediprice.client.hira.auth;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HIRA OpenAPI 인증키 공급자.
 * <p>
 * 일일 호출 한도(개발계정 키당 10,000건)를 키 여러 개로 늘리기 위해 라운드로빈으로 키를 분배한다.
 * <ul>
 *   <li>{@code hira.api-keys} (콤마 구분) — 다중 키 우선</li>
 *   <li>{@code hira.api-key} — 단일 키 fallback</li>
 * </ul>
 * 둘 다 비어 있으면 부팅 실패.
 */
@Slf4j
@Component
public class HiraServiceKeyProvider {

    private final List<String> keys;
    private final AtomicInteger cursor = new AtomicInteger(0);

    public HiraServiceKeyProvider(
            @Value("${hira.api-keys:}") String apiKeysCsv,
            @Value("${hira.api-key:}") String singleKey) {
        this.keys = Collections.unmodifiableList(buildKeys(apiKeysCsv, singleKey));
    }

    @PostConstruct
    void logInitialState() {
        log.info("HiraServiceKeyProvider 초기화 — 키 {}개", keys.size());
    }

    private static List<String> buildKeys(String csv, String single) {
        // LinkedHashSet으로 순서 유지 + 중복 제거.
        Set<String> ordered = new LinkedHashSet<>();
        if (csv != null && !csv.isBlank()) {
            for (String token : csv.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isBlank()) {
                    ordered.add(trimmed);
                }
            }
        }
        if (single != null && !single.isBlank()) {
            ordered.add(single.trim());
        }
        if (ordered.isEmpty()) {
            throw new IllegalStateException(
                    "HIRA API 키 미설정 — hira.api-keys(콤마 구분) 또는 hira.api-key 중 최소 하나 필요");
        }
        return new ArrayList<>(ordered);
    }

    /** 다음 키 반환. 스레드 안전 라운드로빈. */
    public String next() {
        int idx = Math.floorMod(cursor.getAndIncrement(), keys.size());
        return keys.get(idx);
    }

    /** 테스트/디버깅용. 외부 변경 불가. */
    public List<String> keys() {
        return keys;
    }
}
