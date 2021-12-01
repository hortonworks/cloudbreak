package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_START_IGNORED;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.datalake.DataLakeStatusCheckerService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.spot.SpotInstanceUsageCondition;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;

@RunWith(MockitoJUnitRunner.class)
public class StackOperationServiceTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

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
    private UpdateNodeCountValidator updateNodeCountValidator;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Test
    public void testStartWhenStackAvailable() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        stack.setId(1L);

        underTest.start(stack, null, false);

        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), STACK_START_IGNORED);
    }

    @Test
    public void testStartWhenStackStopped() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, STOPPED));
        stack.setId(1L);

        underTest.start(stack, null, false);

        verify(flowManager, times(1)).triggerStackStart(stack.getId());
        verify(stackUpdater, times(1)).updateStackStatus(stack.getId(), DetailedStackStatus.START_REQUESTED);
    }

    @Test
    public void testStartWhenStackStartFailed() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.START_FAILED));
        stack.setId(1L);

        underTest.start(stack, null, false);

        verify(flowManager, times(1)).triggerStackStart(stack.getId());
        verify(stackUpdater, times(1)).updateStackStatus(stack.getId(), DetailedStackStatus.START_REQUESTED);
    }

    @Test
    public void testStartWhenStackStopFailed() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.STOP_FAILED));
        stack.setId(1L);

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("");
        underTest.start(stack, null, false);

        verify(stackUpdater, times(1)).updateStackStatus(stack.getId(), DetailedStackStatus.START_REQUESTED);
    }

    @Test
    public void testStartWhenClusterStopFailed() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, Status.STOPPED, "", STOPPED));
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        underTest.start(stack, cluster, false);
        verify(flowManager, times(1)).triggerStackStart(stack.getId());
        verify(stackUpdater, times(1)).updateStackStatus(stack.getId(), DetailedStackStatus.START_REQUESTED);
    }

    @Test
    public void shouldNotTriggerStopWhenStackRunsOnSpotInstances() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));

        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.NONE);
        when(spotInstanceUsageCondition.isStackRunsOnSpotInstances(stack)).thenReturn(true);
        when(stackService.getByIdWithLists(stack.getId())).thenReturn(stack);

        Assertions.assertThatThrownBy(() -> underTest.updateStatus(stack.getId(), StatusRequest.STOPPED, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(String.format("Cannot update the status of stack '%s' to STOPPED, because it runs on spot instances", stack.getName()));
        verify(stackUpdater, never()).updateStackStatus(any(), any());
    }

    @Test
    public void shouldTriggerStopWhenStackRunsOnOnDemandInstances() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));

        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.NONE);
        when(spotInstanceUsageCondition.isStackRunsOnSpotInstances(stack)).thenReturn(false);
        when(stackService.getByIdWithLists(stack.getId())).thenReturn(stack);

        underTest.updateStatus(stack.getId(), StatusRequest.STOPPED, true);

        verify(stackUpdater).updateStackStatus(stack.getId(), STOP_REQUESTED);
    }

    @Test
    public void testStartWhenCheckCallEnvironmentCheck() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, STOPPED));
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        underTest.start(stack, cluster, false);
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

        underTest.updateNodeCount(stack, upscaleAdjustment, true);
        verify(stackUpdater).updateStackStatus(stack.getId(), DetailedStackStatus.UPSCALE_REQUESTED,
                "Requested node count for upscaling: " + upscaleAdjustment.getScalingAdjustment());
        verify(flowManager).triggerStackUpscale(stack.getId(), upscaleAdjustment, true);

        InstanceGroupAdjustmentV4Request downscaleAdjustment = new InstanceGroupAdjustmentV4Request();
        downscaleAdjustment.setScalingAdjustment(-5);
        underTest.updateNodeCount(stack, downscaleAdjustment, true);
        verify(stackUpdater).updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_REQUESTED,
                "Requested node count for downscaling: " + 5);
        verify(flowManager).triggerStackDownscale(stack.getId(), downscaleAdjustment);
    }

    @Test
    public void testUpdateNodeCountStartInstances() throws TransactionService.TransactionExecutionException {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        Cluster cluster = new Cluster();
        cluster.setStatus(Status.AVAILABLE);
        stack.setCluster(cluster);

        when(stackService.getByIdWithLists(stack.getId())).thenReturn(stack);

        InstanceGroupAdjustmentV4Request upscaleAdjustment = new InstanceGroupAdjustmentV4Request();
        upscaleAdjustment.setScalingAdjustment(5);

        when(transactionService.required(any(Supplier.class))).thenAnswer(ans -> ((Supplier) ans.getArgument(0)).get());

        // Regular
        underTest.updateNodeCountStartInstances(stack, upscaleAdjustment, true, ScalingStrategy.STOPSTART);
        String expectedStatusReason = "Requested node count for upscaling (stopstart): " + upscaleAdjustment.getScalingAdjustment();
        verify(stackUpdater).updateStackStatus(stack.getId(), DetailedStackStatus.UPSCALE_REQUESTED, expectedStatusReason);
        verify(flowManager).triggerStopStartStackUpscale(stack.getId(), upscaleAdjustment, true);

        // Somehow invoked with a negative value
        upscaleAdjustment.setScalingAdjustment(-1);
        assertThrows(BadRequestException.class,
                () -> underTest.updateNodeCountStartInstances(stack, upscaleAdjustment, true, ScalingStrategy.STOPSTART));

        // TODO CB-14929: Post CB-15162 Test what happens when the instanceCountAdjustment is 0
        // TODO CB-14929: Post CB-15154 Potentially additional validation tests
    }

    @Test
    public void testRemoveInstances() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        Cluster cluster = new Cluster();
        cluster.setStatus(Status.AVAILABLE);
        stack.setCluster(cluster);

        User user = new User();

        Collection<String> instanceIds = new LinkedList<>();
        InstanceMetaData im1 = createInstanceMetadataForTest(1L, "group1");
        InstanceMetaData im2 = createInstanceMetadataForTest(2L, "group1");
        InstanceMetaData im3 = createInstanceMetadataForTest(3L, "group1");
        instanceIds.add("i1");
        instanceIds.add("i2");
        instanceIds.add("i3");

        // This ends up skipping the actual validation that is run here.
        when(updateNodeCountValidator.validateInstanceForDownscale(im1.getInstanceId(), stack)).thenReturn(im1);
        when(updateNodeCountValidator.validateInstanceForDownscale(im2.getInstanceId(), stack)).thenReturn(im2);
        when(updateNodeCountValidator.validateInstanceForDownscale(im3.getInstanceId(), stack)).thenReturn(im3);

        ArgumentCaptor<Map<String, Set<Long>>> capturedInstances;
        Map<String, Set<Long>> captured;

        // Verify non stop-start invocation
        capturedInstances = ArgumentCaptor.forClass(Map.class);
        underTest.removeInstances(stack, 0L, instanceIds, false, user, null);
        verify(flowManager).triggerStackRemoveInstances(eq(stack.getId()), capturedInstances.capture(), eq(false));
        captured = capturedInstances.getValue();
        assertEquals(1, captured.size());
        assertEquals("group1", captured.keySet().iterator().next());
        assertEquals(3, captured.entrySet().iterator().next().getValue().size());

        // Verify stop-start invocation
        reset(flowManager);
        capturedInstances = ArgumentCaptor.forClass(Map.class);
        underTest.removeInstances(stack, 0L, instanceIds, false, user, ScalingStrategy.STOPSTART);
        verify(flowManager).triggerStopStartStackDownscale(eq(stack.getId()), capturedInstances.capture(), eq(false));
        captured = capturedInstances.getValue();
        assertEquals(1, captured.size());
        assertEquals("group1", captured.keySet().iterator().next());
        assertEquals(3, captured.entrySet().iterator().next().getValue().size());


        // No requestIds sent - BadRequest (regular and stopstart)
        assertThrows(BadRequestException.class,
                () -> underTest.removeInstances(stack, 0L, null, false, user, null));
        assertThrows(BadRequestException.class,
                () -> underTest.removeInstances(stack, 0L, null, false, user, ScalingStrategy.STOPSTART));

        // stopstart supports a single hostGroup only
        reset(flowManager);
        InstanceMetaData im4 = createInstanceMetadataForTest(4L, "group2");
        when(updateNodeCountValidator.validateInstanceForDownscale(im4.getInstanceId(), stack)).thenReturn(im4);
        instanceIds.add("i4");
        assertThrows(BadRequestException.class,
                () -> underTest.removeInstances(stack, 0L, instanceIds, false, user, ScalingStrategy.STOPSTART));

        // regular scaling supports multiple hostgroups
        reset(flowManager);
        underTest.removeInstances(stack, 0L, instanceIds, false, user, null);
        verify(flowManager).triggerStackRemoveInstances(eq(stack.getId()), capturedInstances.capture(), eq(false));
        captured = capturedInstances.getValue();
        assertEquals(2, captured.size());
        assertTrue(captured.containsKey("group1"));
        assertTrue(captured.containsKey("group2"));
        assertEquals(3, captured.get("group1").size());
        assertEquals(1, captured.get("group2").size());
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