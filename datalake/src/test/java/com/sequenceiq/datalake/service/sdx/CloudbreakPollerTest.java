package com.sequenceiq.datalake.service.sdx;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowcheck.FlowState;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@ExtendWith(MockitoExtension.class)
class CloudbreakPollerTest {
    private static final Long ID = 1L;

    private SdxCluster sdxCluster;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private CloudbreakPoller underTest;

    private PollingConfig pollingConfig;

    @BeforeEach
    public void init() {
        sdxCluster = new SdxCluster();
        sdxCluster.setId(ID);
        sdxCluster.setAccountId("accountid");
        sdxCluster.setClusterName("clusterName");
        DatalakeInMemoryStateStore.put(ID, PollGroup.POLLABLE);
        pollingConfig = new PollingConfig(100, TimeUnit.MILLISECONDS, 2, TimeUnit.SECONDS);
    }

    @AfterEach
    public void destroy() {
        DatalakeInMemoryStateStore.delete(ID);
    }

    @Test
    public void testPollerTimeout() {
        whenCheckFlowState().thenReturn(FlowState.RUNNING);

        assertThrows(PollerStoppedException.class,
                () -> underTest.pollStartUntilAvailable(sdxCluster, pollingConfig));
    }

    @Test
    public void testCancelledFlow() {
        DatalakeInMemoryStateStore.put(ID, PollGroup.CANCELLED);

        UserBreakException exception = assertThrows(UserBreakException.class,
                () -> underTest.pollStartUntilAvailable(sdxCluster, pollingConfig));

        assertEquals("Start polling cancelled on 'clusterName' cluster.", exception.getMessage());
        verifyNoInteractions(cloudbreakFlowService, stackV4Endpoint, sdxStatusService);
    }

    @Test
    public void testAvailableClusterWhenFlowStateIsUnknown() {
        whenCheckFlowState().thenReturn(FlowState.UNKNOWN);
        whenCheckStackStatus().thenReturn(statusResponse(Status.AVAILABLE, Status.AVAILABLE));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.pollStartUntilAvailable(sdxCluster, pollingConfig);
    }

    @Test
    public void testWhenClusterEventuallyReachesAvailable() {
        whenCheckFlowState().thenReturn(FlowState.UNKNOWN);
        whenCheckStackStatus()
                .thenReturn(statusResponse(Status.UPDATE_IN_PROGRESS, Status.UPDATE_IN_PROGRESS))
                .thenReturn(statusResponse(Status.UPDATE_IN_PROGRESS, Status.UPDATE_IN_PROGRESS))
                .thenReturn(statusResponse(Status.AVAILABLE, Status.AVAILABLE));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.pollStartUntilAvailable(sdxCluster, pollingConfig);
    }

