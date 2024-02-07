package com.sequenceiq.datalake.flow.imdupdate.handler;

import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.sequenceiq.datalake.flow.imdupdate.event.InstanceMetadataUpdateRequest;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateFailedEvent;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateSuccessEvent;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class SdxInstanceMetadataUpdateHandlerTest {

    @Mock
    private CloudbreakStackService cloudbreakStackService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxInstanceMetadataUpdateHandler underTest;

    @Test
    void testAccept() {
        when(sdxService.getById(any())).thenReturn(new SdxCluster());
        doNothing().when(cloudbreakStackService).updateInstanceMetadata(any(), any());

        Selectable event = underTest.doAccept(new HandlerEvent<>(new Event<>(new InstanceMetadataUpdateRequest(1L, "", IMDS_HTTP_TOKEN_REQUIRED))));

        verify(cloudbreakStackService).updateInstanceMetadata(any(), eq(IMDS_HTTP_TOKEN_REQUIRED));
        assertEquals(SdxInstanceMetadataUpdateSuccessEvent.class, event.getClass());
    }

    @Test
    void testAcceptFailure() {
        when(sdxService.getById(any())).thenReturn(new SdxCluster());
        doThrow(new CloudbreakServiceException("fail")).when(cloudbreakStackService).updateInstanceMetadata(any(), any());

        Selectable event = underTest.doAccept(new HandlerEvent<>(new Event<>(new InstanceMetadataUpdateRequest(1L, "", IMDS_HTTP_TOKEN_REQUIRED))));

        verify(cloudbreakStackService).updateInstanceMetadata(any(), eq(IMDS_HTTP_TOKEN_REQUIRED));
        assertEquals(SdxInstanceMetadataUpdateFailedEvent.class, event.getClass());
    }
}
