package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateEvent;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
public class VerticalScaleServiceTest {

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private CloudbreakPoller cloudbreakPoller;

    @Mock
    private EventSender eventSender;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @InjectMocks
    private VerticalScaleService underTest;

    @Test
    public void testVerticalScaleDatalake() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        DiskUpdateRequest updateRequest = mock(DiskUpdateRequest.class);
        String userCrn = "USER_CRN";
        doReturn(new FlowIdentifier(FlowType.FLOW, "1")).when(eventSender).sendEvent(any(DatalakeDiskUpdateEvent.class),
                any(Event.Headers.class));
        FlowIdentifier flowIdentifier = underTest.updateDisksDatalake(sdxCluster, updateRequest, userCrn);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("1", flowIdentifier.getPollableId());
    }
}
