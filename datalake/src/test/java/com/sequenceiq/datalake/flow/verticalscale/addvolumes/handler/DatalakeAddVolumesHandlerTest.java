package com.sequenceiq.datalake.flow.verticalscale.addvolumes.handler;

import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_FINISH_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_HANDLER_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.FAILED_DATALAKE_ADD_VOLUMES_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesEvent;
import com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesFailedEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DatalakeAddVolumesHandlerTest {

    private static final Long SDX_ID = 1L;

    private static final String SDX_CRN = "test-crn";

    @Mock
    private SdxService sdxService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private SdxWaitService sdxWaitService;

    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @InjectMocks
    private DatalakeAddVolumesHandler underTest;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    private DatalakeAddVolumesEvent datalakeAddVolumesEvent;

    private HandlerEvent<DatalakeAddVolumesEvent> event;

    @BeforeEach
    void setUp() {
        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("test");
        stackAddVolumesRequest.setNumberOfDisks(2L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setCloudVolumeUsageType(CloudVolumeUsageType.GENERAL.name());
        datalakeAddVolumesEvent = new DatalakeAddVolumesEvent(DATALAKE_ADD_VOLUMES_HANDLER_EVENT.selector(), SDX_ID, "TEST_USER",
                stackAddVolumesRequest, "TEST_SDX");
        event = new HandlerEvent<>(new Event<>(datalakeAddVolumesEvent));
        doReturn(sdxCluster).when(sdxService).getById(SDX_ID);
        doReturn(SDX_CRN).when(sdxCluster).getStackCrn();
        doReturn(regionAwareInternalCrnGenerator).when(regionAwareInternalCrnGeneratorFactory).sdxAdmin();
        doReturn("crn").when(regionAwareInternalCrnGenerator).getInternalCrnForServiceAsString();
    }

    @Test
    void testAddVolumesHandler() {
        FlowIdentifier flowIdentifier = mock(FlowIdentifier.class);
        doReturn(flowIdentifier).when(distroXV1Endpoint).addVolumesByStackCrn(eq(SDX_CRN),
                eq(datalakeAddVolumesEvent.getStackAddVolumesRequest()));
        DatalakeAddVolumesEvent response = (DatalakeAddVolumesEvent) underTest.doAccept(event);
        verify(sdxService).getById(SDX_ID);
        verify(distroXV1Endpoint).addVolumesByStackCrn(eq(SDX_CRN), eq(datalakeAddVolumesEvent.getStackAddVolumesRequest()));
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(eq(sdxCluster), eq(flowIdentifier));
        verify(sdxWaitService).waitForCloudbreakFlow(anyLong(), any(PollingConfig.class), eq("Polling add volumes flow"));
        assertEquals(DATALAKE_ADD_VOLUMES_FINISH_EVENT.selector(), response.getSelector());
        assertEquals(SDX_ID, response.getResourceId());
        assertEquals("TEST_SDX", response.getSdxName());
        assertEquals("test", response.getStackAddVolumesRequest().getInstanceGroup());
        assertEquals(SDX_ID, response.getResourceId());
    }

    @Test
    void testAddVolumesHandlerFailureEvent() {
        doThrow(new CloudbreakServiceException("Test Exception")).when(distroXV1Endpoint).addVolumesByStackCrn(eq(SDX_CRN),
                eq(datalakeAddVolumesEvent.getStackAddVolumesRequest()));
        DatalakeAddVolumesFailedEvent response = (DatalakeAddVolumesFailedEvent) underTest.doAccept(event);
        verify(sdxService).getById(SDX_ID);
        verify(distroXV1Endpoint).addVolumesByStackCrn(eq(SDX_CRN), eq(datalakeAddVolumesEvent.getStackAddVolumesRequest()));
        verifyNoInteractions(cloudbreakFlowService);
        verifyNoInteractions(sdxWaitService);
        assertEquals(FAILED_DATALAKE_ADD_VOLUMES_EVENT.selector(), response.getSelector());
        assertEquals(SDX_ID, response.getResourceId());
        assertEquals("com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException: Test Exception", response.getException().getMessage());
        assertEquals(CloudbreakServiceException.class, response.getException().getClass());
    }
}