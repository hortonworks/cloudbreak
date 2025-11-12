package com.sequenceiq.cloudbreak.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.metrics.MetricsClient;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.ServiceStatusCheckerLogLocationDecorator;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceStatusChecker;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;

@ExtendWith(MockitoExtension.class)
public class StackStatusCheckerJobTest {

    private static final String BLUEPRINT_TEXT = "blueprintText";

    @InjectMocks
    private StackStatusCheckerJob underTest;

    @Mock
    private StatusCheckerJobService jobService;

    @Mock
    private StackDtoService stackDtoService;

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

    @Spy
    private StackDto stackDto;

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
    private MetricsClient metricsClient;

    @Mock
    private ServiceStatusCheckerLogLocationDecorator serviceStatusCheckerLogLocationDecorator;

    @Mock
    private Clock clock;

    @BeforeEach
    public void init() {
        underTest = new StackStatusCheckerJob();
        MockitoAnnotations.openMocks(this);
        lenient().when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(Boolean.FALSE);
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

        lenient().when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        lenient().when(stackDto.getStack()).thenReturn(stack);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
        lenient().when(jobExecutionContext.getMergedJobDataMap()).thenReturn(new JobDataMap());
        lenient().when(stackDto.getBlueprint()).thenReturn(blueprint);
        lenient().when(blueprint.getBlueprintJsonText()).thenReturn(BLUEPRINT_TEXT);
        lenient().when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        lenient().when(stackDtoService.computeMonitoringEnabled(any())).thenReturn(Optional.of(true));
    }

    @AfterEach
    public void tearDown() {
        validateMockitoUsage();
    }

    @Test
    public void testNotRunningIfFlowEndedIn2Minutes() {
        FlowLog lastFlowLog = new FlowLog();
        lastFlowLog.setEndTime(1676030290000L);
        when(flowLogService.getLastFlowLogWithEndTime(anyLong())).thenReturn(Optional.of(lastFlowLog));
        ArgumentCaptor<TemporalAmount> temporalAmountArgumentCaptor = ArgumentCaptor.forClass(TemporalAmount.class);
        Instant nowMinus2Minutes = Instant.ofEpochMilli(1676030245000L);
        when(clock.nowMinus(temporalAmountArgumentCaptor.capture())).thenReturn(nowMinus2Minutes);
        ReflectionTestUtils.setField(underTest, "skipWindow", 2);

        underTest.executeJob(jobExecutionContext);

        assertEquals(120, ((Duration) temporalAmountArgumentCaptor.getValue()).getSeconds());
        verify(stackDtoService, times(0)).getById(anyLong());
    }

    @Test
    public void testNotRunningIfFlowEndedAtTheSameTime() {
        FlowLog lastFlowLog = new FlowLog();
        lastFlowLog.setEndTime(1676030290000L);
        when(flowLogService.getLastFlowLogWithEndTime(anyLong())).thenReturn(Optional.of(lastFlowLog));
        ArgumentCaptor<TemporalAmount> temporalAmountArgumentCaptor = ArgumentCaptor.forClass(TemporalAmount.class);
        Instant nowMinus2Minutes = Instant.ofEpochMilli(1676030290000L);
        when(clock.nowMinus(temporalAmountArgumentCaptor.capture())).thenReturn(nowMinus2Minutes);
        ReflectionTestUtils.setField(underTest, "skipWindow", 2);

        underTest.executeJob(jobExecutionContext);

        assertEquals(120, ((Duration) temporalAmountArgumentCaptor.getValue()).getSeconds());
        verify(stackDtoService, times(1)).getById(anyLong());
    }

    @Test
    public void testNotRunningIfFlowEndedInTime() {
        FlowLog lastFlowLog = new FlowLog();
        lastFlowLog.setEndTime(1676030290000L);
        when(flowLogService.getLastFlowLogWithEndTime(anyLong())).thenReturn(Optional.of(lastFlowLog));
        ArgumentCaptor<TemporalAmount> temporalAmountArgumentCaptor = ArgumentCaptor.forClass(TemporalAmount.class);
        Instant nowMinus2Minutes = Instant.ofEpochMilli(1676030005000L);
        when(clock.nowMinus(temporalAmountArgumentCaptor.capture())).thenReturn(nowMinus2Minutes);
        ReflectionTestUtils.setField(underTest, "skipWindow", 2);

        underTest.executeJob(jobExecutionContext);

        assertEquals(120, ((Duration) temporalAmountArgumentCaptor.getValue()).getSeconds());
        verify(stackDtoService, times(0)).getById(anyLong());
    }

