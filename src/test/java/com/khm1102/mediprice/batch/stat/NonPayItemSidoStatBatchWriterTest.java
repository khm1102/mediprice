package com.khm1102.mediprice.batch.stat;

import com.khm1102.mediprice.client.hira.NonPaySidoStatItem;
import com.khm1102.mediprice.entity.NonPayItemSidoStat;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonPayItemSidoStatBatchWriterTest {

    @Mock EntityManager em;

    @InjectMocks NonPayItemSidoStatBatchWriter writer;

    @BeforeEach
    void wireEm() throws Exception {
        var f = NonPayItemSidoStatBatchWriter.class.getDeclaredField("em");
        f.setAccessible(true);
        f.set(writer, em);
    }

    /** All + 17 시도 모두 채워진 경우 18개 long row merge. */
    @Test
    void expandsAllPopulatedRegionsIntoLongRows() {
        NonPaySidoStatItem item = new NonPaySidoStatItem(
                "X001", "테스트", "20260517",
                1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L,
                1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L,
                1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L,
                1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L,
                1L, 1L, 1L, 1L,  1L, 1L, 1L, 1L
        );
        when(em.merge(any())).thenAnswer(inv -> inv.getArgument(0));

        int saved = writer.saveBatch(List.of(item));

        assertThat(saved).isEqualTo(18);
        ArgumentCaptor<NonPayItemSidoStat> captor = ArgumentCaptor.forClass(NonPayItemSidoStat.class);
        verify(em, times(18)).merge(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NonPayItemSidoStat::getSidoKey)
                .containsExactlyInAnyOrder(
                        "All", "Sl", "Bs", "Tg", "Ich", "Kw", "Dj", "Usn", "Sj",
                        "Kyg", "Kaw", "Cb", "Ccn", "Jb", "Jn", "Ksb", "Ksn", "Jj");
        verify(em).flush();
        verify(em).clear();
    }

    /** 부분만 채워진 경우 채워진 것만 row 생성. */
    @Test
    void onlyExpandsPopulatedRegions() {
        NonPaySidoStatItem item = new NonPaySidoStatItem(
                "X001", "모발이식", "20260517",
                1L, 1L, 1L, 1L,
                1L, 1L, 1L, 1L,
                null, null, null, null,
                1L, 1L, 1L, 1L,
                1L, 1L, 1L, 1L,
                null, null, null, null,
                1L, 1L, 1L, 1L,
                null, null, null, null,
                1L, 1L, 1L, 1L,
                1L, 1L, 1L, 1L,
                1L, 1L, 1L, 1L,
                null, null, null, null,
                1L, 1L, 1L, 1L,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null);
        when(em.merge(any())).thenAnswer(inv -> inv.getArgument(0));

        int saved = writer.saveBatch(List.of(item));

        assertThat(saved).isEqualTo(9);
    }
}