    @Test
    public void testStartFailedStack() {
        whenCheckFlowState().thenReturn(FlowState.UNKNOWN);
        whenCheckStackStatus().thenReturn(statusResponse(Status.START_FAILED, "Stack start failed"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        UserBreakException exception = assertThrows(UserBreakException.class,
                () -> underTest.pollStartUntilAvailable(sdxCluster, pollingConfig));

        assertEquals("Start failed on 'clusterName' cluster. Reason: Stack start failed", exception.getMessage());
    }

    @Test
    public void testStartFailedCluster() {
        whenCheckFlowState().thenReturn(FlowState.UNKNOWN);
        whenCheckStackStatus().thenReturn(statusResponse(Status.AVAILABLE, Status.START_FAILED, "Cluster start failed"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        UserBreakException exception = assertThrows(UserBreakException.class,
                () -> underTest.pollStartUntilAvailable(sdxCluster, pollingConfig));

        assertEquals("Start failed on 'clusterName' cluster. Reason: Cluster start failed", exception.getMessage());
    }

    @Test
    public void testFlowFinishedClusterNotFailedNotAvailable() {
        whenCheckFlowState().thenReturn(FlowState.FINISHED);
        whenCheckStackStatus().thenReturn(statusResponse(Status.UPDATE_IN_PROGRESS, Status.UPDATE_IN_PROGRESS));
        when(sdxStatusService.getShortStatusMessage(any(StackStatusV4Response.class))).thenReturn("testMessage");
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        UserBreakException exception = assertThrows(UserBreakException.class,
                () -> underTest.pollStartUntilAvailable(sdxCluster, pollingConfig));

        assertEquals("Start failed on 'clusterName' cluster. Reason: testMessage", exception.getMessage());
    }

    @Test
    public void testCcmUpgradeFinished() {
        whenCheckFlowState().thenReturn(FlowState.UNKNOWN);
        whenCheckStackStatus()
                .thenReturn(statusResponse(Status.UPGRADE_CCM_IN_PROGRESS, Status.UPGRADE_CCM_IN_PROGRESS))
                .thenReturn(statusResponse(Status.AVAILABLE, Status.AVAILABLE));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.pollCcmUpgradeUntilAvailable(sdxCluster, pollingConfig);
    }

    @Test
    public void testCcmUpgradeFailedStack() {
        whenCheckFlowState().thenReturn(FlowState.UNKNOWN);
        whenCheckStackStatus()
                .thenReturn(statusResponse(Status.UPGRADE_CCM_IN_PROGRESS, Status.UPGRADE_CCM_IN_PROGRESS))
                .thenReturn(statusResponse(Status.UPGRADE_CCM_FAILED, "stack error"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        assertThatThrownBy(() -> underTest.pollCcmUpgradeUntilAvailable(sdxCluster, pollingConfig))
                .hasMessage("CCM upgrade failed on 'clusterName' cluster. Reason: stack error")
                .isInstanceOf(UserBreakException.class);
    }

    @Test
    public void testCcmUpgradeFailedCluster() {
        whenCheckFlowState().thenReturn(FlowState.UNKNOWN);
        whenCheckStackStatus()
                .thenReturn(statusResponse(Status.UPGRADE_CCM_IN_PROGRESS, Status.UPGRADE_CCM_IN_PROGRESS))
                .thenReturn(statusResponse(Status.AVAILABLE, Status.UPGRADE_CCM_FAILED, "cluster error"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        assertThatThrownBy(() -> underTest.pollCcmUpgradeUntilAvailable(sdxCluster, pollingConfig))
                .hasMessage("CCM upgrade failed on 'clusterName' cluster. Reason: cluster error")
                .isInstanceOf(UserBreakException.class);
    }

    private OngoingStubbing<FlowState> whenCheckFlowState() {
        return when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster));
    }

    private OngoingStubbing<StackStatusV4Response> whenCheckStackStatus() {
        return when(stackV4Endpoint.getStatusByName(eq(0L), eq(sdxCluster.getClusterName()), eq(sdxCluster.getAccountId())));
    }

    private StackStatusV4Response statusResponse(Status stackStatus, Status clusterStatus) {
        StackStatusV4Response statusV4Response = new StackStatusV4Response();
        statusV4Response.setStatus(stackStatus);
        statusV4Response.setClusterStatus(clusterStatus);
        return statusV4Response;
    }

    private StackStatusV4Response statusResponse(Status stackStatus, String stackStatusReason) {
        StackStatusV4Response statusV4Response = new StackStatusV4Response();
        statusV4Response.setStatus(stackStatus);
        statusV4Response.setStatusReason(stackStatusReason);
        return statusV4Response;
    }

    private StackStatusV4Response statusResponse(Status stackStatus, Status clusterStatus, String clusterStatusReason) {
        StackStatusV4Response statusV4Response = new StackStatusV4Response();
        statusV4Response.setStatus(stackStatus);
        statusV4Response.setClusterStatus(clusterStatus);
        statusV4Response.setClusterStatusReason(clusterStatusReason);
        return statusV4Response;
    }
}
