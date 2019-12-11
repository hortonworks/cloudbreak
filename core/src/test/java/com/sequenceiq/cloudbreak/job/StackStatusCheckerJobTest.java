package com.sequenceiq.cloudbreak.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sequenceiq.cloudbreak.cloud.handler.InstanceStateQuery;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.client.EnvironmentInternalCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.flow.core.FlowLogService;

@RunWith(MockitoJUnitRunner.class)
public class StackStatusCheckerJobTest {

    @InjectMocks
    private StackStatusCheckerJob underTest;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Mock
    private StackSyncService stackSyncService;

    @Mock
    private StackService stackService;

    @Mock
    private CredentialConverter credentialConverter;

    @Mock
    private CredentialToCloudCredentialConverter cloudCredentialConverter;

    @Mock
    private EnvironmentInternalCrnClient environmentInternalCrnClient;

    @Mock
    private InstanceStateQuery instanceStateQuery;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterStatusService clusterStatusService;

    @Mock
    private Stack stack;

    @Mock
    private Cluster cluster;

    @Mock
    private ClusterStatusResult clusterStatusResult;

    @Mock
    private Blueprint blueprint;

    @Mock
    private User user;

    @Mock
    private Workspace workspace;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private EnvironmentServiceCrnEndpoints environmentServiceCrnEndpoints;

    @Mock
    private CredentialEndpoint credentialEndpoint;

    @Mock
    private CloudInstance cloudInstance;

    @Before
    public void  init() {
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
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(stack.isStackInDeletionOrFailedPhase()).thenReturn(true);
        underTest.executeInternal(jobExecutionContext);

        verify(clusterApiConnectors, times(0)).getConnector(stack);
    }

    private void setupForCMNotAccessible() {
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(stack.isStackInDeletionOrFailedPhase()).thenReturn(false);
        when(stack.isStopped()).thenReturn(true);
        when(stack.getCreator()).thenReturn(user);
        when(user.getUserId()).thenReturn("1");
        when(stack.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(1L);
        when(environmentInternalCrnClient.withInternalCrn()).thenReturn(environmentServiceCrnEndpoints);
        when(environmentServiceCrnEndpoints.credentialV1Endpoint()).thenReturn(credentialEndpoint);
    }

    @Test
    public void testInstanceSyncIfCMNotAccessible() throws JobExecutionException {
        setupForCMNotAccessible();
        when(cloudInstanceConverter.convert(anyList())).thenReturn(List.of(cloudInstance));
        underTest.executeInternal(jobExecutionContext);

        verify(instanceStateQuery, times(1)).getCloudVmInstanceStatuses(any(), any(), any());
    }

    @Test
    public void testInstanceSyncIfCMNotAccessibleAndNoInstances() throws JobExecutionException {
        setupForCMNotAccessible();
        when(cloudInstanceConverter.convert(anyList())).thenReturn(Collections.emptyList());
        underTest.executeInternal(jobExecutionContext);

        verify(instanceStateQuery, times(0)).getCloudVmInstanceStatuses(any(), any(), any());
    }

    private void setupForCM() {
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(stack.isStackInDeletionOrFailedPhase()).thenReturn(false);
        when(stack.isStopped()).thenReturn(false);
        when(stack.getCreator()).thenReturn(user);
        when(user.getUserId()).thenReturn("1");
        when(stack.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(1L);
        when(environmentInternalCrnClient.withInternalCrn()).thenReturn(environmentServiceCrnEndpoints);
        when(environmentServiceCrnEndpoints.credentialV1Endpoint()).thenReturn(credentialEndpoint);
        when(clusterService.retrieveClusterByStackIdWithoutAuth(anyLong())).thenReturn(Optional.of(cluster));
        when(cluster.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getStackName()).thenReturn("name");
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.getStatus(anyBoolean())).thenReturn(clusterStatusResult);
        when(cloudInstanceConverter.convert(anyList())).thenReturn(List.of(cloudInstance));
    }

    @Test
    public void testInstanceSyncCMNotRunning() throws JobExecutionException {
        setupForCM();
        when(clusterStatusResult.getClusterStatus()).thenReturn(ClusterStatus.AMBARISERVER_NOT_RUNNING);
        underTest.executeInternal(jobExecutionContext);

        verify(clusterService, times(0)).reportHealthChange(anyString(), anySet(), anySet());
        verify(instanceStateQuery, times(1)).getCloudVmInstanceStatuses(any(), any(), any());
    }

    @Test
    public void testInstanceSyncCMRunning() throws JobExecutionException {
        setupForCM();
        when(clusterStatusResult.getClusterStatus()).thenReturn(ClusterStatus.AMBARISERVER_RUNNING);
        underTest.executeInternal(jobExecutionContext);

        verify(clusterService, times(1)).reportHealthChange(any(), anySet(), anySet());
        verify(instanceStateQuery, times(1)).getCloudVmInstanceStatuses(any(), any(), any());
    }
}