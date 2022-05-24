package com.sequenceiq.consumption.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;

@ExtendWith(MockitoExtension.class)
class ConsumptionInternalCrnClientTest {

    private static final String INTERNAL_CRN = "InternalCrn";

    @Mock
    private ConsumptionServiceUserCrnClient client;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private ConsumptionInternalCrnClient underTest;

    @Mock
    private ConsumptionServiceCrnEndpoints consumptionServiceCrnEndpoints;

    @Test
    void withInternalCrnTest() {
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_CRN);
        when(client.withCrn(INTERNAL_CRN)).thenReturn(consumptionServiceCrnEndpoints);

        assertThat(underTest.withInternalCrn()).isSameAs(consumptionServiceCrnEndpoints);
    }

}