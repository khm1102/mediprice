package com.khm1102.mediprice.batch.item;

import com.khm1102.mediprice.client.hira.NonPayDescItem;
import com.khm1102.mediprice.entity.NonPayItemDesc;
import com.khm1102.mediprice.repository.NonPayItemDescRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonPayItemDescBatchWriterTest {

    @Mock NonPayItemDescRepository repository;
    @Mock EntityManager em;
    @Mock TypedQuery<Long> longQuery;
    @Mock Query updateQuery;

    @InjectMocks NonPayItemDescBatchWriter writer;

    @BeforeEach
    void wireEm() throws Exception {
        var f = NonPayItemDescBatchWriter.class.getDeclaredField("em");
        f.setAccessible(true);
        f.set(writer, em);
    }

    /** UNIQUE 키 미존재 → INSERT 경로 (repository.save 호출). */
    @Test
    void insertsNewWhenUniqueKeyNotFound() {
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter(anyString(), any())).thenReturn(longQuery);
        when(longQuery.getResultStream()).thenReturn(Stream.empty());

        int saved = writer.saveBatch(List.of(
                new NonPayDescItem("A", "상급병실료", "설명", "A1100", "1인실", "설명2", null, null, null)));

        assertThat(saved).isEqualTo(1);
        verify(repository).save(any(NonPayItemDesc.class));
        verify(em, never()).createQuery(anyString());  // update path 미호출
        verify(em).flush();
        verify(em).clear();
    }

    /** UNIQUE 키 존재 → UPDATE 경로 (executeUpdate, repository.save 미호출). */
    @Test
    void updatesExistingRowWhenFound() {
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter(anyString(), any())).thenReturn(longQuery);
        when(longQuery.getResultStream()).thenReturn(Stream.of(42L));

        when(em.createQuery(anyString())).thenReturn(updateQuery);
        when(updateQuery.setParameter(anyString(), any())).thenReturn(updateQuery);
        when(updateQuery.executeUpdate()).thenReturn(1);

        int saved = writer.saveBatch(List.of(
                new NonPayDescItem("A", "상급병실료", "설명", "A1100", "1인실", "설명2", null, null, null)));

        assertThat(saved).isEqualTo(1);
        verify(repository, never()).save(any());
        verify(updateQuery).executeUpdate();
    }

    /** 페이지 안 여러 row를 각각 처리. */
    @Test
    void processesMultipleItemsInPage() {
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter(anyString(), any())).thenReturn(longQuery);
        when(longQuery.getResultStream())
                .thenReturn(Stream.empty(), Stream.of(7L), Stream.empty());

        when(em.createQuery(anyString())).thenReturn(updateQuery);
        when(updateQuery.setParameter(anyString(), any())).thenReturn(updateQuery);
        when(updateQuery.executeUpdate()).thenReturn(1);

        int saved = writer.saveBatch(List.of(
                new NonPayDescItem("A", "n1", "d1", "A1", "n2", "d2", null, null, null),
                new NonPayDescItem("B", "n1", "d1", "B1", "n2", "d2", null, null, null),
                new NonPayDescItem("C", "n1", "d1", "C1", "n2", "d2", null, null, null)));

        assertThat(saved).isEqualTo(3);
        verify(repository, times(2)).save(any());
        verify(updateQuery, times(1)).executeUpdate();
    }
}
