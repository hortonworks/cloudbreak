package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_START_IGNORED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.workspace.model.User;

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

    @Test
    public void testStartWhenStackAvailable() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        stack.setId(1L);

        underTest.start(stack, null, false, new User());

        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), STACK_START_IGNORED);
    }

    @Test
    public void testStartWhenStackStopped() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.STOPPED));
        stack.setId(1L);

        underTest.start(stack, null, false, new User());

        verify(flowManager, times(1)).triggerStackStart(stack.getId());
        verify(stackUpdater, times(1)).updateStackStatus(stack.getId(),  DetailedStackStatus.START_REQUESTED);
    }

    @Test
    public void testStartWhenStackStartFailed() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.START_FAILED));
        stack.setId(1L);

        underTest.start(stack, null, false, new User());

        verify(flowManager, times(1)).triggerStackStart(stack.getId());
        verify(stackUpdater, times(1)).updateStackStatus(stack.getId(),  DetailedStackStatus.START_REQUESTED);
    }

    @Test
    public void testStartWhenStackStopFailed() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.STOP_FAILED));
        stack.setId(1L);

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("");
        underTest.start(stack, null, false, new User());

        verify(stackUpdater, times(1)).updateStackStatus(stack.getId(),  DetailedStackStatus.START_REQUESTED);
    }

    @Test
    public void testStartWhenClusterStopFailed() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        Cluster cluster = new Cluster();
        cluster.setStatus(Status.STOPPED);
        stack.setCluster(cluster);
        underTest.start(stack, cluster, false, new User());
        verify(flowManager, times(1)).triggerStackStart(stack.getId());
        verify(stackUpdater, times(1)).updateStackStatus(stack.getId(),  DetailedStackStatus.START_REQUESTED);
    }

    @Test
    public void shouldNotTriggerStopWhenStackRunsOnSpotInstances() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        stack.setInstanceGroups(Set.of(createInstanceGroup(100)));

        when(stackService.getByIdWithLists(stack.getId())).thenReturn(stack);

        Assertions.assertThatThrownBy(() -> underTest.updateStatus(stack.getId(), StatusRequest.STOPPED, true, new User()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(String.format("Cannot update the status of stack '%s' to STOPPED, because it runs on spot instances", stack.getName()));
        verify(stackUpdater, never()).updateStackStatus(any(), any());
    }

    @Test
    public void shouldTriggerStopWhenStackRunsOnOnDemandInstances() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        stack.setInstanceGroups(Set.of(createInstanceGroup(0), createInstanceGroup(null)));

        when(stackService.getByIdWithLists(stack.getId())).thenReturn(stack);

        underTest.updateStatus(stack.getId(), StatusRequest.STOPPED, true, new User());

        verify(stackUpdater).updateStackStatus(stack.getId(), STOP_REQUESTED);
    }

    private InstanceGroup createInstanceGroup(Integer spotPercentage) {
        Template template = new Template();
        template.setAttributes(new Json(spotPercentage != null ? Map.of("spotPercentage", spotPercentage) : Map.of()));
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }
}