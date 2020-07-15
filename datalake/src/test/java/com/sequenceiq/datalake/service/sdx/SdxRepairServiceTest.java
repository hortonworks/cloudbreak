package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.settings.SdxRepairSettings;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

@ExtendWith(MockitoExtension.class)
public class SdxRepairServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    private static final String CLUSTER_NAME = "dummyCluster";

    private static final AtomicLong CLUSTER_ID = new AtomicLong(20000L);

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Captor
    private ArgumentCaptor<ClusterRepairV4Request> captor;

    @InjectMocks
    private SdxRepairService underTest;

    @Test
    public void triggerCloudbreakRepair() {
        SdxCluster cluster = new SdxCluster();
        cluster.setId(CLUSTER_ID.incrementAndGet());
        cluster.setInitiatorUserCrn(USER_CRN);
        cluster.setClusterName(CLUSTER_NAME);
        cluster.setCrn(Crn.builder().setAccountId("asd")
                .setResource("asd")
                .setResourceType(Crn.ResourceType.DATALAKE)
                .setService(Crn.Service.DATALAKE)
                .setPartition(Crn.Partition.CDP)
                .build().toString());
        SdxRepairRequest sdxRepairRequest = new SdxRepairRequest();
        sdxRepairRequest.setHostGroupNames(List.of("master"));
        SdxRepairSettings sdxRepairSettings = SdxRepairSettings.from(sdxRepairRequest);

        doNothing().when(cloudbreakFlowService).saveLastCloudbreakFlowChainId(any(), any());
        underTest.startRepairInCb(cluster, sdxRepairSettings);
        verify(stackV4Endpoint).repairCluster(eq(0L), eq(CLUSTER_NAME), captor.capture(), anyString());
        assertEquals("master", captor.getValue().getHostGroups().get(0));
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.REPAIR_IN_PROGRESS, "Datalake repair in progress", cluster);
    }

    @Test
    public void waitCloudbreakClusterRepairWhenThereIsNoActiveFlow() throws JsonProcessingException {
        SdxCluster cluster = new SdxCluster();
        cluster.setId(CLUSTER_ID.incrementAndGet());
        cluster.setInitiatorUserCrn(USER_CRN);
        cluster.setClusterName(CLUSTER_NAME);
        cluster.setCrn(Crn.builder().setAccountId("asd")
                .setResource("asd")
                .setResourceType(Crn.ResourceType.DATALAKE)
                .setService(Crn.Service.DATALAKE)
                .setPartition(Crn.Partition.CDP)
                .build().toString());
        StackV4Response resp = new StackV4Response();
        resp.setStatus(Status.UPDATE_FAILED);
        when(cloudbreakFlowService.getLastKnownFlowState(any())).thenReturn(FlowState.FINISHED);
        when(stackV4Endpoint.get(eq(0L), eq("dummyCluster"), any(), anyString())).thenReturn(resp);
        AttemptResult<StackV4Response> attempt = underTest.checkClusterStatusDuringRepair(cluster);
        assertEquals(AttemptState.BREAK, attempt.getState());
    }

    @Test
    public void waitCloudbreakClusterRepairWhenThereIsActiveFlow() throws JsonProcessingException {
        SdxCluster cluster = new SdxCluster();
        cluster.setId(CLUSTER_ID.incrementAndGet());
        cluster.setInitiatorUserCrn(USER_CRN);
        cluster.setClusterName(CLUSTER_NAME);

        when(cloudbreakFlowService.getLastKnownFlowState(any())).thenReturn(FlowState.RUNNING);
        AttemptResult<StackV4Response> attempt = underTest.checkClusterStatusDuringRepair(cluster);
        assertEquals(AttemptState.CONTINUE, attempt.getState());

        verifyZeroInteractions(stackV4Endpoint);
    }
}
