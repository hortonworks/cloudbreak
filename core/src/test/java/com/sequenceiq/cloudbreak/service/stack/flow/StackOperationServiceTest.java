package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.STOP_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_START_IGNORED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.datalake.DataLakeStatusCheckerService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.spot.SpotInstanceUsageCondition;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
public class StackOperationServiceTest {

    @InjectMocks
    private StackOperationService underTest;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private CommonPermissionCheckingUtils permissionCheckingUtils;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private ClusterOperationService clusterOperationService;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private DataLakeStatusCheckerService statusCheckerService;

    @Mock
    private SpotInstanceUsageCondition spotInstanceUsageCondition;

    @Mock
    private StackStopRestrictionService stackStopRestrictionService;

    @Mock
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    @Spy
    private UpdateNodeCountValidator updateNodeCountValidator = new UpdateNodeCountValidator();

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ClusterBootstrapper clusterBootstrapper;

    @Test
    public void testStartWhenStackAvailable() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        stack.setId(1L);

        underTest.start(stack);

        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), STACK_START_IGNORED);
    }

    @ParameterizedTest(name = "{0}: With stackStatus={1}")
    @MethodSource("stackStatusForStop")
    public void testStop(String methodName, DetailedStackStatus stackStatus) {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, stackStatus));
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.NONE);
        // On demand instances
        when(spotInstanceUsageCondition.isStackRunsOnSpotInstances(stack)).thenReturn(false);
        when(stackService.getByIdWithLists(stack.getId())).thenReturn(stack);
        when(clusterService.findOneWithLists(cluster.getId())).thenReturn(Optional.of(cluster));

        underTest.updateStatus(stack.getId(), StatusRequest.STOPPED, true);

        if (stackStatus == STOP_FAILED) {
            verify(flowManager).triggerStackStop(stack.getId());
        } else {
            verify(clusterOperationService).updateStatus(stack.getId(), StatusRequest.STOPPED);
        }
    }

    @Test
    public void testStartWhenStackStopped() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, STOPPED));
        stack.setId(1L);

        underTest.start(stack);

        verify(flowManager, times(1)).triggerStackStart(stack.getId());
    }

    @Test
    public void testStartWhenStackStartFailed() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.START_FAILED));
        stack.setId(1L);

        underTest.start(stack);

        verify(flowManager, times(1)).triggerStackStart(stack.getId());
    }

    @Test
    public void testStartWhenStackStopFailed() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.STOP_FAILED));
        stack.setId(1L);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.start(stack));
        assertEquals("Can't start the cluster because it is in STOP_FAILED state.",
                badRequestException.getMessage());
    }

    @Test
    public void testStartWhenClusterStopFailed() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, Status.STOPPED, "", STOPPED));
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        underTest.start(stack);
        verify(flowManager, times(1)).triggerStackStart(stack.getId());
    }

    @Test
    public void shouldNotTriggerStopWhenStackRunsOnSpotInstances() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));

        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.NONE);
        when(spotInstanceUsageCondition.isStackRunsOnSpotInstances(stack)).thenReturn(true);
        when(stackService.getByIdWithLists(stack.getId())).thenReturn(stack);

        assertThatThrownBy(() -> underTest.updateStatus(stack.getId(), StatusRequest.STOPPED, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(String.format("Cannot update the status of stack '%s' to STOPPED, because it runs on spot instances", stack.getName()));
        verify(stackUpdater, never()).updateStackStatus(any(), any(DetailedStackStatus.class));
    }

    @Test
    public void testStartWhenCheckCallEnvironmentCheck() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, STOPPED));
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        underTest.start(stack);
        verify(environmentService).checkEnvironmentStatus(stack, EnvironmentStatus.startable());
    }

    @Test
    public void testTriggerStackStopIfNeededWhenCheckCallEnvironmentCheck() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        when(spotInstanceUsageCondition.isStackRunsOnSpotInstances(stack)).thenReturn(false);
        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.NONE);
        underTest.triggerStackStopIfNeeded(stack, cluster, true);
        verify(environmentService).checkEnvironmentStatus(stack, EnvironmentStatus.stoppable());
    }

    @Test
    public void testUpdateNodeCountWhenCheckCallEnvironmentCheck() throws TransactionService.TransactionExecutionException {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        InstanceGroupAdjustmentV4Request adjustment = new InstanceGroupAdjustmentV4Request();

        when(transactionService.required(any(Supplier.class))).thenReturn(null);

        underTest.updateNodeCount(stack, adjustment, false);
        verify(environmentService).checkEnvironmentStatus(stack, EnvironmentStatus.upscalable());
    }

    @Test
    public void testUpdateNodeCountAndCheckDownscaleAndUpscaleStatusChange() throws TransactionService.TransactionExecutionException {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        InstanceGroupAdjustmentV4Request upscaleAdjustment = new InstanceGroupAdjustmentV4Request();
        upscaleAdjustment.setScalingAdjustment(5);

        when(transactionService.required(any(Supplier.class))).thenAnswer(ans -> ((Supplier) ans.getArgument(0)).get());
        when(stackService.getByIdWithLists(stack.getId())).thenReturn(stack);
        when(targetedUpscaleSupportService.targetedUpscaleOperationSupported(any())).thenReturn(Boolean.TRUE);
        doNothing().when(updateNodeCountValidator).validateServiceRoles(any(), any(InstanceGroupAdjustmentV4Request.class));
        doNothing().when(updateNodeCountValidator).validateInstanceGroup(any(), any());
        doNothing().when(updateNodeCountValidator).validateScalabilityOfInstanceGroup(any(), any(InstanceGroupAdjustmentV4Request.class));
        doNothing().when(updateNodeCountValidator).validateScalingAdjustment(any(InstanceGroupAdjustmentV4Request.class), any());
        doNothing().when(updateNodeCountValidator).validateHostGroupIsPresent(any(InstanceGroupAdjustmentV4Request.class), any());

        underTest.updateNodeCount(stack, upscaleAdjustment, true);
        verify(stackUpdater).updateStackStatus(stack.getId(), DetailedStackStatus.UPSCALE_REQUESTED,
                "Requested node count for upscaling: " + upscaleAdjustment.getScalingAdjustment());
        verify(flowManager).triggerStackUpscale(stack.getId(), upscaleAdjustment, true);
        verify(updateNodeCountValidator, times(0)).validateInstanceStatuses(any(), any());
        verify(updateNodeCountValidator, times(0)).validataHostMetadataStatuses(any(), any());

        InstanceGroupAdjustmentV4Request downscaleAdjustment = new InstanceGroupAdjustmentV4Request();
        downscaleAdjustment.setScalingAdjustment(-5);
        underTest.updateNodeCount(stack, downscaleAdjustment, true);
        verify(stackUpdater).updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_REQUESTED,
                "Requested node count for downscaling: " + 5);
        verify(flowManager).triggerStackDownscale(stack.getId(), downscaleAdjustment);

        when(targetedUpscaleSupportService.targetedUpscaleOperationSupported(any())).thenReturn(Boolean.FALSE);
        underTest.updateNodeCount(stack, upscaleAdjustment, true);
        verify(updateNodeCountValidator, times(2)).validateInstanceStatuses(any(), any());
        verify(updateNodeCountValidator, times(2)).validataHostMetadataStatuses(any(), any());


    }

    @ParameterizedTest(name = "{0}: With stackStatus={1}")
    @MethodSource("stackStatusForUpdateNodeCount")
    public void testUpdateNodeCountStartInstances(String methodName, DetailedStackStatus stackStatus) throws TransactionService.TransactionExecutionException {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, stackStatus));
        Cluster cluster = new Cluster();
        cluster.setStatus(Status.AVAILABLE);
        stack.setCluster(cluster);

        when(stackService.getByIdWithLists(stack.getId())).thenReturn(stack);

        InstanceGroupAdjustmentV4Request upscaleAdjustment = new InstanceGroupAdjustmentV4Request();
        upscaleAdjustment.setScalingAdjustment(5);

        when(transactionService.required(any(Supplier.class))).thenAnswer(ans -> ((Supplier) ans.getArgument(0)).get());
        doNothing().when(updateNodeCountValidator).validateServiceRoles(any(), any(InstanceGroupAdjustmentV4Request.class));
        if (stackStatus != CLUSTER_UPGRADE_FAILED) {
            doNothing().when(updateNodeCountValidator).validateInstanceGroup(any(), any());
            doNothing().when(updateNodeCountValidator).validateScalabilityOfInstanceGroup(any(), any(InstanceGroupAdjustmentV4Request.class));
            doNothing().when(updateNodeCountValidator).validateScalingAdjustment(any(InstanceGroupAdjustmentV4Request.class), any());
            doNothing().when(updateNodeCountValidator).validateHostGroupIsPresent(any(InstanceGroupAdjustmentV4Request.class), any());
            doNothing().when(updateNodeCountValidator).validateInstanceGroupForStopStart(any(), any(), anyInt());
        }

        // Regular
        try {
            underTest.updateNodeCountStartInstances(stack, upscaleAdjustment, true, ScalingStrategy.STOPSTART);
            String expectedStatusReason = "Requested node count for upscaling (stopstart): " + upscaleAdjustment.getScalingAdjustment();
            verify(stackUpdater).updateStackStatus(stack.getId(), DetailedStackStatus.UPSCALE_BY_START_REQUESTED, expectedStatusReason);
            verify(flowManager).triggerStopStartStackUpscale(stack.getId(), upscaleAdjustment, true);
        } catch (Exception e) {
            assertSame(CLUSTER_UPGRADE_FAILED, stackStatus);
            assertSame(BadRequestException.class, e.getClass());
        }

        // Somehow invoked with a negative value
        upscaleAdjustment.setScalingAdjustment(-1);
        assertThrows(BadRequestException.class,
                () -> underTest.updateNodeCountStartInstances(stack, upscaleAdjustment, true, ScalingStrategy.STOPSTART));

        upscaleAdjustment.setScalingAdjustment(0);
        assertThrows(BadRequestException.class,
                () -> underTest.updateNodeCountStartInstances(stack, upscaleAdjustment, true, ScalingStrategy.STOPSTART));
    }

    public static Stream<Arguments> stackStatusForUpdateNodeCount() {
        return Stream.of(
                Arguments.of("Stack is available", AVAILABLE),
                Arguments.of("Stack upgrade failure", CLUSTER_UPGRADE_FAILED)
        );
    }

    public static Stream<Arguments> stackStatusForStop() {
        return Stream.of(
                Arguments.of("Stack is available", AVAILABLE),
                Arguments.of("Stack failed to stop", STOP_FAILED)
        );
    }

    @Test
    public void testRemoveInstances() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        Cluster cluster = new Cluster();
        cluster.setStatus(Status.AVAILABLE);
        stack.setCluster(cluster);

        Collection<String> instanceIds = new LinkedList<>();
        InstanceMetaData im1 = createInstanceMetadataForTest(1L, "group1");
        InstanceMetaData im2 = createInstanceMetadataForTest(2L, "group1");
        InstanceMetaData im3 = createInstanceMetadataForTest(3L, "group1");
        instanceIds.add("i1");
        instanceIds.add("i2");
        instanceIds.add("i3");

        // This ends up skipping the actual validation that is run here.
        doReturn(im1).when(updateNodeCountValidator).validateInstanceForDownscale(im1.getInstanceId(), stack);
        doReturn(im2).when(updateNodeCountValidator).validateInstanceForDownscale(im2.getInstanceId(), stack);
        doReturn(im3).when(updateNodeCountValidator).validateInstanceForDownscale(im3.getInstanceId(), stack);

        doNothing().when(updateNodeCountValidator).validateServiceRoles(any(), anyMap());
        doNothing().when(updateNodeCountValidator).validateScalabilityOfInstanceGroup(any(), anyString(), anyInt());
        doNothing().when(updateNodeCountValidator).validateInstanceGroupForStopStart(any(), anyString(), anyInt());
        doNothing().when(updateNodeCountValidator).validateInstanceGroup(any(), anyString());
        doNothing().when(updateNodeCountValidator).validateScalingAdjustment(anyString(), anyInt(), any());

        ArgumentCaptor<Map<String, Set<Long>>> capturedInstances;
        Map<String, Set<Long>> captured;

        // Verify non stop-start invocation
        capturedInstances = ArgumentCaptor.forClass(Map.class);
        underTest.removeInstances(stack, instanceIds, false);
        verify(flowManager).triggerStackRemoveInstances(eq(stack.getId()), capturedInstances.capture(), eq(false));
        captured = capturedInstances.getValue();
        assertEquals(1, captured.size());
        assertEquals("group1", captured.keySet().iterator().next());
        assertEquals(3, captured.entrySet().iterator().next().getValue().size());

        // This ends up skipping the actual validation that is run here.
        doReturn(im1).when(updateNodeCountValidator).validateInstanceForStop(im1.getInstanceId(), stack);
        doReturn(im2).when(updateNodeCountValidator).validateInstanceForStop(im2.getInstanceId(), stack);
        doReturn(im3).when(updateNodeCountValidator).validateInstanceForStop(im3.getInstanceId(), stack);

        // Verify stop-start invocation
        reset(flowManager);
        capturedInstances = ArgumentCaptor.forClass(Map.class);
        underTest.stopInstances(stack, instanceIds, false);
        verify(flowManager).triggerStopStartStackDownscale(eq(stack.getId()), capturedInstances.capture(), eq(false));
        captured = capturedInstances.getValue();
        assertEquals(1, captured.size());
        assertEquals("group1", captured.keySet().iterator().next());
        assertEquals(3, captured.entrySet().iterator().next().getValue().size());

        // No requestIds sent - BadRequest stopstart
        assertThatThrownBy(() -> underTest.stopInstances(stack, null, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Stop request cannot process an empty instanceIds collection");

        // stopstart supports a single hostGroup only
        reset(flowManager);
        InstanceMetaData im4 = createInstanceMetadataForTest(4L, "group2");
        doReturn(im4).when(updateNodeCountValidator).validateInstanceForStop(im4.getInstanceId(), stack);
        instanceIds.add("i4");
        assertThatThrownBy(() -> underTest.stopInstances(stack, instanceIds, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Downscale via Instance Stop cannot process more than one host group");

        // regular scaling supports multiple hostgroups
        reset(flowManager);
        doReturn(im4).when(updateNodeCountValidator).validateInstanceForDownscale(im4.getInstanceId(), stack);
        underTest.removeInstances(stack, instanceIds, false);
        verify(flowManager).triggerStackRemoveInstances(eq(stack.getId()), capturedInstances.capture(), eq(false));
        captured = capturedInstances.getValue();
        assertEquals(2, captured.size());
        assertTrue(captured.containsKey("group1"));
        assertTrue(captured.containsKey("group2"));
        assertEquals(3, captured.get("group1").size());
        assertEquals(1, captured.get("group2").size());
    }

    @Test
    public void testRotateSaltPassword() {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn("crn");
        long workspaceId = 0L;
        Stack stack = new Stack();
        stack.setId(5L);
        when(stackService.getByNameOrCrnAndWorkspaceIdWithLists(nameOrCrn, workspaceId)).thenReturn(stack);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollableId");
        when(flowManager.triggerRotateSaltPassword(stack.getId())).thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.rotateSaltPassword(nameOrCrn, workspaceId);

        assertEquals(flowIdentifier, result);
        verify(stackService).getByNameOrCrnAndWorkspaceIdWithLists(nameOrCrn, workspaceId);
        verify(clusterBootstrapper).validateRotateSaltPassword(stack);
        verify(flowManager).triggerRotateSaltPassword(stack.getId());
    }

    private InstanceMetaData createInstanceMetadataForTest(Long privateId, String instanceGroupName) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("i" + privateId);
        instanceMetaData.setPrivateId(privateId);
        InstanceGroup instanceGroup1 = new InstanceGroup();
        instanceGroup1.setGroupName(instanceGroupName);
        instanceMetaData.setInstanceGroup(instanceGroup1);
        return instanceMetaData;
    }

}
