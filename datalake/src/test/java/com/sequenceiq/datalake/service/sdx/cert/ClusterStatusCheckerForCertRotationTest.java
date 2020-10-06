package com.sequenceiq.datalake.service.sdx.cert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState;
import com.sequenceiq.datalake.service.sdx.status.AvailabilityChecker;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@ExtendWith(MockitoExtension.class)
class ClusterStatusCheckerForCertRotationTest {
    private static final Long ID = 1L;

    private SdxCluster sdxCluster;

    private StackV4Response stackV4Response;

    private ClusterV4Response clusterV4Response;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private AvailabilityChecker availabilityChecker;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private ClusterStatusCheckerForCertRotation underTest;

    @BeforeEach
    public void init() {
        sdxCluster = new SdxCluster();
        sdxCluster.setId(ID);
        sdxCluster.setAccountId("accountid");
        sdxCluster.setClusterName("clusterName");
        DatalakeInMemoryStateStore.put(ID, PollGroup.POLLABLE);
        stackV4Response = new StackV4Response();
        clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setName(sdxCluster.getClusterName());
    }

    @Test
    public void testNotPollable() throws JsonProcessingException {
        DatalakeInMemoryStateStore.put(ID, PollGroup.CANCELLED);

        AttemptResult<StackV4Response> result = underTest.checkClusterStatusDuringRotate(sdxCluster);

        assertEquals(AttemptState.BREAK, result.getState());
    }

    @Test
    public void testRunningFlow() throws JsonProcessingException {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(FlowState.RUNNING);

        AttemptResult<StackV4Response> result = underTest.checkClusterStatusDuringRotate(sdxCluster);

        assertEquals(AttemptState.CONTINUE, result.getState());
    }

    @Test
    public void testAvailableCluster() throws JsonProcessingException {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(FlowState.UNKNOWN);
        when(stackV4Endpoint.get(eq(0L), eq(sdxCluster.getClusterName()), anySet(), eq(sdxCluster.getAccountId()))).thenReturn(stackV4Response);
        when(availabilityChecker.stackAndClusterAvailable(stackV4Response, clusterV4Response)).thenReturn(Boolean.TRUE);

        AttemptResult<StackV4Response> result = underTest.checkClusterStatusDuringRotate(sdxCluster);

        assertEquals(AttemptState.FINISH, result.getState());
        assertEquals(stackV4Response, result.getResult());
    }

    @Test
    public void testUpdateFailedStack() throws JsonProcessingException {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(FlowState.UNKNOWN);
        when(stackV4Endpoint.get(eq(0L), eq(sdxCluster.getClusterName()), anySet(), eq(sdxCluster.getAccountId()))).thenReturn(stackV4Response);
        when(availabilityChecker.stackAndClusterAvailable(stackV4Response, clusterV4Response)).thenReturn(Boolean.FALSE);
        stackV4Response.setStatus(Status.UPDATE_FAILED);
        stackV4Response.setStatusReason("testReason");

        AttemptResult<StackV4Response> result = underTest.checkClusterStatusDuringRotate(sdxCluster);

        assertEquals(AttemptState.BREAK, result.getState());
        assertTrue(result.getMessage().contains(stackV4Response.getName()));
        assertTrue(result.getMessage().contains(stackV4Response.getStatusReason()));
    }

    @Test
    public void testUpdateFailedCluster() throws JsonProcessingException {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(FlowState.UNKNOWN);
        when(stackV4Endpoint.get(eq(0L), eq(sdxCluster.getClusterName()), anySet(), eq(sdxCluster.getAccountId()))).thenReturn(stackV4Response);
        when(availabilityChecker.stackAndClusterAvailable(stackV4Response, clusterV4Response)).thenReturn(Boolean.FALSE);
        stackV4Response.setStatus(Status.AVAILABLE);
        clusterV4Response.setStatus(Status.UPDATE_FAILED);
        clusterV4Response.setStatusReason("testReason");

        AttemptResult<StackV4Response> result = underTest.checkClusterStatusDuringRotate(sdxCluster);

        assertEquals(AttemptState.BREAK, result.getState());
        assertTrue(result.getMessage().contains(stackV4Response.getName()));
        assertTrue(result.getMessage().contains(clusterV4Response.getStatusReason()));
    }

    @Test
    public void testFlowFinishedClusterNotFailedNotAvailable() throws JsonProcessingException {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(FlowState.FINISHED);
        when(stackV4Endpoint.get(eq(0L), eq(sdxCluster.getClusterName()), anySet(), eq(sdxCluster.getAccountId()))).thenReturn(stackV4Response);
        when(availabilityChecker.stackAndClusterAvailable(stackV4Response, clusterV4Response)).thenReturn(Boolean.FALSE);
        stackV4Response.setStatus(Status.UPDATE_IN_PROGRESS);
        clusterV4Response.setStatus(Status.UPDATE_IN_PROGRESS);
        when(sdxStatusService.getShortStatusMessage(stackV4Response)).thenReturn("testMessage");

        AttemptResult<StackV4Response> result = underTest.checkClusterStatusDuringRotate(sdxCluster);

        assertEquals(AttemptState.BREAK, result.getState());
        assertTrue(result.getMessage().contains(stackV4Response.getName()));
        assertTrue(result.getMessage().contains("testMessage"));
    }

    @Test
    public void testFlowUnknownClusterNotFailedNotAvailable() throws JsonProcessingException {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(FlowState.UNKNOWN);
        when(stackV4Endpoint.get(eq(0L), eq(sdxCluster.getClusterName()), anySet(), eq(sdxCluster.getAccountId()))).thenReturn(stackV4Response);
        when(availabilityChecker.stackAndClusterAvailable(stackV4Response, clusterV4Response)).thenReturn(Boolean.FALSE);
        stackV4Response.setStatus(Status.UPDATE_IN_PROGRESS);
        clusterV4Response.setStatus(Status.UPDATE_IN_PROGRESS);

        AttemptResult<StackV4Response> result = underTest.checkClusterStatusDuringRotate(sdxCluster);

        assertEquals(AttemptState.CONTINUE, result.getState());
    }
}