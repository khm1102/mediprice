package com.khm1102.mediprice.batch.item;

import com.khm1102.mediprice.client.hira.nonpay.NonPayCodeItem;
import com.khm1102.mediprice.entity.NonPayItem;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NonPayItemBatchWriterTest {

    @Mock EntityManager em;

    @InjectMocks NonPayItemBatchWriter writer;

    @BeforeEach
    void wireEm() throws Exception {
        var f = NonPayItemBatchWriter.class.getDeclaredField("em");
        f.setAccessible(true);
        f.set(writer, em);
    }

    /** 페이지 1건당 em.merge 1회 + 끝에 flush/clear. */
    @Test
    void mergesEachItemThenFlushAndClear() {
        when(em.merge(any())).thenAnswer(inv -> inv.getArgument(0));

        List<NonPayCodeItem> page = List.of(
                new NonPayCodeItem("N1", "이름1", "M", "중", "S", "소", "2024", "99991231"),
                new NonPayCodeItem("N2", "이름2", "M", "중", "S", "소", "2024", "99991231"));

        int saved = writer.saveBatch(page);

        assertThat(saved).isEqualTo(2);

        ArgumentCaptor<NonPayItem> captor = ArgumentCaptor.forClass(NonPayItem.class);
        verify(em, times(2)).merge(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NonPayItem::getNpayCd)
                .containsExactly("N1", "N2");

        verify(em, times(1)).flush();
        verify(em, times(1)).clear();
    }

    /** merge 실패해도 다른 row 진행 + flush/clear 호출. */
    @Test
    void continuesOnIndividualFailure() {
        when(em.merge(any())).thenThrow(new RuntimeException("fail")).thenAnswer(inv -> inv.getArgument(0));

        List<NonPayCodeItem> page = List.of(
                new NonPayCodeItem("N1", "이름1", null, null, null, null, null, null),
                new NonPayCodeItem("N2", "이름2", null, null, null, null, null, null));

        int saved = writer.saveBatch(page);

        assertThat(saved).isEqualTo(1);
        verify(em, times(2)).merge(any());
        verify(em).flush();
        verify(em).clear();
    }
}
