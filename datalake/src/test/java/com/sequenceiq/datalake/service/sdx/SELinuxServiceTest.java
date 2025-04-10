package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxEvent;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class SELinuxServiceTest {

    @Mock
    private EventSender eventSender;

    @InjectMocks
    private SELinuxService underTest;

    @Mock
    private SdxCluster sdxCluster;

    @Test
    void testEnableSeLinuxOnDatalake() {
        when(sdxCluster.getResourceCrn()).thenReturn("testCrn");
        when(sdxCluster.getId()).thenReturn(1L);
        when(sdxCluster.getName()).thenReturn("test-cluster");
        FlowIdentifier flowIdentifier = mock(FlowIdentifier.class);
        when(flowIdentifier.getPollableId()).thenReturn("flow-id");
        when(eventSender.sendEvent(any(DatalakeModifySeLinuxEvent.class), any(Event.Headers.class))).thenReturn(flowIdentifier);
        FlowIdentifier response = underTest.modifySeLinuxOnDatalake(sdxCluster, "userCrn", SeLinux.ENFORCING);
        ArgumentCaptor<DatalakeModifySeLinuxEvent> eventCaptor = ArgumentCaptor.forClass(DatalakeModifySeLinuxEvent.class);
        ArgumentCaptor<Event.Headers> headerCaptor = ArgumentCaptor.forClass(Event.Headers.class);
        verify(eventSender).sendEvent(eventCaptor.capture(), headerCaptor.capture());
        DatalakeModifySeLinuxEvent responseEvent = eventCaptor.getValue();
        assertEquals("testCrn", responseEvent.getResourceCrn());
        assertEquals(1L, responseEvent.getResourceId());
        assertEquals("test-cluster", responseEvent.getResourceName());
        assertEquals("userCrn", headerCaptor.getValue().get("FLOW_TRIGGER_USERCRN"));
        assertEquals("flow-id", response.getPollableId());
    }
}