    @Test
    public void testNotRunningIfFlowInProgress() {
        when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(Boolean.TRUE);
        underTest.executeJob(jobExecutionContext);

        verify(stackDtoService, times(0)).getById(anyLong());
    }

    @Test
    public void testNotRunningIfStackFailedOrBeingDeleted() {
        setStackStatus(DetailedStackStatus.DELETE_COMPLETED);
        underTest.executeJob(jobExecutionContext);

        verify(metricsClient, times(1)).processStackStatus(anyString(), anyString(), anyString(), anyInt(), any());
        verify(clusterApiConnectors, times(0)).getConnector(stackDto);
    }

    @Test
    public void testInstanceSyncIfCMNotAccessible() {
        setupForCMNotAccessible();
        underTest.executeJob(jobExecutionContext);

        verify(metricsClient, times(1)).processStackStatus(anyString(), anyString(), anyString(), anyInt(), any());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stackDto), any());
    }

    @Test
    public void testInstanceSyncCMNotRunning() {
        setupForCM();
        underTest.executeJob(jobExecutionContext);

        verify(metricsClient, times(1)).processStackStatus(anyString(), anyString(), anyString(), anyInt(), any());
        verify(clusterOperationService, times(0)).reportHealthChange(anyString(), any(), anySet());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stackDto), any());
    }

    @Test
    public void testInstanceSyncCMRunning() {
        setupForCM();
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        underTest.executeJob(jobExecutionContext);

        verify(metricsClient, times(1)).processStackStatus(anyString(), anyString(), anyString(), anyInt(), any());
        verify(clusterOperationService, times(1)).reportHealthChange(any(), any(), anySet());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stackDto), any());
        verify(clusterService, times(1)).updateClusterCertExpirationState(stack.getCluster(), true, "");
    }

    @Test
    public void testInstanceSyncCMRunningNodeStopped() {
        setupForCM();
        Set<HealthCheck> healthChecks = Sets.newHashSet(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.UNHEALTHY, Optional.empty(), Optional.empty()),
                new HealthCheck(HealthCheckType.CERT, HealthCheckResult.UNHEALTHY, Optional.empty(), Optional.empty()));
        ExtendedHostStatuses extendedHostStatuses = new ExtendedHostStatuses(Map.of(HostName.hostName("host1"), healthChecks));
        when(clusterStatusService.getExtendedHostStatuses(any())).thenReturn(extendedHostStatuses);
        when(instanceMetaData.getInstanceStatus()).thenReturn(InstanceStatus.STOPPED);
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn("host1");
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(serviceStatusCheckerLogLocationDecorator.decorate(any(), any(), any())).thenAnswer(i -> i.getArgument(0));
        underTest.executeJob(jobExecutionContext);

        verify(clusterOperationService, times(1)).reportHealthChange(any(), any(), anySet());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stackDto), any());
        verify(clusterService, times(1)).updateClusterCertExpirationState(stack.getCluster(), true, "");
        verify(clusterService, times(1)).updateClusterStatusByStackId(stack.getId(), DetailedStackStatus.NODE_FAILURE);
    }

    @Test
    public void testInstanceSyncCMRunningNodeStoppedAndStopStartEnabledInstanceStopped() {
        internalTestInstanceSyncStopStart("compute", InstanceStatus.STOPPED, DetailedStackStatus.AVAILABLE);
    }

    @Test
    public void testInstanceSyncCMRunningNodeStoppedAndStopStartEnabledInstanceUnhealthy() {
        internalTestInstanceSyncStopStart("compute", InstanceStatus.SERVICES_UNHEALTHY, DetailedStackStatus.NODE_FAILURE);
    }

    @Test
    public void testInstanceSyncCMRunningNodeStoppedAndStopStartEnabledOtherHgStopped() {
        internalTestInstanceSyncStopStart("notcompute", InstanceStatus.STOPPED, DetailedStackStatus.NODE_FAILURE);
    }

    @Test
    public void testInstanceSyncCMRunningNodeStoppedAndStopStartEnabledOtherHgUnhealthy() {
        internalTestInstanceSyncStopStart("notcompute", InstanceStatus.SERVICES_UNHEALTHY, DetailedStackStatus.NODE_FAILURE);
    }

    private void internalTestInstanceSyncStopStart(String instanceHgName, InstanceStatus instanceStatus, DetailedStackStatus expected) {
        setupForCM();
        Set<HealthCheck> healthChecks = Sets.newHashSet(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.UNHEALTHY, Optional.empty(), Optional.empty()),
                new HealthCheck(HealthCheckType.CERT, HealthCheckResult.UNHEALTHY, Optional.empty(), Optional.empty()));
        ExtendedHostStatuses extendedHostStatuses = new ExtendedHostStatuses(Map.of(HostName.hostName("host1"), healthChecks));
        when(clusterStatusService.getExtendedHostStatuses(any())).thenReturn(extendedHostStatuses);
        when(instanceMetaData.getInstanceStatus()).thenReturn(instanceStatus);
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn("host1");
        lenient().when(instanceMetaData.getInstanceGroupName()).thenReturn(instanceHgName);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(stackUtil.stopStartScalingEntitlementEnabled(any())).thenReturn(true);
        Set<String> computeGroups = new HashSet<>();
        computeGroups.add("compute");
        when(cmTemplateProcessor.getComputeHostGroups(any())).thenReturn(computeGroups);
        when(serviceStatusCheckerLogLocationDecorator.decorate(any(), any(), any())).thenAnswer(i -> i.getArgument(0));
        underTest.executeJob(jobExecutionContext);

        verify(clusterOperationService, times(1)).reportHealthChange(any(), any(), anySet());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stackDto), any());
        verify(clusterService, times(1)).updateClusterCertExpirationState(stack.getCluster(), true, "");
        verify(clusterService, times(1)).updateClusterStatusByStackId(stack.getId(), expected);
    }

    @Test
    public void testHandledAllStatesSeparately() {
        Set<Status> unschedulableStates = Status.getUnschedulableStatuses();
        Set<Status> ignoredStates = StackStatusCheckerJob.IGNORED_STATES;
        Set<Status> syncableStates = StackStatusCheckerJob.SYNCABLE_STATES;

        assertTrue(Sets.intersection(unschedulableStates, ignoredStates).isEmpty());
        assertTrue(Sets.intersection(unschedulableStates, syncableStates).isEmpty());
        assertTrue(Sets.intersection(ignoredStates, syncableStates).isEmpty());

        Set<Status> allPossibleStates = EnumSet.allOf(Status.class);
        Set<Status> allHandledStates = EnumSet.copyOf(unschedulableStates);
        allHandledStates.addAll(ignoredStates);
        allHandledStates.addAll(syncableStates);
        assertEquals(allPossibleStates, allHandledStates);
    }

    private void setupForCM() {
        setStackStatus(DetailedStackStatus.AVAILABLE);
        lenient().when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        lenient().when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(true);
        Set<HealthCheck> healthChecks = Sets.newHashSet(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.HEALTHY, Optional.empty(), Optional.empty()),
                new HealthCheck(HealthCheckType.CERT, HealthCheckResult.UNHEALTHY, Optional.empty(), Optional.empty()));
        ExtendedHostStatuses extendedHostStatuses = new ExtendedHostStatuses(Map.of(HostName.hostName("host1"), healthChecks));
        lenient().when(clusterStatusService.getExtendedHostStatuses(any())).thenReturn(extendedHostStatuses);
        lenient().when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of(instanceMetaData));
        lenient().when(instanceMetaData.getInstanceStatus()).thenReturn(InstanceStatus.SERVICES_HEALTHY);
    }

    private void setupForCMNotAccessible() {
        setStackStatus(DetailedStackStatus.STOPPED);
        when(instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of(instanceMetaData));
    }

    private void setStackStatus(DetailedStackStatus detailedStackStatus) {
        stack.setStackStatus(new StackStatus(stack, detailedStackStatus));
    }
}
