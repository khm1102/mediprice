package com.khm1102.mediprice.service;

import com.khm1102.mediprice.client.HiraDetailClient;
import com.khm1102.mediprice.client.HiraDetailClient.HospitalDetailBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalDetailHiraCacheTest {

    @Mock HiraDetailClient detailClient;

    @InjectMocks HospitalDetailHiraCache cache;

    /** lookupBundle은 HiraDetailClient.fetchAll로 위임한다. */
    @Test
    void lookupBundleDelegatesToDetailClient() {
        HospitalDetailBundle bundle = new HospitalDetailBundle(
                List.of(), List.of(), List.of(), Optional.empty(), List.of());
        when(detailClient.fetchAll("YK001")).thenReturn(bundle);

        HospitalDetailBundle result = cache.lookupBundle("YK001");

        assertThat(result).isSameAs(bundle);
        verify(detailClient).fetchAll("YK001");
    }
}
