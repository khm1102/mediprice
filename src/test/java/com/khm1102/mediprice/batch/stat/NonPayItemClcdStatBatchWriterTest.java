package com.khm1102.mediprice.batch.stat;

import com.khm1102.mediprice.client.hira.stat.NonPayClcdStatItem;
import com.khm1102.mediprice.entity.NonPayItemClcdStat;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonPayItemClcdStatBatchWriterTest {

    @Mock EntityManager em;

    @InjectMocks NonPayItemClcdStatBatchWriter writer;

    @BeforeEach
    void wireEm() throws Exception {
        var f = NonPayItemClcdStatBatchWriter.class.getDeclaredField("em");
        f.setAccessible(true);
        f.set(writer, em);
    }

    /** 1 wide row × 4 종별 = 4개 long row merge. */
    @Test
    void expandsOneRowIntoFourClcdEntities() {
        NonPayClcdStatItem item = new NonPayClcdStatItem(
                "X001", "테스트", "20260517",
                1000L, 999L, 500L, 5000L,
                2000L, 1900L, 1500L, 8000L,
                3000L, 2900L, 2500L, 10000L,
                4000L, 3900L, 3500L, 12000L);
        when(em.merge(any())).thenAnswer(inv -> inv.getArgument(0));

        int saved = writer.saveBatch(List.of(item));

        assertThat(saved).isEqualTo(4);
        ArgumentCaptor<NonPayItemClcdStat> captor = ArgumentCaptor.forClass(NonPayItemClcdStat.class);
        verify(em, times(4)).merge(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NonPayItemClcdStat::getClcdKey)
                .containsExactly("All", "Usgh", "Hosp", "Gnhp");
        verify(em).flush();
        verify(em).clear();
    }

    /** 4통계 모두 null인 종별은 skip. */
    @Test
    void skipsClcdEntryWhenAllStatsNull() {
        NonPayClcdStatItem item = new NonPayClcdStatItem(
                "X001", "테스트", "20260517",
                1000L, 999L, 500L, 5000L,    // All
                null, null, null, null,       // Usgh
                null, null, null, null,       // Hosp
                null, null, null, null);      // Gnhp
        when(em.merge(any())).thenAnswer(inv -> inv.getArgument(0));

        int saved = writer.saveBatch(List.of(item));

        assertThat(saved).isEqualTo(1);
        verify(em, times(1)).merge(any());
    }

    /** npayCd 또는 stdDate가 null이면 row 자체 skip. */
    @Test
    void skipsRowWithMissingKeys() {
        NonPayClcdStatItem item = new NonPayClcdStatItem(
                null, "테스트", "20260517",
                1000L, 999L, 500L, 5000L,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null);

        int saved = writer.saveBatch(List.of(item));

        assertThat(saved).isZero();
        verify(em, never()).merge(any());
    }
}
