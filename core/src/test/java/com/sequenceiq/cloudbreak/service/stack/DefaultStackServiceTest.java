package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.domain.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.domain.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.STOPPED;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.UPDATE_IN_PROGRESS;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.core.flow.FlowManager;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;

@RunWith(MockitoJUnitRunner.class)
public class DefaultStackServiceTest {

    @InjectMocks
    private DefaultStackService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private RetryingStackUpdater stackUpdater;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private FlowManager flowManager;

    @Mock
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    @Mock
    private BlueprintValidator blueprintValidator;

    @Before
    public void before() {
        doNothing().when(flowManager).triggerStackStop(anyObject());
        doNothing().when(flowManager).triggerStackStopRequested(anyObject());
        doNothing().when(flowManager).triggerStackStart(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterStoppedThenStackStopTriggered() {
        Stack stack = stack(AVAILABLE, STOPPED);
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED);
        verify(flowManager, times(1)).triggerStackStop(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterInStopInProgressThenTriggeredStackStopRequested() {
        Stack stack = stack(AVAILABLE, STOP_IN_PROGRESS);
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED);
        verify(flowManager, times(1)).triggerStackStopRequested(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterInStoppedAndStackAvailableThenTriggerStackStop() {
        Stack stack = stack(AVAILABLE, STOPPED);
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED);
        verify(flowManager, times(1)).triggerStackStop(anyObject());
    }

    @Test
    public void updateStatusTestStopWhenClusterInStoppedAndStackStopFailedThenTriggerStackStop() {
        Stack stack = stack(STOP_FAILED, STOPPED);
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED);
        verify(flowManager, times(1)).triggerStackStop(anyObject());
    }

    @Test(expected = BadRequestException.class)
    public void updateStatusTestStopWhenClusterInStoppedAndStackUpdateInProgressThenBadRequestExceptionDropping() {
        Stack stack = stack(UPDATE_IN_PROGRESS, STOPPED);
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED);
        verify(flowManager, times(1)).triggerStackStop(anyObject());
    }


    @Test(expected = BadRequestException.class)
    public void updateStatusTestStopWhenClusterAndStackAvailableThenBadRequestExceptionDropping() {
        Stack stack = stack(AVAILABLE, AVAILABLE);
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STOPPED);
    }

    @Test
    public void updateStatusTestStartWhenStackStoppedThenStackStartTriggered() {
        Stack stack = stack(STOPPED, STOPPED);
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STARTED);
        verify(flowManager, times(1)).triggerStackStart(anyObject());
    }

    @Test(expected = BadRequestException.class)
    public void updateStatusTestStartWhenClusterInStoppedAndStackStopFailedThenBadRequestExceptionDropping() {
        Stack stack = stack(UPDATE_IN_PROGRESS, STOPPED);
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STARTED);
    }

    @Test
    public void updateStatusTestStartWhenClusterInStoppedAndStackStartFailedThenTriggerStackStart() {
        Stack stack = stack(START_FAILED, STOPPED);
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STARTED);
        verify(flowManager, times(1)).triggerStackStart(anyObject());
    }

    @Test(expected = BadRequestException.class)
    public void updateStatusTestStartWhenClusterAndStackUpdateInProgressThenBadRequestExceptionDropping() {
        Stack stack = stack(UPDATE_IN_PROGRESS, UPDATE_IN_PROGRESS);
        given(stackRepository.findOne(anyLong())).willReturn(stack);
        given(clusterRepository.findOneWithLists(anyLong())).willReturn(stack.getCluster());
        given(stackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        underTest.updateStatus(1L, StatusRequest.STARTED);
    }

    private Stack stack(Status stackStatus, Status clusterStatus) {
        GcpCredential gcpCredential = new GcpCredential();
        Stack stack = new Stack();
        stack.setStatus(stackStatus);
        stack.setCredential(gcpCredential);
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setStatus(clusterStatus);
        cluster.setId(1L);
        stack.setCluster(cluster);
        return stack;
    }

}