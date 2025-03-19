package com.sequenceiq.datalake.flow.verticalscale.rootvolume.handler;

import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_FINISH_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_HANDLER_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.RootVolumeUpdateRequest;
import com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateFailedEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DatalakeRootVolumeUpdateHandlerTest {

    private static final Long SDX_ID = 1L;

    private static final String SDX_NAME = "TEST_SDX";

    private static final String SDX_CRN = "TEST_SDX_CRN";

    private static final String TEST_ACCOUNT_ID = "ACCOUNT_ID";

    private static final String TEST_CLOUD_PLATFORM = "AWS";

    private static final String USER_CRN = "userCrn";

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxWaitService sdxWaitService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @InjectMocks
    private DatalakeRootVolumeUpdateHandler underTest;

    @Mock
    private SdxCluster sdxCluster;

    private DatalakeRootVolumeUpdateEvent datalakeRootVolumeUpdateEvent;

    private HandlerEvent<DatalakeRootVolumeUpdateEvent> event;

    @BeforeEach
    void setUp() {
        RootVolumeUpdateRequest rootVolumeUpdateRequest = RootVolumeUpdateRequest.builder().withDiskType(DiskType.ROOT_DISK)
                .withVolumeType("gp2").withSize(210).withGroup("test").build();
        datalakeRootVolumeUpdateEvent = new DatalakeRootVolumeUpdateEvent(
                DATALAKE_ROOT_VOLUME_UPDATE_HANDLER_EVENT.selector(), SDX_ID, new Promise<>(), SDX_NAME, SDX_CRN, SDX_CRN, SDX_NAME, TEST_ACCOUNT_ID,
                rootVolumeUpdateRequest, TEST_CLOUD_PLATFORM, SDX_ID, USER_CRN);
        event = new HandlerEvent<>(new Event<>(datalakeRootVolumeUpdateEvent));
    }

    @Test
    void testRootVolumeUpdateHandler() {
        initMocks();
        FlowIdentifier flowIdentifier = mock(FlowIdentifier.class);
        doReturn(flowIdentifier).when(stackV4Endpoint).updateRootVolumeByStackCrnInternal(eq(0L), eq(SDX_CRN), any(DiskUpdateRequest.class),
                eq(USER_CRN));
        doReturn(SDX_ID).when(sdxCluster).getId();
        DatalakeRootVolumeUpdateEvent response = (DatalakeRootVolumeUpdateEvent) underTest.doAccept(event);
        verify(sdxService).getByNameInAccount(any(), eq(SDX_NAME));
        verify(stackV4Endpoint).updateRootVolumeByStackCrnInternal(eq(0L), eq(SDX_CRN), any(DiskUpdateRequest.class), eq(USER_CRN));
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(eq(sdxCluster), eq(flowIdentifier));
        verify(sdxWaitService).waitForCloudbreakFlow(anyLong(), any(PollingConfig.class), eq("Polling Root Disk Update Flow"));
        assertEquals(DATALAKE_ROOT_VOLUME_UPDATE_FINISH_EVENT.selector(), response.getSelector());
        assertEquals(SDX_ID, response.getResourceId());
        assertEquals(SDX_NAME, response.getClusterName());
        assertEquals("test", response.getRootVolumeUpdateRequest().getGroup());
        assertEquals(SDX_ID, response.getResourceId());
    }

    @Test
    void testRootVolumeUpdateHandlerFailureEvent() {
        initMocks();
        FlowIdentifier flowIdentifier = mock(FlowIdentifier.class);
        doReturn(flowIdentifier).when(stackV4Endpoint).updateRootVolumeByStackCrnInternal(eq(0L), eq(SDX_CRN), any(DiskUpdateRequest.class),
                eq(USER_CRN));
        doThrow(new SdxWaitException("Test Exception", new Exception())).when(sdxWaitService).waitForCloudbreakFlow(anyLong(), any(), any());
        DatalakeRootVolumeUpdateFailedEvent response = (DatalakeRootVolumeUpdateFailedEvent) underTest.doAccept(event);
        verify(sdxService).getByNameInAccount(any(), eq(SDX_NAME));
        verify(stackV4Endpoint).updateRootVolumeByStackCrnInternal(eq(0L), eq(SDX_CRN), any(DiskUpdateRequest.class), eq(USER_CRN));
        assertEquals(FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT.selector(), response.getSelector());
        assertEquals(SDX_ID, response.getResourceId());
        assertEquals("Test Exception", response.getException().getMessage());
        assertEquals(SdxWaitException.class, response.getException().getClass());
    }

    @Test
    void testSelector() {
        assertEquals(DATALAKE_ROOT_VOLUME_UPDATE_HANDLER_EVENT.selector(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception ex = new Exception("test");
        DatalakeRootVolumeUpdateFailedEvent result = (DatalakeRootVolumeUpdateFailedEvent) underTest.defaultFailureEvent(SDX_ID, ex, event.getEvent());
        assertEquals(ex, result.getException());
        assertEquals(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_FAILED, result.getDatalakeStatus());
        assertEquals(datalakeRootVolumeUpdateEvent, result.getDatalakeRootVolumeUpdateEvent());
    }

    private void initMocks() {
        doReturn(sdxCluster).when(sdxService).getByNameInAccount(any(), eq(SDX_NAME));
        doReturn(SDX_CRN).when(sdxCluster).getStackCrn();
    }
}
