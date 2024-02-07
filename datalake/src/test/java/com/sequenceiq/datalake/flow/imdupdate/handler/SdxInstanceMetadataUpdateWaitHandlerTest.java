package com.sequenceiq.datalake.flow.imdupdate.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.imdupdate.event.InstanceMetadataUpdateWaitRequest;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateFailedEvent;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateWaitSuccessEvent;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class SdxInstanceMetadataUpdateWaitHandlerTest {

    @Mock
    private SdxInstanceMetadataUpdateWaitConfiguration waitParamService;

    @Mock
    private CloudbreakPoller cloudbreakPoller;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxInstanceMetadataUpdateWaitHandler underTest;

    @Test
    void testAccept() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setLastCbFlowId("1");
        when(sdxService.getById(any())).thenReturn(sdxCluster);
        doNothing().when(cloudbreakPoller).pollFlowStateByFlowIdentifierUntilComplete(any(), any(), any(), any());

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(new InstanceMetadataUpdateWaitRequest(1L, "user"))));

        verify(cloudbreakPoller).pollFlowStateByFlowIdentifierUntilComplete(any(), any(), any(), any());
        assertEquals(SdxInstanceMetadataUpdateWaitSuccessEvent.class, selectable.getClass());
    }

    @Test
    void testAcceptFailure() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setLastCbFlowId("1");
        when(sdxService.getById(any())).thenReturn(sdxCluster);
        doThrow(new CloudbreakServiceException("fail")).when(cloudbreakPoller).pollFlowStateByFlowIdentifierUntilComplete(any(), any(), any(), any());

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(new InstanceMetadataUpdateWaitRequest(1L, "user"))));

        verify(cloudbreakPoller).pollFlowStateByFlowIdentifierUntilComplete(any(), any(), any(), any());
        assertEquals(SdxInstanceMetadataUpdateFailedEvent.class, selectable.getClass());
    }
}
