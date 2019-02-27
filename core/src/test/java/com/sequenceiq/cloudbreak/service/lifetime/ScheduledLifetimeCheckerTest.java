package com.sequenceiq.cloudbreak.service.lifetime;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class ScheduledLifetimeCheckerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private ScheduledLifetimeChecker underTest;

    @Mock
    private StackService stackService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private Clock clock;

    @Test
    public void testValidateWhenOnlyOneStackIsAliveAndNotExceeded() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId(), false, false);
    }

    @Test
    public void testValidateWhenOnlyOneStackIsAliveAndTTLNotSet() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setParameters(Collections.emptyMap());

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId(), false, false);
    }

    @Test
    public void testValidateWhenOnlyOneStackIsAliveAndTTLIsLetter() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setParameters(Collections.singletonMap(PlatformParametersConsts.TTL_MILLIS, "ttl"));

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId(), false, false);
    }

    @Test
    public void testValidateWhenOnlyOneStackIsAliveAndTTLIsSetAndDeleteInProgressAndClusterNull() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCluster(null);
        stack.setParameters(Collections.singletonMap(PlatformParametersConsts.TTL_MILLIS, "1"));
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.DELETE_IN_PROGRESS);
        stack.setStackStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId(), false, false);
    }

    @Test
    public void testValidateWhenOnlyOneStackIsAliveAndTTLIsSetAndDeleteInProgressAndCreationFinished() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        cluster.setCreationFinished(1L);
        stack.setCluster(cluster);
        stack.setParameters(Collections.singletonMap(PlatformParametersConsts.TTL_MILLIS, "1"));
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.DELETE_IN_PROGRESS);
        stack.setStackStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId(), false, false);
    }

    @Test
    public void testValidateWhenOnlyOneStackIsAliveAndTTLIsSetAndDeleteInProgressAndCreationNotFinished() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        cluster.setCreationFinished(null);
        stack.setCluster(cluster);
        stack.setParameters(Collections.singletonMap(PlatformParametersConsts.TTL_MILLIS, "1"));
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.DELETE_IN_PROGRESS);
        stack.setStackStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId(), false, false);
    }

    @Test
    public void testValidateWhenOnlyOneStackIsAliveAndTTLIsSetAndAvailableAndClusterNull() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCluster(null);
        stack.setParameters(Collections.singletonMap(PlatformParametersConsts.TTL_MILLIS, "1"));
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId(), false, false);
    }

    @Test
    public void testValidateWhenOnlyOneStackIsAliveAndTTLIsSetAndStoppedAndCreationNotFinished() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        cluster.setCreationFinished(null);
        stack.setCluster(cluster);
        stack.setParameters(Collections.singletonMap(PlatformParametersConsts.TTL_MILLIS, "1"));
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.STOPPED);
        stack.setStackStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId(), false, false);
    }

    @Test
    public void testValidateWhenClusterExceededByRunningTimeMoreThanTTL() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        workspace.setName("workspace");
        stack.setWorkspace(workspace);
        Cluster cluster = new Cluster();
        long startTimeMillis = 0;
        int ttlMillis = 1;
        cluster.setCreationFinished(startTimeMillis);
        stack.setParameters(Collections.singletonMap(PlatformParametersConsts.TTL_MILLIS, String.valueOf(ttlMillis)));
        stack.setCluster(cluster);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));
        when(clock.getCurrentTimeMillis()).thenReturn(startTimeMillis + ttlMillis + 1);

        underTest.validate();

        verify(flowManager, times(1)).triggerTermination(stack.getId(), false, false);
    }

    @Test
    public void testValidateWhenClusterNotExceededByTTLMoreThanRunningTime() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        workspace.setName("workspace");
        stack.setWorkspace(workspace);
        Cluster cluster = new Cluster();
        cluster.setCreationFinished(System.currentTimeMillis() - 1L);
        stack.setCluster(cluster);
        stack.setParameters(Collections.singletonMap(PlatformParametersConsts.TTL_MILLIS, "10000"));
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId(), false, false);
    }
}
