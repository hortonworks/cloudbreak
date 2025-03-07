package com.sequenceiq.environment.events.sync;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.SYNC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredSyncEvent;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@ExtendWith(MockitoExtension.class)
class StructuredSyncEventFactoryTest {

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private Clock clock;

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private EnvironmentDtoConverter environmentDtoConverter;

    @InjectMocks
    private StructuredSyncEventFactory underTest;

    @Test
    void testCreateCDPFreeipaStructuredSyncEvent() {
        ReflectionTestUtils.setField(underTest, "serviceVersion", "2.95.0");
        Environment environment = mock(Environment.class);
        when(environmentService.findEnvironmentByIdOrThrow(1L)).thenReturn(environment);
        when(environment.getName()).thenReturn("environmentName");
        when(environment.getAccountId()).thenReturn("accountId");
        when(environment.getResourceCrn()).thenReturn("environmentCrn");
        when(clock.getCurrentTimeMillis()).thenReturn(12345L);
        when(nodeConfig.getId()).thenReturn("nodeConfigId");
        EnvironmentDto environmentDetails = mock(EnvironmentDto.class);
        when(environmentDtoConverter.environmentToDto(environment)).thenReturn(environmentDetails);

        CDPEnvironmentStructuredSyncEvent result = ThreadBasedUserCrnProvider.doAs("userCrn", () -> underTest.createCDPEnvironmentStructuredSyncEvent(1L));

        CDPOperationDetails operationDetails = result.getOperation();
        assertEquals(12345L, operationDetails.getTimestamp());
        assertEquals(SYNC, operationDetails.getEventType());
        assertEquals("environment", operationDetails.getResourceType());
        assertEquals(1L, operationDetails.getResourceId());
        assertEquals("environmentName", operationDetails.getResourceName());
        assertEquals("nodeConfigId", operationDetails.getCloudbreakId());
        assertEquals("2.95.0", operationDetails.getCloudbreakVersion());
        assertEquals("accountId", operationDetails.getAccountId());
        assertEquals("environmentCrn", operationDetails.getResourceCrn());
        assertEquals("userCrn", operationDetails.getUserCrn());
        assertEquals("environmentCrn", operationDetails.getEnvironmentCrn());
        assertEquals(environmentDetails, result.getEnvironmentDetails());
    }

}
