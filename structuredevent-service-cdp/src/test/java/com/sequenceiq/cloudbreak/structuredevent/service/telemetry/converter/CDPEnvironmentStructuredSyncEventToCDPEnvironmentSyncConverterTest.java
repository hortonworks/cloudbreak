package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;

@ExtendWith(MockitoExtension.class)
class CDPEnvironmentStructuredSyncEventToCDPEnvironmentSyncConverterTest {

    @Mock
    private CDPEnvironmentStructuredSyncEventToCDPOperationDetailsConverter operationDetailsConverter;

    @Mock
    private EnvironmentDetailsToCDPEnvironmentDetailsConverter environmentDetailsConverter;

    @Mock
    private EnvironmentDetailsToCDPFreeIPADetailsConverter freeIPADetailsConverter;

    @Mock
    private EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter telemetryFeatureDetailsConverter;

    @InjectMocks
    private CDPEnvironmentStructuredSyncEventToCDPEnvironmentSyncConverter underTest;

    @Test
    void testConvert() {
        CDPEnvironmentStructuredSyncEvent cdpEnvironmentStructuredSyncEvent = new CDPEnvironmentStructuredSyncEvent();
        EnvironmentDetails environmentDetails = mock(EnvironmentDetails.class);
        cdpEnvironmentStructuredSyncEvent.setEnvironmentDetails(environmentDetails);
        UsageProto.CDPOperationDetails cdpOperationDetails = mock(UsageProto.CDPOperationDetails.class);
        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = mock(UsageProto.CDPEnvironmentDetails.class);
        UsageProto.CDPEnvironmentTelemetryFeatureDetails cdpEnvironmentTelemetryFeatureDetails = mock(UsageProto.CDPEnvironmentTelemetryFeatureDetails.class);
        UsageProto.CDPFreeIPADetails cdpFreeIPADetails = mock(UsageProto.CDPFreeIPADetails.class);
        when(environmentDetails.getStatusAsString()).thenReturn("status");
        when(operationDetailsConverter.convert(cdpEnvironmentStructuredSyncEvent)).thenReturn(cdpOperationDetails);
        when(environmentDetailsConverter.convert(environmentDetails)).thenReturn(cdpEnvironmentDetails);
        when(telemetryFeatureDetailsConverter.convert(environmentDetails)).thenReturn(cdpEnvironmentTelemetryFeatureDetails);
        when(freeIPADetailsConverter.convert(environmentDetails)).thenReturn(cdpFreeIPADetails);

        UsageProto.CDPEnvironmentSync result = underTest.convert(cdpEnvironmentStructuredSyncEvent);

        assertEquals("status", result.getStatus());
        assertEquals(cdpOperationDetails, result.getOperationDetails());
        assertEquals(cdpEnvironmentDetails, result.getEnvironmentDetails());
        assertEquals(cdpEnvironmentTelemetryFeatureDetails, result.getTelemetryFeatureDetails());
        assertEquals(cdpFreeIPADetails, result.getFreeIPA());
    }
}
