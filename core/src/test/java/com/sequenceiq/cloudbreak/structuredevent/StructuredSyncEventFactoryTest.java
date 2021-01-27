package com.sequenceiq.cloudbreak.structuredevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.SyncDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.flow.ha.NodeConfig;

@ExtendWith(MockitoExtension.class)
public class StructuredSyncEventFactoryTest {

    @Mock
    private Clock clock;

    @Mock
    private ConversionService conversionService;

    @Mock
    private StackService stackService;

    @Mock
    private NodeConfig nodeConfig;

    @InjectMocks
    private StructuredSyncEventFactory underTest;

    @Test
    public void createCDPStructuredSyncEvent() {
        Stack stack = TestUtil.stack();
        stack.setResourceCrn("crn");
        SyncDetails syncDetails = new SyncDetails();
        syncDetails.setName("name");

        when(conversionService.convert(stack, SyncDetails.class)).thenReturn(syncDetails);
        when(stackService.getByIdWithTransaction(1L)).thenReturn(stack);
        when(nodeConfig.getId()).thenReturn("cbid");

        StructuredSyncEvent result = underTest.createStructuredSyncEvent(1L);

        assertNull(result.getException());
        assertEquals("name", result.getsyncDetails().getName());
        assertEquals(CloudbreakEventService.DATAHUB_RESOURCE_TYPE, result.getOperation().getResourceType());
        assertEquals("crn", result.getOperation().getResourceCrn());
        assertEquals("cbid", result.getOperation().getCloudbreakId());
    }
}
