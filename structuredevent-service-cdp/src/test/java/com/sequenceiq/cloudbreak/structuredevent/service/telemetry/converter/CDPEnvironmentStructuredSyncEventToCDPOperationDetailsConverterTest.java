package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;

class CDPEnvironmentStructuredSyncEventToCDPOperationDetailsConverterTest {

    private final CDPEnvironmentStructuredSyncEventToCDPOperationDetailsConverter underTest =
            new CDPEnvironmentStructuredSyncEventToCDPOperationDetailsConverter();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "appVersion", "version-1234");
    }

    @Test
    void testConvert() {
        ReflectionTestUtils.setField(underTest, "appVersion", "version-1234");
        CDPEnvironmentStructuredSyncEvent cdpEnvironmentStructuredSyncEvent = new CDPEnvironmentStructuredSyncEvent();
        CDPOperationDetails cdpOperationDetails = new CDPOperationDetails();
        cdpOperationDetails.setAccountId("accountId");
        cdpOperationDetails.setResourceCrn("environmentCrn");
        cdpOperationDetails.setResourceName("resourceName");
        cdpOperationDetails.setUserCrn("userCrn");
        cdpOperationDetails.setUuid("uuid");
        cdpEnvironmentStructuredSyncEvent.setOperation(cdpOperationDetails);
        EnvironmentDetails environmentDetails = mock(EnvironmentDetails.class);
        when(environmentDetails.getCloudPlatform()).thenReturn("AWS");
        cdpEnvironmentStructuredSyncEvent.setEnvironmentDetails(environmentDetails);

        UsageProto.CDPOperationDetails result = underTest.convert(cdpEnvironmentStructuredSyncEvent);

        assertEquals("accountId", result.getAccountId());
        assertEquals("environmentCrn", result.getResourceCrn());
        assertEquals("resourceName", result.getResourceName());
        assertEquals("userCrn", result.getInitiatorCrn());
        assertEquals("uuid", result.getCorrelationId());
        assertEquals("version-1234", result.getApplicationVersion());
        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.AWS, result.getEnvironmentType());
    }
}
