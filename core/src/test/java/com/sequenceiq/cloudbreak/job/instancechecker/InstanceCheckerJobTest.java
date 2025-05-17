package com.sequenceiq.cloudbreak.job.instancechecker;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.STOPPED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.instancechecker.InstanceCheckerService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;

@ExtendWith(MockitoExtension.class)
class InstanceCheckerJobTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private InstanceCheckerService instanceCheckerService;

    @Mock
    private InstanceCheckerJobService instanceCheckerJobService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private StackStatusService stackStatusService;

    @InjectMocks
    private InstanceCheckerJob underTest;

    @BeforeEach
    public void setUp() {
        underTest.setLocalId(String.valueOf(STACK_ID));
    }

    @Test
    void testExecuteWhenClusterRunning() {
        when(stackStatusService.findFirstByStackIdOrderByCreatedDesc(eq(STACK_ID))).thenReturn(Optional.of(new StackStatus(null, AVAILABLE)));
        underTest.executeJob(jobExecutionContext);
        verify(instanceCheckerJobService, never()).unschedule(eq(String.valueOf(STACK_ID)));
        verify(instanceCheckerService, times(1)).checkInstances(eq(STACK_ID));
    }

    @Test
    void testExecuteWhenClusterStopped() {
        when(stackStatusService.findFirstByStackIdOrderByCreatedDesc(eq(STACK_ID))).thenReturn(Optional.of(new StackStatus(null, STOPPED)));
        underTest.executeJob(jobExecutionContext);
        verify(instanceCheckerJobService, times(1)).unschedule(eq(String.valueOf(STACK_ID)));
        verify(instanceCheckerService, never()).checkInstances(eq(STACK_ID));
    }

    @Test
    void testExecuteWhenClusterDeletedOnProviderSide() {
        when(stackStatusService.findFirstByStackIdOrderByCreatedDesc(eq(STACK_ID)))
                .thenReturn(Optional.of(new StackStatus(null, DELETED_ON_PROVIDER_SIDE)));
        underTest.executeJob(jobExecutionContext);
        verify(instanceCheckerJobService, never()).unschedule(eq(String.valueOf(STACK_ID)));
        verify(instanceCheckerService, never()).checkInstances(eq(STACK_ID));
    }
}