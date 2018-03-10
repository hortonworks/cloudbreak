package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackStatus;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultStackHostServiceTypeTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private StackService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private BlueprintValidator blueprintValidator;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private AmbariClusterService ambariClusterService;

    @Before
    public void before() {
        doNothing().when(flowManager).triggerStackStop(anyObject());
        doNothing().when(flowManager).triggerStackStart(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterStoppedThenStackStopTriggered() {
        Stack stack = stack(AVAILABLE, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
        verify(flowManager, times(1)).triggerStackStop(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterInStopInProgressThenTriggeredStackStopRequested() {
        Stack stack = stack(AVAILABLE, STOP_IN_PROGRESS);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
        verify(eventService, times(1)).fireCloudbreakEvent(eq(1L), eq(STOP_REQUESTED.name()), anyString());
    }

    @Test
    public void updateStatusTestStopWhenClusterInStoppedAndStackAvailableThenTriggerStackStop() {
        Stack stack = stack(AVAILABLE, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
        verify(flowManager, times(1)).triggerStackStop(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterInStoppedAndStackStopFailedThenTriggerStackStop() {
        Stack stack = stack(STOP_FAILED, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
        verify(flowManager, times(1)).triggerStackStop(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterInStoppedAndStackUpdateInProgressThenBadRequestExceptionDropping() {
        Stack stack = stack(UPDATE_IN_PROGRESS, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cannot update the status of stack 'simplestack' to STOPPED, because it isn't in AVAILABLE state.");
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
        verify(flowManager, times(1)).triggerStackStop(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterAndStackAvailableThenBadRequestExceptionDropping() {
        Stack stack = stack(AVAILABLE, AVAILABLE);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cannot update the status of stack 'simplestack' to STOPPED, because the cluster is not in STOPPED state.");
        underTest.updateStatus(1L, StatusRequest.STOPPED, false);
    }

    @Test
    public void updateStatusTestStartWhenStackStoppedThenStackStartTriggered() {
        Stack stack = stack(STOPPED, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STARTED, true);
        verify(flowManager, times(1)).triggerStackStart(anyObject());
    }

    @Test
    public void updateStatusTestStartWhenClusterInStoppedAndStackStopFailedThenBadRequestExceptionDropping() {
        Stack stack = stack(UPDATE_IN_PROGRESS, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cannot update the status of stack 'simplestack' to STARTED, because it isn't in STOPPED state.");
        underTest.updateStatus(1L, StatusRequest.STARTED, true);
    }

    @Test
    public void updateStatusTestStartWhenClusterInStoppedAndStackStartFailedThenTriggerStackStart() {
        Stack stack = stack(START_FAILED, STOPPED);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STARTED, true);
        verify(flowManager, times(1)).triggerStackStart(anyObject());
    }

    @Test
    public void updateStatusTestStartWhenClusterAndStackUpdateInProgressThenBadRequestExceptionDropping() {
        Stack stack = stack(UPDATE_IN_PROGRESS, UPDATE_IN_PROGRESS);
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cannot update the status of stack 'simplestack' to STARTED, because it isn't in STOPPED state.");
        underTest.updateStatus(1L, StatusRequest.STARTED, true);
    }

    @Test
    public void updateStatusTestStopWhenClusterAndStackAvailableAndEphemeralThenBadRequestExceptionDropping() {
        Stack stack = TestUtil.setEphemeral(TestUtil.stack(AVAILABLE, TestUtil.awsCredential()));
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cannot stop a stack 'simplestack'. Reason: Instances with ephemeral volumes cannot be stopped.");
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
    }

    @Test
    public void updateStatusTestStopWhenClusterAndStackAvailableAndSpotInstancesThenBadRequestExceptionDropping() {
        Stack stack = TestUtil.setSpotInstances(TestUtil.stack(AVAILABLE, TestUtil.awsCredential()));
        given(stackRepository.findOneWithLists(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(DetailedStackStatus.class))).willReturn(stack);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cannot stop a stack 'simplestack'. Reason: Spot instances cannot be stopped.");
        underTest.updateStatus(1L, StatusRequest.STOPPED, true);
    }

    private Stack stack(Status stackStatus, Status clusterStatus) {
        Credential gcpCredential = new Credential();
        Stack stack = new Stack();
        stack.setName("simplestack");
        stack.setStackStatus(new StackStatus(stack, stackStatus, "", DetailedStackStatus.UNKNOWN));
        stack.setCredential(gcpCredential);
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setStatus(clusterStatus);
        cluster.setId(1L);
        stack.setCluster(cluster);
        return stack;
    }

}