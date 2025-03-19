package com.sequenceiq.datalake.flow.datalake.verticalscale.diskupdate.handler;

import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DiskUpdateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.handler.DatalakeDiskUpdateHandler;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
public class DatalakeDiskUpdateHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String TEST_CLUSTER = "TEST_CLUSTER";

    private static final String ACCOUNT_ID = "ACCOUNT_ID";

    @Mock
    private DiskUpdateEndpoint diskUpdateEndpoint;

    @Mock
    private SdxService sdxService;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private SdxWaitService sdxWaitService;

    private DatalakeDiskUpdateHandler underTest;

    @Mock
    private EventSender eventSender;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> captor;

    @BeforeEach
    void setUp() {
        underTest = new DatalakeDiskUpdateHandler(eventSender);
        ReflectionTestUtils.setField(underTest, null, cloudbreakFlowService, CloudbreakFlowService.class);
        ReflectionTestUtils.setField(underTest, null, sdxService, SdxService.class);
        ReflectionTestUtils.setField(underTest, null, diskUpdateEndpoint, DiskUpdateEndpoint.class);
        ReflectionTestUtils.setField(underTest, null, sdxWaitService, SdxWaitService.class);
    }

    @Test
    public void testDiskUpdateAction() throws Exception {
        String selector = DATALAKE_DISK_UPDATE_EVENT.event();
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");
        SdxCluster sdxCluster = mock(SdxCluster.class);
        doReturn(1L).when(sdxCluster).getId();
        doReturn(sdxCluster).when(sdxService).getByNameInAccount(any(), anyString());
        DatalakeDiskUpdateEvent event = DatalakeDiskUpdateEvent.builder()
                .withAccepted(new Promise<>())
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withDatalakeDiskUpdateRequest(diskUpdateRequest)
                .withSelector(selector)
                .withVolumesToBeUpdated(List.of(mock(Volume.class)))
                .withCloudPlatform("AWS")
                .withStackId(STACK_ID)
                .build();
        underTest.accept(new Event<>(event));
        verify(eventSender, times(1)).sendEvent(captor.capture(), any());
        assertEquals(DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_FINISH_EVENT.selector(), captor.getValue().getSelector());
        ArgumentCaptor<PollingConfig> pollingConfigArgumentCaptor = ArgumentCaptor.forClass(PollingConfig.class);
        verify(sdxWaitService).waitForCloudbreakFlow(eq(1L), pollingConfigArgumentCaptor.capture(), eq("Polling Resize flow"));
        assertEquals(30, pollingConfigArgumentCaptor.getValue().getSleepTime());
    }

    @Test
    public void testDiskUpdateFailureAction() throws Exception {
        String selector = DATALAKE_DISK_UPDATE_EVENT.event();
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");
        DatalakeDiskUpdateEvent event = DatalakeDiskUpdateEvent.builder()
                .withAccepted(new Promise<>())
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withDatalakeDiskUpdateRequest(diskUpdateRequest)
                .withSelector(selector)
                .withVolumesToBeUpdated(List.of(mock(Volume.class)))
                .withCloudPlatform("AWS")
                .build();
        underTest.accept(new Event<>(event));
        verify(eventSender, times(1)).sendEvent(captor.capture(), any());
        assertEquals(DatalakeDiskUpdateStateSelectors.FAILED_DATALAKE_DISK_UPDATE_EVENT.selector(), captor.getValue().getSelector());
    }
}
