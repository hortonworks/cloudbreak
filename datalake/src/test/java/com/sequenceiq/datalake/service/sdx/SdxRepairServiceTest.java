package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.assertj.core.util.Lists;
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
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.StateStatus;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

@ExtendWith(MockitoExtension.class)
public class SdxRepairServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    private static final String CLUSTER_NAME = "dummyCluster";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private FlowEndpoint flowEndpoint;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxNotificationService notificationService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Captor
    private ArgumentCaptor<ClusterRepairV4Request> captor;

    @InjectMocks
    private SdxRepairService underTest;

    @Test
    public void triggerCloudbreakRepair() {
        SdxCluster cluster = new SdxCluster();
        cluster.setInitiatorUserCrn(USER_CRN);
        cluster.setClusterName(CLUSTER_NAME);
        SdxRepairRequest sdxRepairRequest = new SdxRepairRequest();
        sdxRepairRequest.setHostGroupName("master");
        when(flowEndpoint.getLastFlowByResourceName(anyString())).thenReturn(createFlowLogResponse(StateStatus.SUCCESSFUL, true, null));
        underTest.startRepairInCb(cluster, sdxRepairRequest);
        verify(stackV4Endpoint).repairCluster(eq(0L), eq(CLUSTER_NAME), captor.capture());
        assertEquals(FLOW_CHAIN_ID, cluster.getRepairFlowChainId());
        assertEquals("master", captor.getValue().getHostGroups().get(0));
        verify(sdxStatusService, times(1))
                .setStatusForDatalake(DatalakeStatusEnum.REPAIR_IN_PROGRESS, "Datalake repair in progress", cluster);
        verify(notificationService).send(eq(ResourceEvent.SDX_REPAIR_STARTED), any());
    }

    @Test
    public void waitCloudbreakClusterRepairWhenThereIsNoActiveFlow() throws JsonProcessingException {
        SdxCluster cluster = new SdxCluster();
        cluster.setInitiatorUserCrn(USER_CRN);
        cluster.setClusterName(CLUSTER_NAME);
        cluster.setRepairFlowChainId(FLOW_CHAIN_ID);
        StackV4Response resp = new StackV4Response();
        resp.setStatus(Status.UPDATE_FAILED);
        when(flowEndpoint.getFlowLogsByResourceNameAndChainId(anyString(), eq(FLOW_CHAIN_ID)))
                .thenReturn(Lists.newArrayList(createFlowLogResponse(StateStatus.SUCCESSFUL, true, null)));
        when(stackV4Endpoint.get(eq(0L), eq("dummyCluster"), any())).thenReturn(resp);
        AttemptResult<StackV4Response> attempt = underTest.checkClusterStatusDuringRepair(cluster);
        assertEquals(AttemptState.BREAK, attempt.getState());
        verify(notificationService).send(eq(ResourceEvent.SDX_REPAIR_FAILED), any());
        verify(flowEndpoint).getFlowLogsByResourceNameAndChainId(eq(CLUSTER_NAME), eq(FLOW_CHAIN_ID));
    }

    @Test
    public void waitCloudbreakClusterRepairWhenThereIsActiveFlow() throws JsonProcessingException {
        SdxCluster cluster = new SdxCluster();
        cluster.setInitiatorUserCrn(USER_CRN);
        cluster.setClusterName(CLUSTER_NAME);
        cluster.setRepairFlowChainId(FLOW_CHAIN_ID);

        when(flowEndpoint.getFlowLogsByResourceNameAndChainId(anyString(), eq(FLOW_CHAIN_ID)))
                .thenReturn(Lists.newArrayList(createFlowLogResponse(StateStatus.PENDING, true, null)));
        AttemptResult<StackV4Response> attempt = underTest.checkClusterStatusDuringRepair(cluster);
        assertEquals(AttemptState.CONTINUE, attempt.getState());

        when(flowEndpoint.getFlowLogsByResourceNameAndChainId(anyString(), eq(FLOW_CHAIN_ID)))
                .thenReturn(Lists.newArrayList(createFlowLogResponse(StateStatus.SUCCESSFUL, false, null)));
        attempt = underTest.checkClusterStatusDuringRepair(cluster);
        assertEquals(AttemptState.CONTINUE, attempt.getState());

        verify(flowEndpoint, times(2)).getFlowLogsByResourceNameAndChainId(eq(CLUSTER_NAME), eq(FLOW_CHAIN_ID));
        verifyZeroInteractions(stackV4Endpoint);
    }

    private FlowLogResponse createFlowLogResponse(StateStatus stateStatus, boolean finalized, String nextEvent) {
        FlowLogResponse response = new FlowLogResponse();
        response.setStateStatus(stateStatus);
        response.setFinalized(finalized);
        response.setNextEvent(nextEvent);
        response.setFlowChainId(FLOW_CHAIN_ID);
        return response;
    }
}
