package com.khm1102.mediprice.batch.price;

import com.khm1102.mediprice.repository.PriceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceYkihoCleanupServiceTest {

    @Mock PriceRepository priceRepository;

    /** removeAllActiveByYkiho는 priceRepository.removeAllActiveByYkiho로 위임된다. */
    @Test
    void removeAllActiveDelegatesToRepository() {
        PriceYkihoCleanupService service = new PriceYkihoCleanupService(priceRepository);
        when(priceRepository.removeAllActiveByYkiho("YK1")).thenReturn(7);

        int removed = service.removeAllActiveByYkiho("YK1");

        assertThat(removed).isEqualTo(7);
        verify(priceRepository).removeAllActiveByYkiho("YK1");
    }

    /** removeStaleByYkiho는 priceRepository.removeStaleByYkiho로 위임된다. */
    @Test
    void removeStaleDelegatesToRepository() {
        PriceYkihoCleanupService service = new PriceYkihoCleanupService(priceRepository);
        when(priceRepository.removeStaleByYkiho("YK1", Set.of("N001"))).thenReturn(2);

        int removed = service.removeStaleByYkiho("YK1", Set.of("N001"));

        assertThat(removed).isEqualTo(2);
        verify(priceRepository).removeStaleByYkiho("YK1", Set.of("N001"));
    }

    /**
     * 회귀 방지 — 두 정리 메서드는 반드시 {@code @Transactional(propagation = Propagation.REQUIRES_NEW)}로
     * 격리되어야 한다. outer 트랜잭션이 rollback-only로 오염되는 문제를 막는 핵심 가드.
     */
    @Test
    void cleanupMethodsAreAnnotatedWithRequiresNew() throws IOException {
        String src = Files.readString(Path.of(
                "src/main/java/com/khm1102/mediprice/batch/price/PriceYkihoCleanupService.java"));
        assertThat(src)
                .as("Propagation.REQUIRES_NEW import + 어노테이션이 둘 다 있어야 한다")
                .contains("import org.springframework.transaction.annotation.Propagation;")
                .contains("@Transactional(propagation = Propagation.REQUIRES_NEW)");
        // 두 메서드 모두에 적용 — 단일 발생만 통과하지 않게 별도 검증.
        long count = src.lines()
                .filter(line -> line.contains("@Transactional(propagation = Propagation.REQUIRES_NEW)"))
                .count();
        assertThat(count)
                .as("removeAllActiveByYkiho + removeStaleByYkiho 둘 다 REQUIRES_NEW이어야 한다")
                .isEqualTo(2);
    }
}
