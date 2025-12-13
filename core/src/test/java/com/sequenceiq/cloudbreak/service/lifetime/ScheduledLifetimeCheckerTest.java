package com.sequenceiq.cloudbreak.service.lifetime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class ScheduledLifetimeCheckerTest {

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
    void testValidateWhenOnlyOneStackIsAliveAndNotExceeded() {
        StackTtlViewImpl stack = new StackTtlViewImpl();
        stack.setId(STACK_ID);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId());
    }

    @Test
    void testValidateWhenOnlyOneStackIsAliveAndTTLNotSet() {
        StackTtlViewImpl stack = new StackTtlViewImpl();
        stack.setId(STACK_ID);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId());
    }

    @Test
    void testValidateWhenOnlyOneStackIsAliveAndTTLIsLetter() {
        StackTtlViewImpl stack = new StackTtlViewImpl();
        stack.setId(STACK_ID);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId());
    }

    @Test
    void testValidateWhenOnlyOneStackIsAliveAndTTLIsSetAndDeleteInProgressAndClusterNull() {
        StackTtlViewImpl stack = new StackTtlViewImpl();
        stack.setId(STACK_ID);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.DELETE_IN_PROGRESS);
        stack.setStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId());
    }

    @Test
    void testValidateWhenOnlyOneStackIsAliveAndTTLIsSetAndDeleteInProgressAndCreationFinished() {
        StackTtlViewImpl stack = new StackTtlViewImpl();
        stack.setId(STACK_ID);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.DELETE_IN_PROGRESS);
        stack.setStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId());
    }

    @Test
    void testValidateWhenOnlyOneStackIsAliveAndTTLIsSetAndDeleteInProgressAndCreationNotFinished() {
        StackTtlViewImpl stack = new StackTtlViewImpl();
        stack.setId(STACK_ID);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.DELETE_IN_PROGRESS);
        stack.setStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId());
    }

    @Test
    void testValidateWhenOnlyOneStackIsAliveAndTTLIsSetAndAvailableAndClusterNull() {
        StackTtlViewImpl stack = new StackTtlViewImpl();
        stack.setId(STACK_ID);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId());
    }

    @Test
    void testValidateWhenOnlyOneStackIsAliveAndTTLIsSetAndStoppedAndCreationNotFinished() {
        StackTtlViewImpl stack = new StackTtlViewImpl();
        stack.setId(STACK_ID);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.STOPPED);
        stack.setStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId());
    }

    @Test
    void testValidateWhenClusterExceededByRunningTimeMoreThanTTL() {
        StackTtlViewImpl stack = new StackTtlViewImpl();
        stack.setId(STACK_ID);
        long startTimeMillis = 0;
        stack.setCreationFinished(startTimeMillis);
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        workspace.setName("workspace");
        stack.setWorkspace(workspace);
        int ttlMillis = 1;
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));
        when(stackService.getTtlValueForStack(anyLong())).thenReturn(Optional.of(Duration.ofMillis(ttlMillis)));
        when(clock.getCurrentTimeMillis()).thenReturn(startTimeMillis + ttlMillis + 1);

        underTest.validate();

        verify(flowManager, times(1)).triggerTermination(stack.getId());
    }

    @Test
    void testValidateWhenClusterNotExceededByTTLMoreThanRunningTime() {
        StackTtlViewImpl stack = new StackTtlViewImpl();
        stack.setId(STACK_ID);
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        workspace.setName("workspace");
        stack.setWorkspace(workspace);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStatus(stackStatus);

        when(stackService.getAllAlive()).thenReturn(Collections.singletonList(stack));

        underTest.validate();

        verify(flowManager, times(0)).triggerTermination(stack.getId());
    }

    private static class StackTtlViewImpl implements StackTtlView {

        private Long id;

        private String name;

        private String crn;

        private Workspace workspace;

        private StackStatus status;

        private Long creationFinished;

        @Override
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getCrn() {
            return crn;
        }

        public void setCrn(String crn) {
            this.crn = crn;
        }

        @Override
        public Workspace getWorkspace() {
            return workspace;
        }

        public void setWorkspace(Workspace workspace) {
            this.workspace = workspace;
        }

        @Override
        public StackStatus getStatus() {
            return status;
        }

        public void setStatus(StackStatus status) {
            this.status = status;
        }

        @Override
        public Long getCreationFinished() {
            return creationFinished;
        }

        public void setCreationFinished(Long creationFinished) {
            this.creationFinished = creationFinished;
        }
    }
}
