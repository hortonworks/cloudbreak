package com.sequenceiq.datalake.flow.datalake.verticalscale.diskupdate.handler;

import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DiskUpdateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.handler.DatalakeDiskUpdateHandler;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
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
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Mock
    private CloudbreakServiceCrnEndpoints cloudbreakServiceCrnEndpoints;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private SdxService sdxService;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @InjectMocks
    private DatalakeDiskUpdateHandler underTest;

    @Mock
    private EventSender eventSender;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> captor;

    @BeforeEach
    void setUp() {
        underTest = new DatalakeDiskUpdateHandler(eventSender);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDiskUpdateAction() {
        String selector = DATALAKE_DISK_UPDATE_EVENT.event();
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");
        ClusterV4Response clusterV4Response = mock(ClusterV4Response.class);
        StackV4Response stackV4Response = mock(StackV4Response.class);
        doReturn(stackV4Response).when(stackV4Endpoint).getWithResources(anyLong(), any(), any(), any());
        doReturn(clusterV4Response).when(stackV4Response).getCluster();
        SdxCluster sdxCluster = mock(SdxCluster.class);
        doReturn(sdxCluster).when(sdxService).getByNameAndStackId(anyLong(), any());
        doReturn(1L).when(sdxCluster).getId();
        DatalakeDiskUpdateEvent event = DatalakeDiskUpdateEvent.builder()
                .withAccepted(new Promise<>())
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withDatalakeDiskUpdateRequest(diskUpdateRequest)
                .withSelector(selector)
                .withVolumesToBeUpdated(List.of(mock(Volume.class)))
                .withCloudPlatform("AWS")
                .withStackId(1L)
                .build();
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        doReturn("TEST").when(regionAwareInternalCrnGenerator).getInternalCrnForServiceAsString();
        doReturn(regionAwareInternalCrnGenerator).when(regionAwareInternalCrnGeneratorFactory).sdxAdmin();
        ReflectionTestUtils.setField(underTest, null, eventSender, EventSender.class);
        underTest.accept(new Event<>(event));
        verify(eventSender, times(1)).sendEvent(captor.capture(), any());
        assertEquals(DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_FINISH_EVENT.selector(), captor.getValue().getSelector());
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
        ReflectionTestUtils.setField(underTest, null, eventSender, EventSender.class);
        underTest.accept(new Event<>(event));
        verify(eventSender, times(1)).sendEvent(captor.capture(), any());
        assertEquals(DatalakeDiskUpdateStateSelectors.FAILED_DATALAKE_DISK_UPDATE_EVENT.selector(), captor.getValue().getSelector());
    }
}
