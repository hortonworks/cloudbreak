package com.sequenceiq.cloudbreak.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceStatusChecker;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.core.FlowLogService;

import io.opentracing.Tracer;

@RunWith(MockitoJUnitRunner.class)
public class StackStatusCheckerJobTest {

    private static final String BLUEPRINT_TEXT = "blueprintText";

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
    private InstanceMetaData instanceMetaData;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private Cluster cluster;

    @Mock
    private Blueprint blueprint;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Before
    public void init() {
        Tracer tracer = Mockito.mock(Tracer.class);
        underTest = new StackStatusCheckerJob(tracer);
        MockitoAnnotations.openMocks(this);
        when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(Boolean.FALSE);
        underTest.setLocalId("1");
        underTest.setRemoteResourceCrn("remote:crn");

        stack = new Stack();
        stack.setId(1L);
        stack.setResourceCrn("crn:cdp:datahub:us-west-1:acc1:stack:cluster1");
        workspace = new Workspace();
        workspace.setId(1L);
        stack.setWorkspace(workspace);
        stack.setCluster(cluster);
        user = new User();
        user.setUserId("1");
        stack.setCreator(user);
        stack.setCloudPlatform("AWS");

        when(stackService.get(anyLong())).thenReturn(stack);
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(new JobDataMap());
        when(cluster.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getBlueprintText()).thenReturn(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
    }

    @After
    public void tearDown() {
        validateMockitoUsage();
    }

    @Test
    public void testNotRunningIfFlowInProgress() throws JobExecutionException {
        when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(Boolean.TRUE);
        underTest.executeTracedJob(jobExecutionContext);

        verify(stackService, times(0)).getByIdWithListsInTransaction(anyLong());
    }

    @Test
    public void testNotRunningIfStackFailedOrBeingDeleted() throws JobExecutionException {
        setStackStatus(DetailedStackStatus.DELETE_COMPLETED);
        underTest.executeTracedJob(jobExecutionContext);

        verify(clusterApiConnectors, times(0)).getConnector(stack);
    }

    @Test
    public void testInstanceSyncIfCMNotAccessible() throws JobExecutionException {
        setupForCMNotAccessible();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.datahub()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.executeTracedJob(jobExecutionContext);

        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stack), any());
    }

    @Test
    public void testInstanceSyncCMNotRunning() throws JobExecutionException {
        setupForCM();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.datahub()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.executeTracedJob(jobExecutionContext);

        verify(clusterOperationService, times(0)).reportHealthChange(anyString(), any(), anySet());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stack), any());
    }

    @Test
    public void testInstanceSyncCMRunning() throws JobExecutionException {
        setupForCM();
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.datahub()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.executeTracedJob(jobExecutionContext);

        verify(clusterOperationService, times(1)).reportHealthChange(any(), any(), anySet());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stack), any());
        verify(clusterService, times(1)).updateClusterCertExpirationState(stack.getCluster(), true);
    }

    @Test
    public void testInstanceSyncCMRunningNodeStopped() throws JobExecutionException {
        setupForCM();
        Set<HealthCheck> healthChecks = Sets.newHashSet(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.UNHEALTHY, Optional.empty()),
                new HealthCheck(HealthCheckType.CERT, HealthCheckResult.UNHEALTHY, Optional.empty()));
        ExtendedHostStatuses extendedHostStatuses = new ExtendedHostStatuses(Map.of(HostName.hostName("host1"), healthChecks));
        when(clusterStatusService.getExtendedHostStatuses(any())).thenReturn(extendedHostStatuses);
        when(instanceMetaData.getInstanceStatus()).thenReturn(InstanceStatus.STOPPED);
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn("host1");
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.datahub()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.executeTracedJob(jobExecutionContext);

        verify(clusterOperationService, times(1)).reportHealthChange(any(), any(), anySet());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stack), any());
        verify(clusterService, times(1)).updateClusterCertExpirationState(stack.getCluster(), true);
        verify(clusterService, times(1)).updateClusterStatusByStackId(stack.getId(), DetailedStackStatus.NODE_FAILURE);
    }

    @Test
    public void testInstanceSyncCMRunningNodeStoppedAndStopStartEnabledInstanceStopped() throws JobExecutionException {
        internalTestInstanceSyncStopStart("compute", InstanceStatus.STOPPED, DetailedStackStatus.AVAILABLE);
    }

    @Test
    public void testInstanceSyncCMRunningNodeStoppedAndStopStartEnabledInstanceUnhealthy() throws JobExecutionException {
        internalTestInstanceSyncStopStart("compute", InstanceStatus.SERVICES_UNHEALTHY, DetailedStackStatus.NODE_FAILURE);
    }

    @Test
    public void testInstanceSyncCMRunningNodeStoppedAndStopStartEnabledOtherHgStopped() throws JobExecutionException {
        internalTestInstanceSyncStopStart("notcompute", InstanceStatus.STOPPED, DetailedStackStatus.NODE_FAILURE);
    }

    @Test
    public void testInstanceSyncCMRunningNodeStoppedAndStopStartEnabledOtherHgUnhealthy() throws JobExecutionException {
        internalTestInstanceSyncStopStart("notcompute", InstanceStatus.SERVICES_UNHEALTHY, DetailedStackStatus.NODE_FAILURE);
    }

    private void internalTestInstanceSyncStopStart(String instanceHgName, InstanceStatus instanceStatus, DetailedStackStatus expected)
            throws JobExecutionException {
        setupForCM();
        Set<HealthCheck> healthChecks = Sets.newHashSet(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.UNHEALTHY, Optional.empty()),
                new HealthCheck(HealthCheckType.CERT, HealthCheckResult.UNHEALTHY, Optional.empty()));
        ExtendedHostStatuses extendedHostStatuses = new ExtendedHostStatuses(Map.of(HostName.hostName("host1"), healthChecks));
        when(clusterStatusService.getExtendedHostStatuses(any())).thenReturn(extendedHostStatuses);
        when(instanceMetaData.getInstanceStatus()).thenReturn(instanceStatus);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.datahub()).thenReturn(regionAwareInternalCrnGenerator);
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn("host1");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(instanceHgName);
        when(instanceMetaData.getInstanceGroup()).thenReturn(instanceGroup);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(stackUtil.stopStartScalingEntitlementEnabled(any())).thenReturn(true);
        Set<String> computeGroups = new HashSet<>();
        computeGroups.add("compute");
        when(cmTemplateProcessor.getComputeHostGroups(any())).thenReturn(computeGroups);
        underTest.executeTracedJob(jobExecutionContext);

        verify(clusterOperationService, times(1)).reportHealthChange(any(), any(), anySet());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stack), any());
        verify(clusterService, times(1)).updateClusterCertExpirationState(stack.getCluster(), true);
        verify(clusterService, times(1)).updateClusterStatusByStackId(stack.getId(), expected);
    }

    @Test
    public void testHandledAllStatesSeparately() {
        Set<Status> unshedulableStates = Status.getUnschedulableStatuses();
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
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(true);
        Set<HealthCheck> healthChecks = Sets.newHashSet(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.HEALTHY, Optional.empty()),
                new HealthCheck(HealthCheckType.CERT, HealthCheckResult.UNHEALTHY, Optional.empty()));
        ExtendedHostStatuses extendedHostStatuses = new ExtendedHostStatuses(Map.of(HostName.hostName("host1"), healthChecks));
        when(clusterStatusService.getExtendedHostStatuses(any())).thenReturn(extendedHostStatuses);
        when(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(anyLong())).thenReturn(Set.of(instanceMetaData));
        when(instanceMetaData.getInstanceStatus()).thenReturn(InstanceStatus.SERVICES_HEALTHY);
    }

    private void setupForCMNotAccessible() {
        setStackStatus(DetailedStackStatus.STOPPED);
        when(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(anyLong())).thenReturn(Set.of(instanceMetaData));
    }

    private void setStackStatus(DetailedStackStatus detailedStackStatus) {
        stack.setStackStatus(new StackStatus(stack, detailedStackStatus));
    }
}
