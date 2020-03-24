package com.sequenceiq.cloudbreak.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.handler.InstanceStateQuery;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.client.EnvironmentInternalCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.statuschecker.service.JobService;

@RunWith(MockitoJUnitRunner.class)
public class StackStatusCheckerJobTest {

    @InjectMocks
    private StackStatusCheckerJob underTest;

    @Mock
    private JobService jobService;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterOperationService clusterOperationService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Mock
    private InstanceStateQuery instanceStateQuery;

    @Mock
    private EnvironmentInternalCrnClient environmentInternalCrnClient;

    @Mock
    private CredentialConverter credentialConverter;

    @Mock
    private CredentialToCloudCredentialConverter cloudCredentialConverter;

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

    @Mock
    private Stack stack;

    @Mock
    private ClusterStatusResult clusterStatusResult;

    @Mock
    private User user;

    @Mock
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
    }

    @Test
    public void testNotRunningIfFlowInProgress() throws JobExecutionException {
        when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(Boolean.TRUE);
        underTest.executeInternal(jobExecutionContext);

        verify(stackService, times(0)).getByIdWithListsInTransaction(anyLong());
    }

    @Test
    public void testNotRunningIfStackFailedOrBeingDeleted() throws JobExecutionException {
        when(stackService.get(anyLong())).thenReturn(stack);
        when(stack.getStatus()).thenReturn(Status.DELETE_COMPLETED);
        underTest.executeInternal(jobExecutionContext);

        verify(clusterApiConnectors, times(0)).getConnector(stack);
    }

    @Test
    public void testInstanceSyncIfCMNotAccessible() throws JobExecutionException {
        setupForCMNotAccessible();
        underTest.executeInternal(jobExecutionContext);

        verify(instanceStateQuery, times(1)).getCloudVmInstanceStatuses(any(), any(), any());
    }

    @Test
    public void testInstanceSyncIfCMNotAccessibleAndNoInstances() throws JobExecutionException {
        setupForCMNotAccessible();
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong())).thenReturn(Collections.emptySet());
        underTest.executeInternal(jobExecutionContext);

        verify(instanceStateQuery, times(0)).getCloudVmInstanceStatuses(any(), any(), any());
    }

    @Test
    public void testInstanceSyncCMNotRunning() throws JobExecutionException {
        setupForCM();
        underTest.executeInternal(jobExecutionContext);

        verify(clusterOperationService, times(0)).reportHealthChange(anyString(), anySet(), anySet());
        verify(instanceStateQuery, times(1)).getCloudVmInstanceStatuses(any(), any(), any());
    }

    @Test
    public void testInstanceSyncCMRunning() throws JobExecutionException {
        setupForCM();
        when(clusterStatusResult.getClusterStatus()).thenReturn(ClusterStatus.AMBARISERVER_RUNNING);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        underTest.executeInternal(jobExecutionContext);

        verify(clusterOperationService, times(1)).reportHealthChange(any(), anySet(), anySet());
        verify(instanceStateQuery, times(1)).getCloudVmInstanceStatuses(any(), any(), any());
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
        when(stackService.get(anyLong())).thenReturn(stack);
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        when(stack.isStopped()).thenReturn(false);
        when(stack.getId()).thenReturn(1L);
        when(stack.getCreator()).thenReturn(user);
        when(user.getUserId()).thenReturn("1");
        when(stack.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(1L);
        when(environmentInternalCrnClient.withInternalCrn()).thenReturn(environmentServiceCrnEndpoints);
        when(environmentServiceCrnEndpoints.credentialV1Endpoint()).thenReturn(credentialEndpoint);
        when(clusterStatusService.getStatus(anyBoolean())).thenReturn(clusterStatusResult);
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong())).thenReturn(Set.of(instanceMetaData));
        when(instanceMetaData.getInstanceStatus()).thenReturn(InstanceStatus.SERVICES_HEALTHY);
    }

    private void setupForCMNotAccessible() {
        when(stackService.get(anyLong())).thenReturn(stack);
        when(stack.getStatus()).thenReturn(Status.STOPPED);
        when(stack.isStopped()).thenReturn(true);
        when(stack.getId()).thenReturn(1L);
        when(stack.getCreator()).thenReturn(user);
        when(user.getUserId()).thenReturn("1");
        when(stack.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(1L);
        when(environmentInternalCrnClient.withInternalCrn()).thenReturn(environmentServiceCrnEndpoints);
        when(environmentServiceCrnEndpoints.credentialV1Endpoint()).thenReturn(credentialEndpoint);
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong())).thenReturn(Set.of(instanceMetaData));
    }
}