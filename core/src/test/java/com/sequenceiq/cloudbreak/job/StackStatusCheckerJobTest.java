package com.sequenceiq.cloudbreak.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceStatusChecker;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;

@RunWith(MockitoJUnitRunner.class)
public class StackStatusCheckerJobTest {

    @InjectMocks
    private StackStatusCheckerJob underTest;

    @Mock
    private StatusCheckerJobService jobService;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterOperationService clusterOperationService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackInstanceStatusChecker stackInstanceStatusChecker;

    @Mock
    private StackSyncService stackSyncService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterStatusService clusterStatusService;

    private Stack stack;

    @Mock
    private ClusterStatusResult clusterStatusResult;

    private User user;

    private Workspace workspace;

    @Mock
    private EnvironmentServiceCrnEndpoints environmentServiceCrnEndpoints;

    @Mock
    private CredentialEndpoint credentialEndpoint;

    @Mock
    private InstanceMetaData instanceMetaData;

    @Before
    public void init() {
        when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(Boolean.FALSE);
        underTest.setLocalId("1");
        underTest.setRemoteResourceCrn("remote:crn");

        stack = new Stack();
        stack.setId(1L);
        workspace = new Workspace();
        workspace.setId(1L);
        stack.setWorkspace(workspace);
        user = new User();
        user.setUserId("1");
        stack.setCreator(user);

        when(stackService.get(anyLong())).thenReturn(stack);
    }

    @After
    public void tearDown() {
        validateMockitoUsage();
    }

    @Test
    public void testNotRunningIfFlowInProgress() throws JobExecutionException {
        when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(Boolean.TRUE);
        underTest.executeInternal(jobExecutionContext);

        verify(stackService, times(0)).getByIdWithListsInTransaction(anyLong());
    }

    @Test
    public void testNotRunningIfStackFailedOrBeingDeleted() throws JobExecutionException {
        setStackStatus(DetailedStackStatus.DELETE_COMPLETED);
        underTest.executeInternal(jobExecutionContext);

        verify(clusterApiConnectors, times(0)).getConnector(stack);
    }

    @Test
    public void testInstanceSyncIfCMNotAccessible() throws JobExecutionException {
        setupForCMNotAccessible();
        underTest.executeInternal(jobExecutionContext);

        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stack), any());
    }

    @Test
    public void testInstanceSyncCMNotRunning() throws JobExecutionException {
        setupForCM();
        underTest.executeInternal(jobExecutionContext);

        verify(clusterOperationService, times(0)).reportHealthChange(anyString(), anySet(), anySet());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stack), any());
    }

    @Test
    public void testInstanceSyncCMRunning() throws JobExecutionException {
        setupForCM();
        when(clusterStatusResult.getClusterStatus()).thenReturn(ClusterStatus.CLUSTERMANAGER_RUNNING);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        underTest.executeInternal(jobExecutionContext);

        verify(clusterOperationService, times(1)).reportHealthChange(any(), anySet(), anySet());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stack), any());
    }

    @Test
    public void testHandledAllStatesSeparately() {
        Set<Status> unshedulableStates = underTest.unshedulableStates();
        Set<Status> ignoredStates = underTest.ignoredStates();
        Set<Status> syncableStates = underTest.syncableStates();

        assertTrue(Sets.intersection(unshedulableStates, ignoredStates).isEmpty());
        assertTrue(Sets.intersection(unshedulableStates, syncableStates).isEmpty());
        assertTrue(Sets.intersection(ignoredStates, syncableStates).isEmpty());

        Set<Status> allPossibleStates = EnumSet.allOf(Status.class);
        Set<Status> allHandledStates = EnumSet.copyOf(unshedulableStates);
        allHandledStates.addAll(ignoredStates);
        allHandledStates.addAll(syncableStates);
        assertEquals(allPossibleStates, allHandledStates);
    }

    private void setupForCM() {
        setStackStatus(DetailedStackStatus.AVAILABLE);
        when(clusterStatusService.getStatus(anyBoolean())).thenReturn(clusterStatusResult);
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong())).thenReturn(Set.of(instanceMetaData));
        when(instanceMetaData.getInstanceStatus()).thenReturn(InstanceStatus.SERVICES_HEALTHY);
    }

    private void setupForCMNotAccessible() {
        setStackStatus(DetailedStackStatus.STOPPED);
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong())).thenReturn(Set.of(instanceMetaData));
    }

    private void setStackStatus(DetailedStackStatus detailedStackStatus) {
        stack.setStackStatus(new StackStatus(stack, detailedStackStatus));
    }
}
