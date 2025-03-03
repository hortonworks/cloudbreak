package com.sequenceiq.freeipa.events.sync;

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
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeipaStructuredSyncEvent;
import com.sequenceiq.freeipa.converter.stack.StackToStackDetailsConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class StructuredSyncEventFactoryTest {

    @Mock
    private StackService stackService;

    @Mock
    private Clock clock;

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private StackToStackDetailsConverter stackToStackDetailsConverter;

    @InjectMocks
    private StructuredSyncEventFactory underTest;

    @Test
    void testCreateCDPFreeipaStructuredSyncEvent() {
        ReflectionTestUtils.setField(underTest, "serviceVersion", "2.95.0");
        Stack stack = mock(Stack.class);
        when(stackService.getStackById(1L)).thenReturn(stack);
        when(stack.getName()).thenReturn("stackName");
        when(stack.getAccountId()).thenReturn("accountId");
        when(stack.getResourceCrn()).thenReturn("resourceCrn");
        when(stack.getEnvironmentCrn()).thenReturn("environmentCrn");
        when(clock.getCurrentTimeMillis()).thenReturn(12345L);
        when(nodeConfig.getId()).thenReturn("nodeConfigId");
        StackDetails stackDetails = mock(StackDetails.class);
        when(stackToStackDetailsConverter.convert(stack)).thenReturn(stackDetails);

        CDPFreeipaStructuredSyncEvent result = ThreadBasedUserCrnProvider.doAs("userCrn", () -> underTest.createCDPFreeipaStructuredSyncEvent(1L));

        CDPOperationDetails operationDetails = result.getOperation();
        assertEquals(12345L, operationDetails.getTimestamp());
        assertEquals(SYNC, operationDetails.getEventType());
        assertEquals("freeipa", operationDetails.getResourceType());
        assertEquals(1L, operationDetails.getResourceId());
        assertEquals("stackName", operationDetails.getResourceName());
        assertEquals("nodeConfigId", operationDetails.getCloudbreakId());
        assertEquals("2.95.0", operationDetails.getCloudbreakVersion());
        assertEquals("accountId", operationDetails.getAccountId());
        assertEquals("resourceCrn", operationDetails.getResourceCrn());
        assertEquals("userCrn", operationDetails.getUserCrn());
        assertEquals("environmentCrn", operationDetails.getEnvironmentCrn());
        assertEquals(stackDetails, result.getStackDetails());
    }
}
